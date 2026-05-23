# Startup Fetch & Code Quality Design

**Date**: 2026-05-23
**Scope**: Two concerns — (1) fetch exchange rates eagerly on app start, and (2) a rigorous audit of the codebase covering bugs, dead code, error handling, theme cleanliness, efficiency, and extensibility.

---

## 1. Startup & Loading

### 1a. Eager repository initialization

**Problem**: `RateRepositoryImpl` is a Hilt `@Singleton` but is created lazily — only when first injected into `CalculatorViewModel`. The ViewModel's `init` collects DataStore preferences (disk I/O) before calling `observeRateForCurrency`. On a cold start with no cached data, this means the first network fetch doesn't begin until after the DataStore emits, adding 200–600 ms of avoidable delay.

**Fix**:
- Add an `@ApplicationScope` coroutine scope qualifier and a corresponding DI provider (a `CoroutineScope(SupervisorJob() + Dispatchers.Default)` in `SingletonComponent`).
- Inject this scope into `RateRepositoryImpl`. In its `init` block, immediately launch a `fetchAllTickers()` call on `dispatchers.io`. This is the same fetch the poll loop already does — it just fires once at construction time.
- Inject `RateRepository` (the interface) into `ExchangeApp` to force Hilt to create the singleton at application start, before any Activity is created.

```kotlin
// ExchangeApp.kt
@HiltAndroidApp
class ExchangeApp : Application() {
    @Inject lateinit var rateRepository: RateRepository  // forces eager singleton creation
}
```

```kotlin
// RateRepositoryImpl.kt
@Singleton
class RateRepositoryImpl @Inject constructor(
    private val api: DolarApi,
    private val dao: RateTickerDao,
    private val dispatchers: DispatcherProvider,
    @ApplicationScope private val appScope: CoroutineScope,
) : RateRepository {
    init {
        appScope.launch(dispatchers.io) { fetchAllTickers() }
    }
    // fetchAllTickers() is the shared suspend fun used by both init and the poll loop
}
```

The poll loop inside `observeRateTicker`'s `channelFlow` continues unchanged. The `init` fetch is a one-shot warm-up; the poll loop handles ongoing refresh.

### 1b. Loading flash on currency switch

**Problem**: `observeRateTicker` always emits `RateResource.Loading` as its first value before querying Room. Since the "fetch all, cache all" strategy already has the cached row for every currency, the user sees a brief `Loading` flash (typically < 10 ms but visible as a state transition) when switching currencies.

**Fix**: Query the initial Room value before deciding what to emit first:

```kotlin
val initialEntity = dao.observeTicker(book).first()
send(
    if (initialEntity != null) RateResource.Available(initialEntity.toDomain())
    else RateResource.Loading
)
```

Then continue with the existing `combine(dao.observeTicker(book), pollFailedFlow)` for ongoing updates. The `first()` call suspends only until Room returns the current row (< 5 ms). If no cache exists, `Loading` is still sent correctly.

---

## 2. Bugs

### 2a. DTO validation missing (crash risk)

**Problem**: `RateTickerDto.toEntity` stores `ask` and `bid` as raw strings with no validation. `RateTickerEntity.toDomain()` then calls `BigDecimal(ask)`, which throws `NumberFormatException` on any malformed string. This exception propagates through the Room Flow collect inside `observeRateTicker`'s `channelFlow`. The coroutine is a child of `viewModelScope`'s `SupervisorJob`, so the app doesn't crash — but the rate update coroutine dies silently and rate data stops updating for the remainder of the session.

**Fix**: Validate in `toEntity`. Make it return `RateTickerEntity?` and return `null` for any row where `ask` or `bid` is not a parseable positive BigDecimal. Callers skip `null` returns:

```kotlin
fun RateTickerDto.toEntity(fetchedAtEpochMs: Long): RateTickerEntity? {
    val askDecimal = ask.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    val bidDecimal = bid.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    // Re-serialize as canonical string to guarantee toDomain() never throws
    return RateTickerEntity(book, askDecimal.toPlainString(), bidDecimal.toPlainString(), fetchedAtEpochMs)
}
```

