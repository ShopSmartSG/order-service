package sg.edu.nus.iss.order_service.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.order_service.model.Response;

@Service
public class Utils extends Constants{
    public Response getFailedResponse(String message) {
        Response response = new Response();
        response.setStatus(FAILURE);
        response.setMessage(message);
        return response;
    }

    public Response getSuccessResponse(String message, JsonNode data) {
        Response response = new Response();
        response.setStatus(SUCCESS);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    public String getProfileIdentifierFieldBasedOnRole(String profileType) {
        if (CUSTOMER.equalsIgnoreCase(profileType)) {
            return CUSTOMER_ID;
        } else if (MERCHANT.equalsIgnoreCase(profileType)) {
            return MERCHANT_ID;
        } else if (DELIVERY_PARTNER.equalsIgnoreCase(profileType)) {
            return DELIVERY_PARTNER_ID;
        } else{
            return "";
        }
    }
}
