package ru.raysmith.exposedoption

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class Tests : FreeSpec({

    lateinit var connection1: Database
    lateinit var connection2: Database

    fun initDatabase() {
        connection1 = Database.connect(
            url = "jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_UPPER=false;IGNORECASE=true;",
            driver = "org.h2.Driver",
            user = "root"
        )
        connection2 = Database.connect(
            url = "jdbc:h2:mem:test2;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_UPPER=false;IGNORECASE=true;",
            driver = "org.h2.Driver",
            user = "root"
        )
    }

    beforeSpec {
        initDatabase()
        transaction {
            SchemaUtils.create(Options)
            TransactionManager.defaultDatabase  = connection1
        }

        transaction(connection1) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Options)
        }
        transaction(connection2) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Options)
        }
    }

    afterTest {
        transaction(connection1) { Options.deleteAll() }
        transaction(connection2) { Options.deleteAll() }
        Options.clearRequired()
    }

    "option should return null when value is not set" {
        val option by option<Int?>("foo") { getOrNull() }

        option shouldBe null
    }

    "option should return not null value when value is not nullable" {
        transaction { Options.insert {
            it[id] = "foo"
            it[value] = "0"
        } }
        val option by option<Int>("foo")

        option shouldBe 0
    }

    "option should throw IllegalStateException when not null value is not in record set" {
        val option by option<Int>("foo")

        shouldThrow<IllegalStateException> {
            option
        }
    }

    "getOrSet should set value when it is not set" {
        val uuid = UUID.randomUUID().toString()

        val option by option<String>("foo") { getOrSet(uuid) }

        option shouldBe uuid
    }

    "getOrSet should work with nullable values" {
        val option by option<String?>("bar") {
            getOrSet(null)
        }

        option shouldBe null
    }

    "getOrSet should return stored value when fallback is null" {
        transaction {
            Options.insert {
                it[id] = "foo"
                it[value] = "bar"
            }
        }

        val option by option<String?>("foo") { getOrSet(null) }
        option shouldBe "bar"
    }

    "option should return value when value is set" {
        var option by option<Int?>("foo") { getOrNull() }
        option = 1

        option shouldBe 1
    }

    "option should return default value when getter override" {
        var option by option<Int?>("foo") { getOrSet(2) }
        option = 2

        option shouldBe 2
    }

    "option should return cache when it is enabled" {
        var option by option<Int?>("foo", cacheTime = Duration.INFINITE) { getOrNull() }
        option = 3

        connection1.connector().close()
        option shouldBe 3
        initDatabase()
    }

    "option should return cache should refresh when invoke" {
        val optionDelegate = option<Int?>("foo", cacheTime = Duration.INFINITE) { getOrNull() }
        var option by optionDelegate
        option = 4

        transaction { Options.update { it[value] = "44" } }

        option shouldBe 4
        optionDelegate.refresh()
        option shouldBe 44
    }

    "option should return value from correct database when it is set" {
        var option1 by option<Int?>("foo", database = connection1) { getOrNull() }
        var option2 by option<Int?>("foo", database = connection2) { getOrNull() }

        option1 = 5
        option2 = 6

        option1 shouldBe 5
        option2 shouldBe 6
    }

    "option should return null for existed record with null value" {
        val option by option<String?>("foo") { getOrNull() }
        transaction {
            Options.insert {
                it[id] = "foo"
                it[value] = null
            }
        }

        option shouldBe null
    }

    "custom transformer should convert value" {
        var option by option<Enum>("foo", transformer = transformer(
            unwrap = { it.ordinal.toString() },
            wrap = { ordinal -> Enum.entries.first { it.ordinal.toString() == ordinal } }
        )) {
            getOrNull() ?: set(Enum.VALUE_2).value
        }

        option shouldBe Enum.VALUE_2
        transaction {
            Options.selectAll()
                .where { Options.id.eq("foo") }
                .first()[Options.value] shouldBe Enum.VALUE_2.ordinal.toString()
        }

        @Suppress("AssignedValueIsNeverRead")
        option = Enum.VALUE_1
        transaction {
            Options.selectAll()
                .where { Options.id.eq("foo") }
                .first()[Options.value] shouldBe Enum.VALUE_1.ordinal.toString()
        }
    }

    "custom transformer should support nullable values" {
        val option1 by option<Int?>("foo", transformer = transformer(
            unwrap = { it.toString() },
            wrap = { it.toInt() }
        )) {
            getOrNull()
        }

        val option2 by option<Int?>("foo", transformer = transformer(
            unwrap = { it.toString() },
            wrap = { null }
        )) {
            getOrNull()
        }

        option1 shouldBe null
        option2 shouldBe null
    }


    "inc() should increment integer values" {
        var option = option<Int>("foo") { getOrNull() ?: set(5).value }
        option.value shouldBe 5

        option++
        option.value shouldBe 6

        option++
        option.value shouldBe 7
    }

    "inc() should increment nullable integer values" {
        var option = option<Int?>("foo") { getOrNull() ?: set(10).value }
        option.value shouldBe 10

        option++
        option.value shouldBe 11
    }

    "inc() should increment double values" {
        var option = option<Double>("foo") { getOrNull() ?: set(100.0).value }
        option.value shouldBe 100.0

        option++
        option.value shouldBe 101.0
    }

    "inc() should increment nullable double values" {
        var option = option<Double?>("foo") { getOrSet(200.0) }
        option.value shouldBe 200.0

        option++
        option.value shouldBe 201.0
    }

    "inc() should set null for null values" {
        var option = option<Int?>("foo") { getOrNull() }
        option.value shouldBe null

        option++
        option.value shouldBe null
    }

    "dec() should decrement integer values" {
        var option = option<Int>("foo") { getOrNull() ?: set(5).value }
        option.value shouldBe 5

        option--
        option.value shouldBe 4

        option--
        option.value shouldBe 3
    }

    "dec() should decrement nullable integer values" {
        var option = option<Int?>("foo") { getOrNull() ?: set(10).value }
        option.value shouldBe 10

        option--
        option.value shouldBe 9
    }

    "dec() should decrement double values" {
        var option = option<Double>("foo") { getOrNull() ?: set(100.0).value }
        option.value shouldBe 100.0

        option--
        option.value shouldBe 99.0
    }

    "dec() should decrement nullable double values" {
        var option = option<Double?>("foo") { getOrSet(200.0) }
        option.value shouldBe 200.0

        option--
        option.value shouldBe 199.0
    }

    "dec() should set null for null values" {
        var option = option<Int?>("foo") { getOrNull() }
        option.value shouldBe null

        option--
        option.value shouldBe null
    }

    "plusAssign() should add value to integer" {
        val option = option<Int>("foo") { getOrNull() ?: set(5).value }
        option.value shouldBe 5

        option += 10
        option.value shouldBe 15
    }

    "plusAssign() should add value to nullable integer" {
        val option = option<Int?>("foo") { getOrNull() ?: set(5).value }
        option.value shouldBe 5

        option += 10
        option.value shouldBe 15
    }

    "plusAssign() should add value to double" {
        val option = option<Double>("foo") { getOrNull() ?: set(5.0).value }
        option.value shouldBe 5.0

        option += 10.0
        option.value shouldBe 15.0
    }

    "plusAssign() should add value to nullable double" {
        val option = option<Double?>("foo") { getOrNull() ?: set(5.0).value }
        option.value shouldBe 5.0

        option += 10.0
        option.value shouldBe 15.0
    }

    "plusAssign() should set null for null values" {
        val option = option<Int?>("foo") { getOrNull() }
        option.value shouldBe null

        option += 10
        option.value shouldBe null
    }

    "minusAssign() should subtract value from integer" {
        val option = option<Int>("foo") { getOrNull() ?: set(15).value }
        option.value shouldBe 15

        option -= 10
        option.value shouldBe 5
    }

    "minusAssign() should subtract value from nullable integer" {
        val option = option<Int?>("foo") { getOrNull() ?: set(15).value }
        option.value shouldBe 15

        option -= 10
        option.value shouldBe 5
    }

    "minusAssign() should subtract value from double" {
        val option = option<Double>("foo") { getOrNull() ?: set(15.0).value }
        option.value shouldBe 15.0

        option -= 10.0
        option.value shouldBe 5.0
    }

    "minusAssign() should subtract value from nullable double" {
        val option = option<Double?>("foo") { getOrNull() ?: set(15.0).value }
        option.value shouldBe 15.0

        option -= 10.0
        option.value shouldBe 5.0
    }

    "minusAssign() should set null for null values" {
        val option = option<Int?>("foo") { getOrNull() }
        option.value shouldBe null

        option -= 10
        option.value shouldBe null
    }

    "checkRequired should throw error when required option is not set" {
        option<Int>("required1")
        option<Int>("required2")
        option<String>("required3") { getOrSet("value") }

        shouldThrow<IllegalStateException> {
            Options.checkRequired()
        }.message shouldBe "Required options required1, required2 is not set in database"
    }

    "checkRequired should not throw when all required options are set" {
        option<Int>("required1") { getOrSet(1) }
        option<String>("required2") { getOrSet("value") }

        Options.checkRequired()
    }

    "option should support BigDecimal type" {
        val value = BigDecimal("123.456")
        var option by option<BigDecimal>("foo") { getOrSet(value) }

        option shouldBe value

        val newValue = BigDecimal("789.012")
        option = newValue
        option shouldBe newValue
    }

    "option should support LocalDate type" {
        val date = LocalDate.of(2023, 5, 15)
        var option by option<LocalDate>("foo") { getOrSet(date) }

        option shouldBe date

        val newDate = LocalDate.of(2024, 10, 20)
        option = newDate
        option shouldBe newDate
    }

    "option should support LocalDateTime type" {
        val dateTime = LocalDateTime.of(2023, 5, 15, 12, 30, 45)
        val option by option<LocalDateTime>("foo") {
            getOrNull() ?: set(dateTime).value
        }

        option shouldBe dateTime
    }

    "option should support Enum type" {
        var option by option<Enum>("foo") { getOrNull() ?: set(Enum.VALUE_1).value }

        option shouldBe Enum.VALUE_1
        transaction {
            Options.selectAll()
                .where { Options.id.eq("foo") }
                .first()[Options.value] shouldBe Enum.VALUE_1.name
        }

        @Suppress("AssignedValueIsNeverRead")
        option = Enum.VALUE_2
        transaction {
            Options.selectAll()
                .where { Options.id.eq("foo") }
                .first()[Options.value] shouldBe Enum.VALUE_2.name
        }
    }

    "option should throw error when value cannot be converted to requested type" {
        transaction {
            Options.insert {
                it[id] = "invalid_int"
                it[value] = "not_a_number"
            }
        }

        val option by option<Int>("invalid_int")

        shouldThrow<NumberFormatException> {
            option
        }
    }

    "transform not should be called for cached value" {
        var calls = 0

        val transformer = transformer<String, Int>({ it.toString() }, {
            calls++
            it.toInt()
        })
        val option by option<Int>("foo", 5.minutes, transformer = transformer) {
            getOrSet(123)
        }

        repeat(5) {
            option shouldBe 123
        }

        calls shouldBe 1
    }
})

