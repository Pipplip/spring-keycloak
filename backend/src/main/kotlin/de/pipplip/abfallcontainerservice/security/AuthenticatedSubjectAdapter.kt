package de.pipplip.abfallcontainerservice.security

import de.pipplip.abfallcontainerservice.application.port.out.AuthenticatedSubjectProvider
import de.pipplip.abfallcontainerservice.application.port.out.AuthenticatedCustomerIdentity
import de.pipplip.abfallcontainerservice.application.port.out.AuthenticatedCustomerIdentityProvider
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class AuthenticatedSubjectAdapter : AuthenticatedSubjectProvider, AuthenticatedCustomerIdentityProvider {
    override fun currentSubject(): String? = currentIdentity()?.subject

    override fun currentIdentity(): AuthenticatedCustomerIdentity? {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication !is JwtAuthenticationToken || !authentication.isAuthenticated) {
            return null
        }
        val token = authentication.token
        val subject = token.subject?.takeIf(String::isNotBlank) ?: return null
        return AuthenticatedCustomerIdentity(
            subject = subject,
            email = token.getClaimAsString("email"),
            firstName = token.getClaimAsString("given_name"),
            lastName = token.getClaimAsString("family_name"),
        )
    }
}
