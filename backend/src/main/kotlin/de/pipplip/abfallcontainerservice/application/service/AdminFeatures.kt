package de.pipplip.abfallcontainerservice.application.service

import de.pipplip.abfallcontainerservice.application.port.out.BookingRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerAssignmentRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerTypeRepository
import de.pipplip.abfallcontainerservice.application.port.out.CustomerRepository
import de.pipplip.abfallcontainerservice.application.port.out.InvoiceRepository
import de.pipplip.abfallcontainerservice.application.port.out.PageResult
import de.pipplip.abfallcontainerservice.domain.model.Address
import de.pipplip.abfallcontainerservice.domain.model.Booking
import de.pipplip.abfallcontainerservice.domain.model.BookingStatus
import de.pipplip.abfallcontainerservice.domain.model.Container
import de.pipplip.abfallcontainerservice.domain.model.ContainerAssignment
import de.pipplip.abfallcontainerservice.domain.model.ContainerStatus
import de.pipplip.abfallcontainerservice.domain.model.ContainerType
import de.pipplip.abfallcontainerservice.domain.model.Customer
import de.pipplip.abfallcontainerservice.domain.model.InvoiceStatus
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AdminResourceNotFoundException : RuntimeException()

class AdminRequestValidationException : RuntimeException()

class AdminConflictException : RuntimeException()

data class ContainerTypeInput(
    val name: String,
    val description: String,
    val volumeM3: BigDecimal,
    val dailyPriceCents: Long,
    val currencyCode: String,
    val active: Boolean,
)

data class ContainerInput(
    val inventoryNumber: String,
    val containerTypeId: UUID,
    val status: ContainerStatus,
)

data class CustomerProfileInput(
    val email: String,
    val firstName: String,
    val lastName: String,
    val companyName: String?,
    val billingAddress: Address,
)

@Service
class AdminContainerTypeService(
    private val containerTypeRepository: ContainerTypeRepository,
) {
    fun list(active: Boolean?, page: Int, size: Int): PageResult<ContainerType> {
        requireValidPage(page, size)
        return containerTypeRepository.findAdminPage(active, page, size)
    }

    fun get(id: UUID): ContainerType =
        containerTypeRepository.findById(id) ?: throw AdminResourceNotFoundException()

    @Transactional
    fun create(input: ContainerTypeInput): ContainerType {
        validate(input)
        return containerTypeRepository.save(input.toDomain(UUID.randomUUID()))
    }

    @Transactional
    fun update(id: UUID, input: ContainerTypeInput): ContainerType {
        validate(input)
        val existing =
            containerTypeRepository.findByIdForUpdate(id) ?: throw AdminResourceNotFoundException()
        val updated = input.toDomain(id)
        if (existing != updated && containerTypeRepository.hasBookings(id)) {
            throw AdminConflictException()
        }
        return containerTypeRepository.save(updated)
    }

    private fun validate(input: ContainerTypeInput) {
        if (
            input.name.isBlank() ||
            input.volumeM3 <= BigDecimal.ZERO ||
            input.volumeM3.scale() > 2 ||
            input.dailyPriceCents < 0 ||
            !CURRENCY_CODE.matches(input.currencyCode)
        ) {
            throw AdminRequestValidationException()
        }
    }

    private fun ContainerTypeInput.toDomain(id: UUID) =
        ContainerType(
            id = id,
            name = name,
            description = description,
            volumeM3 = volumeM3,
            dailyPriceCents = dailyPriceCents,
            currencyCode = currencyCode,
            active = active,
        )
}

