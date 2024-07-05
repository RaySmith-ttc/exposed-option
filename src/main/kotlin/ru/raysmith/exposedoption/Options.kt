package ru.raysmith.exposedoption

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import ru.raysmith.utils.Cacheable
import java.util.Collections

object Options : IdTable<String>("options") {
    override val id: Column<EntityID<String>> = varchar("key", 255).entityId()
    override val primaryKey = PrimaryKey(id)
    val value = text("value", "utf8mb4_unicode_ci")

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

    private val caches = Collections.synchronizedMap(mutableMapOf<String, Cacheable<*>>())
    internal fun registerOptionCache(key: String, cacheable: Cacheable<*>) {
        caches[key] = cacheable
    }
    fun refreshCaches() {
        caches.forEach { (_, cache) ->
            cache.refresh()
        }
    }
}