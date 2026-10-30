package de.pipplip.abfallcontainerservice.infrastructure.persistence

import de.pipplip.abfallcontainerservice.application.port.out.BookingRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerAssignmentRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerTypeRepository
import de.pipplip.abfallcontainerservice.application.port.out.CustomerRepository
import de.pipplip.abfallcontainerservice.application.port.out.InvoiceRepository
import de.pipplip.abfallcontainerservice.domain.model.Address
import de.pipplip.abfallcontainerservice.domain.model.Booking
import de.pipplip.abfallcontainerservice.domain.model.Container
import de.pipplip.abfallcontainerservice.domain.model.ContainerAssignment
import de.pipplip.abfallcontainerservice.domain.model.ContainerType
import de.pipplip.abfallcontainerservice.domain.model.Customer
import de.pipplip.abfallcontainerservice.domain.model.Invoice
import de.pipplip.abfallcontainerservice.domain.model.InvoiceLine
import jakarta.persistence.EntityManager
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface CustomerJpaRepository : JpaRepository<CustomerJpaEntity, UUID>

interface ContainerTypeJpaRepository : JpaRepository<ContainerTypeJpaEntity, UUID>

interface ContainerJpaRepository : JpaRepository<ContainerJpaEntity, UUID>

interface BookingJpaRepository : JpaRepository<BookingJpaEntity, UUID>

interface ContainerAssignmentJpaRepository : JpaRepository<ContainerAssignmentJpaEntity, UUID>

interface InvoiceJpaRepository : JpaRepository<InvoiceJpaEntity, UUID>

@Component
@Transactional
class CustomerPersistenceAdapter(
    private val repository: CustomerJpaRepository,
) : CustomerRepository {
    override fun save(customer: Customer): Customer =
        repository.save(
            CustomerJpaEntity(
                id = customer.id,
                keycloakSubject = customer.keycloakSubject,
                email = customer.email,
                firstName = customer.firstName,
                lastName = customer.lastName,
                companyName = customer.companyName,
                billingAddress = customer.billingAddress.toJpa(),
                createdAt = customer.createdAt,
                updatedAt = customer.updatedAt,
            ),
        ).toDomain()

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Customer? = repository.findById(id).orElse(null)?.toDomain()
}

@Component
@Transactional
class ContainerTypePersistenceAdapter(
    private val repository: ContainerTypeJpaRepository,
) : ContainerTypeRepository {
    override fun save(containerType: ContainerType): ContainerType =
        repository.save(containerType.toJpa()).toDomain()

    @Transactional(readOnly = true)
    override fun findById(id: UUID): ContainerType? = repository.findById(id).orElse(null)?.toDomain()
}

@Component
@Transactional
class ContainerPersistenceAdapter(
    private val repository: ContainerJpaRepository,
    private val entityManager: EntityManager,
) : ContainerRepository {
    override fun save(container: Container): Container =
        repository.save(
            ContainerJpaEntity(
                id = container.id,
                inventoryNumber = container.inventoryNumber,
                containerType = entityManager.getReference(ContainerTypeJpaEntity::class.java, container.containerTypeId),
                status = container.status,
            ),
        ).toDomain()

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Container? = repository.findById(id).orElse(null)?.toDomain()
}

