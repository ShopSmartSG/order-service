package sg.edu.nus.iss.order_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.order_service.db.MongoManager;
import sg.edu.nus.iss.order_service.exception.ResourceNotFoundException;
import sg.edu.nus.iss.order_service.model.Cart;
import sg.edu.nus.iss.order_service.model.Item;
import sg.edu.nus.iss.order_service.model.Response;
import sg.edu.nus.iss.order_service.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CartService extends Constants {
    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private final ObjectMapper mapper = Json.mapper();

    private final MongoManager mongoManager;

    @Value("${"+CART_DB+"}")
    private String cartDb;

    @Value("${"+CART_COLL+"}")
    private String cartColl;

    @Autowired
    public CartService(MongoManager mongoManager){
        this.mongoManager = mongoManager;
    }

    //right now assumption is that always matching merchant id will be provided.
    public Response addItemToCart(UUID customerId, Item item, UUID merchantId) {
        log.info("Adding item {} to cart for customer with ID {}", item, customerId);
        Cart cart = getCartByCustomerId(customerId);
        if(cart==null){
            log.info("Cart for customer with ID {} not found. Creating new cart.", customerId);
            cart = new Cart();
            cart.setCustomerId(customerId);
            cart.setMerchantId(merchantId);
            cart.setCartItems(List.of(item));
            cart.setCreatedAt(System.currentTimeMillis());
            cart.setUpdatedAt(System.currentTimeMillis());
            log.info("Create cart for customer with ID {}, merchant id {} and products : {}", customerId, merchantId, List.of(item));
            boolean result = createCart(cart);
            if(result){
                log.info("Cart created successfully for customer with ID {} for add item", customerId);
                return getSuccessResponse("Cart created successfully to add new item", null);
            }else{
                log.error("Failed to create cart for customer with ID {} for add item", customerId);
                return getFailedResponse("Failed to create new cart to add item");
            }
        } else {
            log.info("Cart for customer with ID {} found. Adding item {} to cart.", customerId, item);
            boolean updated = false;
            for(Item i : cart.getCartItems()){
                if(i.getProductId() == item.getProductId()){
                    i.setQuantity(i.getQuantity() + item.getQuantity());
                    updated = true;
                }
            }
            if(!updated){
                List<Item> currentItems = cart.getCartItems();
                currentItems.add(item);
                cart.setCartItems(currentItems);
            }
            Document query = new Document(CUSTOMER_ID, customerId);
            Document updateDoc = new Document(CART_ITEMS, cart.getCartItems());
            updateDoc.put(UPDATED_AT, System.currentTimeMillis());
            Document update = new Document("$set", updateDoc);
            log.info("Updating cart for customer with ID {}, with update document : {}", customerId, mapper.convertValue(update, JsonNode.class));
            Document result = mongoManager.findOneAndUpdate(query, update, cartDb, cartColl, true, false);
            if(result!=null){
                log.info("Cart updated successfully for customer with ID {}", customerId);
                return getSuccessResponse("Item added successfully in cart", null);
            }else{
                log.error("Failed to update cart for customer with ID {}", customerId);
                return getFailedResponse("Failed to add item in cart");
            }
        }
    }

    //have a similar method to remove same product from all carts of a particular merchant
    public Response removeItemFromCart(UUID customerId, Item item) {
        log.info("Removing item {} to cart for customer with ID {}", item, customerId);
        Cart cart = getCartByCustomerId(customerId);
        if(cart==null){
            log.info("Cart for customer with ID {} not found. So no action required.", customerId);
            return getFailedResponse("Cart not found for provided customer ID");
        } else {
            log.info("Cart for customer with ID {} found. Starting to remove item {} to cart.", customerId, item);
            boolean updated = false;
            List<Item> updatedCart = new ArrayList<>();
            for(Item curr : cart.getCartItems()){
                if(curr.getProductId() == item.getProductId()){
                    int newQuantity = curr.getQuantity() - item.getQuantity();
                    if(newQuantity>0){
                        curr.setQuantity(newQuantity);
                        updatedCart.add(curr);
                    }
                    updated = true;
                } else {
                    updatedCart.add(curr);
                }
            }
            if(!updated){
                log.info("Item {} not found in cart for customer with ID {}. So no action required.", item, customerId);
                return getFailedResponse("Item not found in cart for provided customer ID");
            }
            if(updatedCart.isEmpty()){
                log.info("Cart is empty after removing last item for customer with ID {}. So deleting the cart.", customerId);
                return deleteCartByCustomerId(customerId);
            }
            Document query = new Document(CUSTOMER_ID, customerId);
            Document updateDoc = new Document(CART_ITEMS, updatedCart);
            updateDoc.put(UPDATED_AT, System.currentTimeMillis());
            Document update = new Document("$set", updateDoc);
            log.info("Updating cart for customer with ID {}, with update document : {} to remove it", customerId,
                    mapper.convertValue(update, JsonNode.class));
            Document result = mongoManager.findOneAndUpdate(query, update, cartDb, cartColl, true, false);
            if(result!=null){
                log.info("Cart updated successfully for removing item for customer with ID {}", customerId);
                return getSuccessResponse("Item removed successfully", null);
            }else{
                log.error("Failed to update cart for removing item for customer with ID {}", customerId);
                return getFailedResponse("Failed to remove item");
            }
        }
    }

    public Cart getCartByCustomerId(UUID customerId) {
        log.info("Finding cart for customer with ID {}", customerId);
        Document query = new Document(CUSTOMER_ID, customerId);
        Document cartDoc = mongoManager.findDocument(query, cartDb, cartColl);
        if(cartDoc!=null){
            log.info("Found cart for customer with ID {}", customerId);
            Cart cart = mapper.convertValue(cartDoc, Cart.class);
            log.debug("Number of items in cart is : {}", cart.getCartItems().size());
            return cart;
        }else{
            log.info("Cart for customer with ID {} not found.", customerId);
            return null;
        }
    }

    public Response findCartByCustomerId(UUID customerId) {
        log.info("Finding cart for customer with ID {}", customerId);
        Document query = new Document(CUSTOMER_ID, customerId);
        Document cartDoc = mongoManager.findDocument(query, cartDb, cartColl);
        if(cartDoc!=null){
            log.info("Found cart for customer with ID {}", customerId);
            Cart cart = mapper.convertValue(cartDoc, Cart.class);
            log.debug("Number of items in cart is : {}", cart.getCartItems().size());
            return getSuccessResponse("Cart found", mapper.convertValue(cart, JsonNode.class));
        }else{
            log.info("Cart for customerID {} not found.", customerId);
            return getFailedResponse("Cart not found");
        }
    }

    // Create a new Cart using CartRepository
    public boolean createCart(Cart cart) {
        log.info("Creating cart for customer with ID {}, merchant id {}", cart.getCustomerId(), cart.getMerchantId());
        Cart existingCart = getCartByCustomerId(cart.getCustomerId());
        if(existingCart!=null){
            log.info("Cart for customer with ID {} already exists", cart.getCustomerId());
            return false;
        }
        Document cartDoc = mapper.convertValue(cart, Document.class);
        return mongoManager.insertDocument(cartDoc, cartDb, cartColl);
    }


    //basically works the same as empty cart out in one go
    public Response deleteCartByCustomerId(UUID customerId) {
        log.info("Deleting/Emptying cart for customer with ID {}", customerId);
        Document query = new Document(CUSTOMER_ID, customerId);
        log.info("Deleting/Emptying cart for customer with ID {} with query : {}", customerId, query);
        boolean result = mongoManager.deleteDocument(query, cartDb, cartColl);
        if(result){
            log.info("Cart deleted/emptied successfully for customer with ID {}", customerId);
            return getSuccessResponse("Cart deleted/emptied successfully", null);
        } else {
            log.error("Failed to delete/empty cart for customer with ID {}", customerId);
            return getFailedResponse("Failed to delete/empty cart");
        }
    }

    private Response getFailedResponse(String message) {
        Response response = new Response();
        response.setStatus(FAILURE);
        response.setMessage(message);
        return response;
    }

    private Response getSuccessResponse(String message, JsonNode data) {
        Response response = new Response();
        response.setStatus(SUCCESS);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
}
