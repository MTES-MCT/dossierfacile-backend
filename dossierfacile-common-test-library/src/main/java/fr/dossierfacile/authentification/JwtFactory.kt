package fr.dossierfacile.authentification

import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.time.temporal.ChronoUnit

fun getDummyJwtWithCustomClaims(
    claims: Map<String, Any>
): Jwt {
    return getDummyJwt(claims = claims)
}

@JvmOverloads
fun getDummyJwt(
    tokenValue: String? = "test",
    headers: Map<String, Any>? = mapOf(
        "alg" to "none"
    ),
    claims: Map<String, Any>? = mapOf(
        "test" to "test"
    ),
    issuedAt: Instant? = Instant.now(),
    expiresAt: Instant? = Instant.now().plus(10, ChronoUnit.MINUTES)
): Jwt {
    val jwtBuilder = Jwt.withTokenValue(tokenValue)
    headers?.forEach(jwtBuilder::header)
    claims?.forEach(jwtBuilder::claim)
    jwtBuilder.issuedAt(issuedAt)
    jwtBuilder.expiresAt(expiresAt)

    return jwtBuilder.build()
}