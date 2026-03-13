package dev.rivikauth.core.database

import dev.rivikauth.core.database.entity.OtpEntryEntity
import dev.rivikauth.core.model.HashAlgorithm
import dev.rivikauth.core.model.OtpEntry
import dev.rivikauth.core.model.OtpType

fun OtpEntryEntity.toModel(): OtpEntry = OtpEntry(
    id = id,
    name = name,
    issuer = issuer,
    type = OtpType.valueOf(type),
    secret = secret,
    algorithm = HashAlgorithm.valueOf(algorithm),
    digits = digits,
    period = period,
    counter = counter,
    pin = pin,
    groupIds = if (groupIds.isBlank()) emptySet() else groupIds.split(",").toSet(),
    sortOrder = sortOrder,
    note = note,
    favorite = favorite,
    iconData = iconData,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun OtpEntry.toEntity(): OtpEntryEntity = OtpEntryEntity(
    id = id,
    name = name,
    issuer = issuer,
    type = type.name,
    secret = secret,
    algorithm = algorithm.name,
    digits = digits,
    period = period,
    counter = counter,
    pin = pin,
    groupIds = groupIds.joinToString(","),
    sortOrder = sortOrder,
    note = note,
    favorite = favorite,
    iconData = iconData,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
