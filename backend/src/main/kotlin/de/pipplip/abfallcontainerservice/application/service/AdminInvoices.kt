package de.pipplip.abfallcontainerservice.application.service

import de.pipplip.abfallcontainerservice.application.port.out.BookingRepository
import de.pipplip.abfallcontainerservice.application.port.out.InvoiceRepository
import de.pipplip.abfallcontainerservice.application.port.out.PageResult
import de.pipplip.abfallcontainerservice.domain.model.BookingStatus
import de.pipplip.abfallcontainerservice.domain.model.Invoice
import de.pipplip.abfallcontainerservice.domain.model.InvoiceCalculation
import de.pipplip.abfallcontainerservice.domain.model.InvoiceStatus
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLException
import java.time.Instant
import java.util.UUID

@Service
class AdminInvoiceService(
    private val bookingRepository: BookingRepository,
    private val invoiceRepository: InvoiceRepository,
) {
    @Transactional
    fun createForBooking(bookingId: UUID): Invoice {
        val booking =
            bookingRepository.findByIdForUpdate(bookingId) ?: throw AdminResourceNotFoundException()
        if (
            booking.status !in setOf(BookingStatus.PENDING, BookingStatus.CONFIRMED) ||
            invoiceRepository.findByBookingId(bookingId) != null
        ) {
            throw AdminConflictException()
        }

        val createdAt = Instant.now()
        val sequence = invoiceRepository.nextInvoiceNumberSequenceValue()
        val invoiceNumber =
            InvoiceCalculation.formatInvoiceNumber(
                InvoiceCalculation.invoiceYear(createdAt),
                sequence,
            )
        val invoice =
            try {
                InvoiceCalculation.create(booking, UUID.randomUUID(), invoiceNumber, createdAt)
            } catch (_: ArithmeticException) {
                throw AdminConflictException()
            } catch (_: IllegalArgumentException) {
                throw AdminConflictException()
            }

        return try {
            invoiceRepository.save(invoice)
        } catch (exception: DataIntegrityViolationException) {
            if (exception.hasSqlState("23505")) throw AdminConflictException()
            throw exception
        }
    }

    fun list(
        status: InvoiceStatus?,
        page: Int,
        size: Int,
    ): PageResult<Invoice> {
        requireValidPage(page, size)
        return invoiceRepository.findPage(status, page, size)
    }

    fun get(invoiceId: UUID): Invoice =
        invoiceRepository.findById(invoiceId) ?: throw AdminResourceNotFoundException()

    @Transactional
    fun transition(
        invoiceId: UUID,
        requestedStatus: InvoiceStatus,
    ): Invoice {
        val invoice =
            invoiceRepository.findByIdForUpdate(invoiceId) ?: throw AdminResourceNotFoundException()
        val allowed =
            when (invoice.status) {
                InvoiceStatus.DRAFT ->
                    requestedStatus == InvoiceStatus.SENT || requestedStatus == InvoiceStatus.VOID
                InvoiceStatus.SENT -> requestedStatus == InvoiceStatus.PAID
                InvoiceStatus.PAID, InvoiceStatus.VOID -> false
            }
        if (!allowed) throw AdminConflictException()
        return invoiceRepository.updateStatus(invoiceId, requestedStatus)
            ?: throw AdminResourceNotFoundException()
    }
}

private fun requireValidPage(
    page: Int,
    size: Int,
) {
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