In `RateRepositoryImpl`:
```kotlin
dtos.forEach { dto -> dto.toEntity(now)?.let { dao.upsertTicker(it) } }
```

### 2b. Dead code in CalculatorViewModel

**Problem**: `onTopFieldFocused`, `onBottomFieldFocused`, and `focusField` exist in `CalculatorViewModel` but are never called from the UI. The bottom `CurrencyAmountRow` in `CalculatorScreen` hardcodes `isActive = false` — the bottom field is intentionally display-only, and focus can never switch there. These methods are dead weight that future developers might call under the mistaken belief they're necessary.

**Fix**: Remove all three methods from `CalculatorViewModel`.

---

## 3. Error Handling

### 3a. Silent error swallowing in the poll loop

**Problem**: `RateRepositoryImpl`'s poll loop catches all exceptions and continues silently. Repeated 403s, malformed JSON, or DNS failures produce zero signal.

**Fix**: Add `Log.e(TAG, "Rate fetch failed", e)` (or Timber if added) in the catch block, where `TAG` is a `companion object` constant (`"RateRepo"`). The retry logic and failure semantics remain unchanged — logging is purely additive.

### 3b. Network timeout too long

**Problem**: The `OkHttpClient` sets `connectTimeout` and `readTimeout` to 30 seconds. For a rate ticker that polls every 60 seconds, a single slow request can consume half the poll interval, stalling the UI in `Loading` or `Stale` state. 30s is far longer than needed for a small JSON payload.

**Fix**: Reduce both timeouts to 10 seconds in `NetworkModule`. This still gives ample time for high-latency mobile connections while keeping the polling cycle healthy.

---

## 4. Theme / Code Cleanliness

### 4a. Hardcoded colors outside Color.kt

Seven hardcoded hex colors are scattered across composable files, invisible to the design system:

| File | Hardcoded value | New constant |
|---|---|---|
| `CurrencyAmountRow` | `0xFF2F7CFF` | `CursorBlue` |
| `NumericKeypad` | `0xFFD1D3D8` | `KeyboardBackground` |
| `NumericKeypad` | `0xFFFCFCFE` | `KeyBackground` |
| `SwapButton` | `0xFFE7FAF1` | `SwapButtonGlow` |
| `CurrencyBottomSheet` | `0xFFD9D9D9` | `HandleGray` |
| `CurrencyBottomSheet` | `0xFFF4F4F4` | `PickerFlagBackground` |
| `CurrencyBottomSheet` | `0xFFD1D1D6` | `SelectionBorderGray` |

**Fix**: Declare all seven in `Color.kt` and replace the inline literals with the named constants.

### 4b. Magic default fiat code

The string `"MXN"` is hardcoded as a default in three places: `CalculatorUiState`, `CurrencyPickerState`, and `SettingsDataStore`. If the default currency ever changes, all three need updating.

**Fix**: Define a top-level `val DEFAULT_FIAT_CODE = FallbackCurrenciesProvider.currencies.first().code` (or a `companion object` constant in `FallbackCurrenciesProvider`) and reference it from all three sites.

### 4c. CurrencyAmountRow card duplication

`CurrencyAmountRow` has two nearly identical `Card` composable branches differing only in whether `onClick` is provided. Material3's `Card` accepts `onClick` and `enabled`; a non-selectable card can use `enabled = false` with a no-op lambda. Collapse the two branches into one.

---

## 5. Efficiency

### 5a. Redundant state updates from StaleRecheckTicker

`observeRateForCurrency` combines the rate resource flow with `staleRecheckTicker.ticks()` (every 10 s). Every tick triggers `_uiState.update { ... }` even when staleness status hasn't actually changed — the rate is either fresh or stale, not flipping on every tick. This causes recompositions every 10 seconds for no visual change.

**Fix**: After mapping the combined emission to a `RateDisplayState`, add `.distinctUntilChanged()` on a key that captures `isFresh`:

