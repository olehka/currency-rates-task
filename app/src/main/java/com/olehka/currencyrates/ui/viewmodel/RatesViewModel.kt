package com.olehka.currencyrates.ui.viewmodel

import androidx.lifecycle.*
import com.olehka.currencyrates.R
import com.olehka.currencyrates.data.CurrencyRate
import com.olehka.currencyrates.data.Repository
import com.olehka.currencyrates.data.Result
import com.olehka.currencyrates.util.Event
import com.olehka.currencyrates.util.mapValues
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject

const val DELAY_MILLIS = 1_000L
const val DEFAULT_VALUE = 100f
const val EURO = "EUR"

class RatesViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {

    private lateinit var periodicJob: Job

    private var baseCurrency = EURO
    private var baseValue = DEFAULT_VALUE

    private val mutableRateList =
        MutableLiveData<List<CurrencyRate>>().apply { value = emptyList() }
    val rateList: LiveData<List<CurrencyRate>> = mutableRateList

    val empty: LiveData<Boolean> = Transformations.map(mutableRateList) { it.isEmpty() }

    private val snackbarText = MutableLiveData<Event<Int>>()
    val shackbarMessage: LiveData<Event<Int>> = snackbarText

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Timber.e(exception)
    }

    var isConnected: Boolean = false
        set(value) {
            field = value
            if (value) startPeriodicCurrencyRatesUpdate()
            else cancelPeriodicCurrencyRatesUpdate()
        }

    private fun startPeriodicCurrencyRatesUpdate() {
        cancelPeriodicCurrencyRatesUpdate()
        // alternative: liveData building block
        periodicJob = viewModelScope.launch(exceptionHandler) {
            while (isActive) {
                updateRateList(repository.getCurrencyRates(baseCurrency, fromNetwork = true))
                delay(DELAY_MILLIS)
            }
        }
    }

    fun updateCurrencyRatesFromCache() {
        viewModelScope.launch(exceptionHandler) {
            updateRateList(repository.getCurrencyRates(baseCurrency))
        }
    }

    fun onBaseCurrencyValueChanged(currency: String, value: Float) {
        Timber.v("onBaseCurrencyValueChanged: $currency: $value")
        if (isConnected && baseCurrency != currency) {
            baseCurrency = currency
            baseValue = value
            startPeriodicCurrencyRatesUpdate()
        } else {
            showErrorMessage()
        }
    }

    fun onBaseValueChanged(value: Float) {
        Timber.v("onBaseValueChanged: $value")
        if (baseValue != value) {
            baseValue = value
            updateCurrencyRatesFromCache()
        }
    }

    private fun cancelPeriodicCurrencyRatesUpdate() {
        if (::periodicJob.isInitialized) {
            periodicJob.cancel()
        }
    }

    private fun updateRateList(result: Result<List<CurrencyRate>>) {
        when (result) {
            is Result.Success -> {
                mutableRateList.value = result.data.mapValues(baseValue)
            }
            is Result.Error -> {
                showErrorMessage()
            }
        }
    }

    fun showErrorMessage() {
        snackbarText.value = Event(R.string.currency_rates_loading_error)
    }
}