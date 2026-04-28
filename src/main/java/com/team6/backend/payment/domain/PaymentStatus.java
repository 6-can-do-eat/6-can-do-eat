package com.team6.backend.payment.domain;

public enum PaymentStatus {
    PENDING,
    COMPLETED,
    CANCELLED;

    public boolean canChangeTo(PaymentStatus next) {
        return switch (this) {
            case PENDING -> next == COMPLETED || next == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
