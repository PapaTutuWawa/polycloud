package me.polynom.polycloud.apps.files.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Converter
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import me.polynom.polycloud.apps.files.storage.StoragePath
import java.util.UUID

/**
 * An upload describes a pending file upload.
 */
@Entity
@Table(name = "uploads")
class Upload(
    /** Size of the file that is being uploaded. */
    var size: Long,
    /** The user uploading the file. */
    @Column(name = "\"user\"")
    var user: String,
    /** The file's intended storage location. */
    var path: StoragePath,
    /** Bytes already uploaded */
    @Column(name = "\"offset\"")
    var offset: Long = 0L,
) {
    /** Primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    fun done() = offset >= size
}
