package me.polynom.polycloud.apps.files.exceptions

import me.polynom.polycloud.apps.files.persistence.entities.Upload

class StorageQuotaException(
    upload: Upload,
    left: Long,
) : RuntimeException("Upload ${upload.path} too big (${upload.size} bytes). Only $left bytes left on storage.")
