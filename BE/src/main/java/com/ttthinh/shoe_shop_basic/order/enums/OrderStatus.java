package com.ttthinh.shoe_shop_basic.order.enums;

public enum OrderStatus {

    PENDING,
    CONFIRMED,
    PACKING,
    READY_TO_SHIP,
    SHIPPING,
    DELIVERED,
    CANCELLED,
    FAILED,
    RETURNED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {

            case PENDING ->
                    next == CONFIRMED || next == CANCELLED;

            case CONFIRMED ->
                    next == PACKING || next == CANCELLED;

            case PACKING ->
                    next == READY_TO_SHIP;

            case READY_TO_SHIP ->
                    false;

            case SHIPPING ->
                    false;

            case FAILED ->
                    false;

            case DELIVERED, CANCELLED, RETURNED ->
                    false;
        };
    }

    public boolean userCanCancel() {
        return this == PENDING || this == CONFIRMED;
    }

    public boolean adminCanCancel() {
        return this == PENDING || this == CONFIRMED;
    }

    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED || this == FAILED || this == RETURNED;
    }
}
