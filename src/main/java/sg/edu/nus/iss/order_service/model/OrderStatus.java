package sg.edu.nus.iss.order_service.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
    INVALID,
    CREATED,
    CANCELLED,
    ACCEPTED,
    READY,
    DELIVERY_ACCEPTED,
    DELIVERY_PICKED_UP,
    COMPLETED;

    @JsonCreator
    public static OrderStatus fromValue(String value) {
        return OrderStatus.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return this.name().toUpperCase();
    }
}
