package servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.bittercode.model.UserRole;
import com.bittercode.util.DBUtil;
import com.bittercode.util.StoreUtil;

public class AdminStatisticsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        HttpSession session = req.getSession();
        if (!StoreUtil.isLoggedIn(UserRole.SELLER, session)) {
            res.sendRedirect("SellerLogin.html");
            return;
        }

        double totalRevenue = 0.0;
        int totalOrders = 0;
        int totalBooksSold = 0;
        int totalCustomers = 0;
        int totalStock = 0;
        int totalTitles = 0;

        List<String> revDates = new ArrayList<>();
        List<Double> revAmounts = new ArrayList<>();
        List<Integer> revOrders = new ArrayList<>();

        List<String> topNames = new ArrayList<>();
        List<Integer> topSales = new ArrayList<>();
        List<Double> topRevenues = new ArrayList<>();

        List<String> statusLabels = new ArrayList<>();
        List<Integer> statusCounts = new ArrayList<>();

        List<Map<String, Object>> lowStockBooks = new ArrayList<>();
        List<Map<String, Object>> recentOrders = new ArrayList<>();

        try {
            Connection con = DBUtil.getConnection();

            // 1. Total Revenue
            try (PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status != 'CANCELLED'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) totalRevenue = rs.getDouble(1);
                }
            }

            // 2. Total Orders
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM orders")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) totalOrders = rs.getInt(1);
                }
            }

            // 3. Total Books Sold
            try (PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(quantity), 0) FROM order_details")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) totalBooksSold = rs.getInt(1);
                }
            }

            // 4. Total Customers
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM users WHERE usertype = 2")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) totalCustomers = rs.getInt(1);
                }
            }

            // 5. Total Stock & Titles
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*), COALESCE(SUM(quantity), 0) FROM books")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalTitles = rs.getInt(1);
                        totalStock = rs.getInt(2);
                    }
                }
            }

            // 6. Revenue Timeline (Daily breakdown)
            String revQuery = "SELECT DATE_FORMAT(order_date, '%d/%m') AS day_label, "
                    + "COALESCE(SUM(total_amount), 0) AS daily_rev, "
                    + "COUNT(*) AS daily_cnt "
                    + "FROM orders "
                    + "WHERE status != 'CANCELLED' "
                    + "GROUP BY DATE(order_date), DATE_FORMAT(order_date, '%d/%m') "
                    + "ORDER BY DATE(order_date) ASC LIMIT 10";

            try (PreparedStatement ps = con.prepareStatement(revQuery);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    revDates.add(rs.getString("day_label"));
                    revAmounts.add(rs.getDouble("daily_rev"));
                    revOrders.add(rs.getInt("daily_cnt"));
                }
            }

            // Fallback if no order timeline exists yet
            if (revDates.isEmpty()) {
                java.time.LocalDate today = java.time.LocalDate.now();
                for (int i = 6; i >= 0; i--) {
                    java.time.LocalDate d = today.minusDays(i);
                    revDates.add(d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")));
                    revAmounts.add(0.0);
                    revOrders.add(0);
                }
            }

            // 7. Top Selling Books (Top 5)
            String topBooksQuery = "SELECT b.barcode, b.name, b.author, b.price, "
                    + "COALESCE(SUM(od.quantity), 0) AS sold_qty, "
                    + "COALESCE(SUM(od.amount), 0) AS total_amount "
                    + "FROM books b "
                    + "JOIN order_details od ON b.barcode = od.book_barcode "
                    + "GROUP BY b.barcode, b.name, b.author, b.price "
                    + "ORDER BY sold_qty DESC, total_amount DESC LIMIT 5";

            try (PreparedStatement ps = con.prepareStatement(topBooksQuery);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String bName = rs.getString("name");
                    if (bName != null && bName.length() > 22) {
                        bName = bName.substring(0, 20) + "...";
                    }
                    topNames.add(bName);
                    topSales.add(rs.getInt("sold_qty"));
                    topRevenues.add(rs.getDouble("total_amount"));
                }
            }

            // If no sales yet, grab top 5 highest priced/stocked books as baseline preview
            if (topNames.isEmpty()) {
                try (PreparedStatement ps = con.prepareStatement("SELECT name, quantity, price FROM books ORDER BY quantity DESC LIMIT 5");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String bName = rs.getString("name");
                        if (bName != null && bName.length() > 22) {
                            bName = bName.substring(0, 20) + "...";
                        }
                        topNames.add(bName);
                        topSales.add(0);
                        topRevenues.add(0.0);
                    }
                }
            }

            // 8. Order Status Breakdown
            try (PreparedStatement ps = con.prepareStatement("SELECT status, COUNT(*) AS cnt FROM orders GROUP BY status");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String st = rs.getString("status");
                    String label = "ĐÃ THANH TOÁN";
                    if ("PROCESSING".equalsIgnoreCase(st)) label = "ĐANG XỬ LÝ";
                    else if ("SHIPPED".equalsIgnoreCase(st)) label = "ĐÃ GIAO";
                    else if ("CANCELLED".equalsIgnoreCase(st)) label = "ĐÃ HỦY";
                    else if (st != null && !st.trim().isEmpty()) label = st;
                    statusLabels.add(label);
                    statusCounts.add(rs.getInt("cnt"));
                }
            }
            if (statusLabels.isEmpty()) {
                statusLabels.add("ĐÃ THANH TOÁN");
                statusCounts.add(0);
            }

            // 9. Low Stock Books (quantity <= 5)
            String lowStockQuery = "SELECT barcode, name, author, price, quantity FROM books WHERE quantity <= 5 ORDER BY quantity ASC LIMIT 8";
            try (PreparedStatement ps = con.prepareStatement(lowStockQuery);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> b = new HashMap<>();
                    b.put("barcode", rs.getString("barcode"));
                    b.put("name", rs.getString("name"));
                    b.put("author", rs.getString("author"));
                    b.put("price", rs.getDouble("price"));
                    b.put("quantity", rs.getInt("quantity"));
                    lowStockBooks.add(b);
                }
            }

            // 10. Recent Orders (Latest 8)
            String recentOrdersQuery = "SELECT order_id, username, order_date, total_amount, status FROM orders ORDER BY order_date DESC LIMIT 8";
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            try (PreparedStatement ps = con.prepareStatement(recentOrdersQuery);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> o = new HashMap<>();
                    o.put("orderId", rs.getString("order_id"));
                    o.put("username", rs.getString("username"));
                    java.sql.Timestamp ts = rs.getTimestamp("order_date");
                    o.put("orderDate", ts != null ? sdf.format(ts) : "N/A");
                    o.put("totalAmount", rs.getDouble("total_amount"));
                    o.put("status", rs.getString("status"));
                    recentOrders.add(o);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Pass statistics attributes to JSP
        req.setAttribute("totalRevenue", totalRevenue);
        req.setAttribute("totalOrders", totalOrders);
        req.setAttribute("totalBooksSold", totalBooksSold);
        req.setAttribute("totalCustomers", totalCustomers);
        req.setAttribute("totalStock", totalStock);
        req.setAttribute("totalTitles", totalTitles);

        req.setAttribute("revenueDatesJson", toJsonArrayStrings(revDates));
        req.setAttribute("revenueAmountsJson", toJsonArrayNumbers(revAmounts));
        req.setAttribute("revenueOrdersJson", toJsonArrayIntegers(revOrders));

        req.setAttribute("topNamesJson", toJsonArrayStrings(topNames));
        req.setAttribute("topSalesJson", toJsonArrayIntegers(topSales));
        req.setAttribute("topRevenuesJson", toJsonArrayNumbers(topRevenues));

        req.setAttribute("statusLabelsJson", toJsonArrayStrings(statusLabels));
        req.setAttribute("statusCountsJson", toJsonArrayIntegers(statusCounts));

        req.setAttribute("lowStockBooks", lowStockBooks);
        req.setAttribute("recentOrders", recentOrders);

        // Forward to JSP View
        req.getRequestDispatcher("admin-statistics.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    private static String toJsonArrayStrings(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            String s = list.get(i).replace("\"", "\\\"");
            sb.append("\"").append(s).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toJsonArrayNumbers(List<Double> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format(java.util.Locale.US, "%.2f", list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toJsonArrayIntegers(List<Integer> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
