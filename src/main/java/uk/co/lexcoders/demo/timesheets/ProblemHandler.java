package uk.co.lexcoders.demo.timesheets;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns this application's errors into the problem details the contract documents. */
@RestControllerAdvice
class ProblemHandler {

    @ExceptionHandler(TimesheetNotFoundException.class)
    ProblemDetail notFound(TimesheetNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidTimesheetException.class)
    ProblemDetail invalid(InvalidTimesheetException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
