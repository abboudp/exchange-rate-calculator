# Startup Fetch & Code Quality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fetch exchange rates eagerly on app startup, apply targeted fixes for a silent crash risk and dead code, extract theme colors, add a swap animation, and consolidate currency definitions into a single enum.

**Architecture:** `RateRepositoryImpl` gets an application-scoped coroutine that prefetches all rates at singleton creation time; `ExchangeApp` injects the repository to force eager Hilt instantiation. DTO validation moves upstream so malformed API rows are discarded before reaching Room. A `SupportedCurrency` enum becomes the single source of truth for supported currencies and their flag resources. Color constants are lifted into `Color.kt`. The swap gesture gets a 230 ms crossing animation driven by `Animatable` offsets and a `swapAnimationKey` counter in `CalculatorUiState`.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Room, Retrofit, Kotlinx Coroutines, DataStore Preferences, JUnit 4, MockK

---

## File Map

| File | Action |
|---|---|
| `di/ApplicationScope.kt` | **Create** — `@ApplicationScope` qualifier annotation |
| `di/AppModule.kt` | **Modify** — add `@ApplicationScope CoroutineScope` provider |
| `ExchangeApp.kt` | **Modify** — inject `RateRepository` for eager singleton creation |
| `data/repository/RateRepositoryImpl.kt` | **Modify** — inject app scope; `init` prefetch; `fetchAllTickers()`; fix loading flash; logging; handle nullable `toEntity` |
| `data/remote/RateTickerDto.kt` | **Modify** — `toEntity` returns `RateTickerEntity?`; validates ask/bid |
| `data/remote/SupportedCurrency.kt` | **Create** — `SupportedCurrency` enum + `DEFAULT_FIAT_CODE` |
| `data/remote/FallbackCurrenciesProvider.kt` | **Modify** — derive from `SupportedCurrency` |
| `presentation/calculator/CurrencyFlag.kt` | **Modify** — derive `currencyFlagRes` from `SupportedCurrency` |
| `data/local/SettingsDataStore.kt` | **Modify** — use `DEFAULT_FIAT_CODE` |
| `presentation/calculator/CalculatorUiState.kt` | **Modify** — use `DEFAULT_FIAT_CODE`; add `swapAnimationKey: Int` |
| `presentation/calculator/CalculatorViewModel.kt` | **Modify** — increment `swapAnimationKey` on swap; remove dead focus methods |
| `di/NetworkModule.kt` | **Modify** — reduce timeouts to 10 s |
| `presentation/theme/Color.kt` | **Modify** — add 7 color constants |
| `presentation/calculator/CurrencyAmountRow.kt` | **Modify** — use `CursorBlue`; consolidate card branches |
| `presentation/calculator/NumericKeypad.kt` | **Modify** — use `KeyboardBackground` / `KeyBackground` |
| `presentation/calculator/SwapButton.kt` | **Modify** — use `SwapButtonGlow` |
| `presentation/calculator/CurrencyBottomSheet.kt` | **Modify** — use `HandleGray`, `PickerFlagBackground`, `SelectionBorderGray` |
| `presentation/calculator/CalculatorScreen.kt` | **Modify** — `AmountRowsWithSwap` gains swap row animation |
| `data/remote/RateTickerDtoTest.kt` | **Modify** — add validation test cases |

---

## Task 1: ApplicationScope DI + Eager Startup Fetch

**Files:**
- Create: `app/src/main/java/com/example/exchangeratecalculator/di/ApplicationScope.kt`
- Modify: `app/src/main/java/com/example/exchangeratecalculator/di/AppModule.kt`
- Modify: `app/src/main/java/com/example/exchangeratecalculator/ExchangeApp.kt`
- Modify: `app/src/main/java/com/example/exchangeratecalculator/data/repository/RateRepositoryImpl.kt`

- [ ] **Step 1: Create the qualifier annotation**

Create `app/src/main/java/com/example/exchangeratecalculator/di/ApplicationScope.kt`:

```kotlin
package com.example.exchangeratecalculator.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
```

- [ ] **Step 2: Provide the application-scoped CoroutineScope**

Replace the entire contents of `app/src/main/java/com/example/exchangeratecalculator/di/AppModule.kt`:

```kotlin
package com.example.exchangeratecalculator.di

import com.example.exchangeratecalculator.core.coroutine.DefaultDispatcherProvider
import com.example.exchangeratecalculator.core.coroutine.DefaultStaleRecheckTicker
import com.example.exchangeratecalculator.core.coroutine.DispatcherProvider
import com.example.exchangeratecalculator.core.coroutine.StaleRecheckTicker
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindStaleRecheckTicker(impl: DefaultStaleRecheckTicker): StaleRecheckTicker

    companion object {
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
```

