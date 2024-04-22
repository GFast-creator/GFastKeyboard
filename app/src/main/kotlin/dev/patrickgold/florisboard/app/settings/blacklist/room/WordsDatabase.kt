package dev.patrickgold.florisboard.app.settings.blacklist.room

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisAppActivity
import dev.patrickgold.florisboard.app.settings.blacklist.room.CONSTANTS.CHANNEL_ID
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton


@Entity(tableName = "words")

data class Word(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    @ColumnInfo(defaultValue = "false")
    var isSelected: Boolean = false
)

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg words: Word)

    @Update
    suspend fun updateUsers(vararg words: Word)

    @Query("SELECT * FROM words ORDER BY word ASC")
    fun getAll(): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE isSelected = true")
    fun getSelected(): Flow<List<Word>>

    @Delete
    suspend fun delete(vararg word: Word)

    @Query("DELETE FROM words")
    suspend fun deleteAll()

    @Update
    suspend fun update(word:Word)
}

@Database(
    version = 1,
    entities = [Word::class],
)

abstract class AppDatabase : RoomDatabase() {
    abstract val wordDao: WordDao

    companion object{
        fun create(context: Context) : AppDatabase{
            return Room
                .databaseBuilder(context, AppDatabase::class.java, "database.db")
                .build()
        }
    }
}


object CONSTANTS {
    const val NOTIFICATION_ID = 1990
    const val CHANNEL_ID = "1991"
    const val CHANNEL_NAME = "list_app"
}

//hilt integration
@Module
@InstallIn(SingletonComponent::class)
object HiltModule {
    @Singleton
    @Provides
    fun create(@ApplicationContext context : Context) : AppDatabase {
        return AppDatabase.create(context)
    }

    @Singleton
    @Provides
    fun notificationBuilder(@ApplicationContext context: Context): NotificationCompat.Builder {

        val resultIntent = Intent(context, FlorisAppActivity::class.java)
        val resultPendingIntent = PendingIntent.getActivity(
            context, 0, resultIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("List_app")
            .setSmallIcon(R.drawable.baseline_find_in_page_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(resultPendingIntent)
            .setShowWhen(false)
            .setOngoing(true)
    }

    @Singleton
    @Provides
    fun vibrator(context: Application): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    @Singleton
    @Provides
    fun notificationManager(@ApplicationContext context: Context): NotificationManager {
        return (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        CONSTANTS.CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "channel for app"
                    }
                )
        }
    }
}
