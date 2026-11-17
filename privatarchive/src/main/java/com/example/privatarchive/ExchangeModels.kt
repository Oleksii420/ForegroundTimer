package com.example.privatarchive

data class ExchangeArchiveResponse(
    val date: String?,
    val bank: String?,
    val baseCurrencyLit: String?,
    val exchangeRate: List<ExchangeRateItem>?
)


data class ExchangeRateItem(
    val baseCurrency: String?,
    val currency: String?,
    val purchaseRate: Double?,     // ПриватБанк купує
    val saleRate: Double?,         // ПриватБанк продає
    val purchaseRateNB: Double?,   // НБУ, окремо
    val saleRateNB: Double?
)