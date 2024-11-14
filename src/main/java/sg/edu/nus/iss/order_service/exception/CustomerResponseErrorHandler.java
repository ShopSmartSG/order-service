package sg.edu.nus.iss.order_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CustomerResponseErrorHandler extends DefaultResponseErrorHandler {
    private static final List<HttpStatus> IGNORE_STATUS_CODES = Arrays.asList(
            HttpStatus.NOT_FOUND,
            HttpStatus.BAD_REQUEST,
            HttpStatus.UNAUTHORIZED,
            HttpStatus.FORBIDDEN,
            HttpStatus.INTERNAL_SERVER_ERROR,
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.CONFLICT
    );

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        if (!IGNORE_STATUS_CODES.contains(response.getStatusCode())) {
            super.handleError(response);
        }
    }
}