@Component
@Transactional
class BookingPersistenceAdapter(
    private val repository: BookingJpaRepository,
    private val entityManager: EntityManager,
) : BookingRepository {
    override fun save(booking: Booking): Booking {
        val saved =
            repository.save(
                BookingJpaEntity(
                    id = booking.id,
                    customer = entityManager.getReference(CustomerJpaEntity::class.java, booking.customerId),
                    containerType =
                        entityManager.getReference(
                            ContainerTypeJpaEntity::class.java,
                            booking.containerTypeId,
                        ),
                    quantity = booking.quantity,
                    deliveryDate = booking.deliveryDate,
                    pickupDate = booking.pickupDate,
                    deliveryAddress = booking.deliveryAddress.toJpa(),
                    status = booking.status,
                    dailyPriceCents = booking.dailyPriceCents,
                    deliveryFeeCents = booking.deliveryFeeCents,
                    currencyCode = booking.currencyCode,
                    createdAt = booking.createdAt,
                    updatedAt = booking.updatedAt,
                    version = booking.version,
                ),
            )
        repository.flush()
        return saved.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Booking? = repository.findById(id).orElse(null)?.toDomain()
}

@Component
@Transactional
class ContainerAssignmentPersistenceAdapter(
    private val repository: ContainerAssignmentJpaRepository,
    private val entityManager: EntityManager,
) : ContainerAssignmentRepository {
    override fun save(assignment: ContainerAssignment): ContainerAssignment =
        repository.save(
            ContainerAssignmentJpaEntity(
                id = assignment.id,
                booking = entityManager.getReference(BookingJpaEntity::class.java, assignment.bookingId),
                container = entityManager.getReference(ContainerJpaEntity::class.java, assignment.containerId),
                assignedAt = assignment.assignedAt,
                releasedAt = assignment.releasedAt,
            ),
        ).toDomain()

    @Transactional(readOnly = true)
    override fun findById(id: UUID): ContainerAssignment? = repository.findById(id).orElse(null)?.toDomain()
}

@Component
@Transactional
class InvoicePersistenceAdapter(
    private val repository: InvoiceJpaRepository,
    private val entityManager: EntityManager,
) : InvoiceRepository {
    override fun save(invoice: Invoice): Invoice {
        val entity =
            InvoiceJpaEntity(
                id = invoice.id,
                booking = entityManager.getReference(BookingJpaEntity::class.java, invoice.bookingId),
                invoiceNumber = invoice.invoiceNumber,
                netAmountCents = invoice.netAmountCents,
                taxAmountCents = invoice.taxAmountCents,
                grossAmountCents = invoice.grossAmountCents,
                currencyCode = invoice.currencyCode,
                status = invoice.status,
                createdAt = invoice.createdAt,
                lines = mutableListOf(),
            )
        entity.lines.addAll(invoice.lines.map { it.toJpa(entity) })
        return repository.save(entity).toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Invoice? = repository.findById(id).orElse(null)?.toDomain()
}

private fun Address.toJpa() =
    AddressJpaEmbeddable(
        street = street,
        houseNumber = houseNumber,
        postalCode = postalCode,
        city = city,
        countryCode = countryCode,
    )

private fun AddressJpaEmbeddable.toDomain() =
    Address(
        street = street,
        houseNumber = houseNumber,
        postalCode = postalCode,
        city = city,
        countryCode = countryCode,
    )

private fun CustomerJpaEntity.toDomain() =
    Customer(
        id = id,
        keycloakSubject = keycloakSubject,
        email = email,
        firstName = firstName,
        lastName = lastName,
        companyName = companyName,
        billingAddress = billingAddress.toDomain(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun ContainerType.toJpa() =
    ContainerTypeJpaEntity(
        id = id,
        name = name,
        description = description,
        volumeM3 = volumeM3,
        dailyPriceCents = dailyPriceCents,
        currencyCode = currencyCode,
        active = active,
    )

private fun ContainerTypeJpaEntity.toDomain() =
    ContainerType(
        id = id,
        name = name,
        description = description,
        volumeM3 = volumeM3,
        dailyPriceCents = dailyPriceCents,
        currencyCode = currencyCode,
        active = active,
    )

private fun ContainerJpaEntity.toDomain() =
    Container(
        id = id,
        inventoryNumber = inventoryNumber,
        containerTypeId = containerType.id,
        status = status,
    )

private fun BookingJpaEntity.toDomain() =
    Booking(
        id = id,
        customerId = customer.id,
        containerTypeId = containerType.id,
        quantity = quantity,
        deliveryDate = deliveryDate,
        pickupDate = pickupDate,
        deliveryAddress = deliveryAddress.toDomain(),
        status = status,
        dailyPriceCents = dailyPriceCents,
        deliveryFeeCents = deliveryFeeCents,
        currencyCode = currencyCode,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
    )

private fun ContainerAssignmentJpaEntity.toDomain() =
    ContainerAssignment(
        id = id,
        bookingId = booking.id,
        containerId = container.id,
        assignedAt = assignedAt,
        releasedAt = releasedAt,
    )

private fun InvoiceLine.toJpa(invoice: InvoiceJpaEntity) =
    InvoiceLineJpaEntity(
        id = id,
        invoice = invoice,
        description = description,
        quantity = quantity,
        unitPriceCents = unitPriceCents,
        taxRate = taxRate,
        totalCents = totalCents,
    )

private fun InvoiceJpaEntity.toDomain() =
    Invoice(
        id = id,
        bookingId = booking.id,
        invoiceNumber = invoiceNumber,
        netAmountCents = netAmountCents,
        taxAmountCents = taxAmountCents,
        grossAmountCents = grossAmountCents,
        currencyCode = currencyCode,
        status = status,
        createdAt = createdAt,
        lines =
            lines.map {
                InvoiceLine(
                    id = it.id,
                    description = it.description,
                    quantity = it.quantity,
                    unitPriceCents = it.unitPriceCents,
                    taxRate = it.taxRate,
                    totalCents = it.totalCents,
                )
            },
    )
