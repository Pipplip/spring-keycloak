package de.pipplip.abfallcontainerservice.infrastructure.persistence

import de.pipplip.abfallcontainerservice.application.port.out.BookingRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerAssignmentRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerTypeRepository
import de.pipplip.abfallcontainerservice.application.port.out.CustomerRepository
import de.pipplip.abfallcontainerservice.application.port.out.InvoiceRepository
import de.pipplip.abfallcontainerservice.application.port.out.PageResult
import de.pipplip.abfallcontainerservice.domain.model.Address
import de.pipplip.abfallcontainerservice.domain.model.Booking
import de.pipplip.abfallcontainerservice.domain.model.BookingStatus
import de.pipplip.abfallcontainerservice.domain.model.Container
import de.pipplip.abfallcontainerservice.domain.model.ContainerAssignment
import de.pipplip.abfallcontainerservice.domain.model.ContainerStatus
import de.pipplip.abfallcontainerservice.domain.model.ContainerType
import de.pipplip.abfallcontainerservice.domain.model.Customer
import de.pipplip.abfallcontainerservice.domain.model.Invoice
import de.pipplip.abfallcontainerservice.domain.model.InvoiceLine
import de.pipplip.abfallcontainerservice.domain.model.InvoiceStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private const val AVAILABLE_CONTAINER_COUNT_SQL =
    """
    SELECT COUNT(*)
    FROM containers c
    WHERE c.container_type_id = :containerTypeId
      AND c.status = 'AVAILABLE'
      AND NOT EXISTS (
          SELECT 1
          FROM container_assignments ca
          JOIN bookings b ON b.id = ca.booking_id
          WHERE ca.container_id = c.id
            AND ca.released_at IS NULL
            AND b.status <> 'CANCELLED'
            AND daterange(b.delivery_date, b.pickup_date, '[)')
                && daterange(:deliveryDate, :pickupDate, '[)')
      )
    """

private const val LOCK_AVAILABLE_CONTAINER_IDS_SQL =
    """
    SELECT c.id
    FROM containers c
    WHERE c.container_type_id = :containerTypeId
      AND c.status = 'AVAILABLE'
      AND NOT EXISTS (
          SELECT 1
          FROM container_assignments ca
          JOIN bookings b ON b.id = ca.booking_id
          WHERE ca.container_id = c.id
            AND ca.released_at IS NULL
            AND b.status <> 'CANCELLED'
            AND daterange(b.delivery_date, b.pickup_date, '[)')
                && daterange(:deliveryDate, :pickupDate, '[)')
      )
    ORDER BY c.id
    FOR UPDATE OF c SKIP LOCKED
    LIMIT :limit
    """

interface CustomerJpaRepository : JpaRepository<CustomerJpaEntity, UUID> {
    fun findByKeycloakSubject(keycloakSubject: String): CustomerJpaEntity?

    fun findByEmail(email: String): CustomerJpaEntity?
}

interface ContainerTypeJpaRepository : JpaRepository<ContainerTypeJpaEntity, UUID> {
    fun findByIdAndActiveTrue(id: UUID): ContainerTypeJpaEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ContainerTypeJpaEntity t where t.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): ContainerTypeJpaEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ContainerTypeJpaEntity t where t.id = :id and t.active = true")
    fun findActiveByIdForUpdate(@Param("id") id: UUID): ContainerTypeJpaEntity?

    fun findAllByActiveTrue(pageable: org.springframework.data.domain.Pageable):
        org.springframework.data.domain.Page<ContainerTypeJpaEntity>

    @Query(
        """
        select case when count(b.id) > 0 then true else false end
        from BookingJpaEntity b
        where b.containerType.id = :containerTypeId
        """,
    )
    fun hasBookings(@Param("containerTypeId") containerTypeId: UUID): Boolean
}

interface ContainerJpaRepository : JpaRepository<ContainerJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ContainerJpaEntity c where c.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): ContainerJpaEntity?

}

interface BookingJpaRepository : JpaRepository<BookingJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BookingJpaEntity b where b.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): BookingJpaEntity?

    @EntityGraph(attributePaths = ["customer", "containerType"])
    fun findByCustomer_Id(
        customerId: UUID,
        pageable: org.springframework.data.domain.Pageable,
    ): org.springframework.data.domain.Page<BookingJpaEntity>

}

interface ContainerAssignmentJpaRepository : JpaRepository<ContainerAssignmentJpaEntity, UUID> {
    @EntityGraph(attributePaths = ["booking", "container"])
    fun findByBooking_Id(
        bookingId: UUID,
        pageable: org.springframework.data.domain.Pageable,
    ): org.springframework.data.domain.Page<ContainerAssignmentJpaEntity>

    @Query(
        """
        select a.booking.id as bookingId, count(a.id) as assignmentCount
        from ContainerAssignmentJpaEntity a
        where a.booking.id in :bookingIds and a.releasedAt is null
        group by a.booking.id
        """,
    )
    fun countUnreleasedByBookingIds(
        @Param("bookingIds") bookingIds: Collection<UUID>,
    ): List<AssignmentCountProjection>

    @Query(
        """
        select case when count(a.id) > 0 then true else false end
        from ContainerAssignmentJpaEntity a
        where a.container.id = :containerId
          and a.releasedAt is null
          and a.booking.status <> de.pipplip.abfallcontainerservice.domain.model.BookingStatus.CANCELLED
          and a.booking.pickupDate > :today
        """,
    )
    fun hasActiveOrFutureAssignments(
        @Param("containerId") containerId: UUID,
        @Param("today") today: java.time.LocalDate,
    ): Boolean
}

interface AssignmentCountProjection {
    fun getBookingId(): UUID

    fun getAssignmentCount(): Long
}

interface InvoiceJpaRepository : JpaRepository<InvoiceJpaEntity, UUID> {
    @EntityGraph(attributePaths = ["booking", "lines"])
    fun findByBooking_Id(bookingId: UUID): InvoiceJpaEntity?

    @EntityGraph(attributePaths = ["booking", "lines"])
    @Query("select i from InvoiceJpaEntity i where i.id = :id")
    fun findWithBookingAndLinesById(@Param("id") id: UUID): InvoiceJpaEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InvoiceJpaEntity i where i.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): InvoiceJpaEntity?

    @EntityGraph(attributePaths = ["booking", "lines"])
    @Query("select i from InvoiceJpaEntity i where i.id in :ids")
    fun findWithBookingAndLinesByIdIn(@Param("ids") ids: Collection<UUID>): List<InvoiceJpaEntity>

    @Query(
        """
        select i.id from InvoiceJpaEntity i
        where (:status is null or i.status = :status)
        order by i.createdAt desc, i.id asc
        """,
    )
    fun findIdsByStatus(
        @Param("status") status: InvoiceStatus?,
        pageable: org.springframework.data.domain.Pageable,
    ): Page<UUID>
}

@Component
@Transactional
class CustomerPersistenceAdapter(
    private val repository: CustomerJpaRepository,
) : CustomerRepository {
    override fun save(customer: Customer): Customer =
        repository.saveAndFlush(
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

    @Transactional(readOnly = true)
    override fun findByKeycloakSubject(keycloakSubject: String): Customer? =
        repository.findByKeycloakSubject(keycloakSubject)?.toDomain()

    @Transactional(readOnly = true)
    override fun findByEmail(email: String): Customer? =
        repository.findByEmail(email)?.toDomain()

    @Transactional(readOnly = true)
    override fun findPage(page: Int, size: Int): PageResult<Customer> {
        val result = repository.findAll(PageRequest.of(page, size, Sort.by("id").ascending()))
        return PageResult(
            items = result.content.map { it.toDomain() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
        )
    }
}

@Component
@Transactional
class ContainerTypePersistenceAdapter(
    private val repository: ContainerTypeJpaRepository,
    private val entityManager: EntityManager,
) : ContainerTypeRepository {
    override fun save(containerType: ContainerType): ContainerType =
        repository.saveAndFlush(containerType.toJpa()).toDomain()

    @Transactional(readOnly = true)
    override fun findById(id: UUID): ContainerType? = repository.findById(id).orElse(null)?.toDomain()

    override fun findByIdForUpdate(id: UUID): ContainerType? =
        repository.findByIdForUpdate(id)?.toDomain()

    @Transactional(readOnly = true)
    override fun findActiveById(id: UUID): ContainerType? =
        repository.findByIdAndActiveTrue(id)?.toDomain()

    override fun findActiveByIdForUpdate(id: UUID): ContainerType? =
        repository.findActiveByIdForUpdate(id)?.toDomain()

    @Transactional(readOnly = true)
    override fun findActivePage(
        page: Int,
        size: Int,
    ): PageResult<ContainerType> {
        val result =
            repository.findAllByActiveTrue(
                PageRequest.of(page, size, Sort.by("id").ascending()),
            )
        return PageResult(
            items = result.content.map { it.toDomain() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
        )
    }

    @Transactional(readOnly = true)
    override fun findAdminPage(active: Boolean?, page: Int, size: Int): PageResult<ContainerType> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ContainerTypeJpaEntity::class.java)
        val root = query.from(ContainerTypeJpaEntity::class.java)
        active?.let { query.where(builder.equal(root.get<Boolean>("active"), it)) }
        query.orderBy(builder.asc(root.get<UUID>("id")))
        val items =
            entityManager.createQuery(query)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .resultList

        val countQuery = builder.createQuery(Long::class.java)
        val countRoot = countQuery.from(ContainerTypeJpaEntity::class.java)
        countQuery.select(builder.count(countRoot))
        active?.let { countQuery.where(builder.equal(countRoot.get<Boolean>("active"), it)) }
        return PageResult(
            items = items.map { it.toDomain() },
            page = page,
            size = size,
            totalElements = entityManager.createQuery(countQuery).singleResult,
        )
    }

    @Transactional(readOnly = true)
    override fun hasBookings(containerTypeId: UUID): Boolean =
        repository.hasBookings(containerTypeId)
}

@Component
@Transactional
class ContainerPersistenceAdapter(
    private val repository: ContainerJpaRepository,
    private val entityManager: EntityManager,
) : ContainerRepository {
    override fun save(container: Container): Container =
        repository.saveAndFlush(
            ContainerJpaEntity(
                id = container.id,
                inventoryNumber = container.inventoryNumber,
                containerType = entityManager.getReference(ContainerTypeJpaEntity::class.java, container.containerTypeId),
                status = container.status,
            ),
        ).toDomain()

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Container? = repository.findById(id).orElse(null)?.toDomain()

    override fun findByIdForUpdate(id: UUID): Container? =
        repository.findByIdForUpdate(id)?.toDomain()

    @Transactional(readOnly = true)
    override fun findAdminPage(
        containerTypeId: UUID?,
        status: ContainerStatus?,
        page: Int,
        size: Int,
    ): PageResult<Container> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ContainerJpaEntity::class.java)
        val root = query.from(ContainerJpaEntity::class.java)
        val predicates = containerPredicates(root, builder, containerTypeId, status)
        query.where(*predicates.toTypedArray())
        query.orderBy(builder.asc(root.get<String>("inventoryNumber")), builder.asc(root.get<UUID>("id")))
        val items =
            entityManager.createQuery(query)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .resultList

        val countQuery = builder.createQuery(Long::class.java)
        val countRoot = countQuery.from(ContainerJpaEntity::class.java)
        countQuery.select(builder.count(countRoot))
        countQuery.where(*containerPredicates(countRoot, builder, containerTypeId, status).toTypedArray())
        return PageResult(
            items = items.map { it.toDomain() },
            page = page,
            size = size,
            totalElements = entityManager.createQuery(countQuery).singleResult,
        )
    }
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

    override fun findByIdForUpdate(id: UUID): Booking? =
        repository.findByIdForUpdate(id)?.toDomain()

    @Transactional(readOnly = true)
    override fun findByCustomerId(
        customerId: UUID,
        page: Int,
        size: Int,
    ): PageResult<Booking> {
        val result =
            repository.findByCustomer_Id(
                customerId,
                PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").ascending())),
            )
        return PageResult(
            items = result.content.map { it.toDomain() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
        )
    }

    @Transactional(readOnly = true)
    override fun findAdminPage(
        status: BookingStatus?,
        customerId: UUID?,
        containerTypeId: UUID?,
        deliveryFrom: java.time.LocalDate?,
        deliveryTo: java.time.LocalDate?,
        page: Int,
        size: Int,
    ): PageResult<Booking> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(BookingJpaEntity::class.java)
        val root = query.from(BookingJpaEntity::class.java)
        val predicates =
            bookingPredicates(root, builder, status, customerId, containerTypeId, deliveryFrom, deliveryTo)
        query.where(*predicates.toTypedArray())
        query.orderBy(builder.desc(root.get<java.time.Instant>("createdAt")), builder.asc(root.get<UUID>("id")))
        val items =
            entityManager.createQuery(query)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .resultList

        val countQuery = builder.createQuery(Long::class.java)
        val countRoot = countQuery.from(BookingJpaEntity::class.java)
        countQuery.select(builder.count(countRoot))
        countQuery.where(
            *bookingPredicates(
                countRoot,
                builder,
                status,
                customerId,
                containerTypeId,
                deliveryFrom,
                deliveryTo,
            ).toTypedArray(),
        )
        return PageResult(
            items = items.map { it.toDomain() },
            page = page,
            size = size,
            totalElements = entityManager.createQuery(countQuery).singleResult,
        )
    }
}

@Component
@Transactional
class ContainerAssignmentPersistenceAdapter(
    private val repository: ContainerAssignmentJpaRepository,
    private val entityManager: EntityManager,
) : ContainerAssignmentRepository {
    override fun save(assignment: ContainerAssignment): ContainerAssignment =
        repository.saveAndFlush(
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

    @Transactional(readOnly = true)
    override fun findByBookingId(
        bookingId: UUID,
        page: Int,
        size: Int,
    ): PageResult<ContainerAssignment> {
        val result =
            repository.findByBooking_Id(
                bookingId,
                PageRequest.of(page, size, Sort.by("assignedAt").descending().and(Sort.by("id").ascending())),
            )
        return PageResult(
            items = result.content.map { it.toDomain() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
        )
    }

    @Transactional(readOnly = true)
    override fun countActiveAssignments(bookingIds: Collection<UUID>): Map<UUID, Int> {
        if (bookingIds.isEmpty()) return emptyMap()
        return repository.countUnreleasedByBookingIds(bookingIds)
            .associate { it.getBookingId() to it.getAssignmentCount().toInt() }
    }

    @Transactional(readOnly = true)
    override fun hasActiveOrFutureAssignments(containerId: UUID, today: java.time.LocalDate): Boolean =
        repository.hasActiveOrFutureAssignments(containerId, today)

    @Transactional(readOnly = true)
    override fun countAvailableContainers(
        containerTypeId: UUID,
        deliveryDate: java.time.LocalDate,
        pickupDate: java.time.LocalDate,
    ): Long =
        (entityManager.createNativeQuery(AVAILABLE_CONTAINER_COUNT_SQL)
            .setParameter("containerTypeId", containerTypeId)
            .setParameter("deliveryDate", deliveryDate)
            .setParameter("pickupDate", pickupDate)
            .singleResult as Number).toLong()

    override fun lockAvailableContainerIds(
        containerTypeId: UUID,
        deliveryDate: java.time.LocalDate,
        pickupDate: java.time.LocalDate,
        limit: Int,
    ): List<UUID> =
        entityManager.createNativeQuery(LOCK_AVAILABLE_CONTAINER_IDS_SQL)
            .setParameter("containerTypeId", containerTypeId)
            .setParameter("deliveryDate", deliveryDate)
            .setParameter("pickupDate", pickupDate)
            .setParameter("limit", limit)
            .resultList
            .map { it as UUID }

    override fun releaseActiveAssignments(
        bookingId: UUID,
        releasedAt: java.time.Instant,
    ) {
        entityManager.createNativeQuery(
            """
            UPDATE container_assignments
            SET released_at = :releasedAt
            WHERE booking_id = :bookingId
              AND released_at IS NULL
            """,
        )
            .setParameter("bookingId", bookingId)
            .setParameter("releasedAt", releasedAt)
            .executeUpdate()
    }
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
        return repository.saveAndFlush(entity).toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Invoice? = repository.findWithBookingAndLinesById(id)?.toDomain()

    @Transactional(readOnly = true)
    override fun findByIdForUpdate(id: UUID): Invoice? =
        repository.findByIdForUpdate(id)?.toDomain()

    @Transactional(readOnly = true)
    override fun findByBookingId(bookingId: UUID): Invoice? =
        repository.findByBooking_Id(bookingId)?.toDomain()

    @Transactional(readOnly = true)
    override fun findPage(
        status: InvoiceStatus?,
        page: Int,
        size: Int,
    ): PageResult<Invoice> {
        val idPage = repository.findIdsByStatus(status, PageRequest.of(page, size))
        val invoicesById =
            if (idPage.content.isEmpty()) {
                emptyMap()
            } else {
                repository.findWithBookingAndLinesByIdIn(idPage.content).associateBy { it.id }
            }
        return PageResult(
            items = idPage.content.map { id -> requireNotNull(invoicesById[id]).toDomain() },
            page = idPage.number,
            size = idPage.size,
            totalElements = idPage.totalElements,
        )
    }

    override fun nextInvoiceNumberSequenceValue(): Long =
        (entityManager.createNativeQuery("select nextval('invoice_number_seq')").singleResult as Number).toLong()

    override fun updateStatus(
        id: UUID,
        status: InvoiceStatus,
    ): Invoice? {
        val entity = repository.findByIdForUpdate(id) ?: return null
        entity.status = status
        return repository.saveAndFlush(entity).toDomain()
    }
}

private fun containerPredicates(
    root: Root<ContainerJpaEntity>,
    builder: CriteriaBuilder,
    containerTypeId: UUID?,
    status: ContainerStatus?,
): List<Predicate> =
    buildList {
        containerTypeId?.let {
            add(builder.equal(root.get<ContainerTypeJpaEntity>("containerType").get<UUID>("id"), it))
        }
        status?.let { add(builder.equal(root.get<ContainerStatus>("status"), it)) }
    }

private fun bookingPredicates(
    root: Root<BookingJpaEntity>,
    builder: CriteriaBuilder,
    status: BookingStatus?,
    customerId: UUID?,
    containerTypeId: UUID?,
    deliveryFrom: java.time.LocalDate?,
    deliveryTo: java.time.LocalDate?,
): List<Predicate> =
    buildList {
        status?.let { add(builder.equal(root.get<BookingStatus>("status"), it)) }
        customerId?.let {
            add(builder.equal(root.get<CustomerJpaEntity>("customer").get<UUID>("id"), it))
        }
        containerTypeId?.let {
            add(builder.equal(root.get<ContainerTypeJpaEntity>("containerType").get<UUID>("id"), it))
        }
        deliveryFrom?.let {
            add(builder.greaterThanOrEqualTo(root.get("deliveryDate"), it))
        }
        deliveryTo?.let {
            add(builder.lessThanOrEqualTo(root.get("deliveryDate"), it))
        }
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
