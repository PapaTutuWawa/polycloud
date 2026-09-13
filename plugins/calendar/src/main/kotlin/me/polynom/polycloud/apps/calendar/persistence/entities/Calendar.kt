package me.polynom.polycloud.apps.calendar.persistence.entities

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

/**
 * Entity that describes a calendar.
 */
@Entity
@Table(name = "calendar")
class Calendar(
    /** Entity primary key. */
    @Id
    @GeneratedValue(GenerationType.UUID)
    var id: UUID? = null,

    /** The display name of the calendar. */
    var name: String? = null,

    /** The optional description of the calendar. */
    var description: String? = null,

    /** The owning user. */
    var owner: String? = null,

    /** The color of the calendar. */
    var color: String? = null,

    /** Flag controlling whether the calendar is public. */
    var public: Boolean? = null,
)