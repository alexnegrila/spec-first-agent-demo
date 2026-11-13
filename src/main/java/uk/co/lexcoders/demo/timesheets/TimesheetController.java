package uk.co.lexcoders.demo.timesheets;

import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import uk.co.lexcoders.demo.timesheets.api.TimesheetsApi;
import uk.co.lexcoders.demo.timesheets.api.model.Entry;
import uk.co.lexcoders.demo.timesheets.api.model.NewTimesheet;
import uk.co.lexcoders.demo.timesheets.api.model.Timesheet;

/**
 * Implements the interface generated from api/openapi.yaml.
 * The signatures come from the contract, so they cannot drift without a compile error.
 */
@RestController
class TimesheetController implements TimesheetsApi {

    private final Map<UUID, Timesheet> timesheets = new ConcurrentHashMap<>();

    @Override
    public ResponseEntity<Timesheet> createTimesheet(NewTimesheet newTimesheet) {
        LocalDate weekStarting = newTimesheet.getWeekStarting();
        if (weekStarting.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new InvalidTimesheetException("weekStarting must be a Monday, but " + weekStarting + " is a " + weekStarting.getDayOfWeek() + ".");
        }
        for (Entry entry : newTimesheet.getEntries()) {
            if (entry.getDate().isBefore(weekStarting) || entry.getDate().isAfter(weekStarting.plusDays(6))) {
                throw new InvalidTimesheetException("Entry date " + entry.getDate() + " is outside the week starting " + weekStarting + ".");
            }
        }

        Timesheet timesheet = new Timesheet()
                .id(UUID.randomUUID())
                .weekStarting(weekStarting)
                .entries(newTimesheet.getEntries())
                .status(Timesheet.StatusEnum.DRAFT);
        timesheets.put(timesheet.getId(), timesheet);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{timesheetId}")
                .buildAndExpand(timesheet.getId())
                .toUri();
        return ResponseEntity.created(location).body(timesheet);
    }

    @Override
    public ResponseEntity<Timesheet> getTimesheet(UUID timesheetId) {
        Timesheet timesheet = timesheets.get(timesheetId);
        if (timesheet == null) {
            throw new TimesheetNotFoundException(timesheetId);
        }
        return ResponseEntity.ok(timesheet);
    }
}
