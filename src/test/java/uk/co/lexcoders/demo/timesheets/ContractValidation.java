package uk.co.lexcoders.demo.timesheets;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;

import java.nio.file.Path;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * Checks every MockMvc exchange in the tests against api/openapi.yaml.
 *
 * <p>Tests need no extra code for this. A response field, status code, header or content type the
 * contract does not document fails the test that produced it, and so does a request to a path or
 * method the contract does not have. See docs/adr/0001-the-openapi-file-is-the-contract.md.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContractValidation {

    static final String CONTRACT = Path.of("api/openapi.yaml").toUri().toString();

    /**
     * The validator every test shares. Request bodies and parameters are not checked here, so that
     * negative tests can send invalid input on purpose; the application enforces request shape itself.
     */
    static OpenApiInteractionValidator contractValidator() {
        return OpenApiInteractionValidator.createForSpecificationUrl(CONTRACT)
                .withLevelResolver(LevelResolver.create()
                        .withLevel("validation.request.body", ValidationReport.Level.IGNORE)
                        .withLevel("validation.request.parameter", ValidationReport.Level.IGNORE)
                        .build())
                .build();
    }

    @Bean
    MockMvcBuilderCustomizer checkEveryExchangeAgainstTheContract() {
        OpenApiInteractionValidator validator = contractValidator();
        return builder -> builder.alwaysExpect(openApi().isValid(validator));
    }
}