- [ ] **Step 3: Force eager singleton creation in ExchangeApp**

Replace the entire contents of `app/src/main/java/com/example/exchangeratecalculator/ExchangeApp.kt`:

```kotlin
package com.example.exchangeratecalculator

import android.app.Application
import com.example.exchangeratecalculator.domain.repository.RateRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ExchangeApp : Application() {
    // Injecting the singleton here forces Hilt to create RateRepositoryImpl
    // (and fire its init-block startup fetch) at Application.onCreate time,
    // before any Activity is created.
    @Inject lateinit var rateRepository: RateRepository
}
```

- [ ] **Step 4: Inject app scope, extract fetchAllTickers, add init-block prefetch, fix loading flash, add logging**

Replace the entire contents of `app/src/main/java/com/example/exchangeratecalculator/data/repository/RateRepositoryImpl.kt`:

```kotlin
package com.example.exchangeratecalculator.data.repository

import android.util.Log
import com.example.exchangeratecalculator.core.coroutine.DispatcherProvider
import com.example.exchangeratecalculator.data.local.RateTickerDao
import com.example.exchangeratecalculator.data.local.toDomain
import com.example.exchangeratecalculator.data.remote.DolarApi
import com.example.exchangeratecalculator.data.remote.FallbackCurrenciesProvider
import com.example.exchangeratecalculator.data.remote.toEntity
import com.example.exchangeratecalculator.di.ApplicationScope
import com.example.exchangeratecalculator.domain.model.RateResource
import com.example.exchangeratecalculator.domain.repository.RateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateRepositoryImpl
    @Inject
    constructor(
        private val api: DolarApi,
        private val dao: RateTickerDao,
        private val dispatchers: DispatcherProvider,
        @ApplicationScope private val appScope: CoroutineScope,
    ) : RateRepository {

        init {
            appScope.launch(dispatchers.io) {
                try {
                    fetchAllTickers()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Startup fetch failed", e)
                }
            }
        }

        override fun observeRateTicker(fiatCode: String): Flow<RateResource> =
            channelFlow {
                val book = "usdc_${fiatCode.lowercase()}"
                val pollFailedFlow = MutableStateFlow(false)

                // Read the cached row before emitting Loading so a cached currency
                // switch shows Available immediately instead of flashing Loading.
                val initialEntity = dao.observeTicker(book).first()
                send(
                    if (initialEntity != null) RateResource.Available(initialEntity.toDomain())
                    else RateResource.Loading,
                )

                launch {
                    while (isActive) {
                        val succeeded =
                            try {
                                fetchAllTickers()
                                pollFailedFlow.value = false
                                true
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                Log.e(TAG, "Rate fetch failed", e)
                                pollFailedFlow.value = true
                                false
                            }
                        delay(if (succeeded) POLL_INTERVAL_MS else RETRY_INTERVAL_MS)
                    }
                }

                combine(dao.observeTicker(book), pollFailedFlow) { entity, failed ->
                    entity to failed
                }.collect { (entity, failed) ->
                    when {
                        entity != null -> send(RateResource.Available(entity.toDomain()))
                        failed -> send(RateResource.Unavailable("offline"))
                    }
                }
            }.flowOn(dispatchers.io)

        private suspend fun fetchAllTickers() {
            val dtos = api.getTickers(FallbackCurrenciesProvider.queryCodes)
            val now = System.currentTimeMillis()
            dtos.forEach { dto -> dao.upsertTicker(dto.toEntity(fetchedAtEpochMs = now)) }
        }

        companion object {
            private const val TAG = "RateRepo"
            const val POLL_INTERVAL_MS = 60_000L
            const val RETRY_INTERVAL_MS = 5_000L
        }
    }
```

- [ ] **Step 5: Build the project**

Run: `./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL. If Hilt complains about missing binding for `@ApplicationScope CoroutineScope`, verify the `companion object` in `AppModule` has both `@Provides` and `@ApplicationScope`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/exchangeratecalculator/di/ApplicationScope.kt \
        app/src/main/java/com/example/exchangeratecalculator/di/AppModule.kt \
        app/src/main/java/com/example/exchangeratecalculator/ExchangeApp.kt \
        app/src/main/java/com/example/exchangeratecalculator/data/repository/RateRepositoryImpl.kt
git commit -m "feat: eager rate prefetch on app start; fix loading flash on currency switch"
```

