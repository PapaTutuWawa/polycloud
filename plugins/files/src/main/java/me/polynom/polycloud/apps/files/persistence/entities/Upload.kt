package me.polynom.polycloud.apps.files.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

/**
 * An upload describes a pending file upload.
 */
@Entity
@Table(name = "uploads")
class Upload(
    /** Primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    /** Size of the file that is being uploaded. */
    var size: Long,
    /** The user uploading the file. */
    @Column(name = "\"user\"")
    var user: String,
    /** The file's intended storage location. */
    var path: String,
    /** Bytes already uploaded */
    @Column(name = "\"offset\"")
    var offset: Long,
) {
    fun done() = offset >= size
}
