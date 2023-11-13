package ru.raysmith.exposedoption

import org.jetbrains.exposed.sql.Database
import ru.raysmith.utils.Cacheable
import java.math.BigDecimal
import java.sql.Clob
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.full.isSubclassOf
import kotlin.time.Duration

const val COLLATE_UTF8MB4_GENERAL_CI = "utf8mb4_general_ci"
val dbDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
val dbDateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

inline fun <reified T> option(
    key: String,
    cacheTime: Duration? = null,
    isolationLevel: Int? = null,
    database: Database? = null,
    readOnly: Boolean = false,
    crossinline value: Option<T>.() -> T = { getOrThrow() }
) = object : Option<T> {
    override val key: String = key
    override val value: T get() = value()
    override val cache: Cacheable<T>? = if (cacheTime != null) Cacheable(cacheTime) { value() } else null
    override val isolationLevel: Int? = isolationLevel
    override val database: Database? = database
    override val readOnly: Boolean = readOnly
}.also {
    if (null !is T) {
        Options.trackRequired(it)
    }
}

inline fun <reified T> Option<T>.getOrThrow() = getOrNull() ?: error("Required option $key is not set in database")

inline fun <reified T> Option<T>.getOrNull(): T? = optionTransaction {
    val value = record()?.getOrNull(Options.value) ?: return@optionTransaction null
    if (T::class.isSubclassOf(Enum::class)) {
        T::class.java.enumConstants.find { (it as Enum<*>).name == value }
            ?: error("$value can't be associated with any from enum ${T::class.qualifiedName}")
    } else {
        when (T::class) {
            Int::class -> value.toInt()
            UInt::class -> value.toUInt()
            Long::class -> value.toLong()
            ULong::class -> value.toULong()
            Double::class -> value.toDouble()
            Float::class -> value.toFloat()
            Short::class -> value.toShort()
            BigDecimal::class -> value.toBigDecimal()
            Boolean::class -> value.toBoolean()
            String::class -> value
            Char::class -> value.first()
            Byte::class -> value.toByte()
            UByte::class -> value.toUByte()
            Clob::class -> value
            LocalDate::class -> value.let { v -> LocalDate.parse(v, dbDateFormat) }
            LocalDateTime::class -> value.let { v -> LocalDateTime.parse(v, dbDateTimeFormat) }
            LocalTime::class -> value.let { v -> LocalTime.parse(v) }
            else -> throw UnsupportedOperationException("Option cannot be cast to class ${T::class.java.name}")
        } as T
    }
}

@JvmName("incInt") fun Option<Int>.inc() = optionTransaction { set(value.plus(1)) }
@JvmName("incIntNullable") fun Option<Int?>.inc() = optionTransaction { set(value!!.plus(1)) }

@JvmName("incLong") fun Option<Long>.inc() = optionTransaction { set(value.plus(1)) }
@JvmName("incLongNullable") fun Option<Long?>.inc() = optionTransaction { set(value!!.plus(1)) }