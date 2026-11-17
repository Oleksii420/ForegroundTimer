package com.example.privatroom

data class ExchangeArchiveResponse(
    val date: String?,
    val bank: String?,
    val baseCurrencyLit: String?,
    val exchangeRate: List<ExchangeRateItem>?
)

data class ExchangeRateItem(
    val baseCurrency: String?,
    val currency: String?,
    val purchaseRate: Double?,
    val saleRate: Double?,
    val purchaseRateNB: Double?,
    val saleRateNB: Double?
)