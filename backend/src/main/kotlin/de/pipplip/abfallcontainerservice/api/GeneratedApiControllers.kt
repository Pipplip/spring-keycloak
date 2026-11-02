package de.pipplip.abfallcontainerservice.api

import de.pipplip.abfallcontainerservice.generated.api.AvailabilityApi
import de.pipplip.abfallcontainerservice.generated.api.AvailabilityApiDelegate
import de.pipplip.abfallcontainerservice.generated.api.AdministrationApi
import de.pipplip.abfallcontainerservice.generated.api.AdministrationApiDelegate
import de.pipplip.abfallcontainerservice.generated.api.BookingsApi
import de.pipplip.abfallcontainerservice.generated.api.BookingsApiDelegate
import de.pipplip.abfallcontainerservice.generated.api.CatalogueApi
import de.pipplip.abfallcontainerservice.generated.api.CatalogueApiDelegate
import de.pipplip.abfallcontainerservice.generated.api.InvoicesApi
import de.pipplip.abfallcontainerservice.generated.api.InvoicesApiDelegate
import org.springframework.web.bind.annotation.RestController

@RestController
class AdministrationApiController(
    private val delegate: AdministrationApiDelegate,
) : AdministrationApi {
    override fun getDelegate(): AdministrationApiDelegate = delegate
}

@RestController
class CatalogueApiController(
    private val delegate: CatalogueApiDelegate,
) : CatalogueApi {
    override fun getDelegate(): CatalogueApiDelegate = delegate
}

@RestController
class AvailabilityApiController(
    private val delegate: AvailabilityApiDelegate,
) : AvailabilityApi {
    override fun getDelegate(): AvailabilityApiDelegate = delegate
}

@RestController
class BookingsApiController(
    private val delegate: BookingsApiDelegate,
) : BookingsApi {
    override fun getDelegate(): BookingsApiDelegate = delegate
}

@RestController
class InvoicesApiController(
    private val delegate: InvoicesApiDelegate,
) : InvoicesApi {
    override fun getDelegate(): InvoicesApiDelegate = delegate
}
