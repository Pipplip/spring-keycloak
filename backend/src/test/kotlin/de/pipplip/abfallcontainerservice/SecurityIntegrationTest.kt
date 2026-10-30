package de.pipplip.abfallcontainerservice

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.sun.net.httpserver.HttpServer
import de.pipplip.abfallcontainerservice.application.port.out.CustomerRepository
import de.pipplip.abfallcontainerservice.domain.model.Address
import de.pipplip.abfallcontainerservice.domain.model.Customer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.InetSocketAddress
import java.time.Instant
import java.util.Date
import java.util.UUID

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var jwtDecoder: JwtDecoder

    @Test
    fun `public catalogue route permits anonymous requests`() {
        mockMvc.perform(get("/api/v1/container-types"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `protected customer route returns problem details for anonymous requests`() {
        val result =
            mockMvc.perform(get("/api/v1/bookings"))
                .andExpect(status().isUnauthorized)
                .andReturn()

        assertThat(result.response.contentType).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        assertThat(result.response.contentAsString).contains("\"status\":401")
    }

    @Test
    fun `customer routes require USER role and a local customer profile`() {
        mockMvc.perform(
            get("/api/v1/bookings")
                .with(jwt().jwt { it.subject("missing-customer") }.authorities(SimpleGrantedAuthority("ROLE_USER"))),
        ).andExpect(status().isForbidden)

        val customer = saveCustomer("known-customer")
        mockMvc.perform(
            get("/api/v1/bookings")
                .with(jwt().jwt { it.subject(customer.keycloakSubject) }.authorities(SimpleGrantedAuthority("ROLE_USER"))),
        ).andExpect(status().isNotFound)

        mockMvc.perform(
            get("/api/v1/bookings")
                .with(jwt().jwt { it.subject(customer.keycloakSubject) }.authorities(SimpleGrantedAuthority("ROLE_ADMIN"))),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `admin routes require ADMIN role and unmatched API routes are denied`() {
        mockMvc.perform(
            get("/api/v1/admin/bookings")
                .with(jwt().jwt { it.subject("administrator") }.authorities(SimpleGrantedAuthority("ROLE_ADMIN"))),
        ).andExpect(status().isNotFound)

        mockMvc.perform(
            get("/api/v1/admin/bookings")
                .with(jwt().jwt { it.subject("customer") }.authorities(SimpleGrantedAuthority("ROLE_USER"))),
        ).andExpect(status().isForbidden)

        val forbidden =
            mockMvc.perform(
                get("/api/v1/not-in-the-contract")
                    .with(jwt().jwt { it.subject("administrator") }.authorities(SimpleGrantedAuthority("ROLE_ADMIN"))),
            ).andExpect(status().isForbidden)
                .andReturn()

        mockMvc.perform(
                get("/api/v1/container-types/unclassified/nested")
                    .with(jwt().jwt { it.subject("administrator") }.authorities(SimpleGrantedAuthority("ROLE_ADMIN"))),
            ).andExpect(status().isForbidden)
        assertThat(forbidden.response.contentType).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        assertThat(forbidden.response.contentAsString).contains("\"status\":403")
    }

    @Test
    fun `decoder validates signature issuer expiration and audience`() {
        assertThat(jwtDecoder.decode(signedToken(signingKey))).isNotNull

        assertThatThrownBy { jwtDecoder.decode(signedToken(signingKey, issuer = "https://wrong-issuer.test")) }
            .isInstanceOf(JwtException::class.java)
        assertThatThrownBy { jwtDecoder.decode(signedToken(signingKey, audience = listOf("another-api"))) }
            .isInstanceOf(JwtException::class.java)
        assertThatThrownBy { jwtDecoder.decode(signedToken(otherSigningKey)) }
            .isInstanceOf(JwtException::class.java)
        assertThatThrownBy {
            jwtDecoder.decode(signedToken(signingKey, expiresAt = Instant.now().minusSeconds(120)))
        }.isInstanceOf(JwtException::class.java)
    }

    private fun saveCustomer(subject: String): Customer =
        customerRepository.save(
            Customer(
                id = UUID.randomUUID(),
                keycloakSubject = subject,
                email = "$subject@example.test",
                firstName = "Test",
                lastName = "Customer",
                companyName = null,
                billingAddress =
                    Address(
                        street = "Main Street",
                        houseNumber = "1",
                        postalCode = "12345",
                        city = "Test City",
                        countryCode = "DE",
                    ),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

    private fun signedToken(
        key: RSAKey,
        issuer: String = issuerUri,
        audience: List<String> = listOf("abfallcontainer-backend"),
        expiresAt: Instant = Instant.now().plusSeconds(120),
    ): String {
        val now = Instant.now()
        val claims =
            JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("test-subject")
                .audience(audience)
                .issueTime(Date.from(now.minusSeconds(5)))
                .expirationTime(Date.from(expiresAt))
                .build()
        val signed =
            SignedJWT(
                JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.keyID).build(),
                claims,
            )
        signed.sign(RSASSASigner(key))
        return signed.serialize()
    }

    companion object {
        private const val issuerUri = "https://keycloak.test/realms/abfallcontainer-realm"
        private val signingKey = RSAKeyGenerator(2048).keyID("security-test-key").generate()
        private val otherSigningKey = RSAKeyGenerator(2048).keyID("different-key").generate()
        private val jwksServer =
            HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
                createContext("/jwks") { exchange ->
                    val body = JWKSet(signingKey.toPublicJWK()).toString().toByteArray()
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                }
                start()
            }

        @Container
        @JvmStatic
        private val postgres = PostgreSQLContainer<Nothing>("postgres:17-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { issuerUri }
            registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri") {
                "http://127.0.0.1:${jwksServer.address.port}/jwks"
            }
        }

        @AfterAll
        @JvmStatic
        fun stopJwksServer() {
            jwksServer.stop(0)
        }
    }
}
