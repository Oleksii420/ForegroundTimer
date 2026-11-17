package com.example.privatroom

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface PrivatBankArchiveApi {

    @GET("exchange_rates?json")
    //@GET("exchange_ratesX?json")
    suspend fun getExchangeRates(@Query("date") date: String): Response<ExchangeArchiveResponse>
}

object ArchiveNetwork {
    private const val BASE_URL = "https://api.privatbank.ua/p24api/"

    val api: PrivatBankArchiveApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PrivatBankArchiveApi::class.java)
}