---

## Task 2: DTO Validation

`RateTickerDto.toEntity` currently stores `ask`/`bid` as raw strings with no validation. `RateTickerEntity.toDomain()` calls `BigDecimal(ask)`, which throws `NumberFormatException` on any malformed string. The exception propagates through the Room Flow's `collect`, killing the rate update coroutine silently (under `viewModelScope`'s `SupervisorJob` the app doesn't crash, but rate updates stop for the session). Fix: validate at write time and discard invalid rows.

**Files:**
- Modify: `app/src/main/java/com/example/exchangeratecalculator/data/remote/RateTickerDto.kt`
- Modify: `app/src/main/java/com/example/exchangeratecalculator/data/repository/RateRepositoryImpl.kt`
- Modify: `app/src/test/java/com/example/exchangeratecalculator/data/remote/RateTickerDtoTest.kt`

- [ ] **Step 1: Write failing tests for the validation cases**

Open `app/src/test/java/com/example/exchangeratecalculator/data/remote/RateTickerDtoTest.kt` and add these test cases (keep any existing tests):

```kotlin
package com.example.exchangeratecalculator.data.remote

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RateTickerDtoTest {

    private fun dto(ask: String = "18.41", bid: String = "18.40") =
        RateTickerDto(ask = ask, bid = bid, book = "usdc_mxn", date = "2026-05-23")

    @Test
    fun `toEntity returns entity for valid positive decimals`() {
        val entity = dto(ask = "18.41", bid = "18.40").toEntity(fetchedAtEpochMs = 1000L)
        assertNotNull(entity)
    }

    @Test
    fun `toEntity returns null when ask is not a decimal`() {
        val entity = dto(ask = "N/A", bid = "18.40").toEntity(fetchedAtEpochMs = 1000L)
        assertNull(entity)
    }

    @Test
    fun `toEntity returns null when bid is empty`() {
        val entity = dto(ask = "18.41", bid = "").toEntity(fetchedAtEpochMs = 1000L)
        assertNull(entity)
    }

    @Test
    fun `toEntity returns null when ask is zero`() {
        val entity = dto(ask = "0", bid = "18.40").toEntity(fetchedAtEpochMs = 1000L)
        assertNull(entity)
    }

    @Test
    fun `toEntity returns null when bid is negative`() {
        val entity = dto(ask = "18.41", bid = "-1.00").toEntity(fetchedAtEpochMs = 1000L)
        assertNull(entity)
    }

    @Test
    fun `toEntity stores ask and bid as canonical plain string`() {
        val entity = dto(ask = "18.410", bid = "18.400").toEntity(fetchedAtEpochMs = 1000L)!!
        // toPlainString strips trailing zeros and ensures BigDecimal(stored) never throws
        assertNotNull(entity.ask.toBigDecimalOrNull())
        assertNotNull(entity.bid.toBigDecimalOrNull())
    }
}
```

- [ ] **Step 2: Run the tests to confirm they fail**

Run: `./gradlew :app:test --tests "*.RateTickerDtoTest" --info`

Expected: Several tests FAIL because `toEntity` currently returns non-null unconditionally and stores the raw string without validation.

- [ ] **Step 3: Update toEntity to validate and return nullable**

Replace the entire contents of `app/src/main/java/com/example/exchangeratecalculator/data/remote/RateTickerDto.kt`:

```kotlin
package com.example.exchangeratecalculator.data.remote

import com.example.exchangeratecalculator.data.local.RateTickerEntity
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class RateTickerDto(
    val ask: String,
    val bid: String,
    val book: String,
    val date: String,
)

fun RateTickerDto.toEntity(fetchedAtEpochMs: Long): RateTickerEntity? {
    val askDecimal = ask.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    val bidDecimal = bid.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    return RateTickerEntity(
        book = book,
        ask = askDecimal.toPlainString(),
        bid = bidDecimal.toPlainString(),
        fetchedAtEpochMs = fetchedAtEpochMs,
    )
}
```

- [ ] **Step 4: Update fetchAllTickers in RateRepositoryImpl to handle the nullable return**

In `RateRepositoryImpl.kt`, update the `fetchAllTickers` method (the `forEach` line):

```kotlin
private suspend fun fetchAllTickers() {
    val dtos = api.getTickers(FallbackCurrenciesProvider.queryCodes)
    val now = System.currentTimeMillis()
    dtos.forEach { dto -> dto.toEntity(fetchedAtEpochMs = now)?.let { dao.upsertTicker(it) } }
}
```

