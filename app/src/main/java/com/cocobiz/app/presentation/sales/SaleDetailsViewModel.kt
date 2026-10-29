package com.cocobiz.app.presentation.sales

import androidx.lifecycle.ViewModel
import com.cocobiz.app.domain.model.SalesEntry
import com.cocobiz.app.domain.repository.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SaleDetailsViewModel @Inject constructor(
    private val salesRepository: SalesRepository
) : ViewModel() {

    /** Latest earlier sale of the same coconut type that has a market rate, or null. */
    suspend fun previousMarketRateSale(sale: SalesEntry): SalesEntry? =
        salesRepository.getPreviousMarketRateSale(sale)
}
