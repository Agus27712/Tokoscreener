package com.tokoreader.data.repository

import com.tokoreader.data.local.dao.LocalOrderDao
import com.tokoreader.data.local.dao.PaperAccountDao
import com.tokoreader.data.local.dao.PositionDao
import com.tokoreader.data.local.entity.LocalOrderEntity
import com.tokoreader.data.local.entity.PaperAccountEntity
import com.tokoreader.data.local.entity.PositionEntity
import com.tokoreader.domain.model.LocalOrder
import com.tokoreader.domain.model.OrderResult
import com.tokoreader.domain.model.PaperBalances
import com.tokoreader.domain.model.Position
import com.tokoreader.domain.repository.PaperTradeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

class PaperTradeRepositoryImpl(
    private val paperAccountDao: PaperAccountDao,
    private val positionDao: PositionDao,
    private val localOrderDao: LocalOrderDao
) : PaperTradeRepository {

    override fun observePaperBalance(): Flow<Double> =
        paperAccountDao.observeAccount().map { account ->
            account?.balanceIdr ?: 100_000_000.0
        }

    override fun observePaperBalances(): Flow<PaperBalances> =
        paperAccountDao.observeAccount().map { account ->
            PaperBalances(
                balanceIdr = account?.balanceIdr ?: 100_000_000.0,
                balanceUsdt = account?.balanceUsdt ?: 5_000.0
            )
        }

    override fun observePaperPositions(): Flow<List<Position>> =
        positionDao.observeAllPositions().map { list ->
            list.map { entity ->
                Position(
                    symbol = entity.symbol,
                    quantity = entity.quantity,
                    averageEntryPrice = entity.averageEntryPrice
                )
            }
        }

    override fun observePaperOrders(): Flow<List<LocalOrder>> =
        localOrderDao.observeOrders(isPaper = true).map { list ->
            list.map { entity ->
                LocalOrder(
                    id = entity.id,
                    orderId = entity.orderId,
                    symbol = entity.symbol,
                    side = entity.side,
                    type = entity.type,
                    price = entity.price,
                    quantity = entity.quantity,
                    totalAmount = entity.totalAmount,
                    isPaper = entity.isPaper,
                    timestamp = entity.timestamp,
                    status = entity.status
                )
            }
        }

    private fun isUsdtQuote(symbol: String): Boolean {
        val upper = symbol.uppercase(Locale.ROOT)
        return upper.endsWith("USDT") || upper.endsWith("USDC") || upper.endsWith("BUSD")
    }

    override suspend fun executeBuy(symbol: String, amount: Double, price: Double): Result<OrderResult> {
        if (price <= 0.0 || amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Harga atau nominal order tidak valid"))
        }

        var account = paperAccountDao.getAccount()
        if (account == null) {
            account = PaperAccountEntity(id = 1, balanceIdr = 100_000_000.0, balanceUsdt = 5_000.0)
            paperAccountDao.insertOrUpdate(account)
        }

        val isUsdt = isUsdtQuote(symbol)
        val fee = amount * 0.001 // 0.1% Tokocrypto trading fee
        val totalCost = amount + fee

        if (isUsdt) {
            if (account.balanceUsdt < totalCost) {
                return Result.failure(
                    IllegalStateException(
                        "Saldo USDT tidak cukup (Perlu: $${String.format(Locale.US, "%.2f", totalCost)}, Ada: $${String.format(Locale.US, "%.2f", account.balanceUsdt)}). Konversi saldo IDR ke USDT terlebih dahulu!"
                    )
                )
            }
            val updatedAccount = account.copy(balanceUsdt = account.balanceUsdt - totalCost)
            paperAccountDao.insertOrUpdate(updatedAccount)
        } else {
            if (account.balanceIdr < totalCost) {
                return Result.failure(
                    IllegalStateException(
                        "Saldo IDR tidak cukup (Perlu: Rp ${totalCost.toLong()}, Ada: Rp ${account.balanceIdr.toLong()})"
                    )
                )
            }
            val updatedAccount = account.copy(balanceIdr = account.balanceIdr - totalCost)
            paperAccountDao.insertOrUpdate(updatedAccount)
        }

        val buyQty = amount / price

        // Update position
        val existing = positionDao.getPosition(symbol)
        if (existing == null || existing.quantity <= 0.0) {
            positionDao.insertPosition(
                PositionEntity(
                    symbol = symbol,
                    quantity = buyQty,
                    averageEntryPrice = price
                )
            )
        } else {
            val totalQty = existing.quantity + buyQty
            val weightedEntryPrice = ((existing.quantity * existing.averageEntryPrice) + (buyQty * price)) / totalQty
            positionDao.insertPosition(
                PositionEntity(
                    symbol = symbol,
                    quantity = totalQty,
                    averageEntryPrice = weightedEntryPrice
                )
            )
        }

        // Catat order
        val orderId = "SIM-BUY-${System.currentTimeMillis() % 1000000}"
        localOrderDao.insertOrder(
            LocalOrderEntity(
                orderId = orderId,
                symbol = symbol,
                side = "BUY",
                type = "MARKET",
                price = price,
                quantity = buyQty,
                totalAmount = amount,
                isPaper = true,
                timestamp = System.currentTimeMillis(),
                status = "FILLED"
            )
        )

        return Result.success(OrderResult(orderId = orderId, status = "FILLED"))
    }

    override suspend fun executeSell(symbol: String, quantity: Double, price: Double): Result<OrderResult> {
        if (price <= 0.0 || quantity <= 0.0) {
            return Result.failure(IllegalArgumentException("Harga atau kuantitas jual tidak valid"))
        }

        val existing = positionDao.getPosition(symbol)
            ?: return Result.failure(IllegalStateException("Tidak ada posisi hold untuk $symbol"))

        if (existing.quantity < quantity * 0.999) { // tolerance for floating point
            return Result.failure(IllegalStateException("Kuantitas jual melebihi aset yang dimiliki (${existing.quantity})"))
        }

        val grossProceeds = quantity * price
        val fee = grossProceeds * 0.001
        val netProceeds = grossProceeds - fee

        val isUsdt = isUsdtQuote(symbol)

        // Update Saldo
        var account = paperAccountDao.getAccount()
        if (account == null) {
            account = PaperAccountEntity(id = 1, balanceIdr = 100_000_000.0, balanceUsdt = 5_000.0)
        }

        val updatedAccount = if (isUsdt) {
            account.copy(balanceUsdt = account.balanceUsdt + netProceeds)
        } else {
            account.copy(balanceIdr = account.balanceIdr + netProceeds)
        }
        paperAccountDao.insertOrUpdate(updatedAccount)

        // Update/Delete position
        val remainingQty = existing.quantity - quantity
        if (remainingQty <= 0.00000001) {
            positionDao.deletePosition(symbol)
        } else {
            positionDao.insertPosition(
                existing.copy(quantity = remainingQty)
            )
        }

        // Catat order
        val orderId = "SIM-SELL-${System.currentTimeMillis() % 1000000}"
        localOrderDao.insertOrder(
            LocalOrderEntity(
                orderId = orderId,
                symbol = symbol,
                side = "SELL",
                type = "MARKET",
                price = price,
                quantity = quantity,
                totalAmount = grossProceeds,
                isPaper = true,
                timestamp = System.currentTimeMillis(),
                status = "FILLED"
            )
        )

        return Result.success(OrderResult(orderId = orderId, status = "FILLED"))
    }

    override suspend fun swapPaperCurrency(
        fromAsset: String,
        toAsset: String,
        amount: Double,
        rateUsdtIdr: Double
    ): Result<Boolean> {
        if (amount <= 0.0 || rateUsdtIdr <= 0.0) {
            return Result.failure(IllegalArgumentException("Nominal konversi atau kurs tidak valid"))
        }

        var account = paperAccountDao.getAccount()
        if (account == null) {
            account = PaperAccountEntity(id = 1, balanceIdr = 100_000_000.0, balanceUsdt = 5_000.0)
        }

        if (fromAsset.equals("IDR", ignoreCase = true) && toAsset.equals("USDT", ignoreCase = true)) {
            if (account.balanceIdr < amount) {
                return Result.failure(IllegalStateException("Saldo IDR tidak cukup untuk konversi ke USDT"))
            }
            val obtainedUsdt = amount / rateUsdtIdr
            val updated = account.copy(
                balanceIdr = account.balanceIdr - amount,
                balanceUsdt = account.balanceUsdt + obtainedUsdt
            )
            paperAccountDao.insertOrUpdate(updated)
            return Result.success(true)
        } else if (fromAsset.equals("USDT", ignoreCase = true) && toAsset.equals("IDR", ignoreCase = true)) {
            if (account.balanceUsdt < amount) {
                return Result.failure(IllegalStateException("Saldo USDT tidak cukup untuk konversi ke IDR"))
            }
            val obtainedIdr = amount * rateUsdtIdr
            val updated = account.copy(
                balanceUsdt = account.balanceUsdt - amount,
                balanceIdr = account.balanceIdr + obtainedIdr
            )
            paperAccountDao.insertOrUpdate(updated)
            return Result.success(true)
        }

        return Result.failure(IllegalArgumentException("Pasangan konversi tidak didukung"))
    }

    override suspend fun resetPaperAccount() {
        paperAccountDao.insertOrUpdate(
            PaperAccountEntity(id = 1, balanceIdr = 100_000_000.0, balanceUsdt = 5_000.0)
        )
        positionDao.clearAllPositions()
        localOrderDao.clearPaperOrders()
    }
}

