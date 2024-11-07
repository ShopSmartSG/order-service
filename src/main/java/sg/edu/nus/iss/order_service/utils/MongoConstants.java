package sg.edu.nus.iss.order_service.utils;

public interface MongoConstants {
    String MONGO_SRV = "mongo.srv";
    String ORDER_DB = "mongo.order.db";
    String ORDER_DB_USERNAME = "mongo.order.username";
    String ORDER_DB_PASSWORD = "mongo.order.password";
    String ORDER_COLLECTION = "mongo.order.collection";
    String CART_DB = "mongo.cart.db";
    String CART_DB_USERNAME = "mongo.cart.username";
    String CART_DB_PASSWORD = "mongo.cart.password";
    String CART_COLL = "mongo.cart.collection";
    String COMPLETED_ORDERS_COLL = "mongo.completedOrders.collection";
    String CANCELLED_ORDERS_COLL = "mongo.cancelledOrders.collection";
    String OPERATOR_PUSH = "$push";
    String OPERATOR_SET = "$set";
    String OPERATOR_INC = "$inc";
    String OPERATOR_PULL = "$pull";
    String OPERATOR_POSITIONAL = "$";
}
