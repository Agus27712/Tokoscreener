package com.tokoreader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tokoreader.data.local.dao.LocalOrderDao
import com.tokoreader.data.local.dao.PaperAccountDao
import com.tokoreader.data.local.dao.PositionDao
import com.tokoreader.data.local.dao.SignalHistoryDao
import com.tokoreader.data.local.entity.LocalOrderEntity
import com.tokoreader.data.local.entity.PaperAccountEntity
import com.tokoreader.data.local.entity.PositionEntity
import com.tokoreader.data.local.entity.SignalHistoryEntity

@Database(
    entities = [
        PositionEntity::class,
        SignalHistoryEntity::class,
        LocalOrderEntity::class,
        PaperAccountEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TokoDatabase : RoomDatabase() {
    abstract fun positionDao(): PositionDao
    abstract fun signalHistoryDao(): SignalHistoryDao
    abstract fun localOrderDao(): LocalOrderDao
    abstract fun paperAccountDao(): PaperAccountDao
}
