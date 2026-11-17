package com.example.privatarchive

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface PrivatBankArchiveApi {

    // GET https://api.privatbank.ua/p24api/exchange_rates?json&date=01.11.2026
    @GET("exchange_rates?json")
    fun getExchangeRates(@Query("date") date: String): Call<ExchangeArchiveResponse>
}

object ArchiveNetwork {
    private const val BASE_URL = "https://api.privatbank.ua/p24api/"

    val api: PrivatBankArchiveApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PrivatBankArchiveApi::class.java)
}