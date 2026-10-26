package com.example.airssreader.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.airssreader.data.local.dao.ArticleDao
import com.example.airssreader.data.local.dao.FeedDao
import com.example.airssreader.data.local.entity.ArticleEntity
import com.example.airssreader.data.local.entity.FeedEntity

/**
 * 应用唯一的本地数据库。
 *
 * 两张表：feeds（订阅源）与 articles（缓存的资讯条目），
 * 通过外键级联删除建立从属关系。Room 默认开启外键约束，
 * 因此删除源时其条目会被一并清理。
 */
@Database(
    entities = [FeedEntity::class, ArticleEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun feedDao(): FeedDao

    abstract fun articleDao(): ArticleDao

    companion object {
        private const val DB_NAME = "ai_rss.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME,
                )
                    // 课程设计阶段表结构可能调整，破坏性迁移可接受；正式产品应写 Migration
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
