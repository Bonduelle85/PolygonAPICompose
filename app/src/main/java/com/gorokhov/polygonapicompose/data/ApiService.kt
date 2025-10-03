package com.gorokhov.polygonapicompose.data

import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    @GET("aggs/ticker/AAPL/range/{timeframe}/2022-01-09/2024-02-10?adjusted=true&sort=desc&limit=50000&apiKey=uqug7EHnfh8KTUu_RJeyje0JILWLRKRN")
    suspend fun loadBars(
        @Path("timeframe") timeFrame: String
    ): Result
}