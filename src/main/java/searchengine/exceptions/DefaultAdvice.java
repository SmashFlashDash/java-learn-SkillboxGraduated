package searchengine.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import searchengine.dto.error.ErrorResponse;

@ControllerAdvice
public class DefaultAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(OkError.class)
    ResponseEntity<ErrorResponse> handle(RuntimeException ex, WebRequest request) {
        return ResponseEntity.ok(new ErrorResponse(false, ex.getMessage()));
    }
}
