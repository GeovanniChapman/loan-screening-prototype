package dev.geo.loanscreening;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test with the whole stack: HTTP -> H2 (schema from Liquibase) -> JobRunr worker ->
 * Drools -> status update. Nothing is mocked.
 *
 * <p>The dashboard is switched off so the test does not bind port 8000. The four parameterized
 * cases are submitted and decided concurrently by the JobRunr workers.
 */
@SpringBootTest(properties = {
        "jobrunr.dashboard.enabled=false",
        // JobRunr workers poll the storage; 5 s is the minimum, which keeps this test quick.
        "jobrunr.background-job-server.poll-interval-in-seconds=5"
})
@AutoConfigureMockMvc
class LoanScreeningIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void liquibaseCreatedTheSchema() {
        Integer applied = jdbc.queryForObject("SELECT COUNT(*) FROM databasechangelog", Integer.class);
        Integer table = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) = 'loan_application'",
                Integer.class);

        assertThat(applied).isGreaterThanOrEqualTo(2);
        assertThat(table).isEqualTo(1);
    }

    @ParameterizedTest(name = "score={0}, income={1}, amount={2} -> {3}")
    @CsvSource({
            "800, 100000, 20000,  APPROVED",
            "600, 100000, 20000,  MANUAL_REVIEW",
            "450, 100000, 20000,  REJECTED",
            "800, 10000,  150000, REJECTED",
    })
    void applicationIsDecidedAsynchronously(int score, int income, int amount, String expectedStatus)
            throws Exception {
        var body = """
                { "applicantName": "Integration Test", "annualIncome": %d,
                  "requestedAmount": %d, "creditScore": %d }
                """.formatted(income, amount, score);

        // 1. Submit: answered immediately with 202 and a PENDING resource.
        var response = mvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andReturn().getResponse();
        String location = response.getHeader("Location");
        assertThat(JsonPath.<String>read(response.getContentAsString(), "$.status")).isEqualTo("PENDING");

        // 2. A JobRunr worker picks up the job and runs Drools; poll until the verdict appears.
        await().atMost(Duration.ofSeconds(45)).pollInterval(Duration.ofMillis(500)).untilAsserted(() -> {
            var json = mvc.perform(get(location)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(JsonPath.<String>read(json, "$.status")).isEqualTo(expectedStatus);
            assertThat(JsonPath.<String>read(json, "$.decisionReason")).isNotBlank();
            assertThat(JsonPath.<String>read(json, "$.evaluatedAt")).isNotBlank();
        });
    }
}