- [ ] **Step 5: Run the tests again to confirm they pass**

Run: `./gradlew :app:test --tests "*.RateTickerDtoTest" --info`

Expected: All tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/exchangeratecalculator/data/remote/RateTickerDto.kt \
        app/src/main/java/com/example/exchangeratecalculator/data/repository/RateRepositoryImpl.kt \
        app/src/test/java/com/example/exchangeratecalculator/data/remote/RateTickerDtoTest.kt
git commit -m "fix: validate DTO ask/bid before storing in Room; discard malformed rows"
```

---

## Task 3: SupportedCurrency Enum + DEFAULT_FIAT_CODE

Consolidate currency definitions. Currently adding a new currency requires editing `FallbackCurrenciesProvider` (the list) and `CurrencyFlag.kt` (the `when` block) separately, with no compile-time enforcement they stay in sync. A `SupportedCurrency` enum pairs `code` with `@DrawableRes flagRes` so a new currency is one line.

`DEFAULT_FIAT_CODE` replaces the hardcoded `"MXN"` string in three places.

**Files:**
- Create: `app/src/main/java/com/example/exchangeratecalculator/data/remote/SupportedCurrency.kt`
- Modify: `app/src/main/java/com/example/exchangeratecalculator/data/remote/FallbackCurrenciesProvider.kt`
- Modify: `app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CurrencyFlag.kt`
- Modify: `app/src/main/java/com/example/exchangeratecalculator/data/local/SettingsDataStore.kt`
- Modify: `app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CalculatorUiState.kt`

- [ ] **Step 1: Create SupportedCurrency enum**

Create `app/src/main/java/com/example/exchangeratecalculator/data/remote/SupportedCurrency.kt`:

```kotlin
package com.example.exchangeratecalculator.data.remote

import androidx.annotation.DrawableRes
import com.example.exchangeratecalculator.R

enum class SupportedCurrency(val code: String, @DrawableRes val flagRes: Int) {
    MXN("MXN", R.drawable.flag_mxn),
    ARS("ARS", R.drawable.flag_ars),
    BRL("BRL", R.drawable.flag_brl),
    COP("COP", R.drawable.flag_cop),
}

val DEFAULT_FIAT_CODE: String = SupportedCurrency.entries.first().code
```

- [ ] **Step 2: Derive FallbackCurrenciesProvider from the enum**

Replace the entire contents of `app/src/main/java/com/example/exchangeratecalculator/data/remote/FallbackCurrenciesProvider.kt`:

```kotlin
package com.example.exchangeratecalculator.data.remote

import com.example.exchangeratecalculator.domain.model.Currency

object FallbackCurrenciesProvider {
    val currencies: List<Currency> =
        SupportedCurrency.entries.map { Currency(code = it.code, isBase = false) }

    val queryCodes: String = currencies.joinToString(separator = ",") { it.code }
}
```

- [ ] **Step 3: Derive currencyFlagRes from the enum**

Replace the entire contents of `app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CurrencyFlag.kt`:

```kotlin
package com.example.exchangeratecalculator.presentation.calculator

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.exchangeratecalculator.R
import com.example.exchangeratecalculator.data.remote.SupportedCurrency

@DrawableRes
fun currencyFlagRes(code: String): Int? =
    when (code) {
        "USDC" -> R.drawable.flag_usdc
        else -> SupportedCurrency.entries.find { it.code == code }?.flagRes
    }

@Composable
fun CurrencyFlag(
    code: String,
    modifier: Modifier = Modifier,
) {
    val res = currencyFlagRes(code) ?: return
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = modifier.clip(CircleShape),
        contentScale = ContentScale.Fit,
    )
}
```

- [ ] **Step 4: Use DEFAULT_FIAT_CODE in SettingsDataStore**

In `app/src/main/java/com/example/exchangeratecalculator/data/local/SettingsDataStore.kt`, update the `DEFAULTS` companion object entry:

```kotlin
import com.example.exchangeratecalculator.data.remote.DEFAULT_FIAT_CODE

