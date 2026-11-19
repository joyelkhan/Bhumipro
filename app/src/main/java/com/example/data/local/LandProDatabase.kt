package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        DraftProjectEntity::class,
        PinnedToolEntity::class,
        ImportedAssetEntity::class,
        ProjectSnapshotEntity::class,
        BoundaryDocumentEntity::class,
        ConversionHistoryEntity::class,
        ExportRecordEntity::class,
        MouzaLibraryEntity::class,
        PrintPresetEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class LandProDatabase : RoomDatabase() {
    abstract fun draftProjectDao(): DraftProjectDao
    abstract fun pinnedToolDao(): PinnedToolDao
    abstract fun importedAssetDao(): ImportedAssetDao
    abstract fun projectSnapshotDao(): ProjectSnapshotDao
    abstract fun boundaryDocumentDao(): BoundaryDocumentDao
    abstract fun conversionHistoryDao(): ConversionHistoryDao
    abstract fun exportRecordDao(): ExportRecordDao
    abstract fun mouzaLibraryDao(): MouzaLibraryDao
    abstract fun printPresetDao(): PrintPresetDao

    companion object {
        const val DATABASE_NAME = "landpro_surveying.db"

        @Volatile
        private var INSTANCE: LandProDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN scaleId TEXT NOT NULL DEFAULT 'scale_16'")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN scanDpi INTEGER NOT NULL DEFAULT 300")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN feetPerCanvasUnit REAL NOT NULL DEFAULT 0.55")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN sqFeetPerShotok REAL NOT NULL DEFAULT 435.6")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN ownerName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN district TEXT NOT NULL DEFAULT 'চট্টগ্রাম'")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN upazila TEXT NOT NULL DEFAULT 'রাউজান'")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN jlNumber TEXT NOT NULL DEFAULT '৩৫'")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN khatianNo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN dagNo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN surveyDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN referenceCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN layersJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN status TEXT NOT NULL DEFAULT 'সক্রিয়'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN calibrationRmsePx REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN isCalibrated INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN calibrationMethod TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN calibrationPixelDistance REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN calibrationGroundFeet REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN calibrationUpdatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN selectedPlotIdsJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE draft_projects ADD COLUMN canvasSettingsJson TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mouza_library_items (
                        mapKey TEXT NOT NULL PRIMARY KEY,
                        divisionId TEXT NOT NULL,
                        divisionNameBn TEXT NOT NULL,
                        districtId TEXT NOT NULL,
                        districtNameBn TEXT NOT NULL,
                        upazilaNameBn TEXT NOT NULL,
                        mouzaNameBn TEXT NOT NULL,
                        jlNumber TEXT NOT NULL,
                        sheetNumber TEXT NOT NULL DEFAULT '১',
                        surveyType TEXT NOT NULL DEFAULT 'R.S',
                        scaleId TEXT NOT NULL DEFAULT 'scale_16',
                        scanDpi INTEGER NOT NULL DEFAULT 300,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        isOfflineSaved INTEGER NOT NULL DEFAULT 1,
                        lastOpenedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mouza_library_items_divisionId_districtId ON mouza_library_items(divisionId, districtId)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS print_presets (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        configJson TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        fun allMigrations(): Array<Migration> = arrayOf(
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7
        )

        fun getInstance(context: Context): LandProDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LandProDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*allMigrations())
                    .addCallback(SeedCallback())
                    .build()
                    .also { db ->
                        INSTANCE = db
                        CoroutineScope(Dispatchers.IO).launch {
                            seedDefaultsIfEmpty(db)
                        }
                    }
            }
        }

        fun createInMemory(context: Context): LandProDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                LandProDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }

        suspend fun seedDefaultsIfEmpty(db: LandProDatabase) {
            if (db.draftProjectDao().getById("draft_raujan_4") == null && db.draftProjectDao().count() == 0) {
                val now = System.currentTimeMillis()
                db.draftProjectDao().upsert(
                    DraftProjectEntity(
                        id = "draft_raujan_4",
                        title = "35-PASCHIM RAUJAN-4_result_rev",
                        mouzaName = "৩৫-পশ্চিম রাউজান-৪",
                        scaleText = "১৬ ইঞ্চি",
                        plotsJson = "[]",
                        activePlotIndex = 0,
                        totalAreaShotangsho = 0.0,
                        updatedAt = now,
                        scaleId = "scale_16",
                        scanDpi = 300,
                        feetPerCanvasUnit = 0.55,
                        sqFeetPerShotok = 435.6,
                        district = "চট্টগ্রাম",
                        upazila = "রাউজান",
                        jlNumber = "৩৫",
                        isCalibrated = 1,
                        calibrationMethod = "PRESET_SCALE_DPI",
                        status = "সক্রিয়",
                        selectedPlotIdsJson = "[]"
                    )
                )
            }

            if (db.pinnedToolDao().list().isEmpty()) {
                db.pinnedToolDao().upsertAll(defaultPinnedTools())
            }

            val catalog = defaultMouzaCatalog()
            if (db.mouzaLibraryDao().offlineSavedCount() < catalog.size) {
                db.mouzaLibraryDao().upsertAll(catalog)
            }
        }

        fun defaultPinnedTools(): List<PinnedToolEntity> = listOf(
            PinnedToolEntity(
                id = "tool_mouza_survey",
                titleBn = "মৌজা ম্যাপে জমি পরিমাপ",
                subtitleBn = "জমি মাপুন, ভাগবণ্টন করুন সুন্দর ভাবে",
                route = "workspace/draft_raujan_4?mode=mouza_survey",
                iconKey = "MOUZA_SURVEY",
                accentHex = "#2563EB",
                containerHex = "#EFF6FF",
                sortOrder = 1,
                isPinned = true
            ),
            PinnedToolEntity(
                id = "tool_ana_gonda",
                titleBn = "আনা-গন্ডা ক্যালকুলেটর",
                subtitleBn = "খতিয়ান অংশ ও জমি হিসাব",
                route = "ana_gonda",
                iconKey = "ANA_GONDA",
                accentHex = "#0D9488",
                containerHex = "#F0FDFA",
                sortOrder = 2,
                isPinned = true
            ),
            PinnedToolEntity(
                id = "tool_unit_converter",
                titleBn = "জমি একক রূপান্তর",
                subtitleBn = "শতক, কাঠা, বিঘা, একর, বর্গফুট",
                route = "unit_converter",
                iconKey = "UNIT_CONVERTER",
                accentHex = "#EA580C",
                containerHex = "#FFF7ED",
                sortOrder = 3,
                isPinned = true
            ),
            PinnedToolEntity(
                id = "tool_compass",
                titleBn = "ডিজিটাল কম্পাস",
                subtitleBn = "মাঠ জরিপের দিক নির্ণয়",
                route = "compass",
                iconKey = "COMPASS",
                accentHex = "#7C3AED",
                containerHex = "#F5F3FF",
                sortOrder = 4,
                isPinned = true
            ),
            PinnedToolEntity(
                id = "tool_mouza_library",
                titleBn = "মৌজা ম্যাপ লাইব্রেরি",
                subtitleBn = "অফলাইন সংরক্ষিত ম্যাপ ক্যাটালগ",
                route = "mouza_library",
                iconKey = "MOUZA_LIBRARY",
                accentHex = "#16A34A",
                containerHex = "#F0FDF4",
                sortOrder = 5,
                isPinned = true
            )
        )

        fun defaultMouzaCatalog(): List<MouzaLibraryEntity> = listOf(
            MouzaLibraryEntity(
                mapKey = "khl_dumuria_18_1",
                divisionId = "khl",
                divisionNameBn = "খুলনা",
                districtId = "khulna",
                districtNameBn = "খুলনা",
                upazilaNameBn = "ডুমুরিয়া",
                mouzaNameBn = "১৮-ডুমুরিয়া সদর-১",
                jlNumber = "১৮",
                sheetNumber = "১",
                surveyType = "R.S",
                scaleId = "scale_16",
                scanDpi = 300,
                isFavorite = false,
                isOfflineSaved = true
            ),
            MouzaLibraryEntity(
                mapKey = "khl_jessore_21_2",
                divisionId = "khl",
                divisionNameBn = "খুলনা",
                districtId = "jashore",
                districtNameBn = "যশোর",
                upazilaNameBn = "সদর",
                mouzaNameBn = "২১-উপশহর-২",
                jlNumber = "২১",
                sheetNumber = "২",
                surveyType = "S.A",
                scaleId = "scale_16",
                scanDpi = 300,
                isFavorite = false,
                isOfflineSaved = true
            ),
            MouzaLibraryEntity(
                mapKey = "ctg_raujan_35_4",
                divisionId = "ctg",
                divisionNameBn = "চট্টগ্রাম",
                districtId = "chattogram",
                districtNameBn = "চট্টগ্রাম",
                upazilaNameBn = "রাউজান",
                mouzaNameBn = "৩৫-পশ্চিম রাউজান-৪",
                jlNumber = "৩৫",
                sheetNumber = "৪",
                surveyType = "R.S",
                scaleId = "scale_16",
                scanDpi = 300,
                isFavorite = true,
                isOfflineSaved = true
            ),
            MouzaLibraryEntity(
                mapKey = "ctg_halishahar_09_1",
                divisionId = "ctg",
                divisionNameBn = "চট্টগ্রাম",
                districtId = "chattogram",
                districtNameBn = "চট্টগ্রাম",
                upazilaNameBn = "হালিশহর",
                mouzaNameBn = "০৯-উত্তর হালিশহর-১",
                jlNumber = "০৯",
                sheetNumber = "১",
                surveyType = "B.S",
                scaleId = "scale_32",
                scanDpi = 300,
                isFavorite = false,
                isOfflineSaved = true
            ),
            MouzaLibraryEntity(
                mapKey = "dhk_savar_12_1",
                divisionId = "dhk",
                divisionNameBn = "ঢাকা",
                districtId = "dhaka",
                districtNameBn = "ঢাকা",
                upazilaNameBn = "সাভার",
                mouzaNameBn = "১২-বিরুলিয়া-১",
                jlNumber = "১২",
                sheetNumber = "১",
                surveyType = "B.R.S",
                scaleId = "scale_16",
                scanDpi = 300,
                isFavorite = false,
                isOfflineSaved = true
            ),
            MouzaLibraryEntity(
                mapKey = "dhk_gazipur_07_3",
                divisionId = "dhk",
                divisionNameBn = "ঢাকা",
                districtId = "gazipur",
                districtNameBn = "গাজীপুর",
                upazilaNameBn = "কালিয়াকৈর",
                mouzaNameBn = "০৭-সফিপুর-৩",
                jlNumber = "০৭",
                sheetNumber = "৩",
                surveyType = "R.S",
                scaleId = "scale_16",
                scanDpi = 300,
                isFavorite = false,
                isOfflineSaved = true
            ),
            MouzaLibraryEntity(
                mapKey = "bar_bakergonj_14_1",
                divisionId = "bar",
                divisionNameBn = "বরিশাল",
                districtId = "barishal",
                districtNameBn = "বরিশাল",
                upazilaNameBn = "বাকেরগঞ্জ",
                mouzaNameBn = "১৪-চরামদ্দি-১",
                jlNumber = "১৪",
                sheetNumber = "১",
                surveyType = "S.A",
                scaleId = "scale_16",
                scanDpi = 300,
                isFavorite = false,
                isOfflineSaved = true
            ),
            MouzaLibraryEntity(
                mapKey = "mym_trishal_27_2",
                divisionId = "mym",
                divisionNameBn = "ময়মনসিংহ",
                districtId = "mymensingh",
                districtNameBn = "ময়মনসিংহ",
                upazilaNameBn = "ত্রিশাল",
                mouzaNameBn = "২৭-বৈলর-২",
                jlNumber = "২৭",
                sheetNumber = "২",
                surveyType = "R.S",
                scaleId = "scale_16",
                scanDpi = 300,
                isFavorite = false,
                isOfflineSaved = true
            ),
            MouzaLibraryEntity(
                mapKey = "rng_mithapukur_31_1",
                divisionId = "rng",
                divisionNameBn = "রংপুর",
                districtId = "rangpur",
                districtNameBn = "রংপুর",
                upazilaNameBn = "মিঠাপুকুর",
                mouzaNameBn = "৩১-জায়গীরহাট-১",
                jlNumber = "৩১",
                sheetNumber = "১",
                surveyType = "R.S",
                scaleId = "scale_16",
                scanDpi = 300,
                isFavorite = false,
                isOfflineSaved = true
            ),
            MouzaLibraryEntity(
                mapKey = "raj_paba_44_2",
                divisionId = "raj",
                divisionNameBn = "রাজশাহী",
                districtId = "rajshahi",
                districtNameBn = "রাজশাহী",
                upazilaNameBn = "পবা",
                mouzaNameBn = "৪৪-নওহাটা-২",
                jlNumber = "৪৪",
                sheetNumber = "২",
                surveyType = "R.S",
                scaleId = "scale_32",
                scanDpi = 300,
                isFavorite = false,
                isOfflineSaved = true
            ),
            MouzaLibraryEntity(
                mapKey = "syl_beanibazar_05_1",
                divisionId = "syl",
                divisionNameBn = "সিলেট",
                districtId = "sylhet",
                districtNameBn = "সিলেট",
                upazilaNameBn = "বিয়ানীবাজার",
                mouzaNameBn = "০৫-চারখাই-১",
                jlNumber = "০৫",
                sheetNumber = "১",
                surveyType = "S.A",
                scaleId = "scale_16",
                scanDpi = 300,
                isFavorite = false,
                isOfflineSaved = true
            )
        )

        private class SeedCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
            }
        }
    }
}
