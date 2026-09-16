package com.tokoreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "positions")
data class PositionEntity(
    @PrimaryKey val symbol: String,
    val quantity: Double,
    val averageEntryPrice: Double
)

@Entity(tableName = "signal_history")
data class SignalHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val signalType: String,
    val reason: String,
    val price: Double,
    val timestamp: Long
)

@Entity(tableName = "local_orders")
data class LocalOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: String,
    val symbol: String,
    val side: String, // BUY or SELL
    val type: String,
    val price: Double,
    val quantity: Double,
    val totalAmount: Double,
    val isPaper: Boolean,
    val timestamp: Long,
    val status: String
)

@Entity(tableName = "paper_account")
data class PaperAccountEntity(
    @PrimaryKey val id: Int = 1,
    val balanceIdr: Double = 100_000_000.0,
    val balanceUsdt: Double = 10_000.0
)

