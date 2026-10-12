package me.polynom.polycloud.apps.files.converters

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import me.polynom.polycloud.apps.files.storage.StoragePath

@Converter(autoApply = true)
class StoragePathConverter : AttributeConverter<StoragePath, String> {
    override fun convertToDatabaseColumn(attribute: StoragePath) = attribute.toString()

    override fun convertToEntityAttribute(dbData: String) = StoragePath(dbData)
}
