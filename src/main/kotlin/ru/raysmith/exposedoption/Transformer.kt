package ru.raysmith.exposedoption

import java.math.BigDecimal
import java.sql.Clob
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.javaType

/**
 * An interface defining the transformation between a source column type and a target type.
 *
 * @param Wrapped The type of the column values after transformation
 * @param Unwrapped The type of the column values without transformation
 */
interface Transformer<Unwrapped, Wrapped> {
    /**
     * Returns the underlying column value without a transformation applied ([Wrapped] -> [Unwrapped]).
     */
    fun unwrap(value: Wrapped & Any): Unwrapped

    /**
     * Applies transformation to the underlying column value ([Unwrapped] -> [Wrapped])
     */
    fun wrap(value: Unwrapped): Wrapped
}

/**
 * Creates a [Transformer] instance that transforms between two types.
 *
 * @param unwrap Function to convert the wrapped value to the unwrapped value
 * @param wrap Function to convert the unwrapped value to the wrapped value
 * @return A [Transformer] instance that performs the specified transformations
 */
fun <Unwrapped, Wrapped> transformer(
    unwrap: (value: Wrapped & Any) -> Unwrapped,
    wrap: (value: Unwrapped) -> Wrapped
): Transformer<Unwrapped, Wrapped> {
    return object : Transformer<Unwrapped, Wrapped> {
        override fun unwrap(value: Wrapped & Any): Unwrapped = unwrap(value)
        override fun wrap(value: Unwrapped): Wrapped = wrap(value)
    }
}

private val defaultTransformersCache = mutableMapOf<KType, Transformer<String, *>>()


/** Returns a default transformer for the specified type */
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalStdlibApi::class)
fun getDefaultTransformerOrCreate(type: KType): Transformer<String, *> {
    return defaultTransformersCache.getOrPut(type) {
        transformer({ it.toString() }, { value ->
            if (type.isSubtypeOf(Enum::class.starProjectedType)) {
                val enumClass = (type.classifier as KClass<*>).java as Class<out Enum<*>>
                return@transformer enumClass.enumConstants.find { it.name == value }
                    ?: error("$value не соответствует ни одному значению из перечисления ${type.javaType.typeName}")
            }

            when(type.classifier) {
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
                LocalDate::class -> value.let { v -> LocalDate.parse(v) }
                LocalDateTime::class -> value.let { v -> LocalDateTime.parse(v) }
                LocalTime::class -> value.let { v -> LocalTime.parse(v) }
                else -> throw UnsupportedOperationException("Option cannot be cast to class ${type.javaType.typeName}") // TODO upd message: provide custom transformer
            }
        })
    }
}