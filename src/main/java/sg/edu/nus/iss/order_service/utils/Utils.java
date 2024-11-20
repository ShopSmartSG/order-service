package sg.edu.nus.iss.order_service.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import sg.edu.nus.iss.order_service.model.DeliveryStatusReqModel;
import sg.edu.nus.iss.order_service.model.OrderStatus;
import sg.edu.nus.iss.order_service.model.Response;

import java.math.BigDecimal;
import java.util.HashMap;

import java.util.List;

@Service
public class Utils extends Constants{
    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    private final ObjectMapper mapper = Json.mapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${profile.service.url}")
    private String profileServiceUrl = "http://profile-service:80/";

    @Value("${delivery.service.url}")
    private String deliveryServiceUrl = "http://delivery-service:92/";

    private final WSUtils wsUtils;

    @Autowired
    public Utils(WSUtils wsUtils) {
        this.wsUtils = wsUtils;
    }

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

    public JsonNode getRewardPointsOffsetForCustomer(String customerId){
        //from profile service needs to hit path : customers/{customer-id}/rewards  a GET call
        String url = profileServiceUrl.concat("customers").concat(SLASH).concat(customerId).concat(SLASH).concat("rewards");
        log.debug("URL to get reward points and offset amount for customerId: {} is {}", customerId, url);
        try{
            Response response = wsUtils.makeWSCallObject(url, null, new HashMap<>(), HttpMethod.GET, 1000, 30000);
            if(FAILURE.equalsIgnoreCase(response.getStatus())){
                log.error("Failed to get reward points and offset amount for customerId: {}", customerId);
            }
            JsonNode data = response.getData();
            return data;
        }catch(Exception ex){
            log.error("Exception occurred while getting reward points and offset amount for customerId: {}", customerId);
            return null;
        }
    }

    public void updateCustomerRewardPoints(String orderId, String customerId, BigDecimal orderPrice){
        log.info("Updating customer reward points for orderId: {} and customerId : {}", orderId, customerId);
        //url is /customers/{customer-id}/rewards/{order-price}
        String url = profileServiceUrl.concat("customers").concat(SLASH).concat(customerId)
                .concat(SLASH).concat("rewards").concat(SLASH).concat(orderPrice.toString());
        log.debug("URL to update customer reward points is : {}", url);
        try{
            Response response = wsUtils.makeWSCallString(url, null, new HashMap<>(), HttpMethod.PUT, 1000, 30000);
            if(FAILURE.equalsIgnoreCase(response.getStatus())){
                log.error("Failed to update customer reward points for orderId: {}", orderId);
            }else{
                log.info("Customer {} reward points updated successfully for orderId: {} with order amount {}",
                        customerId, orderId, orderPrice);
            }
        }catch(Exception ex){
            log.error("Exception occurred while updating customer reward points for orderId: {}", orderId);
        }
    }

    public void updateMerchantEarnings(String orderId, String merchantId, BigDecimal orderPrice){
        log.info("Updating merchant earnings for orderId : {} and merchantId {}", orderId, merchantId);
        //url is /merchants/{merchant-id}/rewards/{order-price}
        String url = profileServiceUrl.concat("merchants").concat(SLASH).concat(merchantId)
                .concat(SLASH).concat("rewards").concat(SLASH).concat(orderPrice.toString());
        log.debug("URL to update merchant earnings is : {}", url);
        try{
            Response response = wsUtils.makeWSCallString(url, null, new HashMap<>(), HttpMethod.PUT, 1000, 30000);
            if(FAILURE.equalsIgnoreCase(response.getStatus())){
                log.error("Failed to update merchant earnings for orderId: {}", orderId);
            }else{
                log.info("Merchant {} earnings updated successfully for orderId: {} with order amount {}",
                        merchantId, orderId, orderPrice);
            }
        }catch(Exception ex){
            log.error("Exception occurred while updating merchant earnings for orderId: {}", orderId);
        }
    }

    public boolean updateDeliveryStatusforOrder(Document orderDoc, boolean isCreateNewDelivery, String deliveryPartnerId, OrderStatus status){
        log.info("Updating delivery status for orderId : {} and deliveryPartnerId {} with status {}", orderDoc.get(ORDER_ID), deliveryPartnerId, status);
        String orderId = orderDoc.get(ORDER_ID, String.class);
        String customerId = orderDoc.get(CUSTOMER_ID, String.class);
        DeliveryStatusReqModel reqModel = new DeliveryStatusReqModel();
        reqModel.setOrderId(orderId);
        reqModel.setCustomerId(customerId);
        reqModel.setDeliveryPersonId(deliveryPartnerId);
        reqModel.setStatus(status);
        reqModel.setMessage("Delivery status updated for orderId: ".concat(orderId).concat(" to status : ").concat(status.toString()));
        String url = deliveryServiceUrl.concat("deliveries");
        HttpMethod method;
        JsonNode payload;
        if(isCreateNewDelivery){
            //to create new delivery : deliveries/?orderId={orderId}&deliveryPersonId={deliveryPartnerId}&customerId={customerId}
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url.concat(SLASH));
            uriBuilder.queryParam(ORDER_ID, orderId);
            uriBuilder.queryParam("deliveryPersonId", deliveryPartnerId);
            uriBuilder.queryParam(CUSTOMER_ID, customerId);
            url=uriBuilder.toUriString();
            method = HttpMethod.POST;
            payload = null;
        }else{
            //to update delivery status : deliveries/status
            url = url.concat(SLASH).concat("status");
            method = HttpMethod.PUT;
            payload = mapper.convertValue(reqModel, JsonNode.class);
        }
        log.debug("URL to update delivery status is : {} with payload {}", url, payload);
        try{
            Response response = wsUtils.makeWSCallObject(url, payload, new HashMap<>(), method, 1000, 30000);
            if(FAILURE.equalsIgnoreCase(response.getStatus())){
                log.error("Failed to update delivery status for orderId: {} for status {}", orderDoc.get(ORDER_ID), status);
                return false;
            }else{
                log.info("Delivery status updated successfully for orderId: {} with deliveryPartnerId {} for status {}",
                        orderDoc.get(ORDER_ID), orderDoc.get(DELIVERY_PARTNER), status);
                return true;
            }
        }catch(Exception ex){
            log.error("Exception occurred while updating delivery status for orderId: {} for status {}", orderDoc.get(ORDER_ID), status);
            return false;
        }
    }
}
