package com.scprojekt.infrastructure.service

import com.scprojekt.domain.model.user.repository.UserRepository
import com.scprojekt.infrastructure.abstrct.AbstractUserReadOnlyService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Reads a User or a list of Users from Backend
 * Implementation of @see{AbstractUserReadOnlyService}
 */
@ApplicationScoped
class BaseUserReadOnlyService @Inject constructor(userRepository: UserRepository) : AbstractUserReadOnlyService(userRepository) {
}