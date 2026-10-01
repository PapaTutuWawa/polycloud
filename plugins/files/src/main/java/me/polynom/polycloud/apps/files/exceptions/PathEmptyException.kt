package me.polynom.polycloud.apps.files.exceptions

import me.polynom.polycloud.apps.files.storage.StoragePath

class PathEmptyException(
    val user: String,
    val path: StoragePath,
) : RuntimeException()
