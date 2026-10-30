package de.pipplip.abfallcontainerservice.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class KeycloakRealmRoleConverter : Converter<Jwt, Collection<GrantedAuthority>> {
    override fun convert(source: Jwt): Collection<GrantedAuthority> {
        val realmAccess = source.claims["realm_access"] as? Map<*, *> ?: return emptyList()
        val roles = realmAccess["roles"] as? Collection<*> ?: return emptyList()

        return roles
            .filterIsInstance<String>()
            .filter { it == "USER" || it == "ADMIN" }
            .distinct()
            .map { SimpleGrantedAuthority("ROLE_$it") }
    }
}