// Inside the companion object:
private val DEFAULTS = AppSettings(selectedFiatCode = DEFAULT_FIAT_CODE)
```

The full companion object becomes:

```kotlin
companion object {
    private val KEY_SELECTED_FIAT_CODE = stringPreferencesKey("selected_fiat_code")
    private val KEY_IS_SWAPPED = booleanPreferencesKey("is_swapped")
    private val DEFAULTS = AppSettings(selectedFiatCode = DEFAULT_FIAT_CODE)
}
```

- [ ] **Step 5: Use DEFAULT_FIAT_CODE in CalculatorUiState**

In `app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CalculatorUiState.kt`, update both defaults and add the `swapAnimationKey` field (used in Task 6):

```kotlin
package com.example.exchangeratecalculator.presentation.calculator

import com.example.exchangeratecalculator.data.remote.DEFAULT_FIAT_CODE
import com.example.exchangeratecalculator.domain.model.Currency
import com.example.exchangeratecalculator.domain.model.USDC_CURRENCY

enum class AmountField { TOP, BOTTOM }

data class CalculatorUiState(
    val topCurrencyCode: String = USDC_CURRENCY.code,
    val bottomCurrencyCode: String = DEFAULT_FIAT_CODE,
    val topAmountText: String = "",
    val bottomAmountText: String = "",
    val activeField: AmountField = AmountField.TOP,
    val isSwapped: Boolean = false,
    val rateDisplayState: RateDisplayState = RateDisplayState.Loading,
    val pickerState: CurrencyPickerState = CurrencyPickerState(),
    val canBackspace: Boolean = false,
    val canInsertDecimal: Boolean = true,
    val swapAnimationKey: Int = 0,
)

sealed interface RateDisplayState {
    data object Loading : RateDisplayState

    data class Available(val text: String, val isFresh: Boolean) : RateDisplayState

    data object Unavailable : RateDisplayState
}

data class CurrencyPickerState(
    val isVisible: Boolean = false,
    val currencies: List<Currency> = emptyList(),
    val selectedCode: String = DEFAULT_FIAT_CODE,
    val isLoading: Boolean = false,
)
```

- [ ] **Step 6: Build the project**

Run: `./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL. Verify there are no remaining references to the literal `"MXN"` in `CalculatorUiState.kt`, `SettingsDataStore.kt`, or `FallbackCurrenciesProvider.kt` (use find-in-files to check).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/exchangeratecalculator/data/remote/SupportedCurrency.kt \
        app/src/main/java/com/example/exchangeratecalculator/data/remote/FallbackCurrenciesProvider.kt \
        app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CurrencyFlag.kt \
        app/src/main/java/com/example/exchangeratecalculator/data/local/SettingsDataStore.kt \
        app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CalculatorUiState.kt
git commit -m "refactor: SupportedCurrency enum as single source of truth; DEFAULT_FIAT_CODE constant"
```

---

## Task 4: Error Logging, Timeout Reduction, Dead Code Removal

**Files:**
- Modify: `app/src/main/java/com/example/exchangeratecalculator/di/NetworkModule.kt`
- Modify: `app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CalculatorViewModel.kt`

Note: logging in `RateRepositoryImpl` was added in Task 1.

- [ ] **Step 1: Reduce network timeouts**

In `app/src/main/java/com/example/exchangeratecalculator/di/NetworkModule.kt`, change the timeout constant:

```kotlin
private const val NETWORK_TIMEOUT_SECONDS = 10L
```

(Was `30L`. A 30 s timeout on a 60 s poll cycle stalls the UI in Loading/Stale for half the interval on a slow connection.)

- [ ] **Step 2: Remove dead ViewModel methods**

`onTopFieldFocused`, `onBottomFieldFocused`, and `focusField` are never called from the UI — the bottom row hardcodes `isActive = false` and there is no tap-to-focus gesture. Delete all three from `CalculatorViewModel.kt`:

Remove these three methods:

```kotlin
fun onTopFieldFocused() = focusField(AmountField.TOP)

fun onBottomFieldFocused() = focusField(AmountField.BOTTOM)

