package ru.raysmith.exposedoption

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class OptionEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, OptionEntity>(Options)

    var value by Options.value
}