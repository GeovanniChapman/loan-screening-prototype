package dev.geo.loanscreening.rules;

import dev.geo.loanscreening.domain.Decision;
import org.drools.model.codegen.ExecutableModelProject;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Thin adapter between the application and Drools.
 *
 * <p><b>Lifecycle and thread-safety</b>
 * <ul>
 *   <li>The DRL is compiled <em>once</em>, at startup, into a {@link KieBase} (an immutable,
 *       thread-safe rule base). A syntax error in the DRL therefore fails the application
 *       start-up, not the first request.</li>
 *   <li>Each {@link #evaluate} call creates its own cheap {@link StatelessKieSession} and its
 *       own {@link DecisionCollector}, so the engine can be called concurrently from many
 *       threads (including virtual threads) with no locking.</li>
 * </ul>
 */
@Component
public class LoanRiskRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(LoanRiskRuleEngine.class);

    /** Location of the rules on the classpath. */
    static final String DRL_RESOURCE = "dev/geo/loanscreening/rules/loan-risk.drl";

    private final KieBase kieBase;

    public LoanRiskRuleEngine() {
        this.kieBase = compileRules();
    }

    /**
     * Runs the rules against one fact.
     *
     * @return the verdict of the highest-priority rule that matched; if (unexpectedly) no rule
     *         matched, a fail-safe {@link Decision.ManualReview} so that nobody is approved
     *         or rejected by accident
     */
    public Decision evaluate(LoanFact fact) {
        var collector = new DecisionCollector();
        StatelessKieSession session = kieBase.newStatelessKieSession();
        session.setGlobal("verdicts", collector);
        session.execute(fact);

        Decision decision = collector.winner()
                .orElseGet(() -> new Decision.ManualReview("No rule matched; manual review as fail-safe"));
        log.debug("Facts {} -> {} ({} rule(s) fired)", fact, decision.summary(), collector.verdicts().size());
        return decision;
    }

    private static KieBase compileRules() {
        KieServices services = KieServices.Factory.get();
        KieFileSystem files = services.newKieFileSystem()
                .write("src/main/resources/" + DRL_RESOURCE, readDrl());

        // Drools 10 only supports the "executable model": the DRL is translated to plain Java
        // lambdas and compiled, which is faster at runtime and type-checked at build time.
        KieBuilder builder = services.newKieBuilder(files).buildAll(ExecutableModelProject.class);

        // Fail fast with the compiler's messages if the DRL is invalid.
        Results results = builder.getResults();
        if (results.hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException("Invalid DRL in %s: %s"
                    .formatted(DRL_RESOURCE, results.getMessages(Message.Level.ERROR)));
        }
        log.info("Drools rules compiled from {}", DRL_RESOURCE);
        KieContainer container = services.newKieContainer(builder.getKieModule().getReleaseId());
        return container.getKieBase();
    }

    private static String readDrl() {
        try (InputStream in = LoanRiskRuleEngine.class.getClassLoader().getResourceAsStream(DRL_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Rules file not found on classpath: " + DRL_RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + DRL_RESOURCE, e);
        }
    }
}
