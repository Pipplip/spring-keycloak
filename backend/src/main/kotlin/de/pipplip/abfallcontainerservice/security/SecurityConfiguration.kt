package de.pipplip.abfallcontainerservice.security

import de.pipplip.abfallcontainerservice.application.port.out.CustomerRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.authorization.AuthorizationDecision
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import java.net.URI

@Configuration
@EnableWebSecurity
class SecurityConfiguration(
    private val customerRepository: CustomerRepository,
    private val realmRoleConverter: KeycloakRealmRoleConverter,
    private val objectMapper: ObjectMapper,
    @param:Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private val issuerUri: String,
    @param:Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private val jwkSetUri: String,
    @param:Value("\${spring.security.oauth2.resourceserver.jwt.audience}")
    private val requiredAudience: String,
) {
    @Bean
    fun jwtDecoder(): JwtDecoder {
        val decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                JwtAudienceValidator(requiredAudience),
            ),
        )
        return decoder
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val jwtAuthenticationConverter =
            JwtAuthenticationConverter().apply {
                setJwtGrantedAuthoritiesConverter(realmRoleConverter)
            }

        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(problemAuthenticationEntryPoint())
                it.accessDeniedHandler(problemAccessDeniedHandler())
            }
            .authorizeHttpRequests {
                it.requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/container-types",
                    "/api/v1/container-types/*",
                    "/api/v1/availability",
                ).permitAll()
                it.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                it.requestMatchers("/api/v1/bookings", "/api/v1/bookings/**")
                    .access { authentication, _ ->
                        val principal = authentication.get()
                        val hasUserRole =
                            principal.authorities.any { authority ->
                                authority.authority == "ROLE_USER"
                            }
                        val subject = (principal as? org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken)
                            ?.token
                            ?.subject
                        AuthorizationDecision(
                            hasUserRole &&
                                !subject.isNullOrBlank() &&
                                customerRepository.findByKeycloakSubject(subject) != null,
                        )
                    }
                it.requestMatchers("/api/v1/**").denyAll()
                it.anyRequest().permitAll()
            }
            .oauth2ResourceServer {
                it.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter) }
                it.authenticationEntryPoint(problemAuthenticationEntryPoint())
                it.accessDeniedHandler(problemAccessDeniedHandler())
            }

        return http.build()
    }

    private fun problemAuthenticationEntryPoint(): AuthenticationEntryPoint =
        AuthenticationEntryPoint { _, response, _ ->
            writeProblem(
                response,
                HttpStatus.UNAUTHORIZED,
                "Authentication is required to access this resource.",
            )
        }

    private fun problemAccessDeniedHandler(): AccessDeniedHandler =
        AccessDeniedHandler { _, response, _ ->
            writeProblem(
                response,
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource.",
            )
        }

    private fun writeProblem(
        response: jakarta.servlet.http.HttpServletResponse,
        status: HttpStatus,
        detail: String,
    ) {
        val problem = ProblemDetail.forStatusAndDetail(status, detail).apply {
            type = URI.create("about:blank")
            title = status.reasonPhrase
        }
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(response.outputStream, problem)
    }
}
