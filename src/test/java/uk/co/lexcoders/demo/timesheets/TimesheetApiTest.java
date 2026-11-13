package uk.co.lexcoders.demo.timesheets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Behaviour tests. Every exchange here is also checked against api/openapi.yaml (see ContractValidation). */
@ContractTest
class TimesheetApiTest {

    private static final String ONE_DAY_WITH_NOTE = """
            {"weekStarting": "2026-11-09",
             "entries": [{"date": "2026-11-09", "hours": 7.5, "note": "Spec review and ADR"}]}
            """;

    private static final String TWO_DAYS_WITHOUT_NOTES = """
            {"weekStarting": "2026-11-09",
             "entries": [{"date": "2026-11-09", "hours": 8}, {"date": "2026-11-10", "hours": 6.5}]}
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void createsADraftTimesheet() throws Exception {
        mvc.perform(post("/timesheets").contentType(MediaType.APPLICATION_JSON).content(ONE_DAY_WITH_NOTE))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("http://localhost/timesheets/")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.entries[0].note").value("Spec review and ADR"));
    }

    @Test
    void readsATimesheetBack() throws Exception {
        String location = mvc.perform(post("/timesheets").contentType(MediaType.APPLICATION_JSON).content(TWO_DAYS_WITHOUT_NOTES))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(URI.create(location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStarting").value("2026-11-09"))
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.totalHours").value(14.5));
    }

    @Test
    void anUnknownTimesheetIsA404Problem() throws Exception {
        mvc.perform(get("/timesheets/{timesheetId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void aMalformedIdIsA400Problem() throws Exception {
        mvc.perform(get("/timesheets/{timesheetId}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void moreThan24HoursInADayIsA400Problem() throws Exception {
        mvc.perform(post("/timesheets").contentType(MediaType.APPLICATION_JSON).content("""
                        {"weekStarting": "2026-11-09", "entries": [{"date": "2026-11-09", "hours": 25}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void aWeekStartsOnAMonday() throws Exception {
        mvc.perform(post("/timesheets").contentType(MediaType.APPLICATION_JSON).content("""
                        {"weekStarting": "2026-11-10", "entries": [{"date": "2026-11-10", "hours": 7}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Monday")));
    }

    @Test
    void entriesFallInsideTheWeek() throws Exception {
        mvc.perform(post("/timesheets").contentType(MediaType.APPLICATION_JSON).content("""
                        {"weekStarting": "2026-11-09", "entries": [{"date": "2026-11-16", "hours": 7}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("outside the week")));
    }
}
