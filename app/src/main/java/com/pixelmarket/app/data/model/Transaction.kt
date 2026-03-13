package com.pixelmarket.app.data.model

import com.google.firebase.Timestamp

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val type: String = "", // CREDIT, DEBIT
    val amount: Double = 0.0,
    val description: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val status: String = "COMPLETED" // COMPLETED, PENDING, FAILED
)
