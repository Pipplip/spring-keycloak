package de.pipplip.abfallcontainerservice

import de.pipplip.abfallcontainerservice.application.port.out.AuthenticatedCustomerIdentity
import de.pipplip.abfallcontainerservice.application.port.out.AuthenticatedCustomerIdentityProvider
import de.pipplip.abfallcontainerservice.application.port.out.ContainerAssignmentRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerTypeRepository
import de.pipplip.abfallcontainerservice.application.port.out.CustomerRepository
import de.pipplip.abfallcontainerservice.application.service.AvailabilityService
import de.pipplip.abfallcontainerservice.application.service.CustomerProfileConflict
import de.pipplip.abfallcontainerservice.application.service.CustomerProfileConflictException
import de.pipplip.abfallcontainerservice.application.service.CustomerSelfServiceProfileInput
import de.pipplip.abfallcontainerservice.application.service.CustomerProfileService
import de.pipplip.abfallcontainerservice.application.service.ContainerTypeNotFoundException
import de.pipplip.abfallcontainerservice.application.service.InvalidCustomerFeatureRequestException
import de.pipplip.abfallcontainerservice.domain.model.Address
import de.pipplip.abfallcontainerservice.domain.model.ContainerType
import de.pipplip.abfallcontainerservice.domain.model.Customer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.math.BigDecimal
import java.sql.SQLException
import java.time.LocalDate
import java.time.Instant
import java.util.UUID

class CustomerFeatureUnitTest {
    private val containerTypeRepository = mockk<ContainerTypeRepository>()
    private val assignmentRepository = mockk<ContainerAssignmentRepository>()
    private val availabilityService = AvailabilityService(containerTypeRepository, assignmentRepository)

    @Test
    fun `profile creation derives identity fields and subject from verified identity`() {
        val identityProvider = mockk<AuthenticatedCustomerIdentityProvider>()
        val customerRepository = mockk<CustomerRepository>()
        val service = CustomerProfileService(identityProvider, customerRepository)
        val identity =
            AuthenticatedCustomerIdentity(
                subject = "verified-subject",
                email = "ada@example.test",
                firstName = "Ada",
                lastName = "Lovelace",
            )
        every { identityProvider.currentIdentity() } returns identity
        every { customerRepository.findByKeycloakSubject(identity.subject) } returns null
        every { customerRepository.findByEmail(identity.email!!) } returns null
        every { customerRepository.save(any()) } answers { firstArg() }

        val profile =
            service.create(
                CustomerSelfServiceProfileInput(
                    companyName = "Analytical Engines",
                    billingAddress = profileAddress(),
                ),
            )

        assertThat(profile.keycloakSubject).isEqualTo(identity.subject)
        assertThat(profile.email).isEqualTo(identity.email)
        assertThat(profile.firstName).isEqualTo(identity.firstName)
        assertThat(profile.lastName).isEqualTo(identity.lastName)
        assertThat(profile.billingAddress).isEqualTo(profileAddress())
    }

    @Test
    fun `profile creation rejects an email already associated with another identity`() {
        val identityProvider = mockk<AuthenticatedCustomerIdentityProvider>()
        val customerRepository = mockk<CustomerRepository>()
        val identity =
            AuthenticatedCustomerIdentity(
                subject = "new-subject",
                email = "existing@example.test",
                firstName = "Ada",
                lastName = "Lovelace",
            )
        every { identityProvider.currentIdentity() } returns identity
        every { customerRepository.findByKeycloakSubject(identity.subject) } returns null
        every { customerRepository.findByEmail(identity.email!!) } returns customer()

        assertThatThrownBy {
            CustomerProfileService(identityProvider, customerRepository)
                .create(CustomerSelfServiceProfileInput(null, profileAddress()))
        }.isInstanceOf(CustomerProfileConflictException::class.java)
            .hasFieldOrPropertyWithValue("conflict", CustomerProfileConflict.EMAIL_ALREADY_USED)
    }

    @Test
    fun `profile creation translates a concurrent email uniqueness violation`() {
        val identityProvider = mockk<AuthenticatedCustomerIdentityProvider>()
        val customerRepository = mockk<CustomerRepository>()
        val identity =
            AuthenticatedCustomerIdentity(
                subject = "new-subject",
                email = "ada@example.test",
                firstName = "Ada",
                lastName = "Lovelace",
            )
        val violation =
            DataIntegrityViolationException(
                "duplicate email",
                org.hibernate.exception.ConstraintViolationException(
                    "duplicate email",
                    SQLException("duplicate email", "23505"),
                    "customers_email_key",
                ),
            )
        every { identityProvider.currentIdentity() } returns identity
        every { customerRepository.findByKeycloakSubject(identity.subject) } returns null
        every { customerRepository.findByEmail(identity.email!!) } returns null
        every { customerRepository.save(any()) } throws violation

        assertThatThrownBy {
            CustomerProfileService(identityProvider, customerRepository)
                .create(CustomerSelfServiceProfileInput(null, profileAddress()))
        }.isInstanceOf(CustomerProfileConflictException::class.java)
            .hasFieldOrPropertyWithValue("conflict", CustomerProfileConflict.EMAIL_ALREADY_USED)
    }

    @Test
    fun `availability compares free inventory to requested quantity`() {
        val typeId = UUID.randomUUID()
        val deliveryDate = LocalDate.parse("2026-09-01")
        val pickupDate = LocalDate.parse("2026-09-04")
        every { containerTypeRepository.findActiveById(typeId) } returns containerType(typeId)
        every {
            assignmentRepository.countAvailableContainers(typeId, deliveryDate, pickupDate)
        } returns 2

        assertThat(availabilityService.checkAvailability(typeId, deliveryDate, pickupDate, quantity = 2)).isTrue
        assertThat(availabilityService.checkAvailability(typeId, deliveryDate, pickupDate, quantity = 3)).isFalse

        verify(exactly = 2) { containerTypeRepository.findActiveById(typeId) }
        verify(exactly = 2) {
            assignmentRepository.countAvailableContainers(typeId, deliveryDate, pickupDate)
        }
    }

    @Test
    fun `invalid period and inactive type are rejected before checking inventory`() {
        val typeId = UUID.randomUUID()
        val date = LocalDate.parse("2026-09-01")

        assertThatThrownBy {
            availabilityService.checkAvailability(typeId, date, date, quantity = 1)
        }.isInstanceOf(InvalidCustomerFeatureRequestException::class.java)

        every { containerTypeRepository.findActiveById(typeId) } returns null
        assertThatThrownBy {
            availabilityService.checkAvailability(typeId, date, date.plusDays(1), quantity = 1)
        }.isInstanceOf(ContainerTypeNotFoundException::class.java)

        verify(exactly = 0) { assignmentRepository.countAvailableContainers(any(), any(), any()) }
    }

    private fun containerType(id: UUID) =
        ContainerType(
            id = id,
            name = "Mixed waste",
            description = "General household waste",
            volumeM3 = BigDecimal("5.00"),
            dailyPriceCents = 2500,
            currencyCode = "EUR",
            active = true,
        )

    private fun profileAddress() =
        Address(
            street = "Main Street",
            houseNumber = "1",
            postalCode = "10115",
            city = "Berlin",
            countryCode = "DE",
        )

    private fun customer() =
        Customer(
            id = UUID.randomUUID(),
            keycloakSubject = "existing-subject",
            email = "existing@example.test",
            firstName = "Existing",
            lastName = "Customer",
            companyName = null,
            billingAddress = profileAddress(),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
}
