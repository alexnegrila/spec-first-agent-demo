package uk.co.lexcoders.demo.timesheets;

import java.util.UUID;

class TimesheetNotFoundException extends RuntimeException {

    TimesheetNotFoundException(UUID timesheetId) {
        super("There is no timesheet with id " + timesheetId + ".");
    }
}
