package me.polynom.polycloud.apps.files.storage.impls

import me.polynom.polycloud.apps.files.storage.Storage
import java.nio.file.Path

class LocalStorage (val path: Path, val shared: Boolean) : Storage {
}