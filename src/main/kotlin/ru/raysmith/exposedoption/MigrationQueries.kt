package ru.raysmith.exposedoption

import org.intellij.lang.annotations.Language

object ExposedOptionMigrations {

    private fun sql(@Language("SQL") query: String) = query

    fun migrationStatementsFrom2To3(): List<String> {
        return listOf(
            sql("ALTER TABLE `options` CHANGE `value` `value` LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
        )
    }

    fun migrationStatementsFrom3To4(): List<String> {
        return listOf(
            sql("ALTER TABLE `options` CHANGE `value` `value` LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin")
        )
    }
}