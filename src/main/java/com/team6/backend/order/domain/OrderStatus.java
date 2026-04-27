package com.team6.backend.order.domain;

public enum OrderStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    COMPLETED;

    public boolean canChangeTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == APPROVED || next == REJECTED || next == CANCELLED || next == COMPLETED;
            case APPROVED -> next == CANCELLED || next == COMPLETED;
            case REJECTED, CANCELLED, COMPLETED -> false;
        };
    }
}
