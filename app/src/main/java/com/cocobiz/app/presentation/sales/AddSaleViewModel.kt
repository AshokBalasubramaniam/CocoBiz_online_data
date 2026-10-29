package com.cocobiz.app.presentation.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocobiz.app.domain.model.CoconutType
import com.cocobiz.app.domain.model.Dealer
import com.cocobiz.app.domain.model.SaleStatus
import com.cocobiz.app.domain.model.SalesEntry
import com.cocobiz.app.domain.repository.DealerRepository
import com.cocobiz.app.domain.repository.SalesRepository
import com.cocobiz.app.domain.repository.SettingsRepository
import com.cocobiz.app.util.DateUtils.calculateNextSalesDate
import com.cocobiz.app.util.DateUtils.toEpochMilli
import com.cocobiz.app.util.DateUtils.toLocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class SaleFormState(
    val id: Long = 0L,
    val selectedDealerId: Long = 0L,
    val dealerName: String = "",
    val dealerPlace: String = "",
    val dealerPhone: String = "",
    val salesDate: LocalDate = LocalDate.now(),
    val nextSalesDate: LocalDate = LocalDate.now().plusDays(60),
    val coconutType: CoconutType = CoconutType.TONNAGE,
    val quantity: String = "",
    val rate: String = "",
    val marketRate: String = "",
    val totalAmount: Double = 0.0,
    val cycleDays: Int = 60,
    val notes: String = "",
    val dealers: List<Dealer> = emptyList(),
    val dealerNameError: String = "",
    val quantityError: String = "",
    val rateError: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val status: SaleStatus = SaleStatus.ACTIVE,
    val createdAt: Long = 0L,
    /** Completed more than 30 days ago: sale date and cycle can't be changed. */
    val datesLocked: Boolean = false,
    val showDatePicker: Boolean = false
) {
    val isValid: Boolean
        get() = dealerName.isNotBlank() && quantity.isNotBlank() && rate.isNotBlank() &&
                quantity.toDoubleOrNull() != null && rate.toDoubleOrNull() != null &&
                (quantity.toDoubleOrNull() ?: 0.0) > 0 && (rate.toDoubleOrNull() ?: 0.0) > 0
}

