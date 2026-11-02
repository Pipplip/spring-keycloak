package de.pipplip.abfallcontainerservice.application.service

import de.pipplip.abfallcontainerservice.application.port.out.BookingRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerAssignmentRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerTypeRepository
import de.pipplip.abfallcontainerservice.application.port.out.InvoiceRepository
import de.pipplip.abfallcontainerservice.application.port.out.PageResult
import de.pipplip.abfallcontainerservice.domain.model.Address
import de.pipplip.abfallcontainerservice.domain.model.Booking
import de.pipplip.abfallcontainerservice.domain.model.BookingStatus
import de.pipplip.abfallcontainerservice.domain.model.ContainerAssignment
import de.pipplip.abfallcontainerservice.domain.model.ContainerType
import de.pipplip.abfallcontainerservice.domain.model.Invoice
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class InvalidCustomerFeatureRequestException : RuntimeException()

class ContainerTypeNotFoundException : RuntimeException()

class InsufficientInventoryException : RuntimeException()

class BookingConflictException : RuntimeException()

class InvoiceNotFoundException : RuntimeException()

@Service
class ContainerCatalogueService(
    private val containerTypeRepository: ContainerTypeRepository,
) {
    fun listActiveContainerTypes(
        page: Int,
        size: Int,
    ): PageResult<ContainerType> {
        requireValidPage(page, size)
        return containerTypeRepository.findActivePage(page, size)
    }

    fun getActiveContainerType(id: UUID): ContainerType =
        containerTypeRepository.findActiveById(id) ?: throw ContainerTypeNotFoundException()
}

@Service
class AvailabilityService(
    private val containerTypeRepository: ContainerTypeRepository,
    private val assignmentRepository: ContainerAssignmentRepository,
) {
    fun checkAvailability(
        containerTypeId: UUID,
        deliveryDate: LocalDate,
        pickupDate: LocalDate,
        quantity: Int,
    ): Boolean {
        validateRentalRequest(deliveryDate, pickupDate, quantity)
        containerTypeRepository.findActiveById(containerTypeId) ?: throw ContainerTypeNotFoundException()
        return assignmentRepository.countAvailableContainers(containerTypeId, deliveryDate, pickupDate) >= quantity
    }
}

@Service
class CustomerBookingService(
    private val currentCustomerService: CurrentCustomerService,
    private val containerTypeRepository: ContainerTypeRepository,
    private val bookingRepository: BookingRepository,
    private val assignmentRepository: ContainerAssignmentRepository,
) {
    @Transactional
    fun createBooking(
        containerTypeId: UUID,
        quantity: Int,
        deliveryDate: LocalDate,
        pickupDate: LocalDate,
        deliveryAddress: Address,
    ): Booking {
        validateRentalRequest(deliveryDate, pickupDate, quantity)
        val customer = currentCustomerService.requireCurrentCustomer()
        val containerType =
            containerTypeRepository.findActiveById(containerTypeId) ?: throw ContainerTypeNotFoundException()
        val containerIds =
            assignmentRepository.lockAvailableContainerIds(
                containerTypeId = containerTypeId,
                deliveryDate = deliveryDate,
                pickupDate = pickupDate,
                limit = quantity,
            )
        if (containerIds.size < quantity) {
            throw InsufficientInventoryException()
        }

        val now = Instant.now()
        val booking =
            bookingRepository.save(
                Booking(
                    id = UUID.randomUUID(),
                    customerId = customer.id,
                    containerTypeId = containerType.id,
                    quantity = quantity,
                    deliveryDate = deliveryDate,
                    pickupDate = pickupDate,
                    deliveryAddress = deliveryAddress,
                    status = BookingStatus.PENDING,
                    dailyPriceCents = containerType.dailyPriceCents,
                    currencyCode = containerType.currencyCode,
                    createdAt = now,
                    updatedAt = now,
                    version = 0,
                ),
            )

        try {
            containerIds.forEach { containerId ->
                assignmentRepository.save(
                    ContainerAssignment(
                        id = UUID.randomUUID(),
                        bookingId = booking.id,
                        containerId = containerId,
                        assignedAt = now,
                        releasedAt = null,
                    ),
                )
            }
        } catch (exception: DataIntegrityViolationException) {
            if (exception.hasSqlState("23P01")) {
                throw BookingConflictException()
            }
            throw exception
        }
        return booking
    }
}

@Service
class CustomerBookingQueryService(
    private val currentCustomerService: CurrentCustomerService,
    private val bookingRepository: BookingRepository,
    private val bookingOwnershipAuthorizer: BookingOwnershipAuthorizer,
) {
    fun listMyBookings(
        page: Int,
        size: Int,
    ): PageResult<Booking> {
        requireValidPage(page, size)
        val customer = currentCustomerService.requireCurrentCustomer()
        return bookingRepository.findByCustomerId(customer.id, page, size)
    }

    fun getMyBooking(bookingId: UUID): Booking = bookingOwnershipAuthorizer.requireOwner(bookingId)
}

@Service
class CustomerBookingCancellationService(
    private val bookingOwnershipAuthorizer: BookingOwnershipAuthorizer,
    private val bookingRepository: BookingRepository,
    private val assignmentRepository: ContainerAssignmentRepository,
) {
    @Transactional
    fun cancelMyBooking(bookingId: UUID): Booking {
        val booking = bookingOwnershipAuthorizer.requireOwner(bookingId)
        if (booking.status != BookingStatus.PENDING && booking.status != BookingStatus.CONFIRMED) {
            throw BookingConflictException()
        }

        val now = Instant.now()
        val cancelled = bookingRepository.save(booking.copy(status = BookingStatus.CANCELLED, updatedAt = now))
        assignmentRepository.releaseActiveAssignments(bookingId, now)
        return cancelled
    }
}

@Service
class CustomerInvoiceQueryService(
    private val bookingOwnershipAuthorizer: BookingOwnershipAuthorizer,
    private val invoiceRepository: InvoiceRepository,
) {
    fun getMyBookingInvoice(bookingId: UUID): Invoice {
        bookingOwnershipAuthorizer.requireOwner(bookingId)
        return invoiceRepository.findByBookingId(bookingId) ?: throw InvoiceNotFoundException()
    }
}

private fun validateRentalRequest(
    deliveryDate: LocalDate,
    pickupDate: LocalDate,
    quantity: Int,
) {
    if (!pickupDate.isAfter(deliveryDate) || quantity <= 0) {
        throw InvalidCustomerFeatureRequestException()
    }
}

private fun requireValidPage(
    page: Int,
    size: Int,
) {
    if (page < 0 || size !in 1..100) {
        throw InvalidCustomerFeatureRequestException()
    }
}

private fun Throwable.hasSqlState(sqlState: String): Boolean {
    var cause: Throwable? = this
    while (cause != null) {
        if (cause is SQLException && cause.sqlState == sqlState) {
            return true
        }
        cause = cause.cause
    }
    return false
}
