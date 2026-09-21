package com.bittercode.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.bittercode.model.Order;
import com.bittercode.model.OrderDetail;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

public final class OrderEmailService {

    private static final OrderEmailService INSTANCE = new OrderEmailService();
    private final ExecutorService emailExecutor;

    private OrderEmailService() {
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "Order-Email-Worker");
            thread.setDaemon(true);
            return thread;
        };
        emailExecutor = Executors.newSingleThreadExecutor(factory);
    }

    public static OrderEmailService getInstance() {
        return INSTANCE;
    }

    public void sendOrderConfirmationEmail(Order order) {
        submit(order, "Xác nhận đặt hàng thành công - " + order.getOrderId(), buildConfirmationHtml(order));
    }

    public void sendOrderCancelledEmail(Order order, String reason, String cancelledBy) {
        submit(order, "Đơn hàng " + order.getOrderId() + " đã được hủy", buildCancellationHtml(order, reason, cancelledBy));
    }

    public void shutdown() {
        emailExecutor.shutdown();
    }

    private void submit(Order order, String subject, String html) {
        if (order == null) return;

        emailExecutor.submit(() -> {
            try {
                String recipient = resolveRecipient(order);
                if (isBlank(recipient)) {
                    System.err.println("[OrderEmail] No recipient email for order " + order.getOrderId());
                    return;
                }
                send(recipient, subject, html);
            } catch (Exception e) {
                System.err.println("[OrderEmail] Could not send email for order " + order.getOrderId() + ": " + e.getMessage());
            }
        });
    }

    private void send(String recipient, String subject, String html) throws Exception {
        if (!EmailConfig.isConfigured()) {
            System.err.println("[OrderEmail] SMTP is not configured; email was not sent to " + recipient);
            return;
        }

        String host = EmailConfig.get("SMTP_HOST", "");
        String port = EmailConfig.get("SMTP_PORT", "587");
        String username = EmailConfig.get("SMTP_USERNAME", "");
        String password = EmailConfig.get("SMTP_PASSWORD", "");
        String from = EmailConfig.get("SMTP_FROM", "");
        String fromName = EmailConfig.get("SMTP_FROM_NAME", "BOOKS");

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", Boolean.toString(!isBlank(username)));
        properties.put("mail.smtp.starttls.enable", EmailConfig.get("SMTP_STARTTLS", "true"));
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");

        Authenticator authenticator = isBlank(username) ? null : new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };

        Session session = Session.getInstance(properties, authenticator);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from, fromName, "UTF-8"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient, false));
        message.setSubject(subject, "UTF-8");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(html, "text/html; charset=UTF-8");
        MimeMultipart content = new MimeMultipart("alternative");
        content.addBodyPart(htmlPart);
        message.setContent(content);
        Transport.send(message);
    }

    private String buildConfirmationHtml(Order order) {
        String details = buildItemsRows(order);
        String orderUrl = EmailConfig.get("APP_BASE_URL", "").replaceAll("/+$", "") + "/orders";
        return emailLayout("Đặt hàng thành công", "Cảm ơn bạn đã tin tưởng BOOKS! Đơn hàng của bạn đã được ghi nhận.",
                "#b97831", "Chờ xác nhận", order,
                "<table role=\"presentation\" class=\"items\"><thead><tr><th>Sách</th><th>SL</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead>" +
                "<tbody>" + details + "</tbody></table>" +
                "<div class=\"total\"><span>Tổng thanh toán</span><strong>" + money(order.getTotalAmount()) + "</strong></div>" +
                "<p class=\"button-wrap\"><a class=\"button\" href=\"" + escapeAttribute(orderUrl) + "\">Xem lịch sử đơn hàng</a></p>");
    }

    private String buildCancellationHtml(Order order, String reason, String cancelledBy) {
        String actor = "CUSTOMER".equalsIgnoreCase(cancelledBy) ? "người mua" : "người bán";
        return emailLayout("Đơn hàng đã được hủy", "Đơn hàng đã được hủy bởi " + actor + ".", "#c2413a", "Đã hủy", order,
                "<div class=\"reason\"><strong>Lý do hủy</strong><br>" + escapeHtml(defaultIfBlank(reason, "Không có lý do cụ thể")) + "</div>" +
                "<p class=\"note\">Số lượng sách của đơn hàng đã được hoàn lại kho.</p>");
    }

    private String emailLayout(String title, String intro, String accent, String status, Order order, String body) {
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN"))
                .format(order.getOrderDate() != null ? order.getOrderDate() : new Date());
        String address = defaultIfBlank(order.getShippingAddress(), "Theo thông tin tài khoản của bạn");
        String paymentMethod = defaultIfBlank(order.getPaymentMethod(), "Chưa xác định");
        return "<!doctype html><html lang=\"vi\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>body{margin:0;background:#f7f2eb;color:#302b26;font-family:Arial,sans-serif}.wrap{width:100%;padding:24px 12px;box-sizing:border-box}.card{max-width:680px;margin:auto;background:#fff;border-radius:18px;overflow:hidden;box-shadow:0 8px 28px rgba(61,42,24,.12)}.brand{padding:22px 30px;background:" + accent + ";color:#fff;font:700 28px Georgia,serif;letter-spacing:1px}.body{padding:30px}.body h1{font:700 25px Georgia,serif;margin:0 0 12px}.body p{line-height:1.6;color:#685f56}.summary{margin:24px 0;padding:18px;border:1px solid #eee3d6;border-radius:12px;background:#fffcf8}.summary-row{padding:6px 0}.label{color:#84796e}.status{display:inline-block;margin-left:8px;padding:4px 10px;border-radius:999px;background:#f8e5cf;color:" + accent + ";font-size:12px;font-weight:700}.items{width:100%;border-collapse:collapse;margin:20px 0;font-size:14px}.items th{padding:11px 8px;text-align:left;background:#f8f3ed;color:#75685b}.items td{padding:12px 8px;border-bottom:1px solid #f0e8df;vertical-align:top}.items th:not(:first-child),.items td:not(:first-child){text-align:right}.total{display:flex;justify-content:space-between;padding:16px 0;font-size:16px}.total strong{font-size:20px;color:" + accent + "}.button-wrap{text-align:center;margin:28px 0 4px}.button{display:inline-block;padding:12px 22px;border-radius:999px;background:" + accent + ";color:#fff!important;font-weight:700;text-decoration:none}.reason{margin:22px 0;padding:16px;border-left:4px solid " + accent + ";background:#fff5f3;line-height:1.6}.note{font-size:13px}.footer{padding:18px 30px;background:#faf6f0;color:#8a7d70;font-size:12px;text-align:center}@media(max-width:560px){.body{padding:22px 18px}.brand{padding:18px}.items{font-size:12px}.items th,.items td{padding:9px 4px}.summary{padding:14px}}</style></head><body><div class=\"wrap\"><main class=\"card\"><div class=\"brand\">BOOKS</div><div class=\"body\"><h1>" + escapeHtml(title) + "</h1><p>Xin chào <strong>" + escapeHtml(order.getUsername()) + "</strong>,<br>" + escapeHtml(intro) + "</p><section class=\"summary\"><div class=\"summary-row\"><span class=\"label\">Mã đơn:</span> <strong>" + escapeHtml(order.getOrderId()) + "</strong></div><div class=\"summary-row\"><span class=\"label\">Ngày đặt:</span> " + date + "</div><div class=\"summary-row\"><span class=\"label\">Trạng thái:</span><span class=\"status\">" + escapeHtml(status) + "</span></div><div class=\"summary-row\"><span class=\"label\">Thanh toán:</span> " + escapeHtml(paymentMethod) + "</div><div class=\"summary-row\"><span class=\"label\">Địa chỉ nhận:</span><br>" + escapeHtml(address).replace("\n", "<br>") + "</div></section>" + body + "</div><footer class=\"footer\">Email tự động từ BOOKS. Vui lòng không trả lời email này.</footer></main></div></body></html>";
    }

    private String buildItemsRows(Order order) {
        StringBuilder rows = new StringBuilder();
        if (order.getItems() == null) return rows.toString();
        for (OrderDetail item : order.getItems()) {
            double unitPrice = item.getQuantity() > 0 ? item.getAmount() / item.getQuantity() : item.getAmount();
            rows.append("<tr><td>").append(escapeHtml(defaultIfBlank(item.getBookName(), item.getBookBarcode())))
                    .append("</td><td>").append(item.getQuantity()).append("</td><td>").append(money(unitPrice))
                    .append("</td><td>").append(money(item.getAmount())).append("</td></tr>");
        }
        return rows.toString();
    }

    private String resolveRecipient(Order order) {
        if (!isBlank(order.getShippingEmail())) return order.getShippingEmail().trim();
        try {
            Connection con = DBUtil.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT mailid FROM users WHERE username = ?")) {
                ps.setString(1, order.getUsername());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString("mailid") : null;
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String money(double amount) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " đ";
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String escapeAttribute(String value) {
        return escapeHtml(value);
    }
}
