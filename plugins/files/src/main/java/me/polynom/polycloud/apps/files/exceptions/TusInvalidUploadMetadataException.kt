package me.polynom.polycloud.apps.files.exceptions

class TusInvalidUploadMetadataException(
    reason: String,
) : RuntimeException("Invalid Upload-Metadata header: $reason")
