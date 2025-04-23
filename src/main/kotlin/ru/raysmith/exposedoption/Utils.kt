package ru.raysmith.exposedoption

/**
 * Gets the option value or throws an exception if it's not set.
 *
 * @return The current value of the option
 * @throws IllegalStateException if the option is not set in the database
 */
fun <T> Option<T>.getOrThrow() = getOrNull() ?: error("Required option $key is not set in database")

/**
 * Gets the option value or returns null if it's not set.
 *
 * @return The current value of the option, or null if not set
 */
fun <T> Option<T>.getOrNull(): T? = optionTransaction {
    val value = record()?.getOrNull(Options.value) ?: return@optionTransaction null
    transformer.wrap(value)
}

/**
 * Gets the option value if set, or sets it to the provided default value and returns that.
 *
 * @param value The default value to use if the option is not set
 * @return The current value from the database, or the default value if newly set
 */
fun <T : Any> Option<T>.getOrSet(value: T): T = optionTransaction {
    return@optionTransaction getOrNull() ?: set(value).value
}

/**
 * Gets the nullable option value if set, or sets it to the provided default value and returns that.
 *
 * @param value The default value to use if the option is not set (can be null)
 * @return The current value from the database, or the default value if newly set
 */
@JvmName("getOrSetNullable")
fun <T : Any?> Option<T?>.getOrSet(value: T?): T? = optionTransaction {
    if (value == null) return@optionTransaction null
    return@optionTransaction getOrNull() ?: set(value).value
}