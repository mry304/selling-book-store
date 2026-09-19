package com.bittercode.util;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import com.bittercode.model.UserRole;

/*
 * Store Util File To Store Commonly used methods & Persistent Cart Handling
 */
public class StoreUtil {

    // Thread-safe in-memory cache for fast cart access and offline resilience
    private static final Map<String, Map<String, Integer>> userCartCache = new ConcurrentHashMap<>();
    private static volatile boolean isTableInitialized = false;

    /**
     * Ensure persistent cart table exists in database
     */
    private static void ensureCartTableExists() {
        if (isTableInitialized) return;
        synchronized (StoreUtil.class) {
            if (isTableInitialized) return;
            try {
                Connection con = DBUtil.getConnection();
                if (con != null) {
                    try (Statement stmt = con.createStatement()) {
                        String sql = "CREATE TABLE IF NOT EXISTS user_cart ("
                                + " id INT AUTO_INCREMENT PRIMARY KEY,"
                                + " username VARCHAR(100) NOT NULL,"
                                + " book_barcode VARCHAR(100) NOT NULL,"
                                + " quantity INT NOT NULL DEFAULT 1,"
                                + " UNIQUE KEY uq_user_book (username, book_barcode)"
                                + ")";
                        stmt.executeUpdate(sql);
                        isTableInitialized = true;
                    }
                }
            } catch (Exception e) {
                // Ignore if already created or connection fallback to in-memory
                System.err.println("Note: user_cart DB init fallback to memory cache: " + e.getMessage());
            }
        }
    }

    /**
     * Check if the User is logged in with the requested role
     */
    public static boolean isLoggedIn(UserRole role, HttpSession session) {
        if (session == null || role == null) return false;
        return session.getAttribute(role.toString()) != null;
    }

    /**
     * Get current logged-in customer username, or null
     */
    public static String getCustomerUsername(HttpSession session) {
        if (session == null) return null;
        Object userObj = session.getAttribute(UserRole.CUSTOMER.toString());
        return userObj != null ? userObj.toString() : null;
    }

    /**
     * Calculate total item count currently in cart
     */
    public static int getCartCount(HttpSession session) {
        if (session == null) return 0;
        String items = (String) session.getAttribute("items");
        if (items == null || items.trim().isEmpty()) return 0;

        int totalCount = 0;
        String[] bCodes = items.split(",");
        for (String bCode : bCodes) {
            String code = bCode.trim();
            if (!code.isEmpty()) {
                Object q = session.getAttribute("qty_" + code);
                if (q instanceof Integer) {
                    totalCount += (Integer) q;
                } else {
                    totalCount += 1;
                }
            }
        }
        return totalCount;
    }

    /**
     * Modify the active tab in the page menu bar and render cart count badge
     */
    public static void setActiveTab(PrintWriter pw, String activeTab) {
        pw.println("<script>");
        pw.println("if (typeof activeTab !== 'undefined' && document.getElementById(activeTab)) { document.getElementById(activeTab).classList.remove('active'); }");
        pw.println("activeTab = '" + activeTab + "';");
        pw.println("var el = document.getElementById('" + activeTab + "'); if (el) { el.classList.add('active'); }");
        pw.println("</script>");
    }

    /**
     * Modify the active tab and update dynamic cart badge count
     */
    public static void setActiveTab(PrintWriter pw, String activeTab, HttpSession session) {
        setActiveTab(pw, activeTab);
        int cartCount = getCartCount(session);
        pw.println("<script>");
        pw.println("(function() {");
        pw.println("  var cartEl = document.getElementById('cart');");
        pw.println("  if (cartEl) {");
        pw.println("    var oldBadge = cartEl.querySelector('.cart-badge-count');");
        pw.println("    if (oldBadge) { oldBadge.remove(); }");
        if (cartCount > 0) {
            pw.println("    var badge = document.createElement('span');");
            pw.println("    badge.className = 'cart-badge-count';");
            pw.println("    badge.textContent = '" + cartCount + "';");
            pw.println("    cartEl.appendChild(badge);");
        }
        pw.println("  }");
        pw.println("})();");
        pw.println("</script>");
    }

