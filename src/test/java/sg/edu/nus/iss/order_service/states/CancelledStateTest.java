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

class CancelledStateTest extends Constants {
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

    @Value("${"+CANCELLED_ORDERS_COLL+"}")
    private String cancelledOrderColl = "cancelledOrderColl";

    @InjectMocks
    private CancelledState cancelledState;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        cancelledState = new CancelledState(mongoManager, utils, orderDb, orderColl, cancelledOrderColl);
    }

    @Test
    public void updateStatus_Success() {
        OrderContext orderContext = new OrderContext();
        orderContext.setOrderId("order1");
        orderContext.setOrderDoc(getOrderDocument_withoutDelivery());

        Response successResp = new Response();
        successResp.setStatus(SUCCESS);

        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(getOrderDocument_withoutDelivery());
        when(mongoManager.insertDocument(any(), eq(orderDb), eq(cancelledOrderColl))).thenReturn(true);
        when(mongoManager.deleteDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(true);
        doNothing().when(utils).updateCustomerRewardPoints(anyString(), anyString(), any());
        when(utils.getSuccessResponse(anyString(), any())).thenReturn(getMockedSuccessResponse("Order status updated to cancelled", objectMapper.createObjectNode()));
        Response response = cancelledState.updateStatus(orderContext);
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Order status updated to cancelled", response.getMessage());
    }

    private Document getOrderDocument_withoutDelivery() {
        List<Item> orderItems = new ArrayList<>();
        Item item1 = new Item(globalUUID.toString(),10);
        item1.setPrice(BigDecimal.valueOf(10.0));
        orderItems.add(item1);

        Document orderDocument = new Document();
        orderDocument.put(ORDER_ID, globalUUID.toString());
        orderDocument.put(CUSTOMER_ID, globalUUID.toString());
        orderDocument.put(MERCHANT_ID, globalUUID.toString());
        orderDocument.put(ORDER_ITEMS, orderItems);
        orderDocument.put(STATUS, OrderStatus.READY);
        orderDocument.put(TOTAL_PRICE, BigDecimal.valueOf(100.0));
        orderDocument.put(CREATED_AT, System.currentTimeMillis());
        orderDocument.put(UPDATED_AT, System.currentTimeMillis());
        orderDocument.put(CREATED_BY, CUSTOMER);
        orderDocument.put(UPDATED_BY, MERCHANT);
        orderDocument.put(USE_DELIVERY, false);
        orderDocument.put("useRewards", true);
        orderDocument.put("rewardsAmountUsed", BigDecimal.valueOf(5.0));
        orderDocument.put("customerRewardsPointsUsed", BigDecimal.valueOf(500.0));
        return orderDocument;
    }
    private Response getMockedSuccessResponse(String message, JsonNode data) {
        Response response = new Response();
        response.setStatus(SUCCESS);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
}