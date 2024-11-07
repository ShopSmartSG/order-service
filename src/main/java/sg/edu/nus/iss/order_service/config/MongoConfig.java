package sg.edu.nus.iss.order_service.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import sg.edu.nus.iss.order_service.utils.Constants;

//@Configuration
public class MongoConfig extends Constants {
    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    @Value("${"+MONGO_SRV+"}")
    private String mongoSrv;

    @Value("${"+ORDER_DB+"}")
    private String orderDb;

    @Value("${"+ORDER_DB_USERNAME+"}")
    private String orderDbUsername;

    @Value("${"+ORDER_DB_PASSWORD+"}")
    private String orderDbPassword;

    @Value("${"+CART_DB+"}")
    private String cartDb;

    @Value("${"+CART_DB_USERNAME+"}")
    private String cartDbUsername;

    @Value("${"+CART_DB_PASSWORD+"}")
    private String cartDbPassword;

//    @Bean
//    public MongoClient mongoClient() {
//        log.info("Creating MongoClient with mongoSrv: {}", mongoSrv);
//
//        ConnectionString connectionString = new ConnectionString(mongoSrv);
//        MongoClientSettings settings = MongoClientSettings.builder()
//                .applyConnectionString(connectionString)
//                .build();
//
//        return MongoClients.create(settings);
//    }

//    @Bean
//    public MongoDatabaseFactory mongoOrderDbFactory() {
//        MongoCredential credential = MongoCredential.createCredential(
//                orderDbUsername, orderDb, orderDbPassword.toCharArray());
//
//        MongoClientSettings settings = MongoClientSettings.builder()
//                .applyConnectionString(new ConnectionString(mongoSrv + "/" + orderDb))
//                .credential(credential)
//                .build();
//
//        return new SimpleMongoClientDatabaseFactory(MongoClients.create(settings), orderDb);
//    }
//
//    @Bean
//    public MongoTemplate mongoOrderDbTemplate(MongoDatabaseFactory mongoOrderDbFactory) {
//        return new MongoTemplate(mongoOrderDbFactory);
//    }
//
//    //public MongoDatabaseFactory mongoCartDbFactory(MongoClient mongoClient)
//    @Bean
//    public MongoDatabaseFactory mongoCartDbFactory() {
//        MongoCredential credential = MongoCredential.createCredential(
//                cartDbUsername, cartDb, cartDbPassword.toCharArray());
//
//        MongoClientSettings settings = MongoClientSettings.builder()
//                .applyConnectionString(new ConnectionString(mongoSrv + "/" + cartDb))
//                .credential(credential)
//                .build();
//
//        return new SimpleMongoClientDatabaseFactory(MongoClients.create(settings), cartDb);
//    }
//
//    @Bean
//    public MongoTemplate mongoCartDbTemplate(MongoDatabaseFactory mongoCartDbFactory) {
//        return new MongoTemplate(mongoCartDbFactory);
//    }
}
