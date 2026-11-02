package de.pipplip.abfallcontainerservice.application.port.out

import de.pipplip.abfallcontainerservice.domain.model.Booking
import de.pipplip.abfallcontainerservice.domain.model.Container
import de.pipplip.abfallcontainerservice.domain.model.ContainerAssignment
import de.pipplip.abfallcontainerservice.domain.model.ContainerType
import de.pipplip.abfallcontainerservice.domain.model.Customer
import de.pipplip.abfallcontainerservice.domain.model.Invoice
import java.time.LocalDate
import java.util.UUID

data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
) {
    val totalPages: Int
        get() = if (totalElements == 0L) 0 else ((totalElements - 1) / size + 1).toInt()
}

interface CustomerRepository {
    fun save(customer: Customer): Customer

    fun findById(id: UUID): Customer?

    fun findByKeycloakSubject(keycloakSubject: String): Customer?
}

interface ContainerTypeRepository {
    fun save(containerType: ContainerType): ContainerType

    fun findById(id: UUID): ContainerType?

    fun findActiveById(id: UUID): ContainerType?

    fun findActivePage(page: Int, size: Int): PageResult<ContainerType>
}

interface ContainerRepository {
    fun save(container: Container): Container

    fun findById(id: UUID): Container?
}

interface BookingRepository {
    fun save(booking: Booking): Booking

    fun findById(id: UUID): Booking?

    fun findByCustomerId(
        customerId: UUID,
        page: Int,
        size: Int,
    ): PageResult<Booking>
}

interface ContainerAssignmentRepository {
    fun save(assignment: ContainerAssignment): ContainerAssignment

    fun findById(id: UUID): ContainerAssignment?

    fun countAvailableContainers(
        containerTypeId: UUID,
        deliveryDate: LocalDate,
        pickupDate: LocalDate,
    ): Long

    fun lockAvailableContainerIds(
        containerTypeId: UUID,
        deliveryDate: LocalDate,
        pickupDate: LocalDate,
        limit: Int,
    ): List<UUID>

    fun releaseActiveAssignments(
        bookingId: UUID,
        releasedAt: java.time.Instant,
    )
}

interface InvoiceRepository {
    fun save(invoice: Invoice): Invoice

    fun findById(id: UUID): Invoice?

    fun findByBookingId(bookingId: UUID): Invoice?
}