@Service
class AdminContainerService(
    private val containerRepository: ContainerRepository,
    private val containerTypeRepository: ContainerTypeRepository,
    private val assignmentRepository: ContainerAssignmentRepository,
) {
    fun list(
        containerTypeId: UUID?,
        status: ContainerStatus?,
        page: Int,
        size: Int,
    ): PageResult<Container> {
        requireValidPage(page, size)
        return containerRepository.findAdminPage(containerTypeId, status, page, size)
    }

    fun get(id: UUID): Container =
        containerRepository.findById(id) ?: throw AdminResourceNotFoundException()

    @Transactional
    fun create(input: ContainerInput): Container {
        validate(input)
        requireContainerType(input.containerTypeId)
        return save(Container(UUID.randomUUID(), input.inventoryNumber, input.containerTypeId, input.status))
    }

    @Transactional
    fun update(id: UUID, input: ContainerInput): Container {
        validate(input)
        val existing =
            containerRepository.findByIdForUpdate(id) ?: throw AdminResourceNotFoundException()
        requireContainerType(input.containerTypeId)

        val typeChanged = existing.containerTypeId != input.containerTypeId
        val statusChanged = existing.status != input.status
        val movesOutOfService =
            statusChanged &&
                (input.status == ContainerStatus.MAINTENANCE || input.status == ContainerStatus.RETIRED)
        if (
            (typeChanged || movesOutOfService) &&
            assignmentRepository.hasActiveOrFutureAssignments(id, LocalDate.now())
        ) {
            throw AdminConflictException()
        }

        return save(Container(id, input.inventoryNumber, input.containerTypeId, input.status))
    }

    private fun validate(input: ContainerInput) {
        if (input.inventoryNumber.isBlank()) {
            throw AdminRequestValidationException()
        }
    }

    private fun requireContainerType(id: UUID) {
        if (containerTypeRepository.findById(id) == null) {
            throw AdminResourceNotFoundException()
        }
    }

    private fun save(container: Container): Container =
        try {
            containerRepository.save(container)
        } catch (exception: DataIntegrityViolationException) {
            if (exception.hasSqlState("23505")) throw AdminConflictException()
            throw exception
        }
}

@Service
class AdminCustomerService(
    private val customerRepository: CustomerRepository,
) {
    fun list(page: Int, size: Int): PageResult<Customer> {
        requireValidPage(page, size)
        return customerRepository.findPage(page, size)
    }

    fun get(id: UUID): Customer =
        customerRepository.findById(id) ?: throw AdminResourceNotFoundException()

    @Transactional
    fun update(id: UUID, input: CustomerProfileInput): Customer {
        validate(input)
        val existing = get(id)
        return try {
            customerRepository.save(
                existing.copy(
                    email = input.email,
                    firstName = input.firstName,
                    lastName = input.lastName,
                    companyName = input.companyName,
                    billingAddress = input.billingAddress,
                    updatedAt = Instant.now(),
                ),
            )
        } catch (exception: DataIntegrityViolationException) {
            if (exception.hasSqlState("23505")) throw AdminConflictException()
            throw exception
        }
    }

    private fun validate(input: CustomerProfileInput) {
        val address = input.billingAddress
        if (
            input.email.isBlank() ||
            input.firstName.isBlank() ||
            input.lastName.isBlank() ||
            address.street.isBlank() ||
            address.houseNumber.isBlank() ||
            address.postalCode.isBlank() ||
            address.city.isBlank() ||
            !COUNTRY_CODE.matches(address.countryCode)
        ) {
            throw AdminRequestValidationException()
        }
    }
}

@Service
class AdminBookingQueryService(
    private val bookingRepository: BookingRepository,
    private val assignmentRepository: ContainerAssignmentRepository,
) {
    fun list(
        status: BookingStatus?,
        customerId: UUID?,
        containerTypeId: UUID?,
        deliveryFrom: LocalDate?,
        deliveryTo: LocalDate?,
        page: Int,
        size: Int,
    ): PageResult<Booking> {
        requireValidPage(page, size)
        if (deliveryFrom != null && deliveryTo != null && deliveryFrom.isAfter(deliveryTo)) {
            throw AdminRequestValidationException()
        }
        return bookingRepository.findAdminPage(
            status,
            customerId,
            containerTypeId,
            deliveryFrom,
            deliveryTo,
            page,
            size,
        )
    }

    fun get(id: UUID): Booking =
        bookingRepository.findById(id) ?: throw AdminResourceNotFoundException()

    fun listAssignments(bookingId: UUID, page: Int, size: Int): PageResult<ContainerAssignment> {
        requireValidPage(page, size)
        if (bookingRepository.findById(bookingId) == null) {
            throw AdminResourceNotFoundException()
        }
        return assignmentRepository.findByBookingId(bookingId, page, size)
    }
}

@Service
class BookingAssignmentCountService(
    private val assignmentRepository: ContainerAssignmentRepository,
) {
    fun count(bookingIds: Collection<UUID>): Map<UUID, Int> =
        assignmentRepository.countActiveAssignments(bookingIds)
}

@Service
class AdminBookingStatusService(
    private val bookingRepository: BookingRepository,
    private val assignmentRepository: ContainerAssignmentRepository,
    private val invoiceRepository: InvoiceRepository,
) {
    @Transactional
    fun update(bookingId: UUID, requestedStatus: BookingStatus): Booking {
        val booking =
            bookingRepository.findByIdForUpdate(bookingId) ?: throw AdminResourceNotFoundException()
        val allowed =
            when (booking.status) {
                BookingStatus.PENDING ->
                    requestedStatus == BookingStatus.CONFIRMED || requestedStatus == BookingStatus.CANCELLED
                BookingStatus.CONFIRMED ->
                    requestedStatus == BookingStatus.DELIVERED || requestedStatus == BookingStatus.CANCELLED
                BookingStatus.DELIVERED -> requestedStatus == BookingStatus.COMPLETED
                BookingStatus.COMPLETED, BookingStatus.CANCELLED -> false
            }
        if (!allowed) throw AdminConflictException()
        if (
            requestedStatus == BookingStatus.CONFIRMED &&
            invoiceRepository.findByBookingId(bookingId)?.status != InvoiceStatus.PAID
        ) {
            throw AdminConflictException()
        }

        try {
            val now = Instant.now()
            val updated = bookingRepository.save(booking.copy(status = requestedStatus, updatedAt = now))
            if (requestedStatus == BookingStatus.CANCELLED) {
                assignmentRepository.releaseActiveAssignments(bookingId, now)
            }
            return updated
        } catch (exception: ObjectOptimisticLockingFailureException) {
            throw AdminConflictException()
        }
    }
}

@Service
class AdminContainerAssignmentService(
    private val bookingRepository: BookingRepository,
    private val containerRepository: ContainerRepository,
    private val assignmentRepository: ContainerAssignmentRepository,
    private val assignmentCountService: BookingAssignmentCountService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun assign(bookingId: UUID, containerId: UUID): ContainerAssignment {
        val booking =
            bookingRepository.findByIdForUpdate(bookingId) ?: throw AdminResourceNotFoundException()
        if (booking.status != BookingStatus.PENDING && booking.status != BookingStatus.CONFIRMED) {
            throw AdminConflictException()
        }
        val container =
            containerRepository.findByIdForUpdate(containerId) ?: throw AdminResourceNotFoundException()
        if (
            container.containerTypeId != booking.containerTypeId ||
            container.status != ContainerStatus.AVAILABLE
        ) {
            throw AdminConflictException()
        }

        val assignment =
            try {
                assignmentRepository.save(
                    ContainerAssignment(
                        id = UUID.randomUUID(),
                        bookingId = bookingId,
                        containerId = containerId,
                        assignedAt = Instant.now(),
                        releasedAt = null,
                    ),
                )
            } catch (exception: DataIntegrityViolationException) {
                if (exception.hasSqlState("23P01")) throw AdminConflictException()
                throw exception
            }

        val count = assignmentCountService.count(listOf(bookingId))[bookingId] ?: 0
        if (count > booking.quantity) {
            logger.warn(
                "Booking {} has {} active container assignments for requested quantity {}",
                bookingId,
                count,
                booking.quantity,
            )
        }
        return assignment
    }

    @Transactional
    fun release(bookingId: UUID, assignmentId: UUID): ContainerAssignment {
        val booking =
            bookingRepository.findByIdForUpdate(bookingId) ?: throw AdminResourceNotFoundException()
        if (booking.status != BookingStatus.PENDING && booking.status != BookingStatus.CONFIRMED) {
            throw AdminConflictException()
        }
        val assignment =
            assignmentRepository.findById(assignmentId) ?: throw AdminResourceNotFoundException()
        if (assignment.bookingId != bookingId) {
            throw AdminResourceNotFoundException()
        }
        if (assignment.releasedAt != null) {
            throw AdminConflictException()
        }
        return assignmentRepository.save(assignment.copy(releasedAt = Instant.now()))
    }
}

private fun requireValidPage(page: Int, size: Int) {
    if (page < 0 || size !in 1..100 || page > Int.MAX_VALUE / size) {
        throw AdminRequestValidationException()
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

private val CURRENCY_CODE = Regex("^[A-Z]{3}$")
private val COUNTRY_CODE = Regex("^[A-Z]{2}$")
