package com.bittercode.model;

public enum OrderCancelledBy {
    CUSTOMER("Người mua"),
    SELLER("Người bán"),
    SYSTEM("Hệ thống");

    private final String displayName;

    OrderCancelledBy(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static OrderCancelledBy fromString(String val) {
        if (val == null) return null;
        try {
            return OrderCancelledBy.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
