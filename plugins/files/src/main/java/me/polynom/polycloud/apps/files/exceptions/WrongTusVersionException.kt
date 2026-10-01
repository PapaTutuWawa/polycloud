package me.polynom.polycloud.apps.files.exceptions

import me.polynom.polycloud.apps.files.constants.TusConstants

class WrongTusVersionException(
    receivedVersion: String,
) : RuntimeException("Version $receivedVersion unsupported, only ${TusConstants.TUS_VERSION} supported.")
