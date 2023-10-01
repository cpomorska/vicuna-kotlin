package com.scprojekt.domain.shared.database

import com.scprojekt.domain.model.user.dto.UuidResponse
import io.quarkus.hibernate.orm.panache.PanacheRepository


interface BaseRepository<T> : PanacheRepository<T> {
    fun removeEntity(entity: T): UuidResponse
    fun createEntity(entity: T): UuidResponse
    fun updateEntity(entity: T): UuidResponse
}