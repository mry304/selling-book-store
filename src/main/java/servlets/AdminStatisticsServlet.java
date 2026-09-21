package servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
            String ajax = req.getParameter("ajax");
            if ("revenue".equalsIgnoreCase(ajax) || "filterRevenue".equalsIgnoreCase(req.getParameter("action"))) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"status\":\"error\",\"message\":\"Chưa đăng nhập quản trị viên\"}");
                return;
            }
            res.sendRedirect("SellerLogin.html");
            return;
        }

        String ajax = req.getParameter("ajax");
        String action = req.getParameter("action");
        if ("revenue".equalsIgnoreCase(ajax) || "filterRevenue".equalsIgnoreCase(action)) {
            handleRevenueFilterAjax(req, res);
            return;
        }

        double totalRevenue = 0.0;
        int totalOrders = 0;
        int totalBooksSold = 0;
        int totalCustomers = 0;
        int totalStock = 0;
        int totalTitles = 0;
        int pendingOrders = 0;
        int confirmedOrders = 0;
        int lowStockTitleCount = 0;
        int slowMovingStockCount = 0;
        double slowMovingStockValue = 0.0;

        List<String> revDates = new ArrayList<>();
        List<Double> revAmounts = new ArrayList<>();
        List<Integer> revOrders = new ArrayList<>();

        List<String> topNames = new ArrayList<>();
        List<Integer> topSales = new ArrayList<>();
        List<Double> topRevenues = new ArrayList<>();

        List<String> statusLabels = new ArrayList<>();
        List<Integer> statusCounts = new ArrayList<>();

        List<Map<String, Object>> lowStockBooks = new ArrayList<>();
        List<Map<String, Object>> slowMovingStockBooks = new ArrayList<>();
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

            // Actionable operations metrics for the seller dashboard.
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COALESCE(SUM(status = 'PENDING'), 0) AS pending_count, " +
                    "COALESCE(SUM(status = 'CONFIRMED'), 0) AS confirmed_count FROM orders")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        pendingOrders = rs.getInt("pending_count");
                        confirmedOrders = rs.getInt("confirmed_count");
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM books WHERE quantity <= 5")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) lowStockTitleCount = rs.getInt(1);
                }
            }

            // Books still in stock with no completed sale in the last 90 days.
            String slowMovingSummaryQuery = "SELECT COUNT(*), COALESCE(SUM(b.quantity * b.price), 0) "
                    + "FROM books b WHERE b.quantity > 0 AND NOT EXISTS ("
                    + "SELECT 1 FROM order_details od JOIN orders o ON o.order_id = od.order_id "
                    + "WHERE od.book_barcode = b.barcode AND o.status = 'COMPLETED' "
                    + "AND o.order_date >= DATE_SUB(CURDATE(), INTERVAL 90 DAY))";
            try (PreparedStatement ps = con.prepareStatement(slowMovingSummaryQuery);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    slowMovingStockCount = rs.getInt(1);
                    slowMovingStockValue = rs.getDouble(2);
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
                    com.bittercode.model.OrderStatus os = com.bittercode.model.OrderStatus.fromString(st);
                    statusLabels.add(os.getDisplayName());
                    statusCounts.add(rs.getInt("cnt"));
                }
            }
            if (statusLabels.isEmpty()) {
                statusLabels.add("Chờ xác nhận");
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

            String slowMovingBooksQuery = "SELECT b.barcode, b.name, b.author, b.price, b.quantity, "
                    + "DATE_FORMAT(MAX(o.order_date), '%d/%m/%Y') AS last_sold "
                    + "FROM books b LEFT JOIN order_details od ON od.book_barcode = b.barcode "
                    + "LEFT JOIN orders o ON o.order_id = od.order_id AND o.status = 'COMPLETED' "
                    + "WHERE b.quantity > 0 GROUP BY b.barcode, b.name, b.author, b.price, b.quantity "
                    + "HAVING MAX(o.order_date) IS NULL OR MAX(o.order_date) < DATE_SUB(CURDATE(), INTERVAL 90 DAY) "
                    + "ORDER BY (b.quantity * b.price) DESC LIMIT 8";
            try (PreparedStatement ps = con.prepareStatement(slowMovingBooksQuery);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> b = new HashMap<>();
                    b.put("barcode", rs.getString("barcode"));
                    b.put("name", rs.getString("name"));
                    b.put("author", rs.getString("author"));
                    b.put("quantity", rs.getInt("quantity"));
                    b.put("stockValue", rs.getDouble("price") * rs.getInt("quantity"));
                    b.put("lastSold", rs.getString("last_sold"));
                    slowMovingStockBooks.add(b);
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
        req.setAttribute("pendingOrders", pendingOrders);
        req.setAttribute("confirmedOrders", confirmedOrders);
        req.setAttribute("lowStockTitleCount", lowStockTitleCount);
        req.setAttribute("slowMovingStockCount", slowMovingStockCount);
        req.setAttribute("slowMovingStockValue", slowMovingStockValue);

        req.setAttribute("revenueDatesJson", toJsonArrayStrings(revDates));
        req.setAttribute("revenueAmountsJson", toJsonArrayNumbers(revAmounts));
        req.setAttribute("revenueOrdersJson", toJsonArrayIntegers(revOrders));

        req.setAttribute("topNamesJson", toJsonArrayStrings(topNames));
        req.setAttribute("topSalesJson", toJsonArrayIntegers(topSales));
        req.setAttribute("topRevenuesJson", toJsonArrayNumbers(topRevenues));

        req.setAttribute("statusLabelsJson", toJsonArrayStrings(statusLabels));
        req.setAttribute("statusCountsJson", toJsonArrayIntegers(statusCounts));

        req.setAttribute("lowStockBooks", lowStockBooks);
        req.setAttribute("slowMovingStockBooks", slowMovingStockBooks);
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

    private void handleRevenueFilterAjax(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setCharacterEncoding("UTF-8");
        res.setContentType("application/json; charset=UTF-8");
        String filterType = req.getParameter("filterType");
        String filterValue = req.getParameter("filterValue");

        if (filterType == null || filterType.trim().isEmpty()) {
            filterType = "all";
        }

        double periodRevenue = 0.0;
        int periodOrders = 0;
        int periodBooksSold = 0;
        List<String> timelineLabels = new ArrayList<>();
        List<Double> timelineAmounts = new ArrayList<>();
        String filterLabel = "Toàn bộ thời gian";

        try (Connection con = DBUtil.getConnection()) {
            LocalDate today = LocalDate.now();

            if ("day".equalsIgnoreCase(filterType)) {
                LocalDate date;
                try {
                    date = LocalDate.parse(filterValue);
                } catch (Exception e) {
                    date = today;
                }
                filterLabel = "Ngày " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                // 1. Revenue & Orders
                String sqlRev = "SELECT COALESCE(SUM(total_amount), 0), COUNT(*) FROM orders WHERE status != 'CANCELLED' AND DATE(order_date) = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlRev)) {
                    ps.setString(1, date.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            periodRevenue = rs.getDouble(1);
                            periodOrders = rs.getInt(2);
                        }
                    }
                }

                // 2. Books Sold
                String sqlBooks = "SELECT COALESCE(SUM(od.quantity), 0) FROM order_details od JOIN orders o ON od.order_id = o.order_id WHERE o.status != 'CANCELLED' AND DATE(o.order_date) = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlBooks)) {
                    ps.setString(1, date.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            periodBooksSold = rs.getInt(1);
                        }
                    }
                }

                // 3. Hourly timeline for day
                String[] timeSlots = new String[]{"00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "23:59"};
                double[] slotAmounts = new double[timeSlots.length];

                String sqlTimeline = "SELECT HOUR(order_date) AS hr, COALESCE(SUM(total_amount), 0) AS rev FROM orders WHERE status != 'CANCELLED' AND DATE(order_date) = ? GROUP BY HOUR(order_date)";
                try (PreparedStatement ps = con.prepareStatement(sqlTimeline)) {
                    ps.setString(1, date.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int hr = rs.getInt("hr");
                            double rev = rs.getDouble("rev");
                            int slotIdx = Math.min(hr / 4, 5);
                            slotAmounts[slotIdx] += rev;
                        }
                    }
                }

                for (int i = 0; i < timeSlots.length; i++) {
                    timelineLabels.add(timeSlots[i]);
                    timelineAmounts.add(slotAmounts[i]);
                }

            } else if ("month".equalsIgnoreCase(filterType)) {
                int month = today.getMonthValue();
                int year = today.getYear();
                if (filterValue != null && filterValue.contains("-")) {
                    String[] parts = filterValue.split("-");
                    try {
                        if (parts[0].length() == 4) {
                            year = Integer.parseInt(parts[0]);
                            month = Integer.parseInt(parts[1]);
                        } else {
                            month = Integer.parseInt(parts[0]);
                            year = Integer.parseInt(parts[1]);
                        }
                    } catch (Exception ignored) {}
                }
                filterLabel = String.format(Locale.US, "Tháng %02d/%d", month, year);

                // 1. Revenue & Orders
                String sqlRev = "SELECT COALESCE(SUM(total_amount), 0), COUNT(*) FROM orders WHERE status != 'CANCELLED' AND MONTH(order_date) = ? AND YEAR(order_date) = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlRev)) {
                    ps.setInt(1, month);
                    ps.setInt(2, year);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            periodRevenue = rs.getDouble(1);
                            periodOrders = rs.getInt(2);
                        }
                    }
                }

                // 2. Books Sold
                String sqlBooks = "SELECT COALESCE(SUM(od.quantity), 0) FROM order_details od JOIN orders o ON od.order_id = o.order_id WHERE o.status != 'CANCELLED' AND MONTH(o.order_date) = ? AND YEAR(o.order_date) = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlBooks)) {
                    ps.setInt(1, month);
                    ps.setInt(2, year);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            periodBooksSold = rs.getInt(1);
                        }
                    }
                }

                // 3. Timeline for month
                int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
                double[] dayAmounts = new double[daysInMonth + 1];

                String sqlTimeline = "SELECT DAY(order_date) AS d, COALESCE(SUM(total_amount), 0) AS rev FROM orders WHERE status != 'CANCELLED' AND MONTH(order_date) = ? AND YEAR(order_date) = ? GROUP BY DAY(order_date)";
                try (PreparedStatement ps = con.prepareStatement(sqlTimeline)) {
                    ps.setInt(1, month);
                    ps.setInt(2, year);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int d = rs.getInt("d");
                            if (d >= 1 && d <= daysInMonth) {
                                dayAmounts[d] = rs.getDouble("rev");
                            }
                        }
                    }
                }

                for (int d = 1; d <= daysInMonth; d++) {
                    timelineLabels.add(String.format(Locale.US, "%02d/%02d", d, month));
                    timelineAmounts.add(dayAmounts[d]);
                }

            } else if ("quarter".equalsIgnoreCase(filterType)) {
                int quarter = (today.getMonthValue() - 1) / 3 + 1;
                int year = today.getYear();
                if (filterValue != null && filterValue.contains("-")) {
                    String[] parts = filterValue.split("-");
                    try {
                        quarter = Integer.parseInt(parts[0]);
                        year = Integer.parseInt(parts[1]);
                    } catch (Exception ignored) {}
                }
                filterLabel = "Quý " + quarter + "/" + year;

                // 1. Revenue & Orders
                String sqlRev = "SELECT COALESCE(SUM(total_amount), 0), COUNT(*) FROM orders WHERE status != 'CANCELLED' AND QUARTER(order_date) = ? AND YEAR(order_date) = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlRev)) {
                    ps.setInt(1, quarter);
                    ps.setInt(2, year);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            periodRevenue = rs.getDouble(1);
                            periodOrders = rs.getInt(2);
                        }
                    }
                }

                // 2. Books Sold
                String sqlBooks = "SELECT COALESCE(SUM(od.quantity), 0) FROM order_details od JOIN orders o ON od.order_id = o.order_id WHERE o.status != 'CANCELLED' AND QUARTER(o.order_date) = ? AND YEAR(o.order_date) = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlBooks)) {
                    ps.setInt(1, quarter);
                    ps.setInt(2, year);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            periodBooksSold = rs.getInt(1);
                        }
                    }
                }

                // 3. Timeline for quarter: 3 months
                int startM = (quarter - 1) * 3 + 1;
                Map<Integer, Double> qMap = new HashMap<>();
                String sqlTimeline = "SELECT MONTH(order_date) AS m, COALESCE(SUM(total_amount), 0) AS rev FROM orders WHERE status != 'CANCELLED' AND QUARTER(order_date) = ? AND YEAR(order_date) = ? GROUP BY MONTH(order_date)";
                try (PreparedStatement ps = con.prepareStatement(sqlTimeline)) {
                    ps.setInt(1, quarter);
                    ps.setInt(2, year);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            qMap.put(rs.getInt("m"), rs.getDouble("rev"));
                        }
                    }
                }

                for (int m = startM; m <= startM + 2; m++) {
                    timelineLabels.add(String.format(Locale.US, "Tháng %02d", m));
                    timelineAmounts.add(qMap.getOrDefault(m, 0.0));
                }

            } else if ("year".equalsIgnoreCase(filterType)) {
                int year = today.getYear();
                if (filterValue != null && !filterValue.trim().isEmpty()) {
                    try {
                        year = Integer.parseInt(filterValue.trim());
                    } catch (Exception ignored) {}
                }
                filterLabel = "Năm " + year;

                // 1. Revenue & Orders
                String sqlRev = "SELECT COALESCE(SUM(total_amount), 0), COUNT(*) FROM orders WHERE status != 'CANCELLED' AND YEAR(order_date) = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlRev)) {
                    ps.setInt(1, year);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            periodRevenue = rs.getDouble(1);
                            periodOrders = rs.getInt(2);
                        }
                    }
                }

                // 2. Books Sold
                String sqlBooks = "SELECT COALESCE(SUM(od.quantity), 0) FROM order_details od JOIN orders o ON od.order_id = o.order_id WHERE o.status != 'CANCELLED' AND YEAR(o.order_date) = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlBooks)) {
                    ps.setInt(1, year);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            periodBooksSold = rs.getInt(1);
                        }
                    }
                }

                // 3. Timeline for year: 12 months
                Map<Integer, Double> yMap = new HashMap<>();
                String sqlTimeline = "SELECT MONTH(order_date) AS m, COALESCE(SUM(total_amount), 0) AS rev FROM orders WHERE status != 'CANCELLED' AND YEAR(order_date) = ? GROUP BY MONTH(order_date)";
                try (PreparedStatement ps = con.prepareStatement(sqlTimeline)) {
                    ps.setInt(1, year);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            yMap.put(rs.getInt("m"), rs.getDouble("rev"));
                        }
                    }
                }

                for (int m = 1; m <= 12; m++) {
                    timelineLabels.add(String.format(Locale.US, "Tháng %02d", m));
                    timelineAmounts.add(yMap.getOrDefault(m, 0.0));
                }

            } else {
                filterLabel = "Toàn bộ thời gian";
                try (PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(total_amount), 0), COUNT(*) FROM orders WHERE status != 'CANCELLED'")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            periodRevenue = rs.getDouble(1);
                            periodOrders = rs.getInt(2);
                        }
                    }
                }

                try (PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(quantity), 0) FROM order_details od JOIN orders o ON od.order_id = o.order_id WHERE o.status != 'CANCELLED'")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) periodBooksSold = rs.getInt(1);
                    }
                }

                String revQuery = "SELECT DATE_FORMAT(order_date, '%d/%m') AS day_label, "
                        + "COALESCE(SUM(total_amount), 0) AS daily_rev "
                        + "FROM orders "
                        + "WHERE status != 'CANCELLED' "
                        + "GROUP BY DATE(order_date), DATE_FORMAT(order_date, '%d/%m') "
                        + "ORDER BY DATE(order_date) ASC LIMIT 10";
                try (PreparedStatement ps = con.prepareStatement(revQuery);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        timelineLabels.add(rs.getString("day_label"));
                        timelineAmounts.add(rs.getDouble("daily_rev"));
                    }
                }

                if (timelineLabels.isEmpty()) {
                    for (int i = 6; i >= 0; i--) {
                        LocalDate d = today.minusDays(i);
                        timelineLabels.add(d.format(DateTimeFormatter.ofPattern("dd/MM")));
                        timelineAmounts.add(0.0);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        String formattedRevenue = String.format(new Locale("vi", "VN"), "%,.0f đ", periodRevenue);

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"status\":\"success\",");
        json.append("\"filterType\":\"").append(escapeJson(filterType)).append("\",");
        json.append("\"filterValue\":\"").append(escapeJson(filterValue != null ? filterValue : "")).append("\",");
        json.append("\"filterLabel\":\"").append(escapeJson(filterLabel)).append("\",");
        json.append("\"periodRevenue\":").append(String.format(Locale.US, "%.2f", periodRevenue)).append(",");
        json.append("\"periodRevenueFormatted\":\"").append(escapeJson(formattedRevenue)).append("\",");
        json.append("\"periodOrders\":").append(periodOrders).append(",");
        json.append("\"periodBooksSold\":").append(periodBooksSold).append(",");
        json.append("\"timelineLabels\":").append(toJsonArrayStrings(timelineLabels)).append(",");
        json.append("\"timelineAmounts\":").append(toJsonArrayNumbers(timelineAmounts));
        json.append("}");

        res.getWriter().write(json.toString());
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
