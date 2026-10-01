package me.polynom.polycloud.apps.files.mappers

import me.polynom.polycloud.apps.files.api.dto.EntryMetaDto
import me.polynom.polycloud.apps.files.storage.EntryMeta
import org.mapstruct.Mapper
import org.mapstruct.MappingConstants

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface EntryMetaMapper {
    fun entryMetaToEntryMetaDto(meta: EntryMeta): EntryMetaDto
}
