package me.polynom.polycloud.apps.files.exceptions

import me.polynom.polycloud.apps.files.storage.StoragePath
import java.util.UUID

class UploadDoesNotExistException(
    id: UUID,
) : RuntimeException("Upload with id $id does not exist")
