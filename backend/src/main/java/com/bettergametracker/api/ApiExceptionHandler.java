package com.bettergametracker.api;

import java.util.LinkedHashMap;
import java.util.Map;

import com.bettergametracker.game.GameNotFoundException;
import com.bettergametracker.play.PlayEntryNotFoundException;
import com.bettergametracker.play.PlayTimeEntryNotFoundException;
import com.bettergametracker.review.ReviewNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({GameNotFoundException.class, PlayEntryNotFoundException.class, ReviewNotFoundException.class,
            PlayTimeEntryNotFoundException.class})
    ProblemDetail handleNotFound(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableMessage() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request body is not valid JSON");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Invalid value for " + exception.getName());
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    ProblemDetail handleAuthentication() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Sign in required");
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ProblemDetail handleAccessDenied() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        if (exception instanceof ErrorResponse error) {
            return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders()).body(error.getBody());
        }
        LOGGER.error("Unexpected API error", exception);
        return ResponseEntity.internalServerError().body(
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"));
    }
}
