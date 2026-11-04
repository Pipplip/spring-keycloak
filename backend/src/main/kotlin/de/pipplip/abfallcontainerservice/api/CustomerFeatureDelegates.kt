package de.pipplip.abfallcontainerservice.api

import de.pipplip.abfallcontainerservice.application.port.out.PageResult
import de.pipplip.abfallcontainerservice.application.service.AvailabilityService
import de.pipplip.abfallcontainerservice.application.service.AdminInvoiceService
import de.pipplip.abfallcontainerservice.application.service.BookingAssignmentCountService
import de.pipplip.abfallcontainerservice.application.service.ContainerCatalogueService
import de.pipplip.abfallcontainerservice.application.service.CustomerBookingCancellationService
import de.pipplip.abfallcontainerservice.application.service.CustomerBookingQueryService
import de.pipplip.abfallcontainerservice.application.service.CustomerBookingService
import de.pipplip.abfallcontainerservice.application.service.CustomerInvoiceQueryService
import de.pipplip.abfallcontainerservice.application.service.CustomerSelfServiceProfileInput
import de.pipplip.abfallcontainerservice.application.service.CustomerProfileService
import de.pipplip.abfallcontainerservice.domain.model.Address as DomainAddress
import de.pipplip.abfallcontainerservice.domain.model.Booking as DomainBooking
import de.pipplip.abfallcontainerservice.domain.model.ContainerType as DomainContainerType
import de.pipplip.abfallcontainerservice.domain.model.Customer as DomainCustomer
import de.pipplip.abfallcontainerservice.domain.model.Invoice as DomainInvoice
import de.pipplip.abfallcontainerservice.domain.model.InvoiceStatus as DomainInvoiceStatus
import de.pipplip.abfallcontainerservice.generated.api.AvailabilityApiDelegate
import de.pipplip.abfallcontainerservice.generated.api.BookingsApiDelegate
import de.pipplip.abfallcontainerservice.generated.api.CatalogueApiDelegate
import de.pipplip.abfallcontainerservice.generated.api.CustomerProfilesApiDelegate
import de.pipplip.abfallcontainerservice.generated.api.InvoicesApiDelegate
import de.pipplip.abfallcontainerservice.generated.model.Address as ApiAddress
import de.pipplip.abfallcontainerservice.generated.model.Availability as ApiAvailability
import de.pipplip.abfallcontainerservice.generated.model.Booking as ApiBooking
import de.pipplip.abfallcontainerservice.generated.model.BookingPage as ApiBookingPage
import de.pipplip.abfallcontainerservice.generated.model.ContainerType as ApiContainerType
import de.pipplip.abfallcontainerservice.generated.model.ContainerTypePage as ApiContainerTypePage
import de.pipplip.abfallcontainerservice.generated.model.CustomerProfile as ApiCustomerProfile
import de.pipplip.abfallcontainerservice.generated.model.CustomerProfileRequest as ApiCustomerProfileRequest
import de.pipplip.abfallcontainerservice.generated.model.Invoice as ApiInvoice
import de.pipplip.abfallcontainerservice.generated.model.InvoiceLine as ApiInvoiceLine
import de.pipplip.abfallcontainerservice.generated.model.InvoicePage as ApiInvoicePage
import de.pipplip.abfallcontainerservice.generated.model.InvoiceStatus as ApiInvoiceStatus
import de.pipplip.abfallcontainerservice.generated.model.PageMetadata as ApiPageMetadata
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.ZoneOffset

@Service
class CatalogueApiDelegateImpl(
    private val catalogueService: ContainerCatalogueService,
) : CatalogueApiDelegate {
    override fun listContainerTypes(
        page: Int,
        size: Int,
    ): ResponseEntity<ApiContainerTypePage> {
        val result = catalogueService.listActiveContainerTypes(page, size)
        return ResponseEntity.ok(
            ApiContainerTypePage(
                items = result.items.map { it.toApi() },
                page = result.toApiMetadata(),
            ),
        )
    }

    override fun getContainerType(containerTypeId: java.util.UUID): ResponseEntity<ApiContainerType> =
        ResponseEntity.ok(catalogueService.getActiveContainerType(containerTypeId).toApi())
}

