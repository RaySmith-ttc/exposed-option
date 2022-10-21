package ru.raysmith.exposedoption

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object Options : IdTable<String>("options") {
    override val id: Column<EntityID<String>> = varchar("key", 255).entityId().uniqueIndex()
        val value = text("value", COLLATE_UTF8MB4_GENERAL_CI)

    private val requiredOptions = mutableListOf<Option<*>>()
    fun trackRequired(option: Option<*>) {
        requiredOptions.add(option)
    }

    fun checkRequired() {
        val failed = requiredOptions.filter {
            try {
                it.value == null
            } catch (e: Exception) {
                if (e !is IllegalStateException) {
                    throw e
                }
                true
            }
        }

        if (failed.isNotEmpty()) {
            error("Required options ${failed.joinToString { it.key }} is not set in database")
        }
    }
}