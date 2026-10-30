package de.pipplip.abfallcontainerservice.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class InvoiceStatus {
    DRAFT,
    SENT,
    PAID,
    VOID,
}

data class InvoiceLine(
    val id: UUID,
    val description: String,
    val quantity: BigDecimal,
    val unitPriceCents: Long,
    val taxRate: BigDecimal,
    val totalCents: Long,
)

data class Invoice(
    val id: UUID,
    val bookingId: UUID,
    val invoiceNumber: String,
    val netAmountCents: Long,
    val taxAmountCents: Long,
    val grossAmountCents: Long,
    val currencyCode: String,
    val status: InvoiceStatus,
    val createdAt: Instant,
    val lines: List<InvoiceLine>,
)
