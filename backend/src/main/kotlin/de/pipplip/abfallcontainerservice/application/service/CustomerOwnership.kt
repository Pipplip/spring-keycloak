package de.pipplip.abfallcontainerservice.application.service

import de.pipplip.abfallcontainerservice.application.port.out.AuthenticatedSubjectProvider
import de.pipplip.abfallcontainerservice.application.port.out.BookingRepository
import de.pipplip.abfallcontainerservice.application.port.out.CustomerRepository
import de.pipplip.abfallcontainerservice.domain.model.Booking
import de.pipplip.abfallcontainerservice.domain.model.Customer
import org.springframework.stereotype.Service
import java.util.UUID

class AuthenticatedSubjectMissingException : RuntimeException()

class CustomerProfileNotFoundException : RuntimeException()

class BookingNotFoundException : RuntimeException()

class BookingOwnershipDeniedException : RuntimeException()

@Service
class CurrentCustomerService(
    private val authenticatedSubjectProvider: AuthenticatedSubjectProvider,
    private val customerRepository: CustomerRepository,
) {
    fun requireCurrentCustomer(): Customer {
        val subject = authenticatedSubjectProvider.currentSubject() ?: throw AuthenticatedSubjectMissingException()
        return customerRepository.findByKeycloakSubject(subject) ?: throw CustomerProfileNotFoundException()
    }
}

@Service
class BookingOwnershipAuthorizer(
    private val currentCustomerService: CurrentCustomerService,
    private val bookingRepository: BookingRepository,
) {
    fun requireOwner(bookingId: UUID): Booking {
        val customer = currentCustomerService.requireCurrentCustomer()
        val booking = bookingRepository.findById(bookingId) ?: throw BookingNotFoundException()
        if (booking.customerId != customer.id) {
            throw BookingOwnershipDeniedException()
        }
        return booking
    }
}
