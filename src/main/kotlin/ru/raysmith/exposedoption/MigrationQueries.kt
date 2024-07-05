package ru.raysmith.exposedoption

import org.jetbrains.exposed.sql.Transaction

object Migrations {

    context(Transaction)
    fun applyMigrationFrom2To3() {
        exec("ALTER TABLE `options` CHANGE `value` `value` LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL")
    }
}