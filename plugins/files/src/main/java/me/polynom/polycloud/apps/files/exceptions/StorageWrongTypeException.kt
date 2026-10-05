package me.polynom.polycloud.apps.files.exceptions

import me.polynom.polycloud.apps.files.storage.StoragePath

class StorageWrongTypeException(
    path: StoragePath,
    expectedFile: Boolean,
) : RuntimeException("Got $path but expected ${ if (expectedFile) "file" else "folder" }")
