package com.sabid.financetracker

data class Transaction(
    val id: Int,
    val date: String,
    val accountId: Int,
    val accountName: String,
    val transactionType: Int,
    val transactionTypeName: String,
    val amount: Double,
    val donatePercent: Double,
    val narration: String
)
