package de.pipplip.abfallcontainerservice.application.port.out

import de.pipplip.abfallcontainerservice.domain.model.Booking
import de.pipplip.abfallcontainerservice.domain.model.Container
import de.pipplip.abfallcontainerservice.domain.model.ContainerAssignment
import de.pipplip.abfallcontainerservice.domain.model.ContainerType
import de.pipplip.abfallcontainerservice.domain.model.ContainerStatus
import de.pipplip.abfallcontainerservice.domain.model.Customer
import de.pipplip.abfallcontainerservice.domain.model.Invoice
import de.pipplip.abfallcontainerservice.domain.model.InvoiceStatus
import de.pipplip.abfallcontainerservice.domain.model.BookingStatus
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

    fun findPage(page: Int, size: Int): PageResult<Customer>
}

interface ContainerTypeRepository {
    fun save(containerType: ContainerType): ContainerType

    fun findById(id: UUID): ContainerType?

    fun findByIdForUpdate(id: UUID): ContainerType?

    fun findActiveById(id: UUID): ContainerType?

    fun findActiveByIdForUpdate(id: UUID): ContainerType?

    fun findActivePage(page: Int, size: Int): PageResult<ContainerType>

    fun findAdminPage(active: Boolean?, page: Int, size: Int): PageResult<ContainerType>

    fun hasBookings(containerTypeId: UUID): Boolean
}

interface ContainerRepository {
    fun save(container: Container): Container

    fun findById(id: UUID): Container?

    fun findByIdForUpdate(id: UUID): Container?

    fun findAdminPage(
        containerTypeId: UUID?,
        status: ContainerStatus?,
        page: Int,
        size: Int,
    ): PageResult<Container>
}

interface BookingRepository {
    fun save(booking: Booking): Booking

    fun findById(id: UUID): Booking?

    fun findByIdForUpdate(id: UUID): Booking?

    fun findByCustomerId(
        customerId: UUID,
        page: Int,
        size: Int,
    ): PageResult<Booking>

    fun findAdminPage(
        status: BookingStatus?,
        customerId: UUID?,
        containerTypeId: UUID?,
        deliveryFrom: LocalDate?,
        deliveryTo: LocalDate?,
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

    fun findByBookingId(
        bookingId: UUID,
        page: Int,
        size: Int,
    ): PageResult<ContainerAssignment>

    fun countActiveAssignments(bookingIds: Collection<UUID>): Map<UUID, Int>

    fun hasActiveOrFutureAssignments(containerId: UUID, today: LocalDate): Boolean
}

interface InvoiceRepository {
    fun save(invoice: Invoice): Invoice

    fun findById(id: UUID): Invoice?

    fun findByIdForUpdate(id: UUID): Invoice?

    fun findByBookingId(bookingId: UUID): Invoice?

    fun findPage(status: InvoiceStatus?, page: Int, size: Int): PageResult<Invoice>

    fun nextInvoiceNumberSequenceValue(): Long

    fun updateStatus(id: UUID, status: InvoiceStatus): Invoice?
}
