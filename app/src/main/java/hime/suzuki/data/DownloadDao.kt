package hime.suzuki.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED','RUNNING') ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status IN ('DONE','FAILED','CANCELED') ORDER BY finishedAt DESC")
    fun observeFinished(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status = 'DONE' ORDER BY finishedAt DESC LIMIT 3")
    fun observeRecent(): Flow<List<DownloadItem>>

    @Insert
    suspend fun insertAll(items: List<DownloadItem>): List<Long>

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'RUNNING'")
    suspend fun runningCount(): Int

    @Query("SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY createdAt ASC LIMIT 1")
    suspend fun nextQueued(): DownloadItem?

    @Query("UPDATE downloads SET status='RUNNING', progress=0, etaSeconds=0, errorMessage=NULL, finishedAt=NULL WHERE id=:id AND status='QUEUED'")
    suspend fun tryStart(id: Long): Int

    @Query("UPDATE downloads SET progress=:progress, etaSeconds=:eta WHERE id=:id AND status='RUNNING'")
    suspend fun setProgress(id: Long, progress: Float, eta: Long)

    @Query("UPDATE downloads SET status='DONE', progress=1.0, etaSeconds=0, filePath=:path, sizeBytes=:size, finishedAt=:time, errorMessage=NULL WHERE id=:id")
    suspend fun setDone(id: Long, path: String?, size: Long, time: Long)

    @Query("UPDATE downloads SET status=:status, errorMessage=:error, finishedAt=:time WHERE id=:id")
    suspend fun setStatus(id: Long, status: String, error: String?, time: Long?)

    @Query("UPDATE downloads SET status='QUEUED', progress=0, etaSeconds=0, errorMessage=NULL, finishedAt=NULL WHERE id=:id")
    suspend fun requeue(id: Long)

    @Query("UPDATE downloads SET status='QUEUED', progress=0, etaSeconds=0, errorMessage=NULL, finishedAt=NULL WHERE status IN ('FAILED','CANCELED')")
    suspend fun requeueFinished()

    @Query("UPDATE downloads SET status='CANCELED', finishedAt=:time WHERE status='QUEUED'")
    suspend fun cancelQueued(time: Long): Int

    @Query("UPDATE downloads SET status='QUEUED', progress=0, etaSeconds=0 WHERE status='RUNNING'")
    suspend fun resetRunning(): Int

    @Query("SELECT COUNT(*) FROM downloads WHERE status='QUEUED'")
    suspend fun queuedCount(): Int

    @Query("DELETE FROM downloads WHERE id=:id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM downloads WHERE status IN ('DONE','FAILED','CANCELED')")
    suspend fun clearFinished()
}
