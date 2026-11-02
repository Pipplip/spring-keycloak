package de.pipplip.abfallcontainerservice

import com.fasterxml.jackson.databind.ObjectMapper
import de.pipplip.abfallcontainerservice.application.port.out.BookingRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerAssignmentRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerTypeRepository
import de.pipplip.abfallcontainerservice.application.port.out.CustomerRepository
import de.pipplip.abfallcontainerservice.application.port.out.InvoiceRepository
import de.pipplip.abfallcontainerservice.application.service.AdminContainerAssignmentService
import de.pipplip.abfallcontainerservice.application.service.AdminConflictException
import de.pipplip.abfallcontainerservice.application.service.AdminInvoiceService
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
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AdminFeaturesIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

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
    private lateinit var adminAssignmentService: AdminContainerAssignmentService

    @Autowired
    private lateinit var adminInvoiceService: AdminInvoiceService

    @BeforeEach
    fun clearDatabase() {
        jdbcTemplate.execute(
            "TRUNCATE TABLE invoice_lines, invoices, container_assignments, bookings, containers, container_types, customers CASCADE",
        )
    }

    @Test
    fun `admin manages container types and inventory while preserving existing bookings`() {
        val typeId =
            mockMvc.perform(
                post("/api/v1/admin/container-types")
                    .with(adminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(containerTypeRequest(name = "Mixed waste", active = false)),
            ).andExpect(status().isCreated)
                .andReturn()
                .let { UUID.fromString(objectMapper.readTree(it.response.contentAsString).get("id").asText()) }

        mockMvc.perform(get("/api/v1/admin/container-types").with(adminToken()).param("active", "false"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].active").value(false))

        mockMvc.perform(get("/api/v1/admin/container-types/$typeId").with(adminToken()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(typeId.toString()))

        mockMvc.perform(get("/api/v1/admin/container-types/${UUID.randomUUID()}").with(adminToken()))
            .andExpect(status().isNotFound)

        val typeRequest = objectMapper.readTree(containerTypeRequest(name = "Mixed waste updated", active = true))
        mockMvc.perform(
            put("/api/v1/admin/container-types/$typeId")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(typeRequest.toString()),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Mixed waste updated"))

        val secondType = saveType()
        mockMvc.perform(
            post("/api/v1/admin/containers")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(containerRequest("INV-MISSING-TYPE", UUID.randomUUID())),
        ).andExpect(status().isNotFound)

        val containerRequest = containerRequest("INV-1", typeId)
        val containerId =
            mockMvc.perform(
                post("/api/v1/admin/containers")
                    .with(adminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(containerRequest),
            ).andExpect(status().isCreated)
                .andReturn()
                .let { UUID.fromString(objectMapper.readTree(it.response.contentAsString).get("id").asText()) }

        mockMvc.perform(
            post("/api/v1/admin/containers")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(containerRequest),
        ).andExpect(status().isConflict)

        mockMvc.perform(
            get("/api/v1/admin/containers")
                .with(adminToken())
                .param("containerTypeId", typeId.toString())
                .param("status", "AVAILABLE"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))

        val customer = saveCustomer()
        val booking = saveBooking(customer.id, typeId, deliveryDate = todayPlus(5), pickupDate = todayPlus(8))
        assignmentRepository.save(newAssignment(booking.id, containerId))

        mockMvc.perform(
            put("/api/v1/admin/containers/$containerId")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(containerRequest("INV-1-UPDATED", typeId, ContainerStatus.MAINTENANCE)),
        ).andExpect(status().isConflict)

        mockMvc.perform(
            put("/api/v1/admin/containers/$containerId")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(containerRequest("INV-1-UPDATED", secondType.id)),
        ).andExpect(status().isConflict)

        mockMvc.perform(
            put("/api/v1/admin/container-types/$typeId")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(containerTypeRequest(name = "Changed after booking")),
        ).andExpect(status().isConflict)

        mockMvc.perform(get("/api/v1/admin/containers/$containerId").with(adminToken()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.inventoryNumber").value("INV-1"))

        mockMvc.perform(get("/api/v1/admin/containers/${UUID.randomUUID()}").with(adminToken()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `admin customer management omits identity subject and preserves it on profile update`() {
        val customer = saveCustomer(subject = "sensitive-keycloak-subject")

        val listResult =
            mockMvc.perform(get("/api/v1/admin/customers").with(adminToken()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.items.length()").value(1))
                .andReturn()
        assertThat(listResult.response.contentAsString).doesNotContain("sensitive-keycloak-subject")

        mockMvc.perform(
            put("/api/v1/admin/customers/${customer.id}")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerRequest("updated@example.test", "Updated")),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("updated@example.test"))
            .andExpect(jsonPath("$.firstName").value("Updated"))

        assertThat(customerRepository.findById(customer.id)?.keycloakSubject)
            .isEqualTo("sensitive-keycloak-subject")

        val otherCustomer = saveCustomer(email = "duplicate@example.test")
        mockMvc.perform(
            put("/api/v1/admin/customers/${customer.id}")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerRequest(otherCustomer.email, "Updated")),
        ).andExpect(status().isConflict)

        mockMvc.perform(get("/api/v1/admin/customers/${UUID.randomUUID()}").with(adminToken()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `admin booking search applies inclusive filters and stable pagination`() {
        val customer = saveCustomer()
        val otherCustomer = saveCustomer()
        val type = saveType()
        val otherType = saveType()
        val targetDate = todayPlus(10)
        val matchingBooking =
            saveBooking(
                customer.id,
                type.id,
                deliveryDate = targetDate,
                pickupDate = targetDate.plusDays(2),
                status = BookingStatus.CONFIRMED,
            )
        saveBooking(
            customer.id,
            type.id,
            deliveryDate = todayPlus(11),
            pickupDate = todayPlus(12),
            status = BookingStatus.PENDING,
        )
        saveBooking(
            otherCustomer.id,
            otherType.id,
            deliveryDate = targetDate,
            pickupDate = targetDate.plusDays(3),
            status = BookingStatus.CONFIRMED,
        )

        val result =
            mockMvc.perform(
                get("/api/v1/admin/bookings")
                    .with(adminToken())
                    .param("status", "CONFIRMED")
                    .param("customerId", customer.id.toString())
                    .param("containerTypeId", type.id.toString())
                    .param("deliveryFrom", targetDate.toString())
                    .param("deliveryTo", targetDate.toString())
                    .param("page", "0")
                    .param("size", "1"),
            ).andExpect(status().isOk)
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(matchingBooking.id.toString()))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andReturn()
        assertThat(result.response.contentAsString).doesNotContain(customer.keycloakSubject)

        mockMvc.perform(
            get("/api/v1/admin/bookings")
                .with(adminToken())
                .param("deliveryFrom", targetDate.plusDays(1).toString())
                .param("deliveryTo", targetDate.toString()),
        ).andExpect(status().isBadRequest)

        mockMvc.perform(get("/api/v1/admin/bookings").with(adminToken()).param("size", "101"))
            .andExpect(status().isBadRequest)

        mockMvc.perform(get("/api/v1/admin/bookings/${UUID.randomUUID()}").with(adminToken()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `all admin paginated routes enforce bounds and report page metadata`() {
        val customer = saveCustomer()
        val type = saveType()
        val booking = saveBooking(customer.id, type.id)
        val container = saveContainer(type.id)
        assignmentRepository.save(
            ContainerAssignment(
                id = UUID.randomUUID(),
                bookingId = booking.id,
                containerId = container.id,
                assignedAt = Instant.now(),
                releasedAt = null,
            ),
        )
        adminInvoiceService.createForBooking(booking.id)

        val routes =
            listOf(
                "/api/v1/admin/container-types",
                "/api/v1/admin/containers",
                "/api/v1/admin/customers",
                "/api/v1/admin/bookings",
                "/api/v1/admin/bookings/${booking.id}/container-assignments",
                "/api/v1/admin/invoices",
            )
        routes.forEach { route ->
            mockMvc.perform(get(route).with(adminToken()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").value(20))

            listOf("1", "100").forEach { size ->
                mockMvc.perform(get(route).with(adminToken()).param("page", "0").param("size", size))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.page.page").value(0))
                    .andExpect(jsonPath("$.page.size").value(size.toInt()))
                    .andExpect(jsonPath("$.page.totalElements").isNumber)
                    .andExpect(jsonPath("$.page.totalPages").isNumber)
            }

            listOf(
                "page" to "-1",
                "size" to "0",
                "size" to "101",
                "page" to "21474837",
            ).forEach { (name, value) ->
                val request = get(route).with(adminToken()).param(name, value)
                if (name == "page" && value == "21474837") request.param("size", "100")
                val result =
                    mockMvc.perform(request)
                        .andExpect(status().isBadRequest)
                        .andReturn()
                assertThat(result.response.contentType).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                assertThat(result.response.contentAsString).contains("\"status\":400")
            }
        }

        val invalidEnum =
            mockMvc.perform(
                get("/api/v1/admin/bookings").with(adminToken()).param("status", "UNKNOWN"),
            ).andExpect(status().isBadRequest)
                .andReturn()
        assertThat(invalidEnum.response.contentType).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        assertThat(invalidEnum.response.contentAsString).contains("\"title\":\"Bad Request\"")

        val invalidBody =
            mockMvc.perform(
                post("/api/v1/admin/container-types")
                    .with(adminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{"),
            ).andExpect(status().isBadRequest)
                .andReturn()
        assertThat(invalidBody.response.contentType).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        assertThat(invalidBody.response.contentAsString)
            .contains("\"status\":400")
            .doesNotContain("JsonParseException")
    }

    @Test
    fun `timestamp ties use stable id ordering across booking assignment and invoice pages`() {
        val customer = saveCustomer()
        val type = saveType()
        val timestamp = Instant.parse("2026-10-01T12:00:00Z")
        val firstId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val secondId = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val firstBooking =
            saveBooking(customer.id, type.id, id = firstId, createdAt = timestamp)
        val secondBooking =
            saveBooking(customer.id, type.id, id = secondId, createdAt = timestamp)

        listOf(firstBooking.id, secondBooking.id).forEach { bookingId ->
            val invoiceId = if (bookingId == firstBooking.id) firstId else secondId
            saveInvoice(bookingId, InvoiceStatus.DRAFT, id = invoiceId, createdAt = timestamp)
        }

        val firstContainer = saveContainer(type.id, inventoryNumber = "PAGE-ORDER-1")
        val secondContainer = saveContainer(type.id, inventoryNumber = "PAGE-ORDER-2")
        val assignmentTime = timestamp.plusSeconds(1)
        listOf(firstContainer.id, secondContainer.id).forEachIndexed { index, containerId ->
            assignmentRepository.save(
                ContainerAssignment(
                    id = if (index == 0) firstId else secondId,
                    bookingId = firstBooking.id,
                    containerId = containerId,
                    assignedAt = assignmentTime,
                    releasedAt = null,
                ),
            )
        }

        mockMvc.perform(
            get("/api/v1/admin/bookings").with(adminToken()).param("size", "1").param("page", "0"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(firstId.toString()))
            .andExpect(jsonPath("$.page.totalElements").value(2))
            .andExpect(jsonPath("$.page.totalPages").value(2))

        mockMvc.perform(
            get("/api/v1/admin/bookings").with(adminToken()).param("size", "1").param("page", "1"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(secondId.toString()))

        mockMvc.perform(
            get("/api/v1/admin/bookings/${firstBooking.id}/container-assignments")
                .with(adminToken())
                .param("size", "1")
                .param("page", "0"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(firstId.toString()))

        mockMvc.perform(
            get("/api/v1/admin/bookings/${firstBooking.id}/container-assignments")
                .with(adminToken())
                .param("size", "1")
                .param("page", "1"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(secondId.toString()))

        mockMvc.perform(
            get("/api/v1/admin/invoices").with(adminToken()).param("size", "1").param("page", "0"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(firstId.toString()))
            .andExpect(jsonPath("$.page.totalElements").value(2))

        mockMvc.perform(
            get("/api/v1/admin/invoices").with(adminToken()).param("size", "1").param("page", "1"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(secondId.toString()))
    }

    @Test
    fun `admin assignments enforce eligibility retain history and expose assignment counts`() {
        val customer = saveCustomer()
        val type = saveType()
        val wrongType = saveType()
        val booking = saveBooking(customer.id, type.id, quantity = 1)
        val container = saveContainer(type.id, inventoryNumber = "ASSIGN-1")
        val extraContainer = saveContainer(type.id, inventoryNumber = "ASSIGN-2")
        val wrongContainer = saveContainer(wrongType.id, inventoryNumber = "ASSIGN-WRONG")
        val maintenanceContainer =
            saveContainer(type.id, status = ContainerStatus.MAINTENANCE, inventoryNumber = "ASSIGN-MAINT")

        mockMvc.perform(
            post("/api/v1/admin/bookings/${booking.id}/container-assignments")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"containerId":"${wrongContainer.id}"}"""),
        ).andExpect(status().isConflict)

        mockMvc.perform(
            post("/api/v1/admin/bookings/${booking.id}/container-assignments")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"containerId":"${maintenanceContainer.id}"}"""),
        ).andExpect(status().isConflict)

        val firstAssignment =
            mockMvc.perform(
                post("/api/v1/admin/bookings/${booking.id}/container-assignments")
                    .with(adminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"containerId":"${container.id}"}"""),
            ).andExpect(status().isCreated)
                .andReturn()
                .let { UUID.fromString(objectMapper.readTree(it.response.contentAsString).get("id").asText()) }

        mockMvc.perform(
            post("/api/v1/admin/bookings/${booking.id}/container-assignments")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"containerId":"${extraContainer.id}"}"""),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/api/v1/bookings/${booking.id}")
                .with(userToken(customer.keycloakSubject)),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.quantity").value(1))
            .andExpect(jsonPath("$.assignedContainerCount").value(2))

        mockMvc.perform(get("/api/v1/admin/bookings/${booking.id}").with(adminToken()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.assignedContainerCount").value(2))

        val overlappingBooking =
            saveBooking(
                customer.id,
                type.id,
                deliveryDate = booking.deliveryDate.plusDays(1),
                pickupDate = booking.pickupDate.plusDays(1),
            )
        mockMvc.perform(
            post("/api/v1/admin/bookings/${overlappingBooking.id}/container-assignments")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"containerId":"${container.id}"}"""),
        ).andExpect(status().isConflict)

        val touchingBooking =
            saveBooking(
                customer.id,
                type.id,
                deliveryDate = booking.pickupDate,
                pickupDate = booking.pickupDate.plusDays(2),
            )
        mockMvc.perform(
            post("/api/v1/admin/bookings/${touchingBooking.id}/container-assignments")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"containerId":"${container.id}"}"""),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/api/v1/admin/bookings/${booking.id}/container-assignments")
                .with(adminToken()),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))

        mockMvc.perform(
            post("/api/v1/admin/bookings/${touchingBooking.id}/container-assignments/$firstAssignment/release")
                .with(adminToken()),
        ).andExpect(status().isNotFound)

        val released =
            mockMvc.perform(
                post("/api/v1/admin/bookings/${booking.id}/container-assignments/$firstAssignment/release")
                    .with(adminToken()),
            ).andExpect(status().isOk)
                .andReturn()
        assertThat(objectMapper.readTree(released.response.contentAsString).get("releasedAt").isNull).isFalse

        mockMvc.perform(
            post("/api/v1/admin/bookings/${booking.id}/container-assignments/$firstAssignment/release")
                .with(adminToken()),
        ).andExpect(status().isConflict)

        mockMvc.perform(get("/api/v1/bookings/${booking.id}").with(userToken(customer.keycloakSubject)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.assignedContainerCount").value(1))
    }

    @Test
    fun `admin booking lifecycle requires paid invoice and cancellation releases assignments`() {
        val customer = saveCustomer()
        val type = saveType()
        val booking = saveBooking(customer.id, type.id)
        val invoice = saveInvoice(booking.id, InvoiceStatus.DRAFT)

        mockMvc.perform(
            patch("/api/v1/admin/bookings/${booking.id}/status")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CONFIRMED"}"""),
        ).andExpect(status().isConflict)

        invoiceRepository.save(invoice.copy(status = InvoiceStatus.PAID))
        mockMvc.perform(
            patch("/api/v1/admin/bookings/${booking.id}/status")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CONFIRMED"}"""),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CONFIRMED"))

        mockMvc.perform(
            patch("/api/v1/admin/bookings/${booking.id}/status")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"DELIVERED"}"""),
        ).andExpect(status().isOk)

        mockMvc.perform(
            patch("/api/v1/admin/bookings/${booking.id}/status")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"COMPLETED"}"""),
        ).andExpect(status().isOk)

        mockMvc.perform(
            patch("/api/v1/admin/bookings/${booking.id}/status")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CANCELLED"}"""),
        ).andExpect(status().isConflict)

        val cancellable = saveBooking(customer.id, type.id)
        val container = saveContainer(type.id)
        val assignment = assignmentRepository.save(newAssignment(cancellable.id, container.id))
        mockMvc.perform(
            patch("/api/v1/admin/bookings/${cancellable.id}/status")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CANCELLED"}"""),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.assignedContainerCount").value(0))

        assertThat(assignmentRepository.findById(assignment.id)?.releasedAt).isNotNull

        val confirmedBooking = saveBooking(customer.id, type.id)
        val confirmedInvoice = saveInvoice(confirmedBooking.id, InvoiceStatus.PAID)
        invoiceRepository.save(confirmedInvoice)
        val confirmedAssignment =
            assignmentRepository.save(
                newAssignment(confirmedBooking.id, saveContainer(type.id).id),
            )
        mockMvc.perform(
            patch("/api/v1/admin/bookings/${confirmedBooking.id}/status")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CONFIRMED"}"""),
        ).andExpect(status().isOk)
        mockMvc.perform(
            patch("/api/v1/admin/bookings/${confirmedBooking.id}/status")
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CANCELLED"}"""),
        ).andExpect(status().isOk)
        assertThat(assignmentRepository.findById(confirmedAssignment.id)?.releasedAt).isNotNull
    }

    @Test
    fun `admin creates retrieves filters and transitions invoices`() {
        val customer = saveCustomer()
        val type = saveType()
        val booking =
            saveBooking(
                customer.id,
                type.id,
                deliveryDate = todayPlus(5),
                pickupDate = todayPlus(8),
                quantity = 2,
            )

        val createResponse =
            mockMvc.perform(
                post("/api/v1/admin/bookings/${booking.id}/invoice").with(adminToken()),
            ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.bookingId").value(booking.id.toString()))
                .andExpect(jsonPath("$.netAmountCents").value(15000))
                .andExpect(jsonPath("$.taxAmountCents").value(2850))
                .andExpect(jsonPath("$.grossAmountCents").value(17850))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].quantity").value(6.0))
                .andExpect(jsonPath("$.lines[0].unitPriceCents").value(2500))
                .andExpect(jsonPath("$.lines[0].taxRate").value(0.19))
                .andExpect(jsonPath("$.lines[0].totalCents").value(15000))
                .andReturn()
        val invoiceJson = objectMapper.readTree(createResponse.response.contentAsString)
        val invoiceId = UUID.fromString(invoiceJson.get("id").asText())
        assertThat(invoiceJson.get("invoiceNumber").asText()).matches("\\d{4}-\\d{6,}")

        mockMvc.perform(
            post("/api/v1/admin/bookings/${booking.id}/invoice").with(adminToken()),
        ).andExpect(status().isConflict)

        mockMvc.perform(
            get("/api/v1/admin/invoices")
                .with(adminToken())
                .param("status", "DRAFT"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(invoiceId.toString()))
            .andExpect(jsonPath("$.page.totalElements").value(1))

        mockMvc.perform(get("/api/v1/admin/invoices/$invoiceId").with(adminToken()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.lines.length()").value(1))

        mockMvc.perform(
            get("/api/v1/admin/invoices").with(userToken(customer.keycloakSubject)),
        ).andExpect(status().isForbidden)

        mockMvc.perform(get("/api/v1/admin/invoices"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            post("/api/v1/admin/invoices/$invoiceId/mark-paid").with(adminToken()),
        ).andExpect(status().isConflict)

        mockMvc.perform(
            post("/api/v1/admin/invoices/$invoiceId/send").with(adminToken()),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SENT"))

        mockMvc.perform(
            post("/api/v1/admin/invoices/$invoiceId/mark-paid").with(adminToken()),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PAID"))

        mockMvc.perform(
            post("/api/v1/admin/invoices/$invoiceId/mark-paid").with(adminToken()),
        ).andExpect(status().isConflict)

        mockMvc.perform(
            post("/api/v1/admin/invoices/$invoiceId/void").with(adminToken()),
        ).andExpect(status().isConflict)

        val voidableBooking = saveBooking(customer.id, type.id)
        val voidableInvoice =
            adminInvoiceService.createForBooking(voidableBooking.id)
        mockMvc.perform(
            post("/api/v1/admin/invoices/${voidableInvoice.id}/void").with(adminToken()),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("VOID"))

        mockMvc.perform(
            post("/api/v1/admin/invoices/${voidableInvoice.id}/send").with(adminToken()),
        ).andExpect(status().isConflict)

        val cancelled = saveBooking(customer.id, type.id, status = BookingStatus.CANCELLED)
        mockMvc.perform(
            post("/api/v1/admin/bookings/${cancelled.id}/invoice").with(adminToken()),
        ).andExpect(status().isConflict)

        mockMvc.perform(
            post("/api/v1/admin/bookings/${UUID.randomUUID()}/invoice").with(adminToken()),
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `concurrent invoice creation preserves one per booking and unique invoice numbers`() {
        val customer = saveCustomer()
        val type = saveType()
        val duplicateBooking = saveBooking(customer.id, type.id)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(4)
        try {
            val duplicateAttempts =
                (1..4).map {
                    executor.submit<Boolean> {
                        start.await()
                        try {
                            adminInvoiceService.createForBooking(duplicateBooking.id)
                            true
                        } catch (_: AdminConflictException) {
                            false
                        }
                    }
                }
            start.countDown()

            assertThat(duplicateAttempts.map { it.get() }.count { it }).isEqualTo(1)
            assertThat(invoiceRepository.findByBookingId(duplicateBooking.id)).isNotNull

            val independentBookings = (1..3).map { saveBooking(customer.id, type.id) }
            val numberStart = CountDownLatch(1)
            val numberAttempts =
                independentBookings.map { booking ->
                    executor.submit<Invoice> {
                        numberStart.await()
                        adminInvoiceService.createForBooking(booking.id)
                    }
                }
            numberStart.countDown()
            val invoices = numberAttempts.map { it.get() }

            assertThat(invoices.map { it.invoiceNumber }.toSet()).hasSize(3)
            assertThat(invoices.map { it.invoiceNumber }.all { it.matches("\\d{4}-\\d{6,}".toRegex()) })
                .isTrue()
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `concurrent admin assignments cannot double allocate one container`() {
        val customer = saveCustomer()
        val type = saveType()
        val firstBooking = saveBooking(customer.id, type.id)
        val secondBooking = saveBooking(customer.id, type.id)
        val container = saveContainer(type.id)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val attempts =
                listOf(firstBooking.id, secondBooking.id).map { bookingId ->
                    executor.submit<Boolean> {
                        start.await()
                        try {
                            adminAssignmentService.assign(bookingId, container.id)
                            true
                        } catch (_: de.pipplip.abfallcontainerservice.application.service.AdminConflictException) {
                            false
                        }
                    }
                }
            start.countDown()

            assertThat(attempts.map { it.get() }.count { it }).isEqualTo(1)
            assertThat(
                assignmentRepository.countActiveAssignments(listOf(firstBooking.id, secondBooking.id)).values.sum(),
            ).isEqualTo(1)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun saveCustomer(
        subject: String = "subject-${UUID.randomUUID()}",
        email: String = "customer-${UUID.randomUUID()}@example.test",
    ) = customerRepository.save(
        Customer(
            id = UUID.randomUUID(),
            keycloakSubject = subject,
            email = email,
            firstName = "Test",
            lastName = "Customer",
            companyName = null,
            billingAddress = address(),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        ),
    )

    private fun saveType(active: Boolean = true) =
        containerTypeRepository.save(
            ContainerType(
                id = UUID.randomUUID(),
                name = "Waste ${UUID.randomUUID()}",
                description = "General waste",
                volumeM3 = BigDecimal("5.00"),
                dailyPriceCents = 2500,
                currencyCode = "EUR",
                active = active,
            ),
        )

    private fun saveContainer(
        typeId: UUID,
        status: ContainerStatus = ContainerStatus.AVAILABLE,
        inventoryNumber: String = "INV-${UUID.randomUUID()}",
    ) =
        containerRepository.save(
            PhysicalContainer(
                id = UUID.randomUUID(),
                inventoryNumber = inventoryNumber,
                containerTypeId = typeId,
                status = status,
            ),
        )

    private fun saveBooking(
        customerId: UUID,
        typeId: UUID,
        deliveryDate: LocalDate = todayPlus(10),
        pickupDate: LocalDate = todayPlus(13),
        quantity: Int = 1,
        status: BookingStatus = BookingStatus.PENDING,
        id: UUID = UUID.randomUUID(),
        createdAt: Instant = Instant.now(),
    ) =
        bookingRepository.save(
            Booking(
                id = id,
                customerId = customerId,
                containerTypeId = typeId,
                quantity = quantity,
                deliveryDate = deliveryDate,
                pickupDate = pickupDate,
                deliveryAddress = address(),
                status = status,
                dailyPriceCents = 2500,
                currencyCode = "EUR",
                createdAt = createdAt,
                updatedAt = Instant.now(),
                version = 0,
            ),
        )

    private fun saveInvoice(
        bookingId: UUID,
        status: InvoiceStatus,
        id: UUID = UUID.randomUUID(),
        createdAt: Instant = Instant.now(),
    ) =
        invoiceRepository.save(
            Invoice(
                id = id,
                bookingId = bookingId,
                invoiceNumber = "INV-${UUID.randomUUID()}",
                netAmountCents = 10000,
                taxAmountCents = 0,
                grossAmountCents = 10000,
                currencyCode = "EUR",
                status = status,
                createdAt = createdAt,
                lines = emptyList(),
            ),
        )

    private fun newAssignment(bookingId: UUID, containerId: UUID) =
        ContainerAssignment(
            id = UUID.randomUUID(),
            bookingId = bookingId,
            containerId = containerId,
            assignedAt = Instant.now(),
            releasedAt = null,
        )

    private fun address() =
        Address(
            street = "Main Street",
            houseNumber = "1",
            postalCode = "12345",
            city = "Test City",
            countryCode = "DE",
        )

    private fun containerTypeRequest(
        name: String,
        active: Boolean = true,
    ) = """{"name":"$name","description":"General waste","volumeM3":5.00,"dailyPriceCents":2500,"currencyCode":"EUR","active":$active}"""

    private fun containerRequest(
        inventoryNumber: String,
        typeId: UUID,
        status: ContainerStatus = ContainerStatus.AVAILABLE,
    ) = """{"inventoryNumber":"$inventoryNumber","containerTypeId":"$typeId","status":"$status"}"""

    private fun customerRequest(email: String, firstName: String) =
        """{"email":"$email","firstName":"$firstName","lastName":"Customer","billingAddress":{"street":"New Street","houseNumber":"2","postalCode":"54321","city":"New City","countryCode":"DE"}}"""

    private fun todayPlus(days: Long): LocalDate = LocalDate.now().plusDays(days)

    private fun adminToken() =
        jwt().jwt { it.subject("admin") }.authorities(SimpleGrantedAuthority("ROLE_ADMIN"))

    private fun userToken(subject: String) =
        jwt().jwt { it.subject(subject) }.authorities(SimpleGrantedAuthority("ROLE_USER"))

    companion object {
        @Container
        @JvmStatic
        private val postgres = PostgreSQLContainer<Nothing>("postgres:17-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") {
                "https://keycloak.test/realms/abfallcontainer-realm"
            }
            registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri") { "http://localhost/jwks" }
        }
    }
}
