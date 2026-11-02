package de.pipplip.abfallcontainerservice.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

object InvoiceCalculation {
    val VAT_RATE: BigDecimal = BigDecimal("0.1900")

    fun create(
        booking: Booking,
        invoiceId: UUID,
        invoiceNumber: String,
        createdAt: Instant,
    ): Invoice {
        require(booking.quantity > 0)
        require(booking.dailyPriceCents >= 0)
        require(booking.pickupDate.isAfter(booking.deliveryDate))

        val rentalDays = ChronoUnit.DAYS.between(booking.deliveryDate, booking.pickupDate)
        val lineQuantity = Math.multiplyExact(booking.quantity.toLong(), rentalDays)
        require(lineQuantity <= MAX_LINE_QUANTITY)

        val netAmountCents = Math.multiplyExact(booking.dailyPriceCents, lineQuantity)
        val taxAmountCents =
            BigDecimal.valueOf(netAmountCents)
                .multiply(VAT_RATE)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        val grossAmountCents = Math.addExact(netAmountCents, taxAmountCents)

        return Invoice(
            id = invoiceId,
            bookingId = booking.id,
            invoiceNumber = invoiceNumber,
            netAmountCents = netAmountCents,
            taxAmountCents = taxAmountCents,
            grossAmountCents = grossAmountCents,
            currencyCode = booking.currencyCode,
            status = InvoiceStatus.DRAFT,
            createdAt = createdAt,
            lines =
                listOf(
                    InvoiceLine(
                        id = UUID.randomUUID(),
                        description = "Container rental (${booking.quantity} containers for $rentalDays days)",
                        quantity = BigDecimal.valueOf(lineQuantity).setScale(3),
                        unitPriceCents = booking.dailyPriceCents,
                        taxRate = VAT_RATE,
                        totalCents = netAmountCents,
                    ),
                ),
        )
    }

    fun formatInvoiceNumber(
        year: Int,
        sequence: Long,
    ): String {
        require(year in 1..9999)
        require(sequence > 0)
        return String.format(Locale.ROOT, "%04d-%06d", year, sequence)
    }

    fun invoiceYear(createdAt: Instant): Int = createdAt.atZone(ZoneOffset.UTC).year

    private const val MAX_LINE_QUANTITY = 999_999_999L
}
