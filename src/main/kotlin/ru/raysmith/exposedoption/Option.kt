package ru.raysmith.exposedoption

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import ru.raysmith.utils.Cacheable
import java.math.BigDecimal
import kotlin.reflect.KProperty
import kotlin.reflect.typeOf
import kotlin.time.Duration

/**
 * Abstract base class representing a configurable option stored in a database.
 *
 * This class provides a type-safe way to read, write, and manage settings stored in a database using Exposed ORM.
 * It supports property delegation, caching, custom value transformations, and transaction management.
 *
 * @param T The type of the option value (String, Int, Boolean, custom type, etc.)
 * */
abstract class Option<T> {

    /** Unique identifier for this option in the database */
    abstract val key: String

    /** The value of the option */
    abstract val value: T

    /** Transformer for converting between database string representation and the typed value */
    abstract val transformer: Transformer<String, T>

    /** Database connection to use for this option */
    open val database: Database? get() = null

    /** Transaction isolation level for database operations */
    open val isolationLevel: Int? get() = null

    /** Cache for storing the option value */
    open val cache: Cacheable<T>? get() = null

    /**
     * Property delegation getter implementation. Returns the option value,
     * using cache if available.
     */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        @Suppress("UNCHECKED_CAST")
        return if (cache == null) value else cache?.getValue(thisRef, property) as T
    }

    /**
     * Property delegation setter implementation. Updates the option value
     * in the database and refreshes the cache.
     */
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
//        cache?.setValue(thisRef, property, value)
        set(value)
    }

    @Suppress("UNCHECKED_CAST")
    operator fun inc(): Option<T> {
        when(value) {
            is Int -> set((value as Int).inc() as T)
            is Int? -> if (value != null) set((value as Int).inc() as T)
            is Long -> set((value as Long).inc() as T)
            is Long? -> if (value != null) set((value as Long).inc() as T)
            is BigDecimal -> set((value as BigDecimal).inc() as T)
            is BigDecimal? -> if (value != null) set((value as BigDecimal).inc() as T)
            is Double -> set((value as Double).inc() as T)
            is Float -> set((value as Float).inc() as T)
            is Short -> set((value as Short).inc() as T)
            is Byte -> set((value as Byte).inc() as T)
            else -> error("Unsupported type for increment")
        }
        return this
    }

    @Suppress("UNCHECKED_CAST")
    operator fun dec(): Option<T> {
        when(value) {
            is Int -> set((value as Int).dec() as T)
            is Int? -> if (value != null) set((value as Int).dec() as T)
            is Long -> set((value as Long).dec() as T)
            is Long? -> if (value != null) set((value as Long).dec() as T)
            is BigDecimal -> set((value as BigDecimal).dec() as T)
            is BigDecimal? -> if (value != null) set((value as BigDecimal).dec() as T)
            is Double -> set((value as Double).dec() as T)
            is Float -> set((value as Float).dec() as T)
            is Short -> set((value as Short).dec() as T)
            is Byte -> set((value as Byte).dec() as T)
            else -> error("Unsupported type for decrement")
        }
        return this
    }

    @Suppress("UNCHECKED_CAST", "SENSELESS_COMPARISON")
    operator fun plusAssign(value: T) {
        when(val v = this.value) {
            is Int -> set((v as Int).plus(value as Int) as T)
            is Int? -> if (v != null) set((v as Int).plus(value as Int) as T)
            is Long -> set((v as Long).plus(value as Long) as T)
            is Long? -> if (v != null) set((v as Long).plus(value as Long) as T)
            is BigDecimal -> set((v as BigDecimal).plus(value as BigDecimal) as T)
            is BigDecimal? -> if (v != null) set((v as BigDecimal).plus(value as BigDecimal) as T)
            is Double -> set((v as Double).plus(value as Double) as T)
            is Float -> set((v as Float).plus(value as Float) as T)
            is Short -> set((v as Short).plus(value as Short) as T)
            is Byte -> set((v as Byte).plus(value as Byte) as T)
            else -> error("Unsupported type for addition")
        }
    }

    @Suppress("UNCHECKED_CAST", "SENSELESS_COMPARISON")
    operator fun minusAssign(value: T) {
        when(val v = this.value) {
            is Int -> set((v as Int).minus(value as Int) as T)
            is Int? -> if (v != null) set((v as Int).minus(value as Int) as T)
            is Long -> set((v as Long).minus(value as Long) as T)
            is Long? -> if (v != null) set((v as Long).minus(value as Long) as T)
            is BigDecimal -> set((v as BigDecimal).minus(value as BigDecimal) as T)
            is BigDecimal? -> if (v != null) set((v as BigDecimal).minus(value as BigDecimal) as T)
            is Double -> set((v as Double).minus(value as Double) as T)
            is Float -> set((v as Float).minus(value as Float) as T)
            is Short -> set((v as Short).minus(value as Short) as T)
            is Byte -> set((v as Byte).minus(value as Byte) as T)
            else -> error("Unsupported type for subtraction")
        }
    }

    @Suppress("UNCHECKED_CAST", "SENSELESS_COMPARISON")
    operator fun divAssign(value: T) {
        when(val v = this.value) {
            is Int -> set((v as Int).div(value as Int) as T)
            is Int? -> if (v != null) set((v as Int).div(value as Int) as T)
            is Long -> set((v as Long).div(value as Long) as T)
            is Long? -> if (v != null) set((v as Long).div(value as Long) as T)
            is BigDecimal -> set((v as BigDecimal).div(value as BigDecimal) as T)
            is BigDecimal? -> if (v != null) set((v as BigDecimal).div(value as BigDecimal) as T)
            is Double -> set((v as Double).div(value as Double) as T)
            is Float -> set((v as Float).div(value as Float) as T)
            is Short -> set((v as Short).div(value as Short) as T)
            is Byte -> set((v as Byte).div(value as Byte) as T)
            else -> error("Unsupported type for division")
        }
    }

    @Suppress("UNCHECKED_CAST", "SENSELESS_COMPARISON")
    operator fun remAssign(value: T) {
        when(val v = this.value) {
            is Int -> set((v as Int).rem(value as Int) as T)
            is Int? -> if (v != null) set((v as Int).rem(value as Int) as T)
            is Long -> set((v as Long).rem(value as Long) as T)
            is Long? -> if (v != null) set((v as Long).rem(value as Long) as T)
            is BigDecimal -> set((v as BigDecimal).rem(value as BigDecimal) as T)
            is BigDecimal? -> if (v != null) set((v as BigDecimal).rem(value as BigDecimal) as T)
            is Double -> set((v as Double).rem(value as Double) as T)
            is Float -> set((v as Float).rem(value as Float) as T)
            is Short -> set((v as Short).rem(value as Short) as T)
            is Byte -> set((v as Byte).rem(value as Byte) as T)
            else -> error("Unsupported type for remainder")
        }
    }

    @Suppress("UNCHECKED_CAST", "SENSELESS_COMPARISON")
    operator fun timesAssign(value: T) {
        when(val v = this.value) {
            is Int -> set((v as Int).times(value as Int) as T)
            is Int? -> if (v != null) set((v as Int).times(value as Int) as T)
            is Long -> set((v as Long).times(value as Long) as T)
            is Long? -> if (v != null) set((v as Long).times(value as Long) as T)
            is BigDecimal -> set((v as BigDecimal).times(value as BigDecimal) as T)
            is BigDecimal? -> if (v != null) set((v as BigDecimal).times(value as BigDecimal) as T)
            is Double -> set((v as Double).times(value as Double) as T)
            is Float -> set((v as Float).times(value as Float) as T)
            is Short -> set((v as Short).times(value as Short) as T)
            is Byte -> set((v as Byte).times(value as Byte) as T)
            else -> error("Unsupported type for multiplication")
        }
    }

    /**
     * Executes a database transaction with the option's specified isolation level
     * and database connection.
     *
     * @param statement The transaction code to execute
     * @return Result of the transaction
     */
    fun <T> optionTransaction(statement: Transaction.() -> T): T {
        return transaction(
            transactionIsolation = isolationLevel ?: database.transactionManager.defaultIsolationLevel,
            readOnly = false,
            db = database,
            statement = statement
        )
    }

    /** Returns the database record for this option, or null if it doesn't exist */
    context(Transaction)
    fun record() = Options.selectAll().where { Options.id eq key }.firstOrNull()

    /**
     * Sets a new value for this option in the database.
     * Creates a new record if the option doesn't exist yet.
     *
     * @param value The new value (can be null for nullable options)
     * @return This option instance
     */
    fun set(value: T): Option<T> {
        cache?.setValue(this, this::value, value)
        optionTransaction {
            if (record() == null) {
                Options.insert { stmt ->
                    stmt[id] = this@Option.key
                    stmt[Options.value] = value?.let { transformer.unwrap(it) }
                }
            } else {
                Options.update({ Options.id eq key }) { table ->
                    table[Options.value] = value?.let { transformer.unwrap(it) }
                }
            }
        }

        return this
    }

    /** Forces refresh of the cached value if caching is enabled */
    fun refresh() {
        cache?.refresh()
    }
}

