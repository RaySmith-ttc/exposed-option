package ru.raysmith.exposedoption

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import ru.raysmith.utils.Cacheable
import kotlin.reflect.KProperty

abstract class Option<T> {
    abstract val key: String
    abstract val value: T

    open val database: Database? get() = null
    open val isolationLevel: Int? get() = null
    open val cache: Cacheable<T>? get() = null

    init {
        if (cache != null) {
            Options.registerOptionCache(key, cache!!)
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        @Suppress("UNCHECKED_CAST")
        return if (cache == null) value else cache?.getValue(thisRef, property) as T
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        cache?.setValue(thisRef, property, value)
        set(value)
    }

    fun <T> optionTransaction(statement: Transaction.() -> T): T {
        return transaction(
            transactionIsolation = isolationLevel ?: database.transactionManager.defaultIsolationLevel,
            readOnly = false,
            db = database,
            statement = statement
        )
    }

    fun record() = Options.select { Options.id eq key }.firstOrNull()

    fun set(value: T?): Option<T> {
        optionTransaction {
            if (record() == null) {
                Options.insert {
                    it[Options.id] = this@Option.key
                    it[Options.value] = value.toString()
                }
            } else {
                Options.update({ Options.id eq key }) { table ->
                    table[Options.value] = value.toString()
                }
            }
        }

        return this
    }

    fun refresh() {
        cache?.refresh()
    }
}