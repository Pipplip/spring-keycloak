package de.pipplip.abfallcontainerservice.domain.model

import java.time.Instant
import java.util.UUID

data class Customer(
    val id: UUID,
    val keycloakSubject: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val companyName: String?,
    val billingAddress: Address,
    val createdAt: Instant,
    val updatedAt: Instant,
)
