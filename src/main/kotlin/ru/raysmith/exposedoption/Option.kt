package ru.raysmith.exposedoption

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import ru.raysmith.utils.Cacheable
import kotlin.reflect.KProperty

interface Option<T> {
    val database: Database? get() = null
    val key: String
    val value: T
    val isolationLevel: Int? get() = null
    val cache: Cacheable<T>? get() = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return cache?.getValue(thisRef, property) ?: value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        cache?.setValue(thisRef, property, value)
        set(value)
    }

    fun <T> optionTransaction(statement: Transaction.() -> T): T {
        return transaction(
            transactionIsolation = isolationLevel ?: database.transactionManager.defaultIsolationLevel,
            repetitionAttempts = database.transactionManager.defaultRepetitionAttempts,
            db = database,
            statement = statement
        )
    }

    fun record() = optionTransaction {
        Options.select { Options.id eq key }.firstOrNull()
    }

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
}