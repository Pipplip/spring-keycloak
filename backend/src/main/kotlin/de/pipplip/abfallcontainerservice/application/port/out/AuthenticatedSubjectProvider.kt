package de.pipplip.abfallcontainerservice.application.port.out

interface AuthenticatedSubjectProvider {
    fun currentSubject(): String?
}
