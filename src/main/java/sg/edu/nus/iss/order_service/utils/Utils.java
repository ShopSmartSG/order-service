package sg.edu.nus.iss.order_service.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.order_service.model.Response;

import java.util.List;

@Service
public class Utils extends Constants{
    private final ObjectMapper objectMapper = new ObjectMapper();
    public Response getFailedResponse(String message) {
        Response response = new Response();
        response.setStatus(FAILURE);
        response.setMessage(message);
        return response;
    }

    /**
     * Converts a List of Documents to a JsonNode.
     *
     * @param documents List of MongoDB documents to convert
     * @return JsonNode representing the documents
     */
    public JsonNode convertToJsonNode(List<Document> documents) {
        try {
            // Convert each Document into a JsonNode
            return objectMapper.valueToTree(documents);
        } catch (Exception e) {
            // Handle any exceptions that might occur during conversion
            e.printStackTrace();
            return null;  // Return null or you can handle it more gracefully
        }
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
