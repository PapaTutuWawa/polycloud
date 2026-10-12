package me.polynom.polycloud.apps.files.exceptions

import me.polynom.polycloud.apps.files.persistence.entities.Upload

class UploadIncompleteException(
    upload: Upload,
) : RuntimeException("Upload ${upload.id} is ${upload.size} bytes big but only ${upload.offset} bytes received.")
