package me.polynom.polycloud.apps.files.exceptions

import me.polynom.polycloud.apps.files.storage.StoragePath

class StorageReadOnlyException(
    path: StoragePath,
) : RuntimeException("Path $path is read-only")
