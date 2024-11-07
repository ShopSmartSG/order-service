package sg.edu.nus.iss.order_service.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import sg.edu.nus.iss.order_service.utils.Constants;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends Constants {
    private final ObjectMapper mapper = Json.mapper();

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<JsonNode> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ObjectNode errResp = mapper.createObjectNode();
        errResp.put(MESSAGE, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errResp);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<JsonNode> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapper.convertValue(errors, JsonNode.class));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<JsonNode> handleIllegalArgumentException(IllegalArgumentException ex) {
        ObjectNode errResp = mapper.createObjectNode();
        errResp.put(MESSAGE, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errResp);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex) {
        ObjectNode errResp = mapper.createObjectNode();
        errResp.put(MESSAGE,"Internal Server Error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errResp);
    }
}
