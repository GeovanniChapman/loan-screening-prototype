package dev.geo.loanscreening.web;

import dev.geo.loanscreening.domain.LoanApplication;
import dev.geo.loanscreening.service.ApplicationNotFoundException;
import dev.geo.loanscreening.service.LoanApplicationService;
import dev.geo.loanscreening.service.SubmitLoanCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web-slice test: only the MVC layer is started; the service is a mock. */
@WebMvcTest(LoanApplicationController.class)
class LoanApplicationControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean LoanApplicationService service;

    private static final String VALID_BODY = """
            {
              "applicantName": "Ada Lovelace",
              "annualIncome": 60000,
              "requestedAmount": 15000,
              "creditScore": 720
            }
            """;

    private static LoanApplication sample() {
        return LoanApplication.submit("Ada Lovelace", new BigDecimal("60000"), new BigDecimal("15000"),
                720, Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Test
    void submitReturns202WithLocation() throws Exception {
        var application = sample();
        when(service.submit(any(SubmitLoanCommand.class))).thenReturn(application);

        mvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "http://localhost/api/applications/" + application.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.applicantName").value("Ada Lovelace"));
    }

    @Test
    void submitRejectsInvalidBodyWithProblemDetail() throws Exception {
        var invalid = """
                { "applicantName": "", "annualIncome": -1, "requestedAmount": 100, "creditScore": 900 }
                """;

        mvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"));
    }

    @Test
    void getReturns404ForUnknownId() throws Exception {
        var id = UUID.randomUUID();
        when(service.get(id)).thenThrow(new ApplicationNotFoundException(id));

        mvc.perform(get("/api/applications/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Loan application %s not found".formatted(id)));
    }

    @Test
    void listReturnsApplications() throws Exception {
        when(service.recent(5)).thenReturn(List.of(sample()));

        mvc.perform(get("/api/applications").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].creditScore").value(720));
    }
}
