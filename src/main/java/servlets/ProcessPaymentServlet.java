package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.bittercode.constant.BookStoreConstants;
import com.bittercode.model.Book;
import com.bittercode.model.Cart;
import com.bittercode.model.Order;
import com.bittercode.model.UserRole;
import com.bittercode.service.BookService;
import com.bittercode.service.OrderService;
import com.bittercode.service.impl.BookServiceImpl;
import com.bittercode.service.impl.OrderServiceImpl;
import com.bittercode.util.StoreUtil;
import com.bittercode.util.OrderEmailService;

public class ProcessPaymentServlet extends HttpServlet {

    BookService bookService = new BookServiceImpl();

    private static final Map<String, Set<String>> DELIVERY_AREAS = Map.of(
            "Hà Nội", Set.of("Ba Đình", "Cầu Giấy", "Đống Đa", "Hà Đông", "Hoàn Kiếm", "Hoàng Mai", "Long Biên", "Nam Từ Liêm", "Thanh Xuân", "Tây Hồ"),
            "TP. Hồ Chí Minh", Set.of("Quận 1", "Quận 3", "Quận 5", "Quận 7", "Quận 10", "Bình Thạnh", "Gò Vấp", "Tân Bình", "Thủ Đức"),
            "Đà Nẵng", Set.of("Hải Châu", "Thanh Khê", "Sơn Trà", "Ngũ Hành Sơn", "Liên Chiểu", "Cẩm Lệ"),
            "Hải Phòng", Set.of("Hồng Bàng", "Lê Chân", "Ngô Quyền", "Hải An", "Kiến An", "Dương Kinh"),
            "Cần Thơ", Set.of("Ninh Kiều", "Bình Thủy", "Cái Răng", "Ô Môn", "Thốt Nốt"),
            "Bình Dương", Set.of("Thủ Dầu Một", "Dĩ An", "Thuận An", "Tân Uyên", "Bến Cát"),
            "Đồng Nai", Set.of("Biên Hòa", "Long Khánh", "Nhơn Trạch", "Trảng Bom", "Long Thành"),
            "Quảng Ninh", Set.of("Hạ Long", "Cẩm Phả", "Uông Bí", "Móng Cái", "Quảng Yên"),
            "Khánh Hòa", Set.of("Nha Trang", "Cam Ranh", "Ninh Hòa", "Diên Khánh", "Vạn Ninh"),
            "Nghệ An", Set.of("Vinh", "Cửa Lò", "Hoàng Mai", "Thái Hòa", "Hưng Nguyên")
    );

    @SuppressWarnings("unchecked")
    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        PrintWriter pw = res.getWriter();
        res.setContentType("text/html; charset=UTF-8");
        if (!StoreUtil.isLoggedIn(UserRole.CUSTOMER, req.getSession())) {
            RequestDispatcher rd = req.getRequestDispatcher("CustomerLogin.html");
            rd.include(req, res);
            pw.println("<table class=\"tab\"><tr><td>Vui lòng đăng nhập để tiếp tục!</td></tr></table>");
            return;
        }

        String validationError = validateCheckout(req);
        if (validationError != null) {
            res.sendRedirect(req.getContextPath() + "/checkout?error="
                    + URLEncoder.encode(validationError, StandardCharsets.UTF_8));
            return;
        }

        try {

            RequestDispatcher rd = req.getRequestDispatcher("CustomerHome.html");
            rd.include(req, res);
            StoreUtil.setActiveTab(pw, "orders");

            HttpSession session = req.getSession();
            List<Cart> cartItems = null;
            if (session.getAttribute("cartItems") != null)
                cartItems = (List<Cart>) session.getAttribute("cartItems");

            String username = (String) session.getAttribute(UserRole.CUSTOMER.toString());
            if (username == null)
                username = "demo";

            double totalAmount = 0.0;
            if (cartItems != null) {
                for (Cart c : cartItems) {
                    totalAmount += c.getBook().getPrice() * c.getQuantity();
                }
            }

            OrderService orderService = new com.bittercode.service.impl.OrderServiceImpl();
            String createdOrderId = null;
            if (cartItems != null && !cartItems.isEmpty()) {
                String shippingEmail = req.getParameter("email");
                String shippingAddress = buildShippingAddress(req);
                String paymentMethod = req.getParameter("paymentMethod");
                if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                    paymentMethod = "Thẻ tín dụng/ghi nợ";
                }

                createdOrderId = orderService.createOrder(username, cartItems, totalAmount,
                        shippingEmail, shippingAddress, paymentMethod);

                // The order transaction has completed. Queue email work so SMTP latency
                // never delays the checkout response.
                Order createdOrder = orderService.getOrderById(createdOrderId);
                OrderEmailService.getInstance().sendOrderConfirmationEmail(createdOrder);
            }

            pw.println("<div class=\"bookshelf-page-container\">");
            pw.println("  <div class=\"bookshelf-page-card\">");
            pw.println("    <header class=\"bookshelf-page-header\" style=\"text-align:center;\">");
            pw.println("      <div style=\"font-size:3rem; color:#16a34a; margin-bottom:12px;\">&#10003;</div>");
            pw.println("      <h1>Đặt Hàng Thành Công!</h1>");
            pw.println("      <p>Mã đơn hàng: <strong>" + (createdOrderId != null ? createdOrderId : "ORD-NEW") + "</strong> | Tổng thanh toán: <strong style='color:#2e6648;'>" + StoreUtil.formatPrice(totalAmount) + "</strong></p>");
            pw.println("    </header>");

