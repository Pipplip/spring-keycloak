package de.pipplip.abfallcontainerservice.security

import de.pipplip.abfallcontainerservice.application.service.AdminConflictException
import de.pipplip.abfallcontainerservice.application.service.AdminRequestValidationException
import de.pipplip.abfallcontainerservice.application.service.AdminResourceNotFoundException
import de.pipplip.abfallcontainerservice.application.service.AuthenticatedSubjectMissingException
import de.pipplip.abfallcontainerservice.application.service.BookingConflictException
import de.pipplip.abfallcontainerservice.application.service.BookingNotFoundException
import de.pipplip.abfallcontainerservice.application.service.BookingOwnershipDeniedException
import de.pipplip.abfallcontainerservice.application.service.ContainerTypeNotFoundException
import de.pipplip.abfallcontainerservice.application.service.CustomerProfileNotFoundException
import de.pipplip.abfallcontainerservice.application.service.InsufficientInventoryException
import de.pipplip.abfallcontainerservice.application.service.InvalidCustomerFeatureRequestException
import de.pipplip.abfallcontainerservice.application.service.InvoiceNotFoundException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.net.URI

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class CustomerOwnershipExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

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

    @ExceptionHandler(
        ContainerTypeNotFoundException::class,
        InvoiceNotFoundException::class,
        AdminResourceNotFoundException::class,
    )
    fun resourceNotFound(): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.NOT_FOUND, "The requested resource was not found.")

    @ExceptionHandler(
        InvalidCustomerFeatureRequestException::class,
        AdminRequestValidationException::class,
        MethodArgumentNotValidException::class,
        ConstraintViolationException::class,
        HandlerMethodValidationException::class,
        HttpMessageNotReadableException::class,
        MissingServletRequestParameterException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun invalidRequest(): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_REQUEST, "The request is invalid.")

    @ExceptionHandler(
        InsufficientInventoryException::class,
        BookingConflictException::class,
        AdminConflictException::class,
    )
    fun conflict(): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.CONFLICT, "The requested operation cannot be completed in the current state.")

    @ExceptionHandler(Exception::class)
    fun unexpectedFailure(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val errorStatus = (exception as? ErrorResponse)?.statusCode
        if (errorStatus?.is4xxClientError == true) {
            return problem(errorStatus, "The request could not be processed.")
        }

        val route =
            request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as? String
                ?: "unmapped"
        logger.error(
            "Unexpected API failure for {} {} (exception type: {})",
            request.method,
            route,
            exception.javaClass.name,
        )
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.")
    }

    private fun problem(
        status: HttpStatusCode,
        detail: String,
    ): ResponseEntity<ProblemDetail> =
        ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problemBody(status, detail))

    private fun problemBody(
        status: HttpStatusCode,
        detail: String,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            type = URI.create("about:blank")
            title = HttpStatus.resolve(status.value())?.reasonPhrase ?: "HTTP Error"
        }
}
