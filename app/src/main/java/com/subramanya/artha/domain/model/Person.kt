package com.subramanya.artha.domain.model

import com.subramanya.artha.data.entity.enums.PersonRelation

data class Person(
    val id: String,
    val name: String,
    val relation: PersonRelation,
    val contact: String?,
    val avatarUri: String?,
    val createdAt: Long,
)
