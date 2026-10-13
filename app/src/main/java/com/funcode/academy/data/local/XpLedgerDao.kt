package com.funcode.academy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface XpLedgerDao {

    /**
     * Inserta un registro de XP. Si la actividad ya tenía XP, el índice único
     * hace que se ignore y devuelve -1.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: XpLedgerEntity): Long

    /** Suma de todo el XP registrado; emite un valor nuevo cada vez que cambia la tabla. */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_ledger")
    fun observeTotalXp(): Flow<Int>

    /** Todos los movimientos, del más reciente al más antiguo. */
    @Query("SELECT * FROM xp_ledger ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<XpLedgerEntity>>
}
