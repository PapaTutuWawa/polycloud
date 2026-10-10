package me.polynom.polycloud.apps.files.exceptions

import me.polynom.polycloud.apps.files.storage.StoragePath

class StoragePathDoesNotExistException(
    path: StoragePath,
) : RuntimeException("Path $path does not exist")
