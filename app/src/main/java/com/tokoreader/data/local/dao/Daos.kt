package com.tokoreader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tokoreader.data.local.entity.PositionEntity
import com.tokoreader.data.local.entity.SignalHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PositionDao {
    @Query("SELECT * FROM positions WHERE symbol = :symbol")
    fun observePosition(symbol: String): Flow<PositionEntity?>

    @Query("SELECT * FROM positions")
    fun observeAllPositions(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE symbol = :symbol")
    suspend fun getPosition(symbol: String): PositionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: PositionEntity)
    
    @Query("DELETE FROM positions WHERE symbol = :symbol")
    suspend fun deletePosition(symbol: String)

    @Query("DELETE FROM positions")
    suspend fun clearAllPositions()
}

@Dao
interface SignalHistoryDao {
    @Query("SELECT * FROM signal_history ORDER BY timestamp DESC LIMIT 50")
    fun observeRecentSignals(): Flow<List<SignalHistoryEntity>>
    
    @Insert
    suspend fun insertSignal(signal: SignalHistoryEntity)
}

@Dao
interface LocalOrderDao {
    @Query("SELECT * FROM local_orders WHERE isPaper = :isPaper ORDER BY timestamp DESC LIMIT 50")
    fun observeOrders(isPaper: Boolean): Flow<List<com.tokoreader.data.local.entity.LocalOrderEntity>>

    @Query("SELECT * FROM local_orders ORDER BY timestamp DESC LIMIT 50")
    fun observeAllOrders(): Flow<List<com.tokoreader.data.local.entity.LocalOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: com.tokoreader.data.local.entity.LocalOrderEntity)

    @Query("DELETE FROM local_orders WHERE isPaper = 1")
    suspend fun clearPaperOrders()
}

@Dao
interface PaperAccountDao {
    @Query("SELECT * FROM paper_account WHERE id = 1")
    fun observeAccount(): Flow<com.tokoreader.data.local.entity.PaperAccountEntity?>

    @Query("SELECT * FROM paper_account WHERE id = 1")
    suspend fun getAccount(): com.tokoreader.data.local.entity.PaperAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(account: com.tokoreader.data.local.entity.PaperAccountEntity)
}

