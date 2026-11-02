package de.pipplip.abfallcontainerservice

import com.fasterxml.jackson.databind.ObjectMapper
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
import de.pipplip.abfallcontainerservice.domain.model.InvoiceStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class CustomerFeaturesIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

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

    @Test
    fun `catalogue and availability expose active inventory only`() {
        val type = containerTypeRepository.save(containerType())
        val inactiveType = containerTypeRepository.save(containerType(active = false))
        val available = containerRepository.save(container(type.id))
        containerRepository.save(container(type.id, status = ContainerStatus.MAINTENANCE))

        val catalogue =
            objectMapper.readTree(
                mockMvc.perform(get("/api/v1/container-types").param("size", "100"))
                    .andExpect(status().isOk)
                    .andReturn()
                    .response.contentAsString,
            )
        val catalogItems = catalogue.get("items")
        assertThat(catalogItems.any { it.get("id").asText() == type.id.toString() }).isTrue
        assertThat(catalogItems.all { it.get("active").asBoolean() }).isTrue

        val notFound =
            mockMvc.perform(get("/api/v1/container-types/${inactiveType.id}"))
            .andExpect(status().isNotFound)
                .andReturn()
        assertThat(notFound.response.contentType).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        assertThat(notFound.response.contentAsString)
            .contains("\"title\":\"Not Found\"")
            .contains("\"status\":404")

        mockMvc.perform(
            get("/api/v1/availability")
                .param("containerTypeId", type.id.toString())
                .param("deliveryDate", "2026-04-01")
                .param("pickupDate", "2026-04-05")
                .param("quantity", "1"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.available").value(true))

        val customer = customerRepository.save(customer())
        val booking = booking(customer.id, type.id, LocalDate.parse("2026-04-05"), LocalDate.parse("2026-04-08"))
        bookingRepository.save(booking)
        assignmentRepository.save(assignment(booking.id, available.id))

        mockMvc.perform(
            get("/api/v1/availability")
                .param("containerTypeId", type.id.toString())
                .param("deliveryDate", "2026-04-01")
                .param("pickupDate", "2026-04-05")
                .param("quantity", "1"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.available").value(true))

        mockMvc.perform(
            get("/api/v1/availability")
                .param("containerTypeId", type.id.toString())
                .param("deliveryDate", "2026-04-04")
                .param("pickupDate", "2026-04-06")
                .param("quantity", "1"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.available").value(false))
    }

    @Test
    fun `booking creation reserves requested inventory and snapshots the daily price`() {
        val customer = customerRepository.save(customer())
        val suppliedCustomer = customerRepository.save(customer())
        val type = containerTypeRepository.save(containerType())
        containerRepository.save(container(type.id))
        containerRepository.save(container(type.id))

        val result =
            mockMvc.perform(
            post("/api/v1/bookings")
                .with(userToken(customer.keycloakSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "containerTypeId" to type.id,
                            "customerId" to suppliedCustomer.id,
                            "quantity" to 2,
                            "deliveryDate" to "2026-05-01",
                            "pickupDate" to "2026-05-04",
                            "deliveryAddress" to address(),
                        ),
                    ),
                ),
            ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.customerId").value(customer.id.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.dailyPriceCents").value(2500))
                .andExpect(jsonPath("$.deliveryFeeCents").doesNotExist())
                .andReturn()
        val bookingId = UUID.fromString(objectMapper.readTree(result.response.contentAsString).get("id").asText())

        assertThat(
            requireNotNull(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM container_assignments WHERE booking_id = ? AND released_at IS NULL",
                Int::class.javaObjectType,
                bookingId,
            )),
        ).isEqualTo(2)
    }

    @Test
    fun `customer booking reads and invoice retrieval enforce ownership`() {
        val owner = customerRepository.save(customer())
        val otherCustomer = customerRepository.save(customer())
        val type = containerTypeRepository.save(containerType())
        val ownedBooking = bookingRepository.save(booking(owner.id, type.id))
        val otherBooking = bookingRepository.save(booking(otherCustomer.id, type.id))
        val invoice =
            invoiceRepository.save(
                Invoice(
                    id = UUID.randomUUID(),
                    bookingId = ownedBooking.id,
                    invoiceNumber = "INV-${UUID.randomUUID()}",
                    netAmountCents = 10000,
                    taxAmountCents = 0,
                    grossAmountCents = 10000,
                    currencyCode = "EUR",
                    status = InvoiceStatus.DRAFT,
                    createdAt = instantNow(),
                    lines = emptyList(),
                ),
            )

        mockMvc.perform(get("/api/v1/bookings").with(userToken(owner.keycloakSubject)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(ownedBooking.id.toString()))

        mockMvc.perform(get("/api/v1/bookings/${ownedBooking.id}").with(userToken(owner.keycloakSubject)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.customerId").value(owner.id.toString()))

        mockMvc.perform(get("/api/v1/bookings/${otherBooking.id}").with(userToken(owner.keycloakSubject)))
            .andExpect(status().isForbidden)

        mockMvc.perform(get("/api/v1/bookings/${ownedBooking.id}/invoice").with(userToken(owner.keycloakSubject)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(invoice.id.toString()))

        mockMvc.perform(get("/api/v1/bookings/${otherBooking.id}/invoice").with(userToken(owner.keycloakSubject)))
            .andExpect(status().isForbidden)

        val noInvoiceBooking = bookingRepository.save(booking(owner.id, type.id))
        mockMvc.perform(get("/api/v1/bookings/${noInvoiceBooking.id}/invoice").with(userToken(owner.keycloakSubject)))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `customers can cancel pending and confirmed bookings and release assignments`() {
        val customer = customerRepository.save(customer())
        val type = containerTypeRepository.save(containerType())
        val physicalContainer = containerRepository.save(container(type.id))
        val pending = bookingRepository.save(booking(customer.id, type.id, status = BookingStatus.PENDING))
        val confirmed = bookingRepository.save(booking(customer.id, type.id, status = BookingStatus.CONFIRMED))
        val delivered = bookingRepository.save(booking(customer.id, type.id, status = BookingStatus.DELIVERED))
        assignmentRepository.save(assignment(pending.id, physicalContainer.id))

        mockMvc.perform(
            post("/api/v1/bookings/${pending.id}/cancellation")
                .with(userToken(customer.keycloakSubject)),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))

        mockMvc.perform(
            post("/api/v1/bookings/${confirmed.id}/cancellation")
                .with(userToken(customer.keycloakSubject)),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))

        mockMvc.perform(
            post("/api/v1/bookings/${delivered.id}/cancellation")
                .with(userToken(customer.keycloakSubject)),
        ).andExpect(status().isConflict)

        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT released_at FROM container_assignments WHERE booking_id = ?",
                java.sql.Timestamp::class.java,
                pending.id,
            ),
        ).isNotNull
        mockMvc.perform(
            get("/api/v1/availability")
                .param("containerTypeId", type.id.toString())
                .param("deliveryDate", "2026-07-01")
                .param("pickupDate", "2026-07-04")
                .param("quantity", "1"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.available").value(true))
    }

    @Test
    fun `concurrent bookings cannot reserve the same physical container`() {
        val customer = customerRepository.save(customer())
        val type = containerTypeRepository.save(containerType())
        containerRepository.save(container(type.id))
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        val requestBody =
            objectMapper.writeValueAsString(
                mapOf(
                    "containerTypeId" to type.id,
                    "quantity" to 1,
                    "deliveryDate" to "2026-08-01",
                    "pickupDate" to "2026-08-04",
                    "deliveryAddress" to address(),
                ),
            )
        val requests =
            (1..2).map {
                executor.submit<Int> {
                    ready.countDown()
                    start.await()
                    mockMvc.perform(
                        post("/api/v1/bookings")
                            .with(userToken(customer.keycloakSubject))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody),
                    ).andReturn().response.status
                }
            }

        ready.await()
        start.countDown()
        val statuses = requests.map { it.get() }
        executor.shutdownNow()

        assertThat(statuses).containsExactlyInAnyOrder(201, 409)
        assertThat(
            requireNotNull(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM bookings WHERE container_type_id = ? AND delivery_date = ?",
                Int::class.javaObjectType,
                type.id,
                LocalDate.parse("2026-08-01"),
            )),
        ).isEqualTo(1)
        assertThat(
            requireNotNull(jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM container_assignments ca
                JOIN bookings b ON b.id = ca.booking_id
                WHERE b.container_type_id = ? AND b.delivery_date = ?
                """,
                Int::class.javaObjectType,
                type.id,
                LocalDate.parse("2026-08-01"),
            )),
        ).isEqualTo(1)
    }

    @Test
    fun `booking creation reports unavailable inventory and rejects invalid periods`() {
        val customer = customerRepository.save(customer())
        val type = containerTypeRepository.save(containerType())
        containerRepository.save(container(type.id))
        val request =
            mapOf(
                "containerTypeId" to type.id,
                "quantity" to 2,
                "deliveryDate" to "2026-06-05",
                "pickupDate" to "2026-06-04",
                "deliveryAddress" to address(),
            )

        mockMvc.perform(
            post("/api/v1/bookings")
                .with(userToken(customer.keycloakSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isBadRequest)

        val validRequest = request + ("pickupDate" to "2026-06-08")
        val conflict =
            mockMvc.perform(
            post("/api/v1/bookings")
                .with(userToken(customer.keycloakSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)),
        ).andExpect(status().isConflict)
                .andReturn()
        assertThat(conflict.response.contentType).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        assertThat(conflict.response.contentAsString)
            .contains("\"title\":\"Conflict\"")
            .contains("\"status\":409")
    }

    @Test
    fun `customer pagination and malformed requests return bounded RFC 7807 errors`() {
        val customer = customerRepository.save(customer())
        val type = containerTypeRepository.save(containerType())
        val bookingTime = Instant.parse("2026-10-01T12:00:00Z")
        val firstBookingId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val secondBookingId = UUID.fromString("00000000-0000-0000-0000-000000000002")
        bookingRepository.save(booking(customer.id, type.id, id = firstBookingId, createdAt = bookingTime))
        bookingRepository.save(booking(customer.id, type.id, id = secondBookingId, createdAt = bookingTime))

        mockMvc.perform(get("/api/v1/container-types").param("page", "0").param("size", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page.page").value(0))
            .andExpect(jsonPath("$.page.size").value(1))

        mockMvc.perform(get("/api/v1/container-types").param("size", "100"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page.size").value(100))

        val outOfRangePage =
            objectMapper.readTree(
                mockMvc.perform(
                    get("/api/v1/container-types").param("page", "100").param("size", "1"),
                ).andExpect(status().isOk)
                    .andReturn()
                    .response.contentAsString,
            )
        assertThat(outOfRangePage.get("items").size()).isZero()
        assertThat(outOfRangePage.get("page").get("page").asInt()).isEqualTo(100)
        assertThat(outOfRangePage.get("page").get("totalElements").asLong()).isGreaterThanOrEqualTo(1)
        assertThat(outOfRangePage.get("page").get("totalPages").asInt())
            .isEqualTo(outOfRangePage.get("page").get("totalElements").asInt())

        val invalidPages =
            listOf(
                mapOf("page" to "-1"),
                mapOf("size" to "0"),
                mapOf("size" to "101"),
                mapOf("page" to "21474837", "size" to "100"),
            )
        invalidPages.forEach { params ->
            val request = get("/api/v1/container-types")
            params.forEach { (name, value) -> request.param(name, value) }
            val result =
                mockMvc.perform(request)
                    .andExpect(status().isBadRequest)
                    .andReturn()
            assertThat(result.response.contentType).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            assertThat(result.response.contentAsString)
                .contains("\"status\":400")
                .doesNotContain("sensitive")
        }

        mockMvc.perform(get("/api/v1/bookings").with(userToken(customer.keycloakSubject)).param("size", "100"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page.size").value(100))
        mockMvc.perform(
            get("/api/v1/bookings").with(userToken(customer.keycloakSubject)).param("page", "0").param("size", "1"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(firstBookingId.toString()))
            .andExpect(jsonPath("$.page.totalElements").value(2))
            .andExpect(jsonPath("$.page.totalPages").value(2))
        mockMvc.perform(
            get("/api/v1/bookings").with(userToken(customer.keycloakSubject)).param("page", "1").param("size", "1"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(secondBookingId.toString()))

        invalidPages.forEach { params ->
            val request = get("/api/v1/bookings").with(userToken(customer.keycloakSubject))
            params.forEach { (name, value) -> request.param(name, value) }
            val result =
                mockMvc.perform(request)
                    .andExpect(status().isBadRequest)
                    .andReturn()
            assertThat(result.response.contentType).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            assertThat(result.response.contentAsString).contains("\"status\":400")
        }

        val invalidQuantity =
            mockMvc.perform(
                get("/api/v1/availability")
                    .param("containerTypeId", type.id.toString())
                    .param("deliveryDate", "2026-09-01")
                    .param("pickupDate", "2026-09-02")
                    .param("quantity", "0"),
            ).andExpect(status().isBadRequest)
                .andReturn()
        assertThat(invalidQuantity.response.contentType).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        assertThat(invalidQuantity.response.contentAsString).contains("\"status\":400")

        val missingQueryParameter =
            mockMvc.perform(
                get("/api/v1/availability")
                    .param("containerTypeId", type.id.toString())
                    .param("deliveryDate", "2026-09-01"),
            ).andExpect(status().isBadRequest)
                .andReturn()
        assertThat(missingQueryParameter.response.contentType).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        assertThat(missingQueryParameter.response.contentAsString).contains("\"status\":400")

        val malformedQuery =
            mockMvc.perform(
                get("/api/v1/availability")
                    .param("containerTypeId", "not-a-uuid")
                    .param("deliveryDate", "not-a-date")
                    .param("pickupDate", "2026-09-02")
                    .param("quantity", "1"),
            ).andExpect(status().isBadRequest)
                .andReturn()
        assertThat(malformedQuery.response.contentType).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        assertThat(malformedQuery.response.contentAsString).contains("\"status\":400")

        val malformed =
            mockMvc.perform(
                post("/api/v1/bookings")
                    .with(userToken(customer.keycloakSubject))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{"),
            ).andExpect(status().isBadRequest)
                .andReturn()
        assertThat(malformed.response.contentType).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        assertThat(malformed.response.contentAsString)
            .contains("\"title\":\"Bad Request\"")
            .contains("\"status\":400")
            .doesNotContain("JsonParseException")

        val missingFields =
            mockMvc.perform(
                post("/api/v1/bookings")
                    .with(userToken(customer.keycloakSubject))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isBadRequest)
                .andReturn()
        assertThat(missingFields.response.contentType).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        assertThat(missingFields.response.contentAsString).contains("\"status\":400")
    }

    private fun userToken(subject: String) =
        jwt().jwt { it.subject(subject) }.authorities(SimpleGrantedAuthority("ROLE_USER"))

    private fun customer() =
        Customer(
            id = UUID.randomUUID(),
            keycloakSubject = "subject-${UUID.randomUUID()}",
            email = "customer-${UUID.randomUUID()}@example.test",
            firstName = "Ada",
            lastName = "Lovelace",
            companyName = null,
            billingAddress = address(),
            createdAt = instantNow(),
            updatedAt = instantNow(),
        )

    private fun containerType(active: Boolean = true) =
        ContainerType(
            id = UUID.randomUUID(),
            name = "Mixed waste",
            description = "General household waste",
            volumeM3 = BigDecimal("5.00"),
            dailyPriceCents = 2500,
            currencyCode = "EUR",
            active = active,
        )

    private fun container(
        containerTypeId: UUID,
        status: ContainerStatus = ContainerStatus.AVAILABLE,
    ) = PhysicalContainer(
        id = UUID.randomUUID(),
        inventoryNumber = "CONT-${UUID.randomUUID()}",
        containerTypeId = containerTypeId,
        status = status,
    )

    private fun booking(
        customerId: UUID,
        containerTypeId: UUID,
        deliveryDate: LocalDate = LocalDate.parse("2026-07-01"),
        pickupDate: LocalDate = LocalDate.parse("2026-07-04"),
        status: BookingStatus = BookingStatus.PENDING,
        id: UUID = UUID.randomUUID(),
        createdAt: Instant = instantNow(),
    ) = Booking(
        id = id,
        customerId = customerId,
        containerTypeId = containerTypeId,
        quantity = 1,
        deliveryDate = deliveryDate,
        pickupDate = pickupDate,
        deliveryAddress = address(),
        status = status,
        dailyPriceCents = 2500,
        currencyCode = "EUR",
        createdAt = createdAt,
        updatedAt = instantNow(),
        version = 0,
    )

    private fun assignment(
        bookingId: UUID,
        containerId: UUID,
    ) = ContainerAssignment(
        id = UUID.randomUUID(),
        bookingId = bookingId,
        containerId = containerId,
        assignedAt = instantNow(),
        releasedAt = null,
    )

    private fun address() =
        Address(
            street = "Main Street",
            houseNumber = "1A",
            postalCode = "12345",
            city = "Example",
            countryCode = "DE",
        )

    private fun instantNow() = Instant.now().truncatedTo(ChronoUnit.MICROS)

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
