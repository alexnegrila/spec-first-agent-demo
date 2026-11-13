package uk.co.lexcoders.demo.timesheets;

/** A timesheet that matches the schema but breaks a rule the schema cannot express. */
class InvalidTimesheetException extends RuntimeException {

    InvalidTimesheetException(String message) {
        super(message);
    }
}
