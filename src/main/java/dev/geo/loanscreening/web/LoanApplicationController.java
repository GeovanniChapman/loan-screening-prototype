package dev.geo.loanscreening.web;

import dev.geo.loanscreening.service.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

/** REST API for loan applications. Contains no business logic: it only translates HTTP. */
@RestController
@RequestMapping("/api/applications")
public class LoanApplicationController {

    private final LoanApplicationService service;

    public LoanApplicationController(LoanApplicationService service) {
        this.service = service;
    }

    /**
     * Submits an application. Responds <b>202 Accepted</b> (not 201): the resource exists, but
     * the decision is produced asynchronously. Poll the {@code Location} header until the
     * status is no longer {@code PENDING}.
     */
    @PostMapping
    public ResponseEntity<LoanApplicationResponse> submit(@Valid @RequestBody SubmitLoanRequest request) {
        var created = service.submit(request.toCommand());
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.accepted().location(location).body(LoanApplicationResponse.from(created));
    }

    @GetMapping("/{id}")
    public LoanApplicationResponse get(@PathVariable UUID id) {
        return LoanApplicationResponse.from(service.get(id));
    }

    /** Newest first. */
    @GetMapping
    public List<LoanApplicationResponse> recent(@RequestParam(defaultValue = "20") int limit) {
        return service.recent(limit).stream().map(LoanApplicationResponse::from).toList();
    }
}