private fun focusField(field: AmountField) {
    if (_uiState.value.activeField == field) return
    val ticker = currentTicker()
    _uiState.update { state ->
        state.copy(activeField = field)
            .withRecomputedInactive(ticker)
            .withRecomputedRateDisplay()
            .withKeypadFlags()
    }
}
```

- [ ] **Step 3: Increment swapAnimationKey in onSwapPressed**

Still in `CalculatorViewModel.kt`, update `onSwapPressed` to increment `swapAnimationKey`:

```kotlin
fun onSwapPressed() {
    val newIsSwapped = !_uiState.value.isSwapped
    val ticker = currentTicker()
    _uiState.update { state ->
        state.copy(
            topCurrencyCode = state.bottomCurrencyCode,
            bottomCurrencyCode = state.topCurrencyCode,
            topAmountText = state.bottomAmountText,
            bottomAmountText = state.topAmountText,
            isSwapped = newIsSwapped,
            swapAnimationKey = state.swapAnimationKey + 1,
        ).withRecomputedInactive(ticker)
            .withRecomputedRateDisplay()
            .withKeypadFlags()
    }
    viewModelScope.launch {
        settingsRepository.updateSwapState(newIsSwapped)
    }
}
```

- [ ] **Step 4: Build and run unit tests**

Run: `./gradlew :app:test`

Expected: All tests pass. If `CalculatorViewModelTest` calls `onTopFieldFocused` or `onBottomFieldFocused`, remove those test calls too.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/exchangeratecalculator/di/NetworkModule.kt \
        app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CalculatorViewModel.kt
git commit -m "refactor: remove dead focus methods; reduce network timeout to 10s; track swap key for animation"
```

---

## Task 5: Color Constants + Composable Cleanup

Extract seven hardcoded hex literals scattered across composable files into named constants in `Color.kt`. Also consolidate the duplicate `Card` branches in `CurrencyAmountRow`.

**Files:**
- Modify: `app/src/main/java/com/example/exchangeratecalculator/presentation/theme/Color.kt`
- Modify: `app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CurrencyAmountRow.kt`
- Modify: `app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/NumericKeypad.kt`
- Modify: `app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/SwapButton.kt`
- Modify: `app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CurrencyBottomSheet.kt`

- [ ] **Step 1: Add color constants to Color.kt**

Append to `app/src/main/java/com/example/exchangeratecalculator/presentation/theme/Color.kt`:

```kotlin
val CursorBlue = Color(0xFF2F7CFF)
val KeyboardBackground = Color(0xFFD1D3D8)
val KeyBackground = Color(0xFFFCFCFE)
val SwapButtonGlow = Color(0xFFE7FAF1)
val HandleGray = Color(0xFFD9D9D9)
val PickerFlagBackground = Color(0xFFF4F4F4)
val SelectionBorderGray = Color(0xFFD1D1D6)
```

- [ ] **Step 2: Update CurrencyAmountRow — use CursorBlue and consolidate Card**

Replace the entire contents of `app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CurrencyAmountRow.kt`:

```kotlin
package com.example.exchangeratecalculator.presentation.calculator

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.exchangeratecalculator.presentation.theme.CardBackground
import com.example.exchangeratecalculator.presentation.theme.CursorBlue
import com.example.exchangeratecalculator.presentation.theme.PrimaryText

@Composable
fun CurrencyAmountRow(
    currencyCode: String,
    isSelectable: Boolean,
    amountDisplay: String,
    isActive: Boolean,
    onCurrencyClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
) {
    val displayCode = if (currencyCode == "USDC") "USDc" else currencyCode
    Card(
        onClick = onCurrencyClick,
        enabled = isSelectable,
        modifier = modifier.fillMaxWidth().height(66.dp).testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground,
            disabledContainerColor = CardBackground,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = if (isSelectable) 4.dp else 0.dp,
        ),
    ) {
        CardContent(
            displayCode = displayCode,
            currencyCode = currencyCode,
            isSelectable = isSelectable,
            amountDisplay = amountDisplay,
            isActive = isActive,
        )
    }
}

@Composable
private fun CardContent(
    displayCode: String,
    currencyCode: String,
    isSelectable: Boolean,
    amountDisplay: String,
    isActive: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CurrencyFlag(code = currencyCode, modifier = Modifier.size(16.dp))
            Text(
                text = displayCode,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = PrimaryText,
            )
            if (isSelectable) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Choose currency",
                    tint = PrimaryText.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        AmountText(
            amountDisplay = amountDisplay,
            isActive = isActive,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
        )
    }
}

@Composable
private fun AmountText(
    amountDisplay: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = amountDisplay,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = PrimaryText,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isActive) {
            BlinkingCursor()
        }
    }
}

@Composable
private fun BlinkingCursor() {
    val transition = rememberInfiniteTransition(label = "amountCursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "amountCursorAlpha",
    )
    Box(
        modifier =
            Modifier
                .padding(start = 2.dp)
                .width(2.dp)
                .height(20.dp)
                .alpha(alpha)
                .background(CursorBlue, shape = RoundedCornerShape(1.dp)),
    )
}

const val CURRENCY_ROW_TOP_TAG = "currency_row_top"
const val CURRENCY_ROW_BOTTOM_TAG = "currency_row_bottom"
```

