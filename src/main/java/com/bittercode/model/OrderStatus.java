package com.bittercode.model;

public enum OrderStatus {
    PENDING("Chờ xác nhận", "badge-order-pending"),
    CONFIRMED("Đã xác nhận", "badge-order-confirmed"),
    SHIPPING("Đang giao hàng", "badge-order-shipping"),
    COMPLETED("Hoàn thành", "badge-order-completed"),
    CANCELLED("Đã hủy", "badge-order-cancelled");

    private final String displayName;
    private final String badgeClass;

    OrderStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public static OrderStatus fromString(String status) {
        if (status == null) return PENDING;
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Handle legacy statuses
            if ("PAID".equalsIgnoreCase(status) || "PROCESSING".equalsIgnoreCase(status)) {
                return CONFIRMED;
            } else if ("SHIPPED".equalsIgnoreCase(status)) {
                return SHIPPING;
            }
            return PENDING;
        }
    }
}
