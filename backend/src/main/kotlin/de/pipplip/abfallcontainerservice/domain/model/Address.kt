package de.pipplip.abfallcontainerservice.domain.model

data class Address(
    val street: String,
    val houseNumber: String,
    val postalCode: String,
    val city: String,
    val countryCode: String,
)
