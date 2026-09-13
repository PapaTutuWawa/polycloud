package me.polynom.polycloud.persistence.entities

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "migration")
class Migration(
    @Id
    var plugin: String? = null,
    var version: Int? = null,
    var name: String? = null,
)
