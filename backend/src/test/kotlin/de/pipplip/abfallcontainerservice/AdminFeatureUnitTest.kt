package de.pipplip.abfallcontainerservice

import de.pipplip.abfallcontainerservice.application.port.out.BookingRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerAssignmentRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerTypeRepository
import de.pipplip.abfallcontainerservice.application.port.out.InvoiceRepository
import de.pipplip.abfallcontainerservice.application.service.AdminBookingStatusService
import de.pipplip.abfallcontainerservice.application.service.AdminConflictException
import de.pipplip.abfallcontainerservice.application.service.AdminContainerTypeService
import de.pipplip.abfallcontainerservice.application.service.AdminRequestValidationException
import de.pipplip.abfallcontainerservice.application.service.ContainerTypeInput
import de.pipplip.abfallcontainerservice.domain.model.Address
import de.pipplip.abfallcontainerservice.domain.model.Booking
import de.pipplip.abfallcontainerservice.domain.model.BookingStatus
import de.pipplip.abfallcontainerservice.domain.model.Invoice
import de.pipplip.abfallcontainerservice.domain.model.InvoiceStatus
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AdminFeatureUnitTest {
    private val bookingRepository = mockk<BookingRepository>()
    private val assignmentRepository = mockk<ContainerAssignmentRepository>()
    private val invoiceRepository = mockk<InvoiceRepository>()
    private val statusService =
        AdminBookingStatusService(bookingRepository, assignmentRepository, invoiceRepository)

    @Test
    fun `confirmation requires a paid invoice`() {
        val booking = booking()
        every { bookingRepository.findByIdForUpdate(booking.id) } returns booking
        every { invoiceRepository.findByBookingId(booking.id) } returns invoice(InvoiceStatus.SENT)

        assertThatThrownBy { statusService.update(booking.id, BookingStatus.CONFIRMED) }
            .isInstanceOf(AdminConflictException::class.java)
        verify(exactly = 0) { bookingRepository.save(any()) }
    }

    @Test
    fun `cancellation changes status and releases assignments`() {
        val booking = booking()
        every { bookingRepository.findByIdForUpdate(booking.id) } returns booking
        every { bookingRepository.save(any()) } answers { firstArg<Booking>() }
        every { assignmentRepository.releaseActiveAssignments(booking.id, any()) } just runs

        assertThat(statusService.update(booking.id, BookingStatus.CANCELLED).status)
            .isEqualTo(BookingStatus.CANCELLED)
        verify(exactly = 1) { assignmentRepository.releaseActiveAssignments(booking.id, any()) }
    }

    @Test
    fun `terminal booking transitions are rejected`() {
        val completed = booking().copy(status = BookingStatus.COMPLETED)
        every { bookingRepository.findByIdForUpdate(completed.id) } returns completed

        assertThatThrownBy { statusService.update(completed.id, BookingStatus.CANCELLED) }
            .isInstanceOf(AdminConflictException::class.java)
        verify(exactly = 0) { bookingRepository.save(any()) }
    }

    @Test
    fun `admin page and type values are validated before persistence`() {
        val typeRepository = mockk<ContainerTypeRepository>()
        val service = AdminContainerTypeService(typeRepository)

        assertThatThrownBy { service.list(active = null, page = 0, size = 101) }
            .isInstanceOf(AdminRequestValidationException::class.java)
        assertThatThrownBy {
            service.create(
                ContainerTypeInput(
                    name = "Waste",
                    description = "",
                    volumeM3 = BigDecimal("1.234"),
                    dailyPriceCents = 100,
                    currencyCode = "EUR",
                    active = true,
                ),
            )
        }.isInstanceOf(AdminRequestValidationException::class.java)
        verify(exactly = 0) { typeRepository.findAdminPage(any(), any(), any()) }
        verify(exactly = 0) { typeRepository.save(any()) }
    }

    private fun booking() =
        Booking(
            id = UUID.randomUUID(),
            customerId = UUID.randomUUID(),
            containerTypeId = UUID.randomUUID(),
            quantity = 1,
            deliveryDate = LocalDate.now().plusDays(5),
            pickupDate = LocalDate.now().plusDays(8),
            deliveryAddress =
                Address(
                    street = "Main Street",
                    houseNumber = "1",
                    postalCode = "12345",
                    city = "Test City",
                    countryCode = "DE",
                ),
            status = BookingStatus.PENDING,
            dailyPriceCents = 100,
            currencyCode = "EUR",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            version = 0,
        )

    private fun invoice(status: InvoiceStatus) =
        Invoice(
            id = UUID.randomUUID(),
            bookingId = UUID.randomUUID(),
            invoiceNumber = "INV-1",
            netAmountCents = 100,
            taxAmountCents = 0,
            grossAmountCents = 100,
            currencyCode = "EUR",
            status = status,
            createdAt = Instant.now(),
            lines = emptyList(),
        )
}
