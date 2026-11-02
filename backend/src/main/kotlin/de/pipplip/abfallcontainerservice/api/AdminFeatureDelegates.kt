package de.pipplip.abfallcontainerservice.api

import de.pipplip.abfallcontainerservice.application.service.AdminBookingQueryService
import de.pipplip.abfallcontainerservice.application.service.AdminBookingStatusService
import de.pipplip.abfallcontainerservice.application.service.AdminContainerAssignmentService
import de.pipplip.abfallcontainerservice.application.service.AdminContainerService
import de.pipplip.abfallcontainerservice.application.service.AdminContainerTypeService
import de.pipplip.abfallcontainerservice.application.service.AdminCustomerService
import de.pipplip.abfallcontainerservice.application.service.BookingAssignmentCountService
import de.pipplip.abfallcontainerservice.application.service.ContainerInput
import de.pipplip.abfallcontainerservice.application.service.ContainerTypeInput
import de.pipplip.abfallcontainerservice.application.service.CustomerProfileInput
import de.pipplip.abfallcontainerservice.domain.model.BookingStatus as DomainBookingStatus
import de.pipplip.abfallcontainerservice.domain.model.Container as DomainContainer
import de.pipplip.abfallcontainerservice.domain.model.ContainerAssignment as DomainContainerAssignment
import de.pipplip.abfallcontainerservice.domain.model.ContainerStatus as DomainContainerStatus
import de.pipplip.abfallcontainerservice.domain.model.Customer as DomainCustomer
import de.pipplip.abfallcontainerservice.generated.api.AdministrationApiDelegate
import de.pipplip.abfallcontainerservice.generated.model.Booking as ApiBooking
import de.pipplip.abfallcontainerservice.generated.model.BookingPage as ApiBookingPage
import de.pipplip.abfallcontainerservice.generated.model.BookingStatus as ApiBookingStatus
import de.pipplip.abfallcontainerservice.generated.model.Container as ApiContainer
import de.pipplip.abfallcontainerservice.generated.model.ContainerAssignment as ApiContainerAssignment
import de.pipplip.abfallcontainerservice.generated.model.ContainerAssignmentPage as ApiContainerAssignmentPage
import de.pipplip.abfallcontainerservice.generated.model.ContainerPage as ApiContainerPage
import de.pipplip.abfallcontainerservice.generated.model.ContainerType as ApiContainerType
import de.pipplip.abfallcontainerservice.generated.model.ContainerTypePage as ApiContainerTypePage
import de.pipplip.abfallcontainerservice.generated.model.Customer as ApiCustomer
import de.pipplip.abfallcontainerservice.generated.model.CustomerPage as ApiCustomerPage
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.util.UUID