/**
 * Creates a database-backed option with type-safe access and optional caching.
 *
 * This function creates an [Option] instance that allows reading and writing typed values
 * to a database. It supports property delegation, value transformation, caching, and
 * custom database connections.
 *
 * For non-nullable types, the option is automatically tracked as "required" by [Options].
 *
 * @param T The type of the option value
 * @param key Unique identifier for this option in the database
 * @param cacheTime Duration for caching the option value to reduce database queries
 * @param transformer Converter between database string representation and typed value
 * @param database Database connection to use for this option
 * @param isolationLevel Transaction isolation level for database operations
 * @param getter Function to retrieve the option value, defaults to [getOrThrow]
 *
 * @return An [Option] instance that can be used directly or with property delegation
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> option(
    key: String,
    cacheTime: Duration? = null,
    transformer: Transformer<String, T> = getDefaultTransformerOrCreate(typeOf<T>()) as Transformer<String, T>,
    database: Database? = null,
    isolationLevel: Int? = null,
    crossinline getter: Option<T>.() -> T = { getOrThrow() }
) = object : Option<T>() {
    override val key: String = key
    override val value: T get() = getter()
    override val database: Database? = database
    override val isolationLevel: Int? = isolationLevel
    override val cache: Cacheable<T>? = if (cacheTime != null) Cacheable(cacheTime) { getter() } else null
    override val transformer: Transformer<String, T> = transformer
}.also {
    if (null !is T) {
        Options.trackRequired(it)
    }
}