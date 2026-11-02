package de.pipplip.abfallcontainerservice

import de.pipplip.abfallcontainerservice.application.port.out.ContainerAssignmentRepository
import de.pipplip.abfallcontainerservice.application.port.out.ContainerTypeRepository
import de.pipplip.abfallcontainerservice.application.service.AvailabilityService
import de.pipplip.abfallcontainerservice.application.service.ContainerTypeNotFoundException
import de.pipplip.abfallcontainerservice.application.service.InvalidCustomerFeatureRequestException
import de.pipplip.abfallcontainerservice.domain.model.ContainerType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class CustomerFeatureUnitTest {
    private val containerTypeRepository = mockk<ContainerTypeRepository>()
    private val assignmentRepository = mockk<ContainerAssignmentRepository>()
    private val availabilityService = AvailabilityService(containerTypeRepository, assignmentRepository)

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
}
