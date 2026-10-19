package dev.striveh.aftersales.agent.eval;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EvalSuiteTest {

    @Test
    void deterministicSuitePassesWithoutApiKey() {
        EvalReport report = EvalHarness.offline();
        assertThat(report.failures()).as(report.summary() + "\n" + String.join("\n", report.failures())).isEmpty();
        assertThat(report.total()).isBetween(20, 40);
    }
}