@Service
class AdminFeatureApiDelegate(
    private val containerTypeService: AdminContainerTypeService,
    private val containerService: AdminContainerService,
    private val customerService: AdminCustomerService,
    private val bookingQueryService: AdminBookingQueryService,
    private val bookingStatusService: AdminBookingStatusService,
    private val assignmentService: AdminContainerAssignmentService,
    private val assignmentCountService: BookingAssignmentCountService,
) : AdministrationApiDelegate {
    override fun listAdminContainerTypes(
        page: Int,
        size: Int,
        active: Boolean?,
    ): ResponseEntity<ApiContainerTypePage> {
        val result = containerTypeService.list(active, page, size)
        return ResponseEntity.ok(
            ApiContainerTypePage(
                items = result.items.map { it.toApi() },
                page = result.toApiMetadata(),
            ),
        )
    }

    override fun createAdminContainerType(
        containerTypeRequest: de.pipplip.abfallcontainerservice.generated.model.ContainerTypeRequest,
    ): ResponseEntity<ApiContainerType> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(containerTypeService.create(containerTypeRequest.toInput()).toApi())

    override fun getAdminContainerType(containerTypeId: UUID): ResponseEntity<ApiContainerType> =
        ResponseEntity.ok(containerTypeService.get(containerTypeId).toApi())

    override fun updateAdminContainerType(
        containerTypeId: UUID,
        containerTypeRequest: de.pipplip.abfallcontainerservice.generated.model.ContainerTypeRequest,
    ): ResponseEntity<ApiContainerType> =
        ResponseEntity.ok(containerTypeService.update(containerTypeId, containerTypeRequest.toInput()).toApi())

    override fun listAdminContainers(
        page: Int,
        size: Int,
        status: de.pipplip.abfallcontainerservice.generated.model.ContainerStatus?,
        containerTypeId: UUID?,
    ): ResponseEntity<ApiContainerPage> {
        val result =
            containerService.list(
                containerTypeId = containerTypeId,
                status = status?.toDomain(),
                page = page,
                size = size,
            )
        return ResponseEntity.ok(
            ApiContainerPage(
                items = result.items.map { it.toApi() },
                page = result.toApiMetadata(),
            ),
        )
    }

    override fun createAdminContainer(
        containerRequest: de.pipplip.abfallcontainerservice.generated.model.ContainerRequest,
    ): ResponseEntity<ApiContainer> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(containerService.create(containerRequest.toInput()).toApi())

    override fun getAdminContainer(containerId: UUID): ResponseEntity<ApiContainer> =
        ResponseEntity.ok(containerService.get(containerId).toApi())

    override fun updateAdminContainer(
        containerId: UUID,
        containerRequest: de.pipplip.abfallcontainerservice.generated.model.ContainerRequest,
    ): ResponseEntity<ApiContainer> =
        ResponseEntity.ok(containerService.update(containerId, containerRequest.toInput()).toApi())

    override fun listAdminCustomers(page: Int, size: Int): ResponseEntity<ApiCustomerPage> {
        val result = customerService.list(page, size)
        return ResponseEntity.ok(
            ApiCustomerPage(
                items = result.items.map { it.toApi() },
                page = result.toApiMetadata(),
            ),
        )
    }

    override fun getAdminCustomer(customerId: UUID): ResponseEntity<ApiCustomer> =
        ResponseEntity.ok(customerService.get(customerId).toApi())

    override fun updateAdminCustomer(
        customerId: UUID,
        customerRequest: de.pipplip.abfallcontainerservice.generated.model.CustomerRequest,
    ): ResponseEntity<ApiCustomer> =
        ResponseEntity.ok(customerService.update(customerId, customerRequest.toInput()).toApi())

    override fun listAdminBookings(
        page: Int,
        size: Int,
        status: ApiBookingStatus?,
        customerId: UUID?,
        containerTypeId: UUID?,
        deliveryFrom: java.time.LocalDate?,
        deliveryTo: java.time.LocalDate?,
    ): ResponseEntity<ApiBookingPage> {
        val result =
            bookingQueryService.list(
                status = status?.let { DomainBookingStatus.valueOf(it.name) },
                customerId = customerId,
                containerTypeId = containerTypeId,
                deliveryFrom = deliveryFrom,
                deliveryTo = deliveryTo,
                page = page,
                size = size,
            )
        val counts = assignmentCountService.count(result.items.map { it.id })
        return ResponseEntity.ok(
            ApiBookingPage(
                items = result.items.map { it.toApi(counts[it.id] ?: 0) },
                page = result.toApiMetadata(),
            ),
        )
    }

    override fun getAdminBooking(bookingId: UUID): ResponseEntity<ApiBooking> =
        bookingQueryService.get(bookingId).let { booking ->
            ResponseEntity.ok(
                booking.toApi(assignmentCountService.count(listOf(booking.id))[booking.id] ?: 0),
            )
        }

    override fun updateAdminBookingStatus(
        bookingId: UUID,
        bookingStatusRequest: de.pipplip.abfallcontainerservice.generated.model.BookingStatusRequest,
    ): ResponseEntity<ApiBooking> =
        bookingStatusService.update(
            bookingId,
            DomainBookingStatus.valueOf(bookingStatusRequest.status.name),
        ).let { booking ->
            ResponseEntity.ok(
                booking.toApi(assignmentCountService.count(listOf(booking.id))[booking.id] ?: 0),
            )
        }

    override fun listAdminContainerAssignments(
        bookingId: UUID,
        page: Int,
        size: Int,
    ): ResponseEntity<ApiContainerAssignmentPage> {
        val result = bookingQueryService.listAssignments(bookingId, page, size)
        return ResponseEntity.ok(
            ApiContainerAssignmentPage(
                items = result.items.map { it.toApi() },
                page = result.toApiMetadata(),
            ),
        )
    }

    override fun assignAdminContainerToBooking(
        bookingId: UUID,
        containerAssignmentRequest:
            de.pipplip.abfallcontainerservice.generated.model.ContainerAssignmentRequest,
    ): ResponseEntity<ApiContainerAssignment> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(assignmentService.assign(bookingId, containerAssignmentRequest.containerId).toApi())

    override fun releaseAdminContainerAssignment(
        bookingId: UUID,
        assignmentId: UUID,
    ): ResponseEntity<ApiContainerAssignment> =
        ResponseEntity.ok(assignmentService.release(bookingId, assignmentId).toApi())
}

private fun de.pipplip.abfallcontainerservice.generated.model.ContainerTypeRequest.toInput() =
    ContainerTypeInput(
        name = name,
        description = description,
        volumeM3 = java.math.BigDecimal.valueOf(volumeM3),
        dailyPriceCents = dailyPriceCents,
        currencyCode = currencyCode,
        active = active,
    )

private fun de.pipplip.abfallcontainerservice.generated.model.ContainerRequest.toInput() =
    ContainerInput(
        inventoryNumber = inventoryNumber,
        containerTypeId = containerTypeId,
        status = status.toDomain(),
    )

private fun de.pipplip.abfallcontainerservice.generated.model.CustomerRequest.toInput() =
    CustomerProfileInput(
        email = email,
        firstName = firstName,
        lastName = lastName,
        companyName = companyName,
        billingAddress = billingAddress.toDomain(),
    )

private fun DomainContainerStatus.toApi() =
    de.pipplip.abfallcontainerservice.generated.model.ContainerStatus.valueOf(name)

private fun de.pipplip.abfallcontainerservice.generated.model.ContainerStatus.toDomain() =
    DomainContainerStatus.valueOf(name)

private fun DomainContainer.toApi() =
    ApiContainer(
        id = id,
        inventoryNumber = inventoryNumber,
        containerTypeId = containerTypeId,
        status = status.toApi(),
    )

private fun DomainCustomer.toApi() =
    ApiCustomer(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        companyName = companyName,
        billingAddress = billingAddress.toApi(),
        createdAt = createdAt.atOffset(ZoneOffset.UTC),
        updatedAt = updatedAt.atOffset(ZoneOffset.UTC),
    )

private fun DomainContainerAssignment.toApi() =
    ApiContainerAssignment(
        id = id,
        bookingId = bookingId,
        containerId = containerId,
        assignedAt = assignedAt.atOffset(ZoneOffset.UTC),
        releasedAt = releasedAt?.atOffset(ZoneOffset.UTC),
    )
