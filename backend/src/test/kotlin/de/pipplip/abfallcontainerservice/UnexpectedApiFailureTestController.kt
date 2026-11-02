package de.pipplip.abfallcontainerservice

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.ConstraintViolationException

@RestController
class UnexpectedApiFailureTestController {
    @GetMapping("/test-api/unexpected-failure/{marker}")
    fun fail(): Nothing = throw IllegalStateException("sensitive-exception-marker")

    @GetMapping("/test-api/constraint-failure")
    fun failValidation(): Nothing = throw ConstraintViolationException("invalid test input", emptySet())
}
