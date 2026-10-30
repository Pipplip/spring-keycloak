package de.pipplip.abfallcontainerservice.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    DELIVERED,
    COMPLETED,
    CANCELLED,
}

data class Booking(
    val id: UUID,
    val customerId: UUID,
    val containerTypeId: UUID,
    val quantity: Int,
    val deliveryDate: LocalDate,
    val pickupDate: LocalDate,
    val deliveryAddress: Address,
    val status: BookingStatus,
    val dailyPriceCents: Long,
    val deliveryFeeCents: Long,
    val currencyCode: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
)

data class ContainerAssignment(
    val id: UUID,
    val bookingId: UUID,
    val containerId: UUID,
    val assignedAt: Instant,
    val releasedAt: Instant?,
)
