package uk.co.lexcoders.demo.timesheets;

import static org.assertj.core.api.Assertions.assertThat;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.api.Test;

/**
 * Proves the contract check still bites. If someone loosens the spec (for example by dropping
 * additionalProperties: false) or the validator's levels, these tests fail.
 */
class ContractGuardTest {

    private static final String TIMESHEET_ID = "5b0f0a3e-8c1d-4c5e-9c55-2d5f1f0b9a11";

    private static final String DOCUMENTED_TIMESHEET = """
            {"id": "%s", "weekStarting": "2026-11-09",
             "entries": [{"date": "2026-11-09", "hours": 7.5}], "status": "DRAFT"}
            """.formatted(TIMESHEET_ID);

    private static final String TIMESHEET_WITH_UNDOCUMENTED_FIELD = """
            {"id": "%s", "weekStarting": "2026-11-09",
             "entries": [{"date": "2026-11-09", "hours": 7.5}], "status": "DRAFT", "totalHours": 7.5}
            """.formatted(TIMESHEET_ID);

    private final OpenApiInteractionValidator validator = ContractValidation.contractValidator();

    @Test
    void aDocumentedResponseMeetsTheContract() {
        ValidationReport report = validator.validateResponse("/timesheets/" + TIMESHEET_ID, Request.Method.GET,
                SimpleResponse.Builder.ok().withContentType("application/json").withBody(DOCUMENTED_TIMESHEET).build());

        assertThat(report.getMessages()).isEmpty();
    }

    @Test
    void anUndocumentedResponseFieldBreaksTheContract() {
        ValidationReport report = validator.validateResponse("/timesheets/" + TIMESHEET_ID, Request.Method.GET,
                SimpleResponse.Builder.ok().withContentType("application/json").withBody(TIMESHEET_WITH_UNDOCUMENTED_FIELD).build());

        assertThat(report.hasErrors()).isTrue();
        assertThat(report.getMessages()).extracting(ValidationReport.Message::getKey)
                .contains("validation.response.body.schema.additionalProperties");
    }

    @Test
    void anUndocumentedStatusCodeBreaksTheContract() {
        ValidationReport report = validator.validateResponse("/timesheets", Request.Method.POST,
                SimpleResponse.Builder.ok().withContentType("application/json").withBody(DOCUMENTED_TIMESHEET).build());

        assertThat(report.hasErrors()).isTrue();
        assertThat(report.getMessages()).extracting(ValidationReport.Message::getKey)
                .contains("validation.response.status.unknown");
    }
}
