package de.pipplip.abfallcontainerservice.security

import de.pipplip.abfallcontainerservice.application.service.BookingNotFoundException
import de.pipplip.abfallcontainerservice.application.service.BookingOwnershipDeniedException
import de.pipplip.abfallcontainerservice.application.service.AuthenticatedSubjectMissingException
import de.pipplip.abfallcontainerservice.application.service.CustomerProfileNotFoundException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class CustomerOwnershipExceptionHandler {
    @ExceptionHandler(AuthenticatedSubjectMissingException::class)
    fun unauthenticated(): ResponseEntity<ProblemDetail> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problemBody(HttpStatus.UNAUTHORIZED, "A verified customer identity is required."))

    @ExceptionHandler(BookingOwnershipDeniedException::class, CustomerProfileNotFoundException::class)
    fun forbidden(): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.")

    @ExceptionHandler(BookingNotFoundException::class)
    fun bookingNotFound(): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.NOT_FOUND, "The requested booking was not found.")

    private fun problem(
        status: HttpStatus,
        detail: String,
    ): ResponseEntity<ProblemDetail> =
        ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problemBody(status, detail))

    private fun problemBody(
        status: HttpStatus,
        detail: String,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            type = URI.create("about:blank")
            title = status.reasonPhrase
        }
}
