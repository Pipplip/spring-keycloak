package de.pipplip.abfallcontainerservice.domain.model

import java.math.BigDecimal
import java.util.UUID

data class ContainerType(
    val id: UUID,
    val name: String,
    val description: String,
    val volumeM3: BigDecimal,
    val dailyPriceCents: Long,
    val currencyCode: String,
    val active: Boolean,
)

enum class ContainerStatus {
    AVAILABLE,
    MAINTENANCE,
    RETIRED,
}

data class Container(
    val id: UUID,
    val inventoryNumber: String,
    val containerTypeId: UUID,
    val status: ContainerStatus,
)
