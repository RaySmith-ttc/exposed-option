import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import ru.raysmith.exposedoption.*
import kotlin.time.Duration

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

    "option should return value when value is set" {
        var option by option<Int?>("foo") { getOrNull() }
        option = 1

        option shouldBe 1
    }

    "option should return default value when getter override" {
        var option by option<Int?>("foo") { getOrNull() ?: set(2).value }
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

})