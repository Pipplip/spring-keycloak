package de.pipplip.abfallcontainerservice

import de.pipplip.abfallcontainerservice.domain.model.Address
import de.pipplip.abfallcontainerservice.domain.model.Booking
import de.pipplip.abfallcontainerservice.domain.model.BookingStatus
import de.pipplip.abfallcontainerservice.domain.model.InvoiceCalculation
import de.pipplip.abfallcontainerservice.domain.model.InvoiceStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class InvoiceCalculationTest {
    @Test
    fun `calculates net amount and line quantity using half-open rental days`() {
        val invoice =
            InvoiceCalculation.create(
                booking = booking(quantity = 2, dailyPriceCents = 2500),
                invoiceId = UUID.randomUUID(),
                invoiceNumber = "2026-000001",
                createdAt = Instant.parse("2026-02-01T10:00:00Z"),
            )

        assertThat(invoice.netAmountCents).isEqualTo(15000)
        assertThat(invoice.taxAmountCents).isEqualTo(2850)
        assertThat(invoice.grossAmountCents).isEqualTo(17850)
        assertThat(invoice.currencyCode).isEqualTo("EUR")
        assertThat(invoice.status).isEqualTo(InvoiceStatus.DRAFT)
        val line = invoice.lines.single()
        assertThat(line.quantity.toPlainString()).isEqualTo("6.000")
        assertThat(line.unitPriceCents).isEqualTo(2500)
        assertThat(line.taxRate.toPlainString()).isEqualTo("0.1900")
        assertThat(line.totalCents).isEqualTo(15000)
    }

    @Test
    fun `rounds VAT half up to whole cents per line`() {
        val invoice =
            InvoiceCalculation.create(
                booking =
                    booking(
                        quantity = 1,
                        dailyPriceCents = 50,
                        deliveryDate = LocalDate.now(),
                        pickupDate = LocalDate.now().plusDays(1),
                    ),
                invoiceId = UUID.randomUUID(),
                invoiceNumber = "2026-000002",
                createdAt = Instant.parse("2026-02-01T10:00:00Z"),
            )

        assertThat(invoice.netAmountCents).isEqualTo(50)
        assertThat(invoice.taxAmountCents).isEqualTo(10)
        assertThat(invoice.grossAmountCents).isEqualTo(60)
    }

    @Test
    fun `formats globally allocated invoice sequence with UTC year`() {
        val createdAt = Instant.parse("2025-12-31T23:30:00Z")

        assertThat(InvoiceCalculation.invoiceYear(createdAt)).isEqualTo(2025)
        assertThat(InvoiceCalculation.formatInvoiceNumber(2025, 1)).isEqualTo("2025-000001")
        assertThat(InvoiceCalculation.formatInvoiceNumber(2025, 1_000_000)).isEqualTo("2025-1000000")
    }

    @Test
    fun `rejects invalid booking interval and unrepresentable totals`() {
        assertThatThrownBy {
            InvoiceCalculation.create(
                booking(deliveryDate = LocalDate.now(), pickupDate = LocalDate.now()),
                UUID.randomUUID(),
                "2026-000003",
                Instant.now(),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)

        assertThatThrownBy {
            InvoiceCalculation.create(
                booking(
                    quantity = 1,
                    dailyPriceCents = Long.MAX_VALUE,
                    pickupDate = LocalDate.now().plusDays(2),
                ),
                UUID.randomUUID(),
                "2026-000004",
                Instant.now(),
            )
        }.isInstanceOf(ArithmeticException::class.java)
    }

    private fun booking(
        quantity: Int = 1,
        dailyPriceCents: Long = 2500,
        deliveryDate: LocalDate = LocalDate.parse("2026-02-01"),
        pickupDate: LocalDate = LocalDate.parse("2026-02-04"),
    ) = Booking(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        containerTypeId = UUID.randomUUID(),
        quantity = quantity,
        deliveryDate = deliveryDate,
        pickupDate = pickupDate,
        deliveryAddress =
            Address(
                street = "Main Street",
                houseNumber = "1",
                postalCode = "12345",
                city = "Test City",
                countryCode = "DE",
            ),
        status = BookingStatus.PENDING,
        dailyPriceCents = dailyPriceCents,
        currencyCode = "EUR",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        version = 0,
    )
}