- [ ] **Step 3: Update NumericKeypad — use color constants**

In `app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/NumericKeypad.kt`:

1. Add imports:
```kotlin
import com.example.exchangeratecalculator.presentation.theme.KeyBackground
import com.example.exchangeratecalculator.presentation.theme.KeyboardBackground
```

2. Remove the two private color declarations at the bottom of the file:
```kotlin
// DELETE these two lines:
private val KeyboardBackground = Color(0xFFD1D3D8)
private val KeyBackground = Color(0xFFFCFCFE)
```

3. Remove the now-unused `import androidx.compose.ui.graphics.Color` if `Color` is no longer referenced anywhere else in the file.

- [ ] **Step 4: Update SwapButton — use SwapButtonGlow**

In `app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/SwapButton.kt`:

1. Add import:
```kotlin
import com.example.exchangeratecalculator.presentation.theme.SwapButtonGlow
```

2. Replace:
```kotlin
color = Color(0xFFE7FAF1),
```
With:
```kotlin
color = SwapButtonGlow,
```

3. Remove the now-unused `import androidx.compose.ui.graphics.Color` if `Color` is no longer referenced elsewhere in the file.

- [ ] **Step 5: Update CurrencyBottomSheet — use color constants**

In `app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CurrencyBottomSheet.kt`:

1. Add imports:
```kotlin
import com.example.exchangeratecalculator.presentation.theme.HandleGray
import com.example.exchangeratecalculator.presentation.theme.PickerFlagBackground
import com.example.exchangeratecalculator.presentation.theme.SelectionBorderGray
```

2. In `BottomSheetHandle`, replace:
```kotlin
.background(Color(0xFFD9D9D9)),
```
With:
```kotlin
.background(HandleGray),
```

3. In `CurrencyPickerFlag`, replace:
```kotlin
Modifier.size(40.dp).background(color = Color(0xFFF4F4F4), shape = CircleShape),
```
With:
```kotlin
Modifier.size(40.dp).background(color = PickerFlagBackground, shape = CircleShape),
```

4. In `SelectionIndicator`, replace:
```kotlin
Modifier.border(2.dp, Color(0xFFD1D1D6), CircleShape)
```
With:
```kotlin
Modifier.border(2.dp, SelectionBorderGray, CircleShape)
```

5. If `Color` is no longer directly referenced in `CurrencyBottomSheet.kt` after these replacements, remove `import androidx.compose.ui.graphics.Color`.

- [ ] **Step 6: Build the project**

Run: `./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL. Verify no remaining inline hex literals in the five composable files with: `grep -r "0xFF" app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/`

The only remaining `0xFF` values should be the Color constants themselves in `Color.kt`.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/exchangeratecalculator/presentation/theme/Color.kt \
        app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CurrencyAmountRow.kt \
        app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/NumericKeypad.kt \
        app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/SwapButton.kt \
        app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CurrencyBottomSheet.kt
git commit -m "refactor: extract hardcoded colors to Color.kt; consolidate CurrencyAmountRow card"
```

---

## Task 6: Swap Row Animation

When the swap button is pressed, the two currency rows animate into their new positions with a 230 ms crossing motion — the row moving to the top slides in from below, and the row moving to the bottom slides in from above.

**Approach**: Use `Animatable<Float>` for each row's vertical offset. When `swapAnimationKey` increments (only on user swap, never on settings restore), `LaunchedEffect` snaps each row to its "came from" position and animates back to zero. `swapAnimationKey == 0` guards against the initial composition firing the animation.

**Files:**
- Modify: `app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CalculatorScreen.kt`

- [ ] **Step 1: Update AmountRowsWithSwap with the crossing animation**

Replace the `AmountRowsWithSwap` composable in `app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CalculatorScreen.kt` with the version below. Also add the required imports.

New imports to add at the top of `CalculatorScreen.kt`:

```kotlin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
```

Replace the `AmountRowsWithSwap` function:

```kotlin
@Composable
private fun AmountRowsWithSwap(
    uiState: CalculatorUiState,
    onSwap: () -> Unit,
    onFiatRowTapped: () -> Unit,
) {
    val density = LocalDensity.current
    // Total distance each row travels = card height (66dp) + gap (16dp)
    val rowAndGapPx = remember(density) { with(density) { (66.dp + 16.dp).toPx() } }

    val topOffset = remember { Animatable(0f) }
    val bottomOffset = remember { Animatable(0f) }
    val animScope = rememberCoroutineScope()

    LaunchedEffect(uiState.swapAnimationKey) {
        // swapAnimationKey == 0 is the initial composition; skip to avoid an
        // unwanted animation on settings restore.
        if (uiState.swapAnimationKey == 0) return@LaunchedEffect

        // Each row starts from where it came from (opposite side) then slides
        // to its natural resting position (offset = 0).
        // isSwapped = true means fiat is now on top (came from bottom → start below).
        val fromDirection = if (uiState.isSwapped) 1f else -1f
        topOffset.snapTo(rowAndGapPx * fromDirection)
        bottomOffset.snapTo(-rowAndGapPx * fromDirection)

        val animSpec = tween<Float>(durationMillis = 230, easing = FastOutSlowInEasing)
        animScope.launch { topOffset.animateTo(0f, animSpec) }
        animScope.launch { bottomOffset.animateTo(0f, animSpec) }
    }

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column {
            CurrencyAmountRow(
                currencyCode = uiState.topCurrencyCode,
                isSelectable = uiState.topCurrencyCode != USDC_CURRENCY.code,
                amountDisplay =
                    MoneyFormatter.formatAmountDisplay(
                        rawText = uiState.topAmountText,
                        isActive = uiState.activeField == AmountField.TOP,
                    ),
                isActive = uiState.activeField == AmountField.TOP,
                onCurrencyClick = onFiatRowTapped,
                modifier = Modifier.offset { IntOffset(0, topOffset.value.roundToInt()) },
                testTag = CURRENCY_ROW_TOP_TAG,
            )
            Spacer(modifier = Modifier.height(16.dp))
            CurrencyAmountRow(
                currencyCode = uiState.bottomCurrencyCode,
                isSelectable = uiState.bottomCurrencyCode != USDC_CURRENCY.code,
                amountDisplay =
                    MoneyFormatter.formatAmountDisplay(
                        rawText = uiState.bottomAmountText,
                        isActive = false,
                    ),
                isActive = false,
                onCurrencyClick = onFiatRowTapped,
                modifier = Modifier.offset { IntOffset(0, bottomOffset.value.roundToInt()) },
                testTag = CURRENCY_ROW_BOTTOM_TAG,
            )
        }
        SwapButton(
            onClick = onSwap,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
```

**Animation note**: During the crossing (when both rows overlap mid-animation), Compose renders the `Column`'s second child (bottom row) on top of the first (top row) due to draw order. This is imperceptible at 230 ms and the visual result is correct — the rows appear to cross cleanly.

- [ ] **Step 2: Build the project**

Run: `./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL. If there is a compile error about `rememberCoroutineScope` not being imported, add `import androidx.compose.runtime.rememberCoroutineScope`.

- [ ] **Step 3: Manually verify the animation**

Install the debug build on a device or emulator. Press the swap button. Confirm:
- The two rows cross each other over ~230 ms (the row going up slides in from below, the row going down slides in from above)
- No animation fires on first app launch (cold start with or without a stored `isSwapped = true`)
- The animation fires correctly on every subsequent swap press
- The swap button's own press ripple is unaffected

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/exchangeratecalculator/presentation/calculator/CalculatorScreen.kt
git commit -m "feat: add 230ms crossing animation to currency row swap"
```

---

## Self-Review

**Spec coverage:**
- ✅ 1a Eager startup fetch — Task 1
- ✅ 1b Loading flash fix — Task 1 (`dao.observeTicker(book).first()`)
- ✅ 2a DTO validation — Task 2
- ✅ 2b Dead code removal — Task 4
- ✅ 3a Error logging — Task 1 (`Log.e(TAG, ...)` in both catch blocks)
- ✅ 3b Network timeout — Task 4
- ✅ 4a Color constants — Task 5
- ✅ 4b DEFAULT_FIAT_CODE — Task 3
- ✅ 4c CurrencyAmountRow card consolidation — Task 5
- ✅ 5a (distinctUntilChanged dropped — `StateFlow` already suppresses equal-value emissions; 10 s wakeup overhead is negligible)
- ✅ 6a SupportedCurrency enum — Task 3
- ✅ 6b book string format — unchanged (already isolated in `observeRateTicker`; a utility function would be a no-op extraction with no call sites, so skipped per YAGNI)
- ✅ Swap animation — Task 6

**Placeholder scan:** No TBDs or incomplete steps found.

**Type consistency:** `swapAnimationKey: Int` added in Task 3 (CalculatorUiState), incremented in Task 4 (ViewModel), and read in Task 6 (CalculatorScreen). Types consistent throughout.