@HiltViewModel
class AddSaleViewModel @Inject constructor(
    private val salesRepository: SalesRepository,
    private val dealerRepository: DealerRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(SaleFormState())
    val formState: StateFlow<SaleFormState> = _formState

    init {
        loadDealersAndSettings()
    }

    private fun loadDealersAndSettings() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings().first()
            val dealers = dealerRepository.getAllDealers().first()
            // When editing, the sale may load first; never overwrite its dates/cycle.
            _formState.update { state ->
                if (state.isEditMode) {
                    state.copy(dealers = dealers)
                } else {
                    state.copy(
                        cycleDays = settings.defaultCycleDays,
                        nextSalesDate = LocalDate.now().plusDays(settings.defaultCycleDays.toLong()),
                        dealers = dealers
                    )
                }
            }
        }
    }

    fun loadSaleForEdit(saleId: Long) {
        viewModelScope.launch {
            _formState.value = _formState.value.copy(isLoading = true)
            val sale = salesRepository.getSaleById(saleId).first()
            sale?.let {
                _formState.value = _formState.value.copy(
                    id = it.id,
                    selectedDealerId = it.dealerId,
                    dealerName = it.dealerName,
                    dealerPlace = it.dealerPlace,
                    salesDate = it.salesDate,
                    nextSalesDate = it.nextSalesDate,
                    coconutType = it.coconutType,
                    quantity = it.quantity.toFormattedString(),
                    rate = it.rate.toString(),
                    marketRate = if (it.marketRate > 0) it.marketRate.toFormattedString() else "",
                    totalAmount = it.totalAmount,
                    cycleDays = it.cycleDays,
                    notes = it.notes,
                    status = it.status,
                    createdAt = it.createdAt,
                    datesLocked = it.status == SaleStatus.COMPLETED &&
                        ChronoUnit.DAYS.between(it.salesDate, LocalDate.now()) > DATE_LOCK_DAYS,
                    isEditMode = true,
                    isLoading = false
                )
            } ?: run {
                _formState.value = _formState.value.copy(isLoading = false)
            }
        }
    }

    fun selectDealer(dealer: Dealer) {
        _formState.value = _formState.value.copy(
            selectedDealerId = dealer.id,
            dealerName = dealer.dealerName,
            dealerPlace = dealer.place,
            dealerPhone = dealer.phone,
            dealerNameError = ""
        )
    }

    fun updateDealerName(name: String) {
        _formState.value = _formState.value.copy(
            dealerName = name,
            selectedDealerId = 0L,
            dealerNameError = ""
        )
    }

    fun updateDealerPlace(place: String) {
        _formState.value = _formState.value.copy(dealerPlace = place)
    }

    fun updateCoconutType(type: CoconutType) {
        _formState.value = _formState.value.copy(coconutType = type)
        recalculate()
    }

    fun updateQuantity(value: String) {
        _formState.value = _formState.value.copy(quantity = value, quantityError = "")
        recalculate()
    }

    fun updateRate(value: String) {
        _formState.value = _formState.value.copy(rate = value, rateError = "")
        recalculate()
    }

    fun updateMarketRate(value: String) {
        _formState.value = _formState.value.copy(marketRate = value)
    }

    fun updateSalesDate(date: LocalDate) {
        if (_formState.value.datesLocked) return
        val nextDate = calculateNextSalesDate(date, _formState.value.cycleDays)
        _formState.value = _formState.value.copy(
            salesDate = date,
            nextSalesDate = nextDate,
            showDatePicker = false
        )
    }

    fun updateCycleDays(days: Int) {
        if (_formState.value.datesLocked) return
        val nextDate = calculateNextSalesDate(_formState.value.salesDate, days)
        _formState.value = _formState.value.copy(
            cycleDays = days,
            nextSalesDate = nextDate
        )
    }

    fun updateNotes(notes: String) {
        _formState.value = _formState.value.copy(notes = notes)
    }

    fun toggleDatePicker(show: Boolean) {
        if (show && _formState.value.datesLocked) return
        _formState.value = _formState.value.copy(showDatePicker = show)
    }

    private fun recalculate() {
        val qty = _formState.value.quantity.toDoubleOrNull() ?: 0.0
        val rate = _formState.value.rate.toDoubleOrNull() ?: 0.0
        _formState.value = _formState.value.copy(totalAmount = qty * rate)
    }

    fun saveSale() {
        val state = _formState.value
        var hasError = false

        if (state.dealerName.isBlank()) {
            _formState.value = _formState.value.copy(dealerNameError = "Dealer name is required")
            hasError = true
        }
        val qty = state.quantity.toDoubleOrNull()
        if (qty == null || qty <= 0) {
            _formState.value = _formState.value.copy(quantityError = "Valid quantity required")
            hasError = true
        }
        val rate = state.rate.toDoubleOrNull()
        if (rate == null || rate <= 0) {
            _formState.value = _formState.value.copy(rateError = "Valid rate required")
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isSaving = true)
            val dealerId = try {
                resolveDealerId(state)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isSaving = false,
                    dealerNameError = "Could not save dealer"
                )
                return@launch
            }
            val sale = SalesEntry(
                id = state.id,
                dealerId = dealerId,
                dealerName = state.dealerName.trim(),
                dealerPlace = state.dealerPlace.trim(),
                salesDate = state.salesDate,
                nextSalesDate = state.nextSalesDate,
                quantity = qty!!,
                rate = rate!!,
                marketRate = state.marketRate.toDoubleOrNull() ?: 0.0,
                totalAmount = qty * rate,
                coconutType = state.coconutType,
                cycleDays = state.cycleDays,
                status = state.status,
                notes = state.notes.trim()
            ).let { if (state.createdAt > 0) it.copy(createdAt = state.createdAt) else it }
            if (state.isEditMode) {
                salesRepository.updateSale(sale)
            } else {
                salesRepository.addSale(sale)
            }
            _formState.value = _formState.value.copy(isSaving = false, isSaved = true)
        }
    }

    /**
     * Sales must reference a real dealer row. If the typed name isn't a selected
     * dealer (or that dealer no longer exists), reuse a dealer with the same name
     * or create a new one.
     */
    private suspend fun resolveDealerId(state: SaleFormState): Long {
        if (state.selectedDealerId != 0L &&
            dealerRepository.getDealerById(state.selectedDealerId).first() != null
        ) return state.selectedDealerId

        val name = state.dealerName.trim()
        dealerRepository.getAllDealers().first()
            .firstOrNull { it.dealerName.trim().equals(name, ignoreCase = true) }
            ?.let { return it.id }

        return dealerRepository.addDealer(
            Dealer(
                dealerName = name,
                place = state.dealerPlace.trim(),
                phone = state.dealerPhone.trim()
            )
        )
    }

    private companion object {
        const val DATE_LOCK_DAYS = 30L
    }

    private fun Double.toFormattedString(): String =
        if (this == kotlin.math.floor(this)) "%.0f".format(this) else "%.2f".format(this)
}
