package com.tokoreader.data.repository

import com.tokoreader.data.local.dao.PositionDao
import com.tokoreader.data.local.entity.PositionEntity
import com.tokoreader.domain.model.Position
import com.tokoreader.domain.repository.PositionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PositionRepositoryImpl(
    private val positionDao: PositionDao
) : PositionRepository {

    override fun observePosition(symbol: String): Flow<Position?> =
        positionDao.observePosition(symbol).map { entity ->
            entity?.let {
                Position(
                    symbol = it.symbol,
                    quantity = it.quantity,
                    averageEntryPrice = it.averageEntryPrice
                )
            }
        }

    override fun observeAllPositions(): Flow<List<Position>> =
        positionDao.observeAllPositions().map { list ->
            list.map { entity ->
                Position(
                    symbol = entity.symbol,
                    quantity = entity.quantity,
                    averageEntryPrice = entity.averageEntryPrice
                )
            }
        }

    override suspend fun getPosition(symbol: String): Position? {
        val entity = positionDao.getPosition(symbol) ?: return null
        return Position(
            symbol = entity.symbol,
            quantity = entity.quantity,
            averageEntryPrice = entity.averageEntryPrice
        )
    }

    override suspend fun savePosition(position: Position) {
        positionDao.insertPosition(
            PositionEntity(
                symbol = position.symbol,
                quantity = position.quantity,
                averageEntryPrice = position.averageEntryPrice
            )
        )
    }

    override suspend fun removePosition(symbol: String) {
        positionDao.deletePosition(symbol)
    }
}
