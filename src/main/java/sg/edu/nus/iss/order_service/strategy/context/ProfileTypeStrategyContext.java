package sg.edu.nus.iss.order_service.strategy.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import sg.edu.nus.iss.order_service.strategy.profile.ProfileTypeStrategy;

import java.util.Map;

@Component
public class ProfileTypeStrategyContext {

    private static final Logger logger = LoggerFactory.getLogger(ProfileTypeStrategyContext.class);

    private final Map<String, ProfileTypeStrategy> strategies;

    @Autowired
    public ProfileTypeStrategyContext(
            @Qualifier("customer") ProfileTypeStrategy customerProfileStrategy,
            @Qualifier("merchant") ProfileTypeStrategy merchantProfileStrategy,
            @Qualifier("deliveryPartner") ProfileTypeStrategy deliveryPartnerProfileStrategy) {

        // Log the initialization of the strategies
        logger.info("Initializing ProfileTypeStrategyContext with customer, merchant, and deliveryPartner strategies.");

        this.strategies = Map.of(
                "customer", customerProfileStrategy,
                "merchant", merchantProfileStrategy,
                "deliveryPartner", deliveryPartnerProfileStrategy
        );

        // Ensure strategies are properly initialized
        if (this.strategies.isEmpty()) {
            logger.error("ProfileType strategies map is empty!");
        } else {
            logger.info("ProfileTypeStrategyContext initialized successfully with {} strategies.", strategies.size());
        }
    }

    // Getter method to fetch strategy based on profile type
    public ProfileTypeStrategy getStrategy(String profileType) {
        logger.debug("Fetching strategy for profileType: {}", profileType);

        if (profileType == null || profileType.isEmpty()) {
            throw new IllegalArgumentException("profileType cannot be null or empty");
        }

        ProfileTypeStrategy strategy = strategies.get(profileType);

        if (strategy != null) {
            logger.info("Strategy found for profileType: {}", profileType);
        } else {
            logger.warn("No strategy found for profileType: {}", profileType);
        }

        return strategy;
    }

    public ProfileTypeStrategy getCustomerProfileStrategy() {
        logger.debug("Fetching customer profile strategy.");
        ProfileTypeStrategy strategy = strategies.get("customer");
        logProfileStrategy("customer", strategy);
        return strategy;
    }

    public ProfileTypeStrategy getMerchantProfileStrategy() {
        logger.debug("Fetching merchant profile strategy.");
        ProfileTypeStrategy strategy = strategies.get("merchant");
        logProfileStrategy("merchant", strategy);
        return strategy;
    }

    public ProfileTypeStrategy getDeliveryPartnerProfileStrategy() {
        logger.debug("Fetching delivery partner profile strategy.");
        ProfileTypeStrategy strategy = strategies.get("deliveryPartner");
        logProfileStrategy("deliveryPartner", strategy);
        return strategy;
    }

    private void logProfileStrategy(String profileType, ProfileTypeStrategy strategy) {
        if (strategy != null) {
            logger.info("{} profile strategy fetched successfully.", profileType);
        } else {
            logger.warn("No {} profile strategy found.", profileType);
        }
    }
}
