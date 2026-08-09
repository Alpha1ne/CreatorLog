package com.voiceofmelody.songdailytracker.data.local

import androidx.room.*
import com.voiceofmelody.songdailytracker.data.model.Promotion
import kotlinx.coroutines.flow.Flow

@Dao
interface PromotionDao {
    @Query("SELECT * FROM promotions ORDER BY createdAt DESC")
    fun getAllPromotions(): Flow<List<Promotion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotion(promotion: Promotion)

    @Update
    suspend fun updatePromotion(promotion: Promotion)

    @Delete
    suspend fun deletePromotion(promotion: Promotion)

    @Query("SELECT * FROM promotions WHERE promotionTitle LIKE :query OR client LIKE :query OR notes LIKE :query ORDER BY createdAt DESC")
    fun searchPromotions(query: String): Flow<List<Promotion>>
}
