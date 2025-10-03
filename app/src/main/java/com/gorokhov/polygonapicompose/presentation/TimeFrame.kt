package com.gorokhov.polygonapicompose.presentation

enum class TimeFrame(
    val label: String,
    val value: String,

) {
    MIN_5 ("M5", "5/minute"),
    MIN_15 ("M15", "15/minute"),
    MIN_30 ("M30", "30/minute"),
    HOUR_1 ("H1", "1/hour")
}

// Тут нужно использовать mapper. Сущность domain в data слое не используем