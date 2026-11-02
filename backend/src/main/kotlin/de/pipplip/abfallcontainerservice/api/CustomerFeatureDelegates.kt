package de.pipplip.abfallcontainerservice.api

import de.pipplip.abfallcontainerservice.application.port.out.PageResult
import de.pipplip.abfallcontainerservice.application.service.AvailabilityService
import de.pipplip.abfallcontainerservice.application.service.ContainerCatalogueService
import de.pipplip.abfallcontainerservice.application.service.CustomerBookingCancellationService
import de.pipplip.abfallcontainerservice.application.service.CustomerBookingQueryService
import de.pipplip.abfallcontainerservice.application.service.CustomerBookingService
import de.pipplip.abfallcontainerservice.application.service.CustomerInvoiceQueryService
import de.pipplip.abfallcontainerservice.domain.model.Address as DomainAddress
import de.pipplip.abfallcontainerservice.domain.model.Booking as DomainBooking
import de.pipplip.abfallcontainerservice.domain.model.ContainerType as DomainContainerType
import de.pipplip.abfallcontainerservice.domain.model.Invoice as DomainInvoice
import de.pipplip.abfallcontainerservice.generated.api.AvailabilityApiDelegate
import de.pipplip.abfallcontainerservice.generated.api.BookingsApiDelegate
import de.pipplip.abfallcontainerservice.generated.api.CatalogueApiDelegate
import de.pipplip.abfallcontainerservice.generated.api.InvoicesApiDelegate
import de.pipplip.abfallcontainerservice.generated.model.Address as ApiAddress
import de.pipplip.abfallcontainerservice.generated.model.Availability as ApiAvailability
import de.pipplip.abfallcontainerservice.generated.model.Booking as ApiBooking
import de.pipplip.abfallcontainerservice.generated.model.BookingPage as ApiBookingPage
import de.pipplip.abfallcontainerservice.generated.model.ContainerType as ApiContainerType
import de.pipplip.abfallcontainerservice.generated.model.ContainerTypePage as ApiContainerTypePage
import de.pipplip.abfallcontainerservice.generated.model.Invoice as ApiInvoice
import de.pipplip.abfallcontainerservice.generated.model.InvoiceLine as ApiInvoiceLine
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
) : BookingsApiDelegate {
    override fun createBooking(
        bookingRequest: de.pipplip.abfallcontainerservice.generated.model.BookingRequest,
    ): ResponseEntity<ApiBooking> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            bookingService.createBooking(
                containerTypeId = bookingRequest.containerTypeId,
                quantity = bookingRequest.quantity,
                deliveryDate = bookingRequest.deliveryDate,
                pickupDate = bookingRequest.pickupDate,
                deliveryAddress = bookingRequest.deliveryAddress.toDomain(),
            ).toApi(),
        )

    override fun getMyBooking(bookingId: java.util.UUID): ResponseEntity<ApiBooking> =
        ResponseEntity.ok(bookingQueryService.getMyBooking(bookingId).toApi())

    override fun listMyBookings(
        page: Int,
        size: Int,
    ): ResponseEntity<ApiBookingPage> {
        val result = bookingQueryService.listMyBookings(page, size)
        return ResponseEntity.ok(
            ApiBookingPage(
                items = result.items.map { it.toApi() },
                page = result.toApiMetadata(),
            ),
        )
    }

    override fun requestBookingCancellation(bookingId: java.util.UUID): ResponseEntity<ApiBooking> =
        ResponseEntity.ok(cancellationService.cancelMyBooking(bookingId).toApi())
}

@Service
class InvoicesApiDelegateImpl(
    private val invoiceQueryService: CustomerInvoiceQueryService,
) : InvoicesApiDelegate {
    override fun getMyBookingInvoice(bookingId: java.util.UUID): ResponseEntity<ApiInvoice> =
        ResponseEntity.ok(invoiceQueryService.getMyBookingInvoice(bookingId).toApi())
}

private fun DomainContainerType.toApi() =
    ApiContainerType(
        id = id,
        name = name,
        description = description,
        volumeM3 = volumeM3.toDouble(),
        dailyPriceCents = dailyPriceCents,
        currencyCode = currencyCode,
        active = active,
    )

private fun ApiAddress.toDomain() =
    DomainAddress(
        street = street,
        houseNumber = houseNumber,
        postalCode = postalCode,
        city = city,
        countryCode = countryCode,
    )

private fun DomainAddress.toApi() =
    ApiAddress(
        street = street,
        houseNumber = houseNumber,
        postalCode = postalCode,
        city = city,
        countryCode = countryCode,
    )

private fun DomainBooking.toApi() =
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

private fun PageResult<*>.toApiMetadata() =
    ApiPageMetadata(
        page = page,
        propertySize = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )
