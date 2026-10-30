package de.pipplip.abfallcontainerservice

import de.pipplip.abfallcontainerservice.security.JwtAudienceValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class JwtAudienceValidatorTest {
    private val validator = JwtAudienceValidator("abfallcontainer-backend")

    @Test
    fun `accepts tokens containing the configured backend audience`() {
        val jwt = token(listOf("other-client", "abfallcontainer-backend"))

        assertThat(validator.validate(jwt).errors).isEmpty()
    }

    @Test
    fun `rejects tokens without the configured backend audience`() {
        val result = validator.validate(token(listOf("abfallcontainer-frontend")))

        assertThat(result.errors).hasSize(1)
        assertThat(result.errors.single().errorCode).isEqualTo("invalid_token")
    }

    private fun token(audience: List<String>) =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("subject")
            .audience(audience)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build()
}
