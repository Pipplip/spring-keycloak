package de.pipplip.abfallcontainerservice

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate

@EnabledIfEnvironmentVariable(named = "COMPOSE_DATABASE_TEST", matches = "true")
@SpringBootTest
class ComposeDatabaseConnectivityTest {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `connects to the configured Compose PostgreSQL database`() {
        val result = requireNotNull(jdbcTemplate.queryForObject("SELECT 1", Int::class.java))
        assertThat(result).isEqualTo(1)
    }
}
