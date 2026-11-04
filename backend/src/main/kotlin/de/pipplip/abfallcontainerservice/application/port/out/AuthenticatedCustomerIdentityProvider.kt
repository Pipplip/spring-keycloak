package de.pipplip.abfallcontainerservice.application.port.out

data class AuthenticatedCustomerIdentity(
    val subject: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
)

interface AuthenticatedCustomerIdentityProvider {
    fun currentIdentity(): AuthenticatedCustomerIdentity?
}