`RateDisplayState` is a sealed interface whose variants are `data object` and `data class`, so structural equality works out of the box. Add `.distinctUntilChanged()` (no custom key needed) on the combined flow before the `_uiState.update` call in `observeRateForCurrency`.

---

## 6. Extensibility

### 6a. Currency definition is split across three locations

Adding a new currency currently requires three edits: `FallbackCurrenciesProvider` (the list), `currencyFlagRes` (the `when` block in `CurrencyFlag.kt`), and the PNG drawable. There's no compile-time enforcement that all three stay in sync.

**Fix**: Define a `SupportedCurrency` enum that pairs `code` with `@DrawableRes flagRes`:

```kotlin
enum class SupportedCurrency(val code: String, @DrawableRes val flagRes: Int) {
    MXN("MXN", R.drawable.flag_mxn),
    ARS("ARS", R.drawable.flag_ars),
    BRL("BRL", R.drawable.flag_brl),
    COP("COP", R.drawable.flag_cop),
}
```

Then:
- `FallbackCurrenciesProvider.currencies` derives from `SupportedCurrency.entries.map { Currency(it.code, isBase = false) }`
- `currencyFlagRes` derives from `SupportedCurrency.entries.find { it.code == code }?.flagRes`
- `DEFAULT_FIAT_CODE` becomes `SupportedCurrency.MXN.code`

Adding a new currency is one line in the enum. USDC stays handled separately since it's the base currency with its own special flag and is not in the picker.

### 6b. Book string format is baked in

`"usdc_${fiatCode.lowercase()}"` is constructed inline in `observeRateTicker`. If the API changes its naming convention, or a future currency pair has a different base, this breaks silently.

**Fix**: Move this derivation into `SupportedCurrency` or a utility function:

```kotlin
fun bookFor(fiatCode: String): String = "usdc_${fiatCode.lowercase()}"
```

Small change but makes the coupling explicit and easy to find.

---

## Files Changed

| File | Change |
|---|---|
| `di/AppModule.kt` | Add `@ApplicationScope` qualifier + `CoroutineScope` provider |
| `ExchangeApp.kt` | Inject `RateRepository` for eagerness |
| `data/repository/RateRepositoryImpl.kt` | Inject `@ApplicationScope` scope; add `init` prefetch; fix Loading flash; add error logging; reduce timeout reference |
| `data/remote/RateTickerDto.kt` | Make `toEntity` return `RateTickerEntity?` with validation |
| `di/NetworkModule.kt` | Reduce network timeouts to 10s |
| `presentation/calculator/CalculatorViewModel.kt` | Remove dead `onTopFieldFocused`, `onBottomFieldFocused`, `focusField` |
| `presentation/theme/Color.kt` | Add 7 new named color constants |
| `presentation/calculator/CurrencyAmountRow.kt` | Replace inline cursor color; collapse card branches |
| `presentation/calculator/NumericKeypad.kt` | Replace inline keyboard colors |
| `presentation/calculator/SwapButton.kt` | Replace inline swap glow color |
| `presentation/calculator/CurrencyBottomSheet.kt` | Replace inline colors |
| `data/remote/FallbackCurrenciesProvider.kt` | Derive from `SupportedCurrency` enum |
| `presentation/calculator/CurrencyFlag.kt` | Derive from `SupportedCurrency` enum |
| `data/remote/SupportedCurrency.kt` (new) | `SupportedCurrency` enum; lives in data layer alongside `FallbackCurrenciesProvider` |
| `data/local/SettingsDataStore.kt` | Use `DEFAULT_FIAT_CODE` constant |
| `presentation/calculator/CalculatorUiState.kt` | Use `DEFAULT_FIAT_CODE` constant |

---

## Testing

- Existing unit tests should continue passing unchanged (the behavioral contract is preserved).
- `RateRepositoryImplTest`: add cases for malformed ask/bid DTOs — verify they are skipped and don't corrupt the cache.
- `CalculatorViewModelTest`: verify `onTopFieldFocused` / `onBottomFieldFocused` are gone (compile-time enforcement).
- No new tests needed for color/theme changes or the `SupportedCurrency` enum (pure data transformation with no logic).
