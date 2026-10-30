package de.pipplip.abfallcontainerservice.infrastructure.persistence

import de.pipplip.abfallcontainerservice.domain.model.BookingStatus
import de.pipplip.abfallcontainerservice.domain.model.ContainerStatus
import de.pipplip.abfallcontainerservice.domain.model.InvoiceStatus
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Embeddable
class AddressJpaEmbeddable(
    @Column(name = "street", nullable = false, length = 255)
    var street: String,
    @Column(name = "house_number", nullable = false, length = 50)
    var houseNumber: String,
    @Column(name = "postal_code", nullable = false, length = 32)
    var postalCode: String,
    @Column(name = "city", nullable = false, length = 255)
    var city: String,
    @Column(name = "country_code", nullable = false, length = 2)
    var countryCode: String,
)

@Entity
@Table(name = "customers")
class CustomerJpaEntity(
    @Id
    var id: UUID,
    @Column(name = "keycloak_subject", nullable = false, unique = true, length = 255)
    var keycloakSubject: String,
    @Column(nullable = false, unique = true, length = 320)
    var email: String,
    @Column(name = "first_name", nullable = false, length = 255)
    var firstName: String,
    @Column(name = "last_name", nullable = false, length = 255)
    var lastName: String,
    @Column(name = "company_name", length = 255)
    var companyName: String?,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "street", column = Column(name = "street", nullable = false, length = 255)),
        AttributeOverride(name = "houseNumber", column = Column(name = "house_number", nullable = false, length = 50)),
        AttributeOverride(name = "postalCode", column = Column(name = "postal_code", nullable = false, length = 32)),
        AttributeOverride(name = "city", column = Column(name = "city", nullable = false, length = 255)),
        AttributeOverride(name = "countryCode", column = Column(name = "country_code", nullable = false, length = 2)),
    )
    var billingAddress: AddressJpaEmbeddable,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)

@Entity
@Table(name = "container_types")
class ContainerTypeJpaEntity(
    @Id
    var id: UUID,
    @Column(nullable = false, length = 255)
    var name: String,
    @Column(nullable = false, columnDefinition = "text")
    var description: String,
    @Column(name = "volume_m3", nullable = false, precision = 8, scale = 2)
    var volumeM3: BigDecimal,
    @Column(name = "daily_price_cents", nullable = false)
    var dailyPriceCents: Long,
    @Column(name = "currency_code", nullable = false, length = 3)
    var currencyCode: String,
    @Column(nullable = false)
    var active: Boolean,
)

@Entity
@Table(name = "containers")
class ContainerJpaEntity(
    @Id
    var id: UUID,
    @Column(name = "inventory_number", nullable = false, unique = true, length = 255)
    var inventoryNumber: String,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "container_type_id", nullable = false)
    var containerType: ContainerTypeJpaEntity,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: ContainerStatus,
)

@Entity
@Table(name = "bookings")
class BookingJpaEntity(
    @Id
    var id: UUID,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    var customer: CustomerJpaEntity,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "container_type_id", nullable = false)
    var containerType: ContainerTypeJpaEntity,
    @Column(nullable = false)
    var quantity: Int,
    @Column(name = "delivery_date", nullable = false)
    var deliveryDate: LocalDate,
    @Column(name = "pickup_date", nullable = false)
    var pickupDate: LocalDate,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "street", column = Column(name = "street", nullable = false, length = 255)),
        AttributeOverride(name = "houseNumber", column = Column(name = "house_number", nullable = false, length = 50)),
        AttributeOverride(name = "postalCode", column = Column(name = "postal_code", nullable = false, length = 32)),
        AttributeOverride(name = "city", column = Column(name = "city", nullable = false, length = 255)),
        AttributeOverride(name = "countryCode", column = Column(name = "country_code", nullable = false, length = 2)),
    )
    var deliveryAddress: AddressJpaEmbeddable,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: BookingStatus,
    @Column(name = "daily_price_cents", nullable = false)
    var dailyPriceCents: Long,
    @Column(name = "delivery_fee_cents", nullable = false)
    var deliveryFeeCents: Long,
    @Column(name = "currency_code", nullable = false, length = 3)
    var currencyCode: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
    @Version
    @Column(nullable = false)
    var version: Long,
)

@Entity
@Table(name = "container_assignments")
class ContainerAssignmentJpaEntity(
    @Id
    var id: UUID,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    var booking: BookingJpaEntity,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "container_id", nullable = false)
    var container: ContainerJpaEntity,
    @Column(name = "assigned_at", nullable = false)
    var assignedAt: Instant,
    @Column(name = "released_at")
    var releasedAt: Instant?,
)

@Entity
@Table(name = "invoices")
class InvoiceJpaEntity(
    @Id
    var id: UUID,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    var booking: BookingJpaEntity,
    @Column(name = "invoice_number", nullable = false, unique = true, length = 255)
    var invoiceNumber: String,
    @Column(name = "net_amount_cents", nullable = false)
    var netAmountCents: Long,
    @Column(name = "tax_amount_cents", nullable = false)
    var taxAmountCents: Long,
    @Column(name = "gross_amount_cents", nullable = false)
    var grossAmountCents: Long,
    @Column(name = "currency_code", nullable = false, length = 3)
    var currencyCode: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: InvoiceStatus,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
    @OneToMany(mappedBy = "invoice", cascade = [jakarta.persistence.CascadeType.ALL], orphanRemoval = true)
    @OrderBy("id ASC")
    var lines: MutableList<InvoiceLineJpaEntity>,
)

@Entity
@Table(name = "invoice_lines")
class InvoiceLineJpaEntity(
    @Id
    var id: UUID,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    var invoice: InvoiceJpaEntity,
    @Column(nullable = false, columnDefinition = "text")
    var description: String,
    @Column(nullable = false, precision = 12, scale = 3)
    var quantity: BigDecimal,
    @Column(name = "unit_price_cents", nullable = false)
    var unitPriceCents: Long,
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    var taxRate: BigDecimal,
    @Column(name = "total_cents", nullable = false)
    var totalCents: Long,
)
