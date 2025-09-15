package com.gorokhov.polygonapicompose.data

import retrofit2.http.GET

interface ApiService {

    @GET("aggs/ticker/AAPL/range/1/hour/2022-01-09/2024-02-10?adjusted=true&sort=asc&limit=50000&apiKey=uqug7EHnfh8KTUu_RJeyje0JILWLRKRN")
    suspend fun loadBars(): Result
}