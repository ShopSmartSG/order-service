package sg.edu.nus.iss.order_service.db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.bson.UuidRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sg.edu.nus.iss.order_service.config.MongoConfig;
import sg.edu.nus.iss.order_service.utils.Constants;
import sg.edu.nus.iss.order_service.utils.MongoConstants;

import java.util.HashMap;
import java.util.Map;

@Component
public class MongoSingleton extends Constants {
    private static final Logger log = LoggerFactory.getLogger(MongoSingleton.class);
    private static final Map<String, MongoClient> mongoClients = new HashMap<>();

    @Value("${"+MONGO_SRV+"}")
    private String mongoSrvVal;

    @Value("${"+ORDER_DB+"}")
    private String orderDbVal;

    @Value("${"+ORDER_DB_USERNAME+"}")
    private String orderDbUsernameVal;

    @Value("${"+ORDER_DB_PASSWORD+"}")
    private String orderDbPasswordVal;

    @Value("${"+CART_DB+"}")
    private String cartDbVal;

    @Value("${"+CART_DB_USERNAME+"}")
    private String cartDbUsernameVal;

    @Value("${"+CART_DB_PASSWORD+"}")
    private String cartDbPasswordVal;

    private static String mongoSrv;
    private static String orderDb;
    private static String orderDbUsername;
    private static String orderDbPassword;
    private static String cartDb;
    private static String cartDbUsername;
    private static String cartDbPassword;

    @PostConstruct
    private void init() {
        mongoSrv = this.mongoSrvVal;
        orderDb = this.orderDbVal;
        orderDbUsername = this.orderDbUsernameVal;
        orderDbPassword = this.orderDbPasswordVal;
        cartDb = this.cartDbVal;
        cartDbUsername = this.cartDbUsernameVal;
        cartDbPassword = this.cartDbPasswordVal;
    }

    private MongoSingleton() {
        // private constructor to prevent instantiation
    }

    public static MongoClient getMongoClient(String dbName) {
        log.debug("Entering getMongoClient method.");
        // First check (outside synchronized block) to avoid unnecessary locking
        if (!mongoClients.containsKey(dbName)) {
            synchronized (MongoSingleton.class) {
                // Second check (inside synchronized block) to ensure only one thread creates the MongoClient
                if (!mongoClients.containsKey(dbName)) {
                    // Initialize MongoClient for the specified database
                    mongoClients.put(dbName, createMongoClient(dbName));
                }
            }
        }
        return mongoClients.get(dbName);
    }

    /*
    * Uses Builder Design Pattern
    */
    private static MongoClient createMongoClient(String dbName) {
        log.debug("Entering createMongoClient method.");
        ConnectionString connectionString = new ConnectionString(mongoSrv);
        if (orderDb.equals(dbName)) {
            log.info("Setting mongo client for Order DB.");
            // Connection for order DB
//            MongoCredential credential = MongoCredential.createCredential(
//                    orderDbUsername, orderDb, orderDbPassword.toCharArray());

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .uuidRepresentation(UuidRepresentation.STANDARD)
//                    .credential(credential)
                    .build();

            return MongoClients.create(settings);
        } else if (cartDb.equals(dbName)) {
            log.info("Setting mongo client for Cart DB.");
            // Connection for cart DB
//            MongoCredential credential = MongoCredential.createCredential(
//                    cartDbUsername, cartDb, cartDbPassword.toCharArray());

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .uuidRepresentation(UuidRepresentation.STANDARD)
//                    .credential(credential)
                    .build();

            return MongoClients.create(settings);
        } else {
            log.error("Unable to identify mongo Db to setup mongo client.");
            throw new IllegalArgumentException("Invalid database name: " + dbName);
        }
    }

    @PreDestroy
    public void closeAllClients() {
        log.info("Starting closing of all mongoClients.");
        for (MongoClient client : mongoClients.values()) {
            client.close();
        }
        mongoClients.clear();
        log.info("All MongoClient connections closed.");
    }
}
