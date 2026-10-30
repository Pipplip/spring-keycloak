package de.pipplip.abfallcontainerservice.application.port.out

import de.pipplip.abfallcontainerservice.domain.model.Booking
import de.pipplip.abfallcontainerservice.domain.model.Container
import de.pipplip.abfallcontainerservice.domain.model.ContainerAssignment
import de.pipplip.abfallcontainerservice.domain.model.ContainerType
import de.pipplip.abfallcontainerservice.domain.model.Customer
import de.pipplip.abfallcontainerservice.domain.model.Invoice
import java.util.UUID

interface CustomerRepository {
    fun save(customer: Customer): Customer

    fun findById(id: UUID): Customer?

    fun findByKeycloakSubject(keycloakSubject: String): Customer?
}

interface ContainerTypeRepository {
    fun save(containerType: ContainerType): ContainerType

    fun findById(id: UUID): ContainerType?
}

interface ContainerRepository {
    fun save(container: Container): Container

    fun findById(id: UUID): Container?
}

interface BookingRepository {
    fun save(booking: Booking): Booking

    fun findById(id: UUID): Booking?
}

interface ContainerAssignmentRepository {
    fun save(assignment: ContainerAssignment): ContainerAssignment

    fun findById(id: UUID): ContainerAssignment?
}

interface InvoiceRepository {
    fun save(invoice: Invoice): Invoice

    fun findById(id: UUID): Invoice?
}
