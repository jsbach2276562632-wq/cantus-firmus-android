package de.orgeljahr.demo.data.catalog

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import de.orgeljahr.demo.BuildConfig

@Database(
    entities = [
        CatalogMetadataEntity::class,
        SourceEntity::class,
        HymnalEditionEntity::class,
        HymnEntity::class,
        HymnTextEntity::class,
        HymnVerseEntity::class,
        MelodyEntity::class,
        HymnMelodyCrossRef::class,
        PersonEntity::class,
        HymnContributorCrossRef::class,
        MusicalWorkEntity::class,
        WorkMelodyCrossRef::class,
        LiturgicalOccasionEntity::class,
        LiturgicalDateRuleEntity::class,
        LectionaryEntity::class,
        SermonReadingEntity::class,
        OccasionHymnEntity::class,
        OccasionHymnLinkEntity::class,
        HymnSearchEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class CatalogDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao

    companion object {
        const val DATABASE_NAME = "orgeljahr-catalog.db"
        const val EG_1993_CORE_ID = "eg1993-stamm"

        @Volatile private var instance: CatalogDatabase? = null

        fun get(context: Context): CatalogDatabase = instance ?: synchronized(this) {
            val appContext = context.applicationContext
            instance ?: run {
                val installPreferences = appContext.getSharedPreferences("catalog_install", Context.MODE_PRIVATE)
                val bundledCatalogVersion = appContext.assets
                    .open("catalog/catalog-version.txt")
                    .bufferedReader()
                    .use { it.readLine() }
                if (
                    installPreferences.getInt("apk_version", -1) != BuildConfig.VERSION_CODE ||
                    installPreferences.getString("content_version", null) != bundledCatalogVersion
                ) {
                    appContext.deleteDatabase(DATABASE_NAME)
                }
                val database = Room.databaseBuilder(
                    appContext,
                    CatalogDatabase::class.java,
                    DATABASE_NAME
                )
                    .createFromAsset("catalog/orgeljahr-catalog.db")
                    // Catalog data is replaceable; user services and attachments live elsewhere.
                    .fallbackToDestructiveMigration()
                    .build()
                database.openHelper.writableDatabase
                installPreferences.edit()
                    .putInt("apk_version", BuildConfig.VERSION_CODE)
                    .putString("content_version", bundledCatalogVersion)
                    .apply()
                database
            }.also { instance = it }
        }
    }
}