    /**
     * Save cart items for a user to database and cache
     */
    public static void saveUserCart(HttpSession session, String username) {
        if (username == null || username.trim().isEmpty() || session == null) return;
        username = username.trim();

        // Extract items and quantities from session
        String items = (String) session.getAttribute("items");
        Map<String, Integer> cartMap = new LinkedHashMap<>();
        if (items != null && !items.trim().isEmpty()) {
            String[] bCodes = items.split(",");
            for (String bCode : bCodes) {
                String code = bCode.trim();
                if (!code.isEmpty()) {
                    Object q = session.getAttribute("qty_" + code);
                    int qty = (q instanceof Integer) ? (Integer) q : 1;
                    if (qty > 0) {
                        cartMap.put(code, qty);
                    }
                }
            }
        }

        // 1. Update in-memory cache
        userCartCache.put(username, cartMap);

        // 2. Persist to DB table
        ensureCartTableExists();
        try {
            Connection con = DBUtil.getConnection();
            if (con != null) {
                // Clear old rows for this user
                try (PreparedStatement psDel = con.prepareStatement("DELETE FROM user_cart WHERE username = ?")) {
                    psDel.setString(1, username);
                    psDel.executeUpdate();
                }

                // Insert new rows
                if (!cartMap.isEmpty()) {
                    try (PreparedStatement psIns = con.prepareStatement("INSERT INTO user_cart (username, book_barcode, quantity) VALUES (?, ?, ?)")) {
                        for (Map.Entry<String, Integer> entry : cartMap.entrySet()) {
                            psIns.setString(1, username);
                            psIns.setString(2, entry.getKey());
                            psIns.setInt(3, entry.getValue());
                            psIns.addBatch();
                        }
                        psIns.executeBatch();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error persisting user_cart to DB for " + username + ": " + e.getMessage());
        }
    }

    /**
     * Restore cart items for a user from database or cache into session
     */
    public static void restoreUserCart(HttpSession session, String username) {
        if (username == null || username.trim().isEmpty() || session == null) return;
        username = username.trim();

        Map<String, Integer> cartMap = new LinkedHashMap<>();
        ensureCartTableExists();

        // 1. Try loading from database
        try {
            Connection con = DBUtil.getConnection();
            if (con != null) {
                try (PreparedStatement ps = con.prepareStatement("SELECT book_barcode, quantity FROM user_cart WHERE username = ? OR username IN (SELECT mailid FROM users WHERE username = ?)")) {
                    ps.setString(1, username);
                    ps.setString(2, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String barcode = rs.getString("book_barcode");
                            int qty = rs.getInt("quantity");
                            if (barcode != null && !barcode.trim().isEmpty() && qty > 0) {
                                cartMap.put(barcode.trim(), qty);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("DB load failed for user_cart, using memory cache fallback: " + e.getMessage());
        }

        // 2. If DB returned empty, fallback to memory cache
        if (cartMap.isEmpty() && userCartCache.containsKey(username)) {
            cartMap.putAll(userCartCache.get(username));
        } else if (!cartMap.isEmpty()) {
            userCartCache.put(username, cartMap);
        }

        // 3. Populate session attributes
        if (!cartMap.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> entry : cartMap.entrySet()) {
                if (sb.length() > 0) sb.append(",");
                sb.append(entry.getKey());
                session.setAttribute("qty_" + entry.getKey(), entry.getValue());
            }
            session.setAttribute("items", sb.toString());
        }
    }

    /**
     * Clear cart items for a user upon successful checkout / order placement
     */
    public static void clearUserCart(HttpSession session, String username) {
        if (username != null && !username.trim().isEmpty()) {
            username = username.trim();
            userCartCache.remove(username);
            ensureCartTableExists();
            try {
                Connection con = DBUtil.getConnection();
                if (con != null) {
                    try (PreparedStatement ps = con.prepareStatement("DELETE FROM user_cart WHERE username = ? OR username IN (SELECT mailid FROM users WHERE username = ?)")) {
                        ps.setString(1, username);
                        ps.setString(2, username);
                        ps.executeUpdate();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error clearing DB user_cart for " + username + ": " + e.getMessage());
            }
        }

        if (session != null) {
            String items = (String) session.getAttribute("items");
            if (items != null && !items.trim().isEmpty()) {
                String[] bCodes = items.split(",");
                for (String code : bCodes) {
                    session.removeAttribute("qty_" + code.trim());
                }
            }
            session.removeAttribute("items");
            session.removeAttribute("cartItems");
            session.removeAttribute("amountToPay");
            session.removeAttribute("selectedBookId");
        }
    }

    /**
     * Add/Remove/Update Item in the cart using the session, with automatic persistence
     */
    public static void updateCartItems(HttpServletRequest req) {
        HttpSession session = req.getSession();
        String username = getCustomerUsername(session);

        // If session cart is empty but user is logged in, attempt restoration
        if (session.getAttribute("items") == null && username != null) {
            restoreUserCart(session, username);
        }

        String selectedBookId = req.getParameter("selectedBookId");
        if (selectedBookId == null || selectedBookId.trim().isEmpty()) {
            return;
        }
        selectedBookId = selectedBookId.trim();

        boolean isAdd = req.getParameter("addToCart") != null;
        boolean isRemove = req.getParameter("removeFromCart") != null;

        // CRITICAL FIX: Only modify cart if addToCart OR removeFromCart is explicitly requested!
        // This prevents accidental removal when reloading pages or navigating.
        if (!isAdd && !isRemove) {
            return;
        }

        // Parse existing items as a LinkedHashSet to prevent barcode substring replacement bugs
        Set<String> itemSet = new LinkedHashSet<>();
        String currentItems = (String) session.getAttribute("items");
        if (currentItems != null && !currentItems.trim().isEmpty()) {
            for (String id : currentItems.split(",")) {
                String trimmed = id.trim();
                if (!trimmed.isEmpty()) {
                    itemSet.add(trimmed);
                }
            }
        }

        if (isAdd) {
            itemSet.add(selectedBookId);
            session.setAttribute("items", String.join(",", itemSet));

            int itemQty = 0;
            Object qObj = session.getAttribute("qty_" + selectedBookId);
            if (qObj instanceof Integer) {
                itemQty = (Integer) qObj;
            }
            itemQty += 1;
            session.setAttribute("qty_" + selectedBookId, itemQty);

        } else if (isRemove) {
            int itemQty = 0;
            Object qObj = session.getAttribute("qty_" + selectedBookId);
            if (qObj instanceof Integer) {
                itemQty = (Integer) qObj;
            }

            if (itemQty > 1) {
                itemQty -= 1;
                session.setAttribute("qty_" + selectedBookId, itemQty);
            } else {
                session.removeAttribute("qty_" + selectedBookId);
                itemSet.remove(selectedBookId);
                if (itemSet.isEmpty()) {
                    session.removeAttribute("items");
                } else {
                    session.setAttribute("items", String.join(",", itemSet));
                }
            }
        }

        // Immediately sync to persistent storage if user is logged in
        if (username != null) {
            saveUserCart(session, username);
        }
    }

    /**
     * Format currency in VNĐ standard
     */
    public static String formatPrice(double price) {
        if (price % 1 == 0) {
            return String.format(java.util.Locale.GERMANY, "%,.0f đ", price);
        } else {
            return String.format(java.util.Locale.GERMANY, "%,.2f đ", price);
        }
    }
}
