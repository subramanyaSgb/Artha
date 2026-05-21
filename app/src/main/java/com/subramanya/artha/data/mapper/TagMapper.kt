package com.subramanya.artha.data.mapper

import com.subramanya.artha.data.entity.TagEntity
import com.subramanya.artha.domain.model.Tag

fun TagEntity.toDomain(): Tag = Tag(id = id, name = name, color = color)

fun Tag.toEntity(): TagEntity = TagEntity(id = id, name = name, color = color)
