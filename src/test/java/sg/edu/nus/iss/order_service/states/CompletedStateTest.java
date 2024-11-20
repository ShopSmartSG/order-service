package sg.edu.nus.iss.order_service.states;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import sg.edu.nus.iss.order_service.db.MongoManager;
import sg.edu.nus.iss.order_service.model.Item;
import sg.edu.nus.iss.order_service.model.OrderContext;
import sg.edu.nus.iss.order_service.model.OrderStatus;
import sg.edu.nus.iss.order_service.model.Response;
import sg.edu.nus.iss.order_service.utils.Constants;
import sg.edu.nus.iss.order_service.utils.Utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class CompletedStateTest extends Constants {
    private final ObjectMapper objectMapper = Json.mapper();
    private UUID globalUUID = UUID.randomUUID();
    @Mock
    private MongoManager mongoManager;
    @Mock
    private Utils utils;

    @Value("${"+ORDER_DB+"}")
    private String orderDb = "orderDb";

    @Value("${"+ORDER_COLLECTION+"}")
    private String orderColl = "orderColl";

    @Value("${"+COMPLETED_ORDERS_COLL+"}")
    private String completedOrderColl = "completedOrderColl";

    @InjectMocks
    private CompletedState completedState;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        completedState = new CompletedState(mongoManager, utils, orderDb, orderColl, completedOrderColl);
    }

    @Test
    public void testUpdateStatus_CompletedStatus_FailedDeliveryUpdate(){
        OrderContext orderContext = new OrderContext();
        orderContext.setOrderId("order1");
        orderContext.setOrderDoc(getOrderDocument_withDelivery());

        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(getOrderDocument_withDelivery());
        when(utils.updateDeliveryStatusforOrder(any(), anyBoolean(), anyString(), any())).thenReturn(false);
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("Failed to update delivery status"));
        Response response = completedState.updateStatus(orderContext);
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Failed to update delivery status", response.getMessage());
    }

    @Test
    public void testUpdateStatus_CompletedStatus_Success(){
        OrderContext orderContext = new OrderContext();
        orderContext.setOrderId("order1");
        orderContext.setOrderDoc(getOrderDocument_withDelivery());

        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(getOrderDocument_withDelivery());

        when(mongoManager.insertDocument(any(), eq(orderDb), eq(completedOrderColl))).thenReturn(true);
        when(mongoManager.deleteDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(true);
        when(utils.updateDeliveryStatusforOrder(any(), anyBoolean(), anyString(), any())).thenReturn(true);
        doNothing().when(utils).updateCustomerRewardPoints(anyString(), anyString(), any());
        doNothing().when(utils).updateMerchantEarnings(anyString(), anyString(), any());
        when(utils.updateDeliveryStatusforOrder(any(), anyBoolean(), anyString(), any())).thenReturn(true);
        when(utils.getSuccessResponse(anyString(), any())).thenReturn(getMockedSuccessResponse("Order status updated to completed", objectMapper.createObjectNode()));
        Response response = completedState.updateStatus(orderContext);
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Order status updated to completed", response.getMessage());
    }

    private Document getOrderDocument_withDelivery() {
        List<Item> orderItems = new ArrayList<>();
        Item item1 = new Item(globalUUID.toString(),10);
        item1.setPrice(BigDecimal.valueOf(10.0));
        orderItems.add(item1);

        Document orderDocument = new Document();
        orderDocument.put(ORDER_ID, globalUUID.toString());
        orderDocument.put(CUSTOMER_ID, globalUUID.toString());
        orderDocument.put(MERCHANT_ID, globalUUID.toString());
        orderDocument.put(DELIVERY_PARTNER_ID, globalUUID.toString());
        orderDocument.put(ORDER_ITEMS, orderItems);
        orderDocument.put(TOTAL_PRICE, BigDecimal.valueOf(100.0));
        orderDocument.put(STATUS, OrderStatus.DELIVERY_PICKED_UP);
        orderDocument.put(CREATED_AT, System.currentTimeMillis());
        orderDocument.put(UPDATED_AT, System.currentTimeMillis());
        orderDocument.put(CREATED_BY, CUSTOMER);
        orderDocument.put(UPDATED_BY, DELIVERY_PARTNER);
        orderDocument.put(USE_DELIVERY, true);
        orderDocument.put("useRewards", true);
        orderDocument.put("rewardsAmountUsed", BigDecimal.valueOf(5.0));
        orderDocument.put("customerRewardsPointsUsed", BigDecimal.valueOf(500.0));
        return orderDocument;
    }
    private Response getMockedFailedResponse(String message) {
        Response response = new Response();
        response.setStatus(FAILURE);
        response.setMessage(message);
        return response;
    }
    private Response getMockedSuccessResponse(String message, JsonNode data) {
        Response response = new Response();
        response.setStatus(SUCCESS);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
}