@Service
class AvailabilityApiDelegateImpl(
    private val availabilityService: AvailabilityService,
) : AvailabilityApiDelegate {
    override fun checkAvailability(
        containerTypeId: java.util.UUID,
        deliveryDate: java.time.LocalDate,
        pickupDate: java.time.LocalDate,
        quantity: Int,
    ): ResponseEntity<ApiAvailability> =
        ResponseEntity.ok(
            ApiAvailability(
                containerTypeId = containerTypeId,
                deliveryDate = deliveryDate,
                pickupDate = pickupDate,
                quantity = quantity,
                available =
                    availabilityService.checkAvailability(
                        containerTypeId,
                        deliveryDate,
                        pickupDate,
                        quantity,
                    ),
            ),
        )
}

@Service
class BookingsApiDelegateImpl(
    private val bookingService: CustomerBookingService,
    private val bookingQueryService: CustomerBookingQueryService,
    private val cancellationService: CustomerBookingCancellationService,
    private val assignmentCountService: BookingAssignmentCountService,
) : BookingsApiDelegate {
    override fun createBooking(
        bookingRequest: de.pipplip.abfallcontainerservice.generated.model.BookingRequest,
    ): ResponseEntity<ApiBooking> =
        bookingService.createBooking(
                containerTypeId = bookingRequest.containerTypeId,
                quantity = bookingRequest.quantity,
                deliveryDate = bookingRequest.deliveryDate,
                pickupDate = bookingRequest.pickupDate,
                deliveryAddress = bookingRequest.deliveryAddress.toDomain(),
            ).let { booking ->
                ResponseEntity.status(HttpStatus.CREATED)
                    .body(booking.toApi(assignedContainerCount = booking.quantity))
            }

    override fun getMyBooking(bookingId: java.util.UUID): ResponseEntity<ApiBooking> =
        bookingQueryService.getMyBooking(bookingId).let { booking ->
            ResponseEntity.ok(
                booking.toApi(
                    assignedContainerCount =
                        assignmentCountService.count(listOf(booking.id))[booking.id] ?: 0,
                ),
            )
        }

    override fun listMyBookings(
        page: Int,
        size: Int,
    ): ResponseEntity<ApiBookingPage> {
        val result = bookingQueryService.listMyBookings(page, size)
        val assignmentCounts = assignmentCountService.count(result.items.map { it.id })
        return ResponseEntity.ok(
            ApiBookingPage(
                items = result.items.map { booking ->
                    booking.toApi(assignedContainerCount = assignmentCounts[booking.id] ?: 0)
                },
                page = result.toApiMetadata(),
            ),
        )
    }

    override fun requestBookingCancellation(bookingId: java.util.UUID): ResponseEntity<ApiBooking> =
        cancellationService.cancelMyBooking(bookingId).let { booking ->
            ResponseEntity.ok(
                booking.toApi(
                    assignedContainerCount =
                        assignmentCountService.count(listOf(booking.id))[booking.id] ?: 0,
                ),
            )
        }
}

@Service
class InvoicesApiDelegateImpl(
    private val invoiceQueryService: CustomerInvoiceQueryService,
    private val adminInvoiceService: AdminInvoiceService,
) : InvoicesApiDelegate {
    override fun getMyBookingInvoice(bookingId: java.util.UUID): ResponseEntity<ApiInvoice> =
        ResponseEntity.ok(invoiceQueryService.getMyBookingInvoice(bookingId).toApi())

    override fun createAdminInvoiceForBooking(bookingId: java.util.UUID): ResponseEntity<ApiInvoice> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(adminInvoiceService.createForBooking(bookingId).toApi())

    override fun getAdminInvoice(invoiceId: java.util.UUID): ResponseEntity<ApiInvoice> =
        ResponseEntity.ok(adminInvoiceService.get(invoiceId).toApi())

    override fun listAdminInvoices(
        page: Int,
        size: Int,
        status: ApiInvoiceStatus?,
    ): ResponseEntity<ApiInvoicePage> {
        val result =
            adminInvoiceService.list(
                status = status?.let { DomainInvoiceStatus.valueOf(it.name) },
                page = page,
                size = size,
            )
        return ResponseEntity.ok(
            ApiInvoicePage(
                items = result.items.map { it.toApi() },
                page = result.toApiMetadata(),
            ),
        )
    }

    override fun markAdminInvoicePaid(invoiceId: java.util.UUID): ResponseEntity<ApiInvoice> =
        ResponseEntity.ok(adminInvoiceService.transition(invoiceId, DomainInvoiceStatus.PAID).toApi())

    override fun sendAdminInvoice(invoiceId: java.util.UUID): ResponseEntity<ApiInvoice> =
        ResponseEntity.ok(adminInvoiceService.transition(invoiceId, DomainInvoiceStatus.SENT).toApi())

    override fun voidAdminInvoice(invoiceId: java.util.UUID): ResponseEntity<ApiInvoice> =
        ResponseEntity.ok(adminInvoiceService.transition(invoiceId, DomainInvoiceStatus.VOID).toApi())
}

@Service
class CustomerProfilesApiDelegateImpl(
    private val customerProfileService: CustomerProfileService,
) : CustomerProfilesApiDelegate {
    override fun createMyCustomerProfile(
        customerProfileRequest: ApiCustomerProfileRequest,
    ): ResponseEntity<ApiCustomerProfile> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(customerProfileService.create(customerProfileRequest.toInput()).toApiProfile())

    override fun getMyCustomerProfile(): ResponseEntity<ApiCustomerProfile> =
        ResponseEntity.ok(customerProfileService.getCurrentProfile().toApiProfile())

    override fun updateMyCustomerProfile(
        customerProfileRequest: ApiCustomerProfileRequest,
    ): ResponseEntity<ApiCustomerProfile> =
        ResponseEntity.ok(customerProfileService.updateCurrentProfile(customerProfileRequest.toInput()).toApiProfile())
}

private fun ApiCustomerProfileRequest.toInput() =
    CustomerSelfServiceProfileInput(
        companyName = companyName,
        billingAddress = billingAddress.toDomain(),
    )

private fun DomainCustomer.toApiProfile() =
    ApiCustomerProfile(
        email = email,
        firstName = firstName,
        lastName = lastName,
        companyName = companyName,
        billingAddress = billingAddress.toApi(),
        createdAt = createdAt.atOffset(ZoneOffset.UTC),
        updatedAt = updatedAt.atOffset(ZoneOffset.UTC),
    )

internal fun DomainContainerType.toApi() =
    ApiContainerType(
        id = id,
        name = name,
        description = description,
        volumeM3 = volumeM3.toDouble(),
        dailyPriceCents = dailyPriceCents,
        currencyCode = currencyCode,
        active = active,
    )

internal fun ApiAddress.toDomain() =
    DomainAddress(
        street = street,
        houseNumber = houseNumber,
        postalCode = postalCode,
        city = city,
        countryCode = countryCode,
    )

internal fun DomainAddress.toApi() =
    ApiAddress(
        street = street,
        houseNumber = houseNumber,
        postalCode = postalCode,
        city = city,
        countryCode = countryCode,
    )

internal fun DomainBooking.toApi(assignedContainerCount: Int) =
    ApiBooking(
        id = id,
        customerId = customerId,
        containerTypeId = containerTypeId,
        quantity = quantity,
        deliveryDate = deliveryDate,
        pickupDate = pickupDate,
        deliveryAddress = deliveryAddress.toApi(),
        status = de.pipplip.abfallcontainerservice.generated.model.BookingStatus.valueOf(status.name),
        dailyPriceCents = dailyPriceCents,
        currencyCode = currencyCode,
        assignedContainerCount = assignedContainerCount,
        createdAt = createdAt.atOffset(ZoneOffset.UTC),
        updatedAt = updatedAt.atOffset(ZoneOffset.UTC),
        version = version,
    )

private fun DomainInvoice.toApi() =
    ApiInvoice(
        id = id,
        bookingId = bookingId,
        invoiceNumber = invoiceNumber,
        netAmountCents = netAmountCents,
        taxAmountCents = taxAmountCents,
        grossAmountCents = grossAmountCents,
        currencyCode = currencyCode,
        status = de.pipplip.abfallcontainerservice.generated.model.InvoiceStatus.valueOf(status.name),
        createdAt = createdAt.atOffset(ZoneOffset.UTC),
        lines =
            lines.map {
                ApiInvoiceLine(
                    id = it.id,
                    description = it.description,
                    quantity = it.quantity.toDouble(),
                    unitPriceCents = it.unitPriceCents,
                    taxRate = it.taxRate.toDouble(),
                    totalCents = it.totalCents,
                )
            },
    )

internal fun PageResult<*>.toApiMetadata() =
    ApiPageMetadata(
        page = page,
        propertySize = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )
