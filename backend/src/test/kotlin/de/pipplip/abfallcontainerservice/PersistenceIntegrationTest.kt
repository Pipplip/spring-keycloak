package de.pipplip.abfallcontainerservice

import de.pipplip.abfallcontainerservice.application.port.out.BookingRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerAssignmentRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerTypeRepository
import de.pipplip.abfallcontainerservice.application.port.out.CustomerRepository
import de.pipplip.abfallcontainerservice.application.port.out.InvoiceRepository
import de.pipplip.abfallcontainerservice.domain.model.Address
import de.pipplip.abfallcontainerservice.domain.model.Booking
import de.pipplip.abfallcontainerservice.domain.model.BookingStatus
import de.pipplip.abfallcontainerservice.domain.model.Container as PhysicalContainer
import de.pipplip.abfallcontainerservice.domain.model.ContainerAssignment
import de.pipplip.abfallcontainerservice.domain.model.ContainerStatus
import de.pipplip.abfallcontainerservice.domain.model.ContainerType
import de.pipplip.abfallcontainerservice.domain.model.Customer
import de.pipplip.abfallcontainerservice.domain.model.Invoice
import de.pipplip.abfallcontainerservice.domain.model.InvoiceLine
import de.pipplip.abfallcontainerservice.domain.model.InvoiceStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Testcontainers
@SpringBootTest
class PersistenceIntegrationTest {
    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var containerTypeRepository: ContainerTypeRepository

    @Autowired
    private lateinit var containerRepository: ContainerRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var assignmentRepository: ContainerAssignmentRepository

    @Autowired
    private lateinit var invoiceRepository: InvoiceRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var flyway: Flyway

    @Test
    fun `maps all persisted models and applies the migration once`() {
        val now = Instant.parse("2026-01-02T03:04:05Z")
        val customer = customerRepository.save(customer())
        val type = containerTypeRepository.save(containerType())
        val container = containerRepository.save(container(type.id))
        val booking = bookingRepository.save(booking(customer.id, type.id))
        val assignment =
            assignmentRepository.save(
                ContainerAssignment(
                    id = UUID.randomUUID(),
                    bookingId = booking.id,
                    containerId = container.id,
                    assignedAt = now,
                    releasedAt = null,
                ),
            )
        val invoice =
            invoiceRepository.save(
                Invoice(
                    id = UUID.randomUUID(),
                    bookingId = booking.id,
                    invoiceNumber = "INV-${UUID.randomUUID()}",
                    netAmountCents = 10000,
                    taxAmountCents = 1900,
                    grossAmountCents = 11900,
                    currencyCode = "EUR",
                    status = InvoiceStatus.DRAFT,
                    createdAt = now,
                    lines =
                        listOf(
                            InvoiceLine(
                                id = UUID.randomUUID(),
                                description = "Container rental",
                                quantity = BigDecimal("2.125"),
                                unitPriceCents = 4700,
                                taxRate = BigDecimal("0.1900"),
                                totalCents = 10000,
                            ),
                        ),
                ),
            )

        assertThat(customerRepository.findById(customer.id)).isEqualTo(customer)
        assertThat(containerTypeRepository.findById(type.id)).isEqualTo(type)
        assertThat(containerRepository.findById(container.id)).isEqualTo(container)
        assertThat(bookingRepository.findById(booking.id))
            .usingRecursiveComparison()
            .ignoringFields("version")
            .isEqualTo(booking)
        assertThat(assignmentRepository.findById(assignment.id)).isEqualTo(assignment)
        assertThat(assignmentRepository.findById(assignment.id)?.releasedAt).isNull()
        val releasedAssignment = assignmentRepository.save(assignment.copy(releasedAt = now))
        assertThat(releasedAssignment.releasedAt).isEqualTo(now)
        assertThat(invoiceRepository.findById(invoice.id)).isEqualTo(invoice)
        assertThatThrownBy {
            invoiceRepository.save(
                invoice.copy(
                    id = UUID.randomUUID(),
                    invoiceNumber = "INV-${UUID.randomUUID()}",
                ),
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)
        assertThat(flyway.info().applied().map { it.version?.version }).containsExactly("1")
        assertThat(flyway.migrate().migrationsExecuted).isZero()
        assertThat(
            requireNotNull(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success",
                Int::class.javaObjectType,
            )),
        ).isEqualTo(1)
    }

    @Test
    fun `database enforces uniqueness checks foreign keys and half-open assignment overlaps`() {
        val customer = customerRepository.save(customer())
        val type = containerTypeRepository.save(containerType())
        val container = containerRepository.save(container(type.id))
        val firstBooking =
            bookingRepository.save(
                booking(
                    customer.id,
                    type.id,
                    deliveryDate = LocalDate.parse("2026-02-01"),
                    pickupDate = LocalDate.parse("2026-02-05"),
                ),
            )
        val firstAssignment =
            assignmentRepository.save(
                ContainerAssignment(UUID.randomUUID(), firstBooking.id, container.id, instantNow(), null),
            )

        assertThatThrownBy {
            customerRepository.save(customer(email = customer.email))
        }.isInstanceOf(DataIntegrityViolationException::class.java)

        assertThatThrownBy {
            jdbcTemplate.update("UPDATE bookings SET quantity = 0 WHERE id = ?", firstBooking.id)
        }.isInstanceOf(DataIntegrityViolationException::class.java)

        assertThatThrownBy {
            jdbcTemplate.update("UPDATE bookings SET customer_id = ? WHERE id = ?", UUID.randomUUID(), firstBooking.id)
        }.isInstanceOf(DataIntegrityViolationException::class.java)

        assertThatThrownBy {
            jdbcTemplate.update("UPDATE containers SET status = 'UNKNOWN' WHERE id = ?", container.id)
        }.isInstanceOf(DataIntegrityViolationException::class.java)

        assertThatThrownBy {
            jdbcTemplate.update("UPDATE container_types SET volume_m3 = 0 WHERE id = ?", type.id)
        }.isInstanceOf(DataIntegrityViolationException::class.java)

        val lineId = UUID.randomUUID()
        invoiceRepository.save(
            Invoice(
                id = UUID.randomUUID(),
                bookingId = firstBooking.id,
                invoiceNumber = "INV-${UUID.randomUUID()}",
                netAmountCents = 0,
                taxAmountCents = 0,
                grossAmountCents = 0,
                currencyCode = "EUR",
                status = InvoiceStatus.DRAFT,
                createdAt = instantNow(),
                lines =
                    listOf(
                        InvoiceLine(
                            id = lineId,
                            description = "Test line",
                            quantity = BigDecimal("1.000"),
                            unitPriceCents = 0,
                            taxRate = BigDecimal("0.1900"),
                            totalCents = 0,
                        ),
                    ),
            ),
        )
        assertThatThrownBy {
            jdbcTemplate.update("UPDATE invoice_lines SET tax_rate = 1.0001 WHERE id = ?", lineId)
        }.isInstanceOf(DataIntegrityViolationException::class.java)

        val touchingBooking =
            bookingRepository.save(
                booking(
                    customer.id,
                    type.id,
                    deliveryDate = LocalDate.parse("2026-02-05"),
                    pickupDate = LocalDate.parse("2026-02-08"),
                ),
            )
        val touchingAssignment =
            assignmentRepository.save(
                ContainerAssignment(UUID.randomUUID(), touchingBooking.id, container.id, instantNow(), null),
            )
        assertThat(assignmentRepository.findById(touchingAssignment.id)).isNotNull

        val overlappingBooking =
            bookingRepository.save(
                booking(
                    customer.id,
                    type.id,
                    deliveryDate = LocalDate.parse("2026-02-04"),
                    pickupDate = LocalDate.parse("2026-02-07"),
                ),
            )
        assertThatThrownBy {
            assignmentRepository.save(
                ContainerAssignment(UUID.randomUUID(), overlappingBooking.id, container.id, instantNow(), null),
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)

        assertThatThrownBy {
            assignmentRepository.save(firstAssignment.copy(bookingId = overlappingBooking.id))
        }.isInstanceOf(DataIntegrityViolationException::class.java)

        assertThat(assignmentRepository.findById(firstAssignment.id)).isNotNull
    }

    @Test
    fun `booking version uses optimistic locking`() {
        val customer = customerRepository.save(customer())
        val type = containerTypeRepository.save(containerType())
        val initial = bookingRepository.save(booking(customer.id, type.id))
        val updated = bookingRepository.save(initial.copy(status = BookingStatus.CONFIRMED))

        assertThat(updated.version).isGreaterThan(initial.version)
        assertThatThrownBy {
            bookingRepository.save(initial.copy(status = BookingStatus.CANCELLED))
        }.isInstanceOf(ObjectOptimisticLockingFailureException::class.java)
    }

    private fun customer(
        email: String = "customer-${UUID.randomUUID()}@example.test",
    ) = Customer(
        id = UUID.randomUUID(),
        keycloakSubject = "subject-${UUID.randomUUID()}",
        email = email,
        firstName = "Ada",
        lastName = "Lovelace",
        companyName = null,
        billingAddress = address(),
        createdAt = instantNow(),
        updatedAt = instantNow(),
    )

    private fun containerType() =
        ContainerType(
            id = UUID.randomUUID(),
            name = "Mixed waste",
            description = "General household waste",
            volumeM3 = BigDecimal("5.00"),
            dailyPriceCents = 2500,
            currencyCode = "EUR",
            active = true,
        )

    private fun container(containerTypeId: UUID) =
        PhysicalContainer(
            id = UUID.randomUUID(),
            inventoryNumber = "CONT-${UUID.randomUUID()}",
            containerTypeId = containerTypeId,
            status = ContainerStatus.AVAILABLE,
        )

    private fun booking(
        customerId: UUID,
        containerTypeId: UUID,
        deliveryDate: LocalDate = LocalDate.parse("2026-03-01"),
        pickupDate: LocalDate = LocalDate.parse("2026-03-05"),
    ) = Booking(
        id = UUID.randomUUID(),
        customerId = customerId,
        containerTypeId = containerTypeId,
        quantity = 2,
        deliveryDate = deliveryDate,
        pickupDate = pickupDate,
        deliveryAddress = address(),
        status = BookingStatus.PENDING,
        dailyPriceCents = 2500,
        deliveryFeeCents = 3000,
        currencyCode = "EUR",
        createdAt = instantNow(),
        updatedAt = instantNow(),
        version = 0,
    )

    private fun instantNow() = Instant.now().truncatedTo(ChronoUnit.MICROS)

    private fun address() =
        Address(
            street = "Main Street",
            houseNumber = "1A",
            postalCode = "12345",
            city = "Example",
            countryCode = "DE",
        )

    companion object {
        @Container
        @JvmStatic
        private val postgres = PostgreSQLContainer<Nothing>("postgres:17-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun configureDatasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
