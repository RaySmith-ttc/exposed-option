package ru.raysmith.exposedoption

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * Database table definition for storing options.
 *
 * This object represents a database table named "options" that stores key-value pairs.
 * Each record in the table consists of a unique string identifier (key) and an optional
 * text value.
 *
 * The object also provides functionality for tracking and validating required
 * options that must be set in the database.
 * */
object Options : IdTable<String>("options") {
    override val id: Column<EntityID<String>> = varchar("key", 255).entityId()
    override val primaryKey = PrimaryKey(id)

    /**
     * Column for storing option values as text.
     * Uses UTF8MB4 character set with Unicode collation.
     */
    val value = text("value", "utf8mb4_unicode_ci").nullable()

    /** Internal list for tracking options that are required to have non-null values */
    private val requiredOptions = mutableListOf<Option<*>>()

    /**
     * Adds an option to the list of required options that will be checked by [checkRequired].
     *
     * @param option The option to mark as required
     */
    fun trackRequired(option: Option<*>) {
        requiredOptions.add(option)
    }

    /**
     * Validates that all registered required options have values set in the database.
     *
     * @throws IllegalStateException if any required option is not set
     */
    fun checkRequired() {
        val failed = requiredOptions.filter {
            try {
                it.value == null
            } catch (e: Exception) {
                if (e is IllegalStateException && e.message == "Required option ${it.key} is not set in database") {
                    true
                } else {
                    throw e
                }
            }
        }

        if (failed.isNotEmpty()) {
            error("Required options ${failed.joinToString { it.key }} is not set in database")
        }
    }

    /** Clears the list of tracked required options */
    fun clearRequired() {
        requiredOptions.clear()
    }
}