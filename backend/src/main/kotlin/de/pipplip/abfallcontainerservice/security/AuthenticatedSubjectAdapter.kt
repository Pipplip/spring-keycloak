package de.pipplip.abfallcontainerservice.security

import de.pipplip.abfallcontainerservice.application.port.out.AuthenticatedSubjectProvider
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class AuthenticatedSubjectAdapter : AuthenticatedSubjectProvider {
    override fun currentSubject(): String? {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication !is JwtAuthenticationToken || !authentication.isAuthenticated) {
            return null
        }
        return authentication.token.subject?.takeIf(String::isNotBlank)
    }
}
