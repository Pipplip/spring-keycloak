package de.pipplip.abfallcontainerservice

import de.pipplip.abfallcontainerservice.security.KeycloakRealmRoleConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt

class KeycloakRealmRoleConverterTest {
    private val converter = KeycloakRealmRoleConverter()

    @Test
    fun `maps only supported realm roles and removes duplicates`() {
        val jwt =
            Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", mapOf("roles" to listOf("USER", "ADMIN", "USER", "offline_access")))
                .build()

        assertThat(converter.convert(jwt).map { it.authority })
            .containsExactly("ROLE_USER", "ROLE_ADMIN")
    }

    @Test
    fun `missing or malformed role claims grant no authorities`() {
        val withoutRealmAccess =
            Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject")
                .build()
        val malformedRoles =
            Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", mapOf("roles" to "ADMIN"))
                .build()
        val unknownRoles =
            Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", mapOf("roles" to listOf("SUPERUSER")))
                .build()

        assertThat(converter.convert(withoutRealmAccess)).isEmpty()
        assertThat(converter.convert(malformedRoles)).isEmpty()
        assertThat(converter.convert(unknownRoles)).isEmpty()
    }
}
