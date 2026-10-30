package de.pipplip.abfallcontainerservice.security

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt

class JwtAudienceValidator(
    private val requiredAudience: String,
) : OAuth2TokenValidator<Jwt> {
    override fun validate(token: Jwt): OAuth2TokenValidatorResult =
        if (requiredAudience in token.audience) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(
                OAuth2Error("invalid_token", "Token audience is not accepted for this API.", null),
            )
        }
}
