package org.koitharu.kotatsu.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import org.intellij.lang.annotations.Language
import org.koitharu.kotatsu.core.db.entity.MangaSourceEntity
import org.koitharu.kotatsu.explore.data.SourcesSortOrder

@Dao
abstract class MangaSourcesDao {

	@Query("SELECT * FROM sources ORDER BY sort_key")
	abstract suspend fun findAll(): List<MangaSourceEntity>

	@Query("SELECT source FROM sources WHERE enabled = 1")
	abstract suspend fun findAllEnabledNames(): List<String>

	@Query("SELECT * FROM sources ORDER BY sort_key")
	abstract fun observeAll(): Flow<List<MangaSourceEntity>>

	@Query("SELECT enabled FROM sources WHERE source = :source")
	abstract fun observeIsEnabled(source: String): Flow<Boolean>

	@Query("SELECT IFNULL(MAX(sort_key),0) FROM sources")
	abstract suspend fun getMaxSortKey(): Int

	@Query("UPDATE sources SET enabled = 0")
	abstract suspend fun disableAllSources()

	@Query("UPDATE sources SET sort_key = :sortKey WHERE source = :source")
	abstract suspend fun setSortKey(source: String, sortKey: Int)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	@Transaction
	abstract suspend fun insertIfAbsent(entries: Collection<MangaSourceEntity>)

	@Upsert
	abstract suspend fun upsert(entry: MangaSourceEntity)

	fun observeEnabled(order: SourcesSortOrder): Flow<List<MangaSourceEntity>> {
		val orderBy = getOrderBy(order)

		@Language("RoomSql")
		val query = SimpleSQLiteQuery("SELECT * FROM sources WHERE enabled = 1 ORDER BY $orderBy")
		return observeImpl(query)
	}

	suspend fun findAllEnabled(order: SourcesSortOrder): List<MangaSourceEntity> {
		val orderBy = getOrderBy(order)

		@Language("RoomSql")
		val query = SimpleSQLiteQuery("SELECT * FROM sources WHERE enabled = 1 ORDER BY $orderBy")
		return findAllImpl(query)
	}

	@Transaction
	open suspend fun setEnabled(source: String, isEnabled: Boolean) {
		if (updateIsEnabled(source, isEnabled) == 0) {
			val entity = MangaSourceEntity(
				source = source,
				isEnabled = isEnabled,
				sortKey = getMaxSortKey() + 1,
			)
			upsert(entity)
		}
	}

	@Query("UPDATE sources SET enabled = :isEnabled WHERE source = :source")
	protected abstract suspend fun updateIsEnabled(source: String, isEnabled: Boolean): Int

	@RawQuery(observedEntities = [MangaSourceEntity::class])
	protected abstract fun observeImpl(query: SupportSQLiteQuery): Flow<List<MangaSourceEntity>>

	@RawQuery
	protected abstract suspend fun findAllImpl(query: SupportSQLiteQuery): List<MangaSourceEntity>

	private fun getOrderBy(order: SourcesSortOrder) = when (order) {
		SourcesSortOrder.ALPHABETIC -> "source ASC"
		SourcesSortOrder.POPULARITY -> "(SELECT COUNT(*) FROM manga WHERE source = sources.source) DESC"
		SourcesSortOrder.MANUAL -> "sort_key ASC"
	}
}