            pw.println("    <div class=\"table-responsive\">");
            pw.println("      <table class=\"bookshelf-table\">");
            pw.println("        <thead>");
            pw.println("          <tr>");
            pw.println("            <th>Tựa sách</th>");
            pw.println("            <th>Tác giả</th>");
            pw.println("            <th>Số lượng</th>");
            pw.println("            <th>Thành tiền</th>");
            pw.println("            <th>Trạng thái</th>");
            pw.println("          </tr>");
            pw.println("        </thead>");
            pw.println("        <tbody>");

            if (cartItems != null) {
                for (Cart cart : cartItems) {
                    Book book = cart.getBook();
                    double bPrice = book.getPrice();
                    String bCode = book.getBarcode();
                    String bName = book.getName();
                    String bAuthor = book.getAuthor();
                    int availableQty = book.getQuantity();
                    int qtToBuy = cart.getQuantity();
                    availableQty = availableQty - qtToBuy;
                    bookService.updateBookQtyById(bCode, availableQty);

                    pw.println("          <tr>");
                    pw.println("            <td class=\"book-title-cell\">" + bName + "</td>");
                    pw.println("            <td>" + bAuthor + "</td>");
                    pw.println("            <td><strong>" + qtToBuy + "</strong></td>");
                    pw.println("            <td><span class=\"badge-order-pending\">Chờ xác nhận</span></td>");
                    pw.println("          </tr>");

                    session.removeAttribute("qty_" + bCode);
                }
            }

            pw.println("        </tbody>");
            pw.println("      </table>");
            pw.println("    </div>");

            pw.println("    <div style=\"display:flex; justify-content:center; gap:20px; margin-top:32px; flex-wrap:wrap;\">");
            pw.println("      <a href=\"orders\" class=\"btn-checkout-shelf\">Xem lịch sử đơn hàng &rarr;</a>");
            pw.println("      <a href=\"viewbook\" class=\"btn-pill-secondary\">&larr; Tiếp tục khám phá sách</a>");
            pw.println("    </div>");

            // Clear cart from session, database user_cart, and memory cache
            StoreUtil.clearUserCart(session, username);

            pw.println("  </div>");
            pw.println("</div>");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildShippingAddress(HttpServletRequest req) {
        StringBuilder address = new StringBuilder();
        appendAddressPart(address, req.getParameter("address"));
        appendAddressPart(address, req.getParameter("state"));
        appendAddressPart(address, req.getParameter("city"));
        appendAddressPart(address, req.getParameter("zip"));
        return address.toString();
    }

    private String validateCheckout(HttpServletRequest req) {
        String fullName = trim(req.getParameter("firstname"));
        String email = trim(req.getParameter("email"));
        String address = trim(req.getParameter("address"));
        String city = trim(req.getParameter("city"));
        String district = trim(req.getParameter("state"));
        String zipCode = trim(req.getParameter("zip"));
        String cardName = trim(req.getParameter("cardname"));
        String cardNumber = trim(req.getParameter("cardnumber")).replaceAll("[ -]", "");
        String monthValue = trim(req.getParameter("expmonth"));
        String yearValue = trim(req.getParameter("expyear"));
        String cvv = trim(req.getParameter("cvv"));

        if (fullName.length() < 2 || email.isEmpty() || address.length() < 5) {
            return "Vui lòng điền đầy đủ họ tên, email và địa chỉ nhận hàng.";
        }
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return "Địa chỉ email không đúng định dạng.";
        }
        if (city.isEmpty() || district.isEmpty()) {
            return "Vui lòng chọn tỉnh/thành phố và quận/huyện hợp lệ.";
        }
        if (!zipCode.matches("\\d{5,6}")) {
            return "Mã bưu điện phải gồm 5 hoặc 6 chữ số.";
        }
        if (!cardName.matches("^[\\p{L} .'-]{2,100}$")) {
            return "Tên in trên thẻ không hợp lệ.";
        }
        if (!cardNumber.matches("\\d{13,19}") || !isValidCardNumber(cardNumber)) {
            return "Số thẻ không hợp lệ. Vui lòng kiểm tra lại số thẻ.";
        }
        if (!cvv.matches("\\d{3,4}")) {
            return "Mã CVV phải gồm 3 hoặc 4 chữ số.";
        }

        try {
            int month = Integer.parseInt(monthValue);
            int year = Integer.parseInt(yearValue);
            if (month < 1 || month > 12 || yearValue.length() != 4 || YearMonth.of(year, month).isBefore(YearMonth.now())) {
                return "Thẻ đã hết hạn hoặc ngày hết hạn không hợp lệ.";
            }
        } catch (RuntimeException e) {
            return "Tháng hoặc năm hết hạn không hợp lệ.";
        }
        return null;
    }

    private boolean isValidCardNumber(String cardNumber) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int index = cardNumber.length() - 1; index >= 0; index--) {
            int digit = cardNumber.charAt(index) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void appendAddressPart(StringBuilder address, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (address.length() > 0) address.append(", ");
        address.append(value.trim());
    }
}
