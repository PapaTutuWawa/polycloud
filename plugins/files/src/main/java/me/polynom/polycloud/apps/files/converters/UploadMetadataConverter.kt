package me.polynom.polycloud.apps.files.converters

import me.polynom.polycloud.apps.files.api.dto.TusUploadMetadata
import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.files.exceptions.InvalidUploadMetadataException
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.Base64

@Component
@PluginEnabled
class UploadMetadataConverter : Converter<String, TusUploadMetadata> {
    private val keyRegex = Regex("^[\\x21-\\x7E]+$")
    private val decoder = Base64.getDecoder()

    override fun convert(source: String): TusUploadMetadata {
        if (source.trim().isBlank()) return TusUploadMetadata(emptyMap())

        val result = HashMap<String, String>()
        for (rawPair in source.split(",")) {
            val pair = rawPair.trim()
            if (pair.isEmpty()) {
                throw InvalidUploadMetadataException("empty key-value pair")
            }

            val parts = pair.split(" ")
            if (parts.size > 2) {
                throw InvalidUploadMetadataException("pair $parts contains multiple whitespaces")
            }

            val key = parts[0]
            if (!key.matches(keyRegex)) {
                throw InvalidUploadMetadataException("key '$key' must be non-empty ASCII without whitespace or commas")
            }
            if (result.containsKey(key)) {
                throw InvalidUploadMetadataException("duplicate key '$key'")
            }

            val encodedValue = parts.getOrNull(1)?.trim()
            val value =
                if (encodedValue.isNullOrEmpty()) {
                    ""
                } else {
                    try {
                        decoder.decode(encodedValue).toString(Charsets.UTF_8)
                    } catch (e: IllegalArgumentException) {
                        throw InvalidUploadMetadataException("value for key '$key' is not valid Base64 or does not contain valid UTF-8")
                    }
                }

            result[key] = value
        }
        return TusUploadMetadata(result)
    }
}
