package com.example.habittracker.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `habit_day_notes` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `habitId` INTEGER NOT NULL,
                    `date` TEXT NOT NULL,
                    `note` TEXT NOT NULL,
                    FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_day_notes_habitId_date` ON `habit_day_notes` (`habitId`, `date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_day_notes_habitId` ON `habit_day_notes` (`habitId`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `who_am_i_notes` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_who_am_i_notes_createdAt` ON `who_am_i_notes` (`createdAt`)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tasks` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `isDone` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `goals` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sub_goals` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `goalId` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `isDone` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sub_goals_goalId` ON `sub_goals` (`goalId`)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `goals` ADD COLUMN `isDone` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reminder_settings` (
                    `id` INTEGER NOT NULL,
                    `habitsEnabled` INTEGER NOT NULL,
                    `tasksEnabled` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("INSERT OR IGNORE INTO `reminder_settings` (`id`, `habitsEnabled`, `tasksEnabled`) VALUES (0, 1, 1)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reminder_times` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `timeValue` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reminder_times_timeValue` ON `reminder_times` (`timeValue`)")
            db.execSQL("INSERT OR IGNORE INTO `reminder_times` (`timeValue`) VALUES ('09:00')")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `habits` ADD COLUMN `reminderEnabled` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `habits` ADD COLUMN `reminderTime` TEXT")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `reminderEnabled` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `reminderTime` TEXT")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `habits` ADD COLUMN `reminderMessage` TEXT")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `reminderMessage` TEXT")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `goals` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `who_am_i_notes` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")

            db.execSQL(
                """
                UPDATE `tasks`
                SET `sortOrder` = (
                    SELECT COUNT(*)
                    FROM `tasks` t2
                    WHERE t2.`isDone` = `tasks`.`isDone`
                      AND (t2.`createdAt` > `tasks`.`createdAt`
                           OR (t2.`createdAt` = `tasks`.`createdAt` AND t2.`id` > `tasks`.`id`))
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                UPDATE `goals`
                SET `sortOrder` = (
                    SELECT COUNT(*)
                    FROM `goals` g2
                    WHERE g2.`isDone` = `goals`.`isDone`
                      AND (g2.`createdAt` > `goals`.`createdAt`
                           OR (g2.`createdAt` = `goals`.`createdAt` AND g2.`id` > `goals`.`id`))
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                UPDATE `who_am_i_notes`
                SET `sortOrder` = (
                    SELECT COUNT(*)
                    FROM `who_am_i_notes` n2
                    WHERE n2.`createdAt` > `who_am_i_notes`.`createdAt`
                      OR (n2.`createdAt` = `who_am_i_notes`.`createdAt` AND n2.`id` > `who_am_i_notes`.`id`)
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `habits` ADD COLUMN `frequencyType` TEXT NOT NULL DEFAULT 'DAILY'")
            db.execSQL("ALTER TABLE `habits` ADD COLUMN `frequencyIntervalDays` INTEGER")
            db.execSQL("ALTER TABLE `habits` ADD COLUMN `frequencyWeekdays` TEXT")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `birthdays` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `year` INTEGER NOT NULL,
                    `month` INTEGER NOT NULL,
                    `day` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_birthdays_month_day` ON `birthdays` (`month`, `day`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_birthdays_name` ON `birthdays` (`name`)")
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `completedAt` INTEGER")
            db.execSQL("ALTER TABLE `goals` ADD COLUMN `completedAt` INTEGER")
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `sub_goals` ADD COLUMN `completedAt` INTEGER")
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'General'")
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `birthdays` ADD COLUMN `reminderDateTimesCsv` TEXT")
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `reminderDateTimesCsv` TEXT")
        }
    }

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add sortOrder to habits table
            db.execSQL("ALTER TABLE `habits` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
            // Initialize sortOrder for existing habits based on current createdAt DESC order
            db.execSQL(
                """
                UPDATE `habits`
                SET `sortOrder` = (
                    SELECT COUNT(*)
                    FROM `habits` h2
                    WHERE h2.`createdAt` > `habits`.`createdAt`
                      OR (h2.`createdAt` = `habits`.`createdAt` AND h2.`id` > `habits`.`id`)
                )
                """.trimIndent()
            )

            // Create task_categories table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `task_categories` (
                    `name` TEXT NOT NULL,
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`name`)
                )
                """.trimIndent()
            )
            // Populate from existing task categories + ensure "General" exists
            db.execSQL("INSERT OR IGNORE INTO `task_categories` (`name`, `sortOrder`) VALUES ('General', 0)")
            db.execSQL(
                """
                INSERT OR IGNORE INTO `task_categories` (`name`, `sortOrder`)
                SELECT DISTINCT `category`, 0 FROM `tasks` WHERE `category` != 'General' AND `category` != ''
                """.trimIndent()
            )
            // Assign sequential sortOrder to inserted categories
            db.execSQL(
                """
                UPDATE `task_categories`
                SET `sortOrder` = (
                    SELECT COUNT(*)
                    FROM `task_categories` c2
                    WHERE c2.`name` < `task_categories`.`name`
                )
                """.trimIndent()
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
        MIGRATION_12_13,
        MIGRATION_13_14,
        MIGRATION_14_15,
        MIGRATION_15_16
    )
}
