package me.polynom.polycloud.apps.files.exceptions

import me.polynom.polycloud.apps.files.persistence.entities.Upload

class UploadWrongOffsetException(
    upload: Upload,
    userOffset: Long,
) : RuntimeException("Upload at offset ${upload.offset} cannot resume at offset $userOffset")
