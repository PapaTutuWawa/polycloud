package me.polynom.polycloud.apps.files.exceptions

class InvalidUploadMetadataException(
    reason: String,
) : RuntimeException("Invalid Upload-Metadata header: $reason")
