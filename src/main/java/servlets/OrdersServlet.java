package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.bittercode.model.Order;
import com.bittercode.model.OrderCancelledBy;
import com.bittercode.model.OrderDetail;
import com.bittercode.model.OrderStatus;
import com.bittercode.model.UserRole;
import com.bittercode.service.OrderService;
import com.bittercode.service.impl.OrderServiceImpl;
import com.bittercode.util.StoreUtil;

public class OrdersServlet extends HttpServlet {

    private final OrderService orderService = new OrderServiceImpl();

    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        PrintWriter pw = res.getWriter();
        res.setContentType("text/html; charset=UTF-8");

        HttpSession session = req.getSession();
        boolean isCustomer = StoreUtil.isLoggedIn(UserRole.CUSTOMER, session);
        boolean isSeller = StoreUtil.isLoggedIn(UserRole.SELLER, session);

        if (!isCustomer && !isSeller) {
            RequestDispatcher rd = req.getRequestDispatcher("CustomerLogin.html");
            rd.include(req, res);
            pw.println("<table class=\"tab\"><tr><td>Vui lòng đăng nhập để tiếp tục!</td></tr></table>");
            return;
        }

        try {
            if (isCustomer) {
                RequestDispatcher rd = req.getRequestDispatcher("CustomerHome.html");
                rd.include(req, res);
                StoreUtil.setActiveTab(pw, "orders", session);
            } else {
                RequestDispatcher rd = req.getRequestDispatcher("SellerHome.html");
                rd.include(req, res);
                StoreUtil.setActiveTab(pw, "orders");
            }

            String username = isCustomer ? (String) session.getAttribute(UserRole.CUSTOMER.toString()) : null;
            
            // Current filter tab
            String currentFilter = req.getParameter("status");
            if (currentFilter == null || currentFilter.trim().isEmpty()) {
                currentFilter = "ALL";
            } else {
                currentFilter = currentFilter.trim().toUpperCase();
            }

            int page = getPage(req);
            final int pageSize = 10;
            int countAll = getOrderCount(username, isCustomer, "ALL");
            int countPending = getOrderCount(username, isCustomer, "PENDING");
            int countConfirmed = getOrderCount(username, isCustomer, "CONFIRMED");
            int countShipping = getOrderCount(username, isCustomer, "SHIPPING");
            int countCompleted = getOrderCount(username, isCustomer, "COMPLETED");
            int countCancelled = getOrderCount(username, isCustomer, "CANCELLED");
            int filteredCount = getOrderCount(username, isCustomer, currentFilter);
            int totalPages = Math.max(1, (int) Math.ceil((double) filteredCount / pageSize));
            page = Math.min(page, totalPages);
            List<Order> displayOrders = isCustomer
                    ? orderService.getOrdersByUsername(username, currentFilter, page, pageSize)
                    : orderService.getAllOrders(currentFilter, page, pageSize);

            pw.println("<div class=\"bookshelf-page-container\">");
            pw.println("  <div class=\"bookshelf-page-card\">");

            // Page Header
            pw.println("    <header class=\"bookshelf-page-header\">");
            pw.println("      <h1>" + (isCustomer ? "Lịch Sử Đơn Hàng Của Tôi" : "Quản Lý Đơn Đặt Hàng") + "</h1>");
            pw.println("      <p>" + (isCustomer ? "Theo dõi hành trình đơn sách, xác nhận nhận hàng hoặc hủy đơn nhanh chóng." : "Xác nhận đơn, chuyển giao hàng, kiểm soát doanh thu và hoàn tồn kho tự động.") + "</p>");
            pw.println("    </header>");

            // Filter Tabs Navigation
            pw.println("    <nav class=\"order-filter-nav\">");
            renderFilterBtn(pw, "ALL", "Tất cả", countAll, currentFilter);
            renderFilterBtn(pw, "PENDING", "Chờ xác nhận", countPending, currentFilter);
            renderFilterBtn(pw, "CONFIRMED", "Đã xác nhận", countConfirmed, currentFilter);
            renderFilterBtn(pw, "SHIPPING", "Đang giao", countShipping, currentFilter);
            renderFilterBtn(pw, "COMPLETED", "Hoàn thành", countCompleted, currentFilter);
            renderFilterBtn(pw, "CANCELLED", "Đã hủy", countCancelled, currentFilter);
            pw.println("    </nav>");

            if (displayOrders.isEmpty()) {
                pw.println("    <div class=\"bookshelf-empty-state\">");
                pw.println("      <div class=\"empty-icon\">&#128220;</div>");
                pw.println("      <h3>Không có đơn hàng nào trong mục này</h3>");
                pw.println("      <p>Các đơn hàng theo bộ lọc đã chọn sẽ xuất hiện tại đây khi phát sinh giao dịch.</p>");
                if (isCustomer) {
                    pw.println("      <a href=\"viewbook\" class=\"btn-checkout-shelf\">&larr; Khám phá danh mục sách</a>");
                }
                pw.println("    </div>");
            } else {
                pw.println("    <div class=\"table-responsive\">");
                pw.println("      <table class=\"bookshelf-table\">");
                pw.println("        <thead>");
                pw.println("          <tr>");
                pw.println("            <th style=\"width:130px;\">Mã đơn</th>");
                if (isSeller) {
                    pw.println("            <th>Khách hàng</th>");
                }
                pw.println("            <th style=\"min-width:140px;\">Thời gian</th>");
                pw.println("            <th style=\"min-width:220px;\">Sách đã mua</th>");
                pw.println("            <th>Tổng thanh toán</th>");
                pw.println("            <th style=\"min-width:180px;\">Trạng thái</th>");
                pw.println("            <th style=\"min-width:160px; text-align:center;\">Thao tác</th>");
                pw.println("          </tr>");
                pw.println("        </thead>");
                pw.println("        <tbody>");

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

                for (Order order : displayOrders) {
                    OrderStatus st = order.getOrderStatus();

                    pw.println("          <tr>");
                    pw.println("            <td><strong>" + order.getOrderId() + "</strong></td>");
                    if (isSeller) {
                        pw.println("            <td><span style=\"font-weight:600; color:var(--accent-hover);\">" + order.getUsername() + "</span></td>");
                    }

                    // Order Dates Info
                    pw.println("            <td style=\"color:var(--text-secondary); font-size:0.85rem;\">");
                    pw.println("              <div>Đặt lúc: " + (order.getOrderDate() != null ? sdf.format(order.getOrderDate()) : "N/A") + "</div>");
                    if (order.getShippedAt() != null) {
                        pw.println("              <div style=\"color:#c2410c; margin-top:3px;\">Giao lúc: " + sdf.format(order.getShippedAt()) + "</div>");
                    }
                    if (order.getCancelledAt() != null) {
                        pw.println("              <div style=\"color:#b91c1c; margin-top:3px;\">Hủy lúc: " + sdf.format(order.getCancelledAt()) + "</div>");
                    }
                    pw.println("            </td>");

                    // Items list
                    pw.println("            <td><ul style=\"margin:0; padding-left:16px; line-height:1.6;\">");
                    for (OrderDetail item : order.getItems()) {
                        String name = item.getBookName() != null ? item.getBookName() : ("Sách (" + item.getBookBarcode() + ")");
                        pw.println("              <li><span class=\"book-title-cell\" style=\"font-size:0.92rem;\">" + name + "</span> &times; <strong>" + item.getQuantity() + "</strong> (" + StoreUtil.formatPrice(item.getAmount()) + ")</li>");
                    }
                    pw.println("            </ul></td>");

                    // Total Amount
                    pw.println("            <td class=\"price-cell\">" + StoreUtil.formatPrice(order.getTotalAmount()) + "</td>");

                    // Status Badge & Extra Status Info
                    pw.println("            <td>");
                    pw.println("              <span class=\"" + st.getBadgeClass() + "\">" + st.getDisplayName() + "</span>");

                    if (st == OrderStatus.SHIPPING) {
                        pw.println("              <div class=\"order-meta-box order-meta-shipping\">");
                        pw.println("                <span>&#128666; Đang giao hàng</span><br>");
                        pw.println("                <small style=\"color:#9a3412;\">(Tự động hoàn thành sau 3 ngày nếu không có khiếu nại)</small>");
                        pw.println("              </div>");
                    } else if (st == OrderStatus.CANCELLED) {
                        String byWho = "Hệ thống";
                        if ("CUSTOMER".equalsIgnoreCase(order.getCancelledBy())) byWho = "Người mua";
                        else if ("SELLER".equalsIgnoreCase(order.getCancelledBy())) byWho = "Người bán";
                        
                        String reasonText = order.getCancelReason() != null ? order.getCancelReason() : "Không ghi rõ lý do";
                        pw.println("              <div class=\"order-meta-box order-meta-cancelled\">");
                        pw.println("                <strong>Hủy bởi:</strong> " + byWho + "<br>");
                        pw.println("                <strong>Lý do:</strong> " + reasonText + "<br>");
                        pw.println("                <small style=\"color:#15803d;\">&#10003; Đã hoàn kho sách</small>");
                        pw.println("              </div>");
                    }
                    pw.println("            </td>");

                    // Actions Cell
                    pw.println("            <td style=\"text-align:center;\">");
                    pw.println("              <div class=\"order-actions-cell\">");

                    if (isCustomer) {
                        // Customer Actions
                        if (st == OrderStatus.PENDING) {
                            pw.println("                <button type=\"button\" class=\"btn-order-action btn-order-cancel\" onclick=\"openCancelModal('" + order.getOrderId() + "')\">&#10005; Hủy đơn hàng</button>");
                        } else if (st == OrderStatus.CONFIRMED) {
                            pw.println("                <span style=\"font-size:0.8rem; color:var(--text-secondary);\">Shop đang đóng gói</span>");
                        } else if (st == OrderStatus.SHIPPING) {
                            pw.println("                <form method=\"POST\" action=\"orders-action\" onsubmit=\"return confirm('Bạn xác nhận đã nhận được đầy đủ kiện sách này?');\">");
                            pw.println("                  <input type=\"hidden\" name=\"action\" value=\"complete\">");
                            pw.println("                  <input type=\"hidden\" name=\"orderId\" value=\"" + order.getOrderId() + "\">");
                            pw.println("                  <button type=\"submit\" class=\"btn-order-action btn-order-complete\">&#10003; Đã nhận được hàng</button>");
                            pw.println("                </form>");
                        } else if (st == OrderStatus.COMPLETED) {
                            pw.println("                <span style=\"font-size:0.82rem; color:#15803d; font-weight:600;\">&#10003; Giao dịch hoàn tất</span>");
                        } else if (st == OrderStatus.CANCELLED) {
                            pw.println("                <span style=\"font-size:0.8rem; color:var(--text-muted);\">Đơn đã đóng</span>");
                        }
                    } else if (isSeller) {
                        // Seller Actions
                        if (st == OrderStatus.PENDING) {
                            pw.println("                <form method=\"POST\" action=\"orders-action\">");
                            pw.println("                  <input type=\"hidden\" name=\"action\" value=\"confirm\">");
                            pw.println("                  <input type=\"hidden\" name=\"orderId\" value=\"" + order.getOrderId() + "\">");
                            pw.println("                  <button type=\"submit\" class=\"btn-order-action btn-order-confirm\" style=\"width:100%; margin-bottom:5px;\">&#10003; Xác nhận đơn</button>");
                            pw.println("                </form>");
                            pw.println("                <button type=\"button\" class=\"btn-order-action btn-order-cancel\" onclick=\"openCancelModal('" + order.getOrderId() + "')\">&#10005; Hủy đơn</button>");
                        } else if (st == OrderStatus.CONFIRMED) {
                            pw.println("                <form method=\"POST\" action=\"orders-action\">");
                            pw.println("                  <input type=\"hidden\" name=\"action\" value=\"ship\">");
                            pw.println("                  <input type=\"hidden\" name=\"orderId\" value=\"" + order.getOrderId() + "\">");
                            pw.println("                  <button type=\"submit\" class=\"btn-order-action btn-order-ship\" style=\"width:100%; margin-bottom:5px;\">&#128666; Chuyển giao hàng</button>");
                            pw.println("                </form>");
                            pw.println("                <button type=\"button\" class=\"btn-order-action btn-order-cancel\" onclick=\"openCancelModal('" + order.getOrderId() + "')\">&#10005; Hủy đơn</button>");
                        } else if (st == OrderStatus.SHIPPING) {
                            pw.println("                <form method=\"POST\" action=\"orders-action\" onsubmit=\"return confirm('Xác nhận giao đơn hàng này thành công thay cho khách hàng?');\">");
                            pw.println("                  <input type=\"hidden\" name=\"action\" value=\"complete\">");
                            pw.println("                  <input type=\"hidden\" name=\"orderId\" value=\"" + order.getOrderId() + "\">");
                            pw.println("                  <button type=\"submit\" class=\"btn-order-action btn-order-complete\">&#10003; Giao thành công</button>");
                            pw.println("                </form>");
                        } else if (st == OrderStatus.COMPLETED) {
                            pw.println("                <span style=\"font-size:0.82rem; color:#15803d; font-weight:600;\">&#10003; Đã hoàn thành</span>");
                        } else if (st == OrderStatus.CANCELLED) {
                            pw.println("                <span style=\"font-size:0.8rem; color:var(--text-muted);\">Đã hoàn kho</span>");
                        }
                    }

                    pw.println("              </div>"); // end order-actions-cell
                    pw.println("            </td>");

                    pw.println("          </tr>");
                }

                pw.println("        </tbody>");
                pw.println("      </table>");
                pw.println("    </div>"); // end table-responsive
                renderPagination(pw, "orders", currentFilter, page, totalPages, filteredCount);
            }

            pw.println("  </div>"); // end bookshelf-page-card
            pw.println("</div>");   // end bookshelf-page-container

            // Cancellation Modal Dialog
            renderCancelModal(pw, isSeller);

            // Modal Trigger Javascript
            pw.println("<script>");
            // Move modal to document.body on load to escape parent stacking context
            // This fixes position:fixed not working correctly inside servlet containers
            pw.println("document.addEventListener('DOMContentLoaded', function() {");
            pw.println("  var modal = document.getElementById('cancelOrderModal');");
            pw.println("  if (modal && modal.parentNode !== document.body) {");
            pw.println("    document.body.appendChild(modal);");
            pw.println("  }");
            pw.println("});");
            pw.println("function openCancelModal(orderId) {");
            pw.println("  var modal = document.getElementById('cancelOrderModal');");
            pw.println("  if (modal.parentNode !== document.body) { document.body.appendChild(modal); }");
            pw.println("  document.getElementById('modalOrderIdInput').value = orderId;");
            pw.println("  document.getElementById('modalOrderIdDisplay').innerText = orderId;");
            pw.println("  modal.style.display = 'flex';");
            pw.println("  document.body.style.overflow = 'hidden';");
            pw.println("}");
            pw.println("function closeCancelModal() {");
            pw.println("  var modal = document.getElementById('cancelOrderModal');");
            pw.println("  modal.style.display = 'none';");
            pw.println("  document.body.style.overflow = '';");
            pw.println("}");
            pw.println("function onReasonSelectChange(select) {");
            pw.println("  var customBox = document.getElementById('customReasonWrapper');");
            pw.println("  var customInput = document.getElementById('customReasonInput');");
            pw.println("  if (select.value === 'OTHER') {");
            pw.println("    customBox.style.display = 'block';");
            pw.println("    customInput.required = true;");
            pw.println("    customInput.focus();");
            pw.println("  } else {");
            pw.println("    customBox.style.display = 'none';");
            pw.println("    customInput.required = false;");
            pw.println("    customInput.value = '';");
            pw.println("  }");
            pw.println("}");
            pw.println("function submitCancelForm(e) {");
            pw.println("  var select = document.getElementById('reasonSelect');");
            pw.println("  var finalInput = document.getElementById('finalReasonInput');");
            pw.println("  var customInput = document.getElementById('customReasonInput');");
            pw.println("  if (select.value === 'OTHER') {");
            pw.println("    finalInput.value = customInput.value.trim();");
            pw.println("    if (!finalInput.value) { alert('Vui lòng nhập lý do hủy cụ thể.'); return false; }");
            pw.println("  } else {");
            pw.println("    finalInput.value = select.options[select.selectedIndex].text;");
            pw.println("  }");
            pw.println("  return true;");
            pw.println("}");
            pw.println("// Close modal when pressing Escape key");
            pw.println("document.addEventListener('keydown', function(e) {");
            pw.println("  if (e.key === 'Escape') { closeCancelModal(); }");
            pw.println("});");
            pw.println("</script>");

        } catch (Exception e) {
            e.printStackTrace();
            pw.println("<div class='container my-4'><div class='alert alert-danger'>Lỗi tải danh sách đơn hàng: " + e.getMessage() + "</div></div>");
        }
    }

    private void renderFilterBtn(PrintWriter pw, String statusKey, String title, int count, String currentFilter) {
        boolean active = statusKey.equalsIgnoreCase(currentFilter);
        String icon = getOrderFilterIcon(statusKey);
        pw.println("      <a href=\"orders?status=" + statusKey + "&page=1\" class=\"order-filter-btn order-filter-" + statusKey.toLowerCase() + " " + (active ? "active" : "") + "\"" + (active ? " aria-current=\"page\"" : "") + ">");
        pw.println("        <span class=\"order-filter-icon\" aria-hidden=\"true\">" + icon + "</span>");
        pw.println("        <span class=\"order-filter-label\">" + title + "</span>");
        pw.println("        <span class=\"order-filter-badge\">" + count + "</span>");
        pw.println("      </a>");
    }

    private int getOrderCount(String username, boolean isCustomer, String status) throws Exception {
        return isCustomer ? orderService.getOrderCountByUsername(username, status) : orderService.getOrderCount(status);
    }

    private int getPage(HttpServletRequest req) {
        try {
            return Math.max(1, Integer.parseInt(req.getParameter("page")));
        } catch (Exception ignored) {
            return 1;
        }
    }

    private void renderPagination(PrintWriter pw, String baseUrl, String status, int page, int totalPages, int totalItems) {
        if (totalPages <= 1) return;
        pw.println("    <nav class=\"pagination-nav\" aria-label=\"Phân trang đơn hàng\">");
        pw.println("      <span class=\"pagination-summary\">" + totalItems + " đơn hàng · Trang " + page + "/" + totalPages + "</span>");
        pw.println("      <div class=\"pagination-links\">");
        if (page > 1) pw.println("<a href=\"" + baseUrl + "?status=" + status + "&page=" + (page - 1) + "\">&larr; Trước</a>");
        for (int i = Math.max(1, page - 2); i <= Math.min(totalPages, page + 2); i++) {
            pw.println("<a class=\"" + (i == page ? "active" : "") + "\" href=\"" + baseUrl + "?status=" + status + "&page=" + i + "\">" + i + "</a>");
        }
        if (page < totalPages) pw.println("<a href=\"" + baseUrl + "?status=" + status + "&page=" + (page + 1) + "\">Tiếp &rarr;</a>");
        pw.println("      </div></nav>");
    }

    private String getOrderFilterIcon(String statusKey) {
        if ("PENDING".equals(statusKey)) return "&#128337;";
        if ("CONFIRMED".equals(statusKey)) return "&#10003;";
        if ("SHIPPING".equals(statusKey)) return "&#128666;";
        if ("COMPLETED".equals(statusKey)) return "&#10004;";
        if ("CANCELLED".equals(statusKey)) return "&#10005;";
        return "&#128203;";
    }

    private void renderCancelModal(PrintWriter pw, boolean isSeller) {
        // Modal is initially hidden via inline style (not CSS class) so JS can control it
        // JS will move this to document.body to fix position:fixed stacking context issues
        pw.println("<div id=\"cancelOrderModal\" onclick=\"if(event.target===this) closeCancelModal();\" style=\"display:none; position:fixed; inset:0; z-index:99999; align-items:center; justify-content:center; background:rgba(43,40,37,0.7); backdrop-filter:blur(5px); padding:16px;\">");
        pw.println("  <div style=\"width:100%; max-width:500px; background:#ffffff; border-radius:20px; box-shadow:0 24px 48px rgba(0,0,0,0.28); overflow:hidden; animation:modalFadeIn 0.25s cubic-bezier(0.16,1,0.3,1);\">");
        pw.println("    <div style=\"display:flex; align-items:center; justify-content:space-between; padding:18px 24px; border-bottom:1px solid #f1ece4; background:#faf7f2;\">");
        pw.println("      <h4 style=\"margin:0; font-size:1.1rem; font-weight:700; color:#2b2825; font-family:Plus Jakarta Sans,sans-serif;\">" + (isSeller ? "&#128683; Hủy đơn hàng (Người bán)" : "&#128683; Hủy đơn hàng của bạn") + "</h4>");
        pw.println("      <button type=\"button\" onclick=\"closeCancelModal()\" style=\"background:transparent; border:0; font-size:1.5rem; line-height:1; color:#7c746b; cursor:pointer; padding:4px 8px;\">&times;</button>");
        pw.println("    </div>");
        pw.println("    <form method=\"POST\" action=\"orders-action\" onsubmit=\"return submitCancelForm(event);\">");
        pw.println("      <input type=\"hidden\" name=\"action\" value=\"cancel\">");
        pw.println("      <input type=\"hidden\" name=\"orderId\" id=\"modalOrderIdInput\">");
        pw.println("      <input type=\"hidden\" name=\"reason\" id=\"finalReasonInput\">");
        pw.println("      <div style=\"padding:22px 24px;\">");
        pw.println("        <p style=\"margin-bottom:10px; font-size:0.95rem;\">Bạn có chắc chắn muốn hủy đơn hàng <strong id=\"modalOrderIdDisplay\" style=\"color:#ae7634;\"></strong>?</p>");
        pw.println("        <p style=\"font-size:0.83rem; color:#15803d; margin-bottom:18px; background:#dcfce7; padding:8px 12px; border-radius:8px;\">&#10003; Số lượng sách trong đơn sẽ được tự động hoàn lại vào kho.</p>");
        pw.println("        <div style=\"margin-bottom:14px;\">");
        pw.println("          <label style=\"display:block; font-weight:600; font-size:0.88rem; margin-bottom:7px; color:#2b2825;\">Vui lòng chọn lý do hủy:</label>");
        pw.println("          <select id=\"reasonSelect\" class=\"form-control\" onchange=\"onReasonSelectChange(this)\" style=\"width:100%; padding:10px 14px; border:1.5px solid #d1cbbf; border-radius:10px; font-size:0.9rem; font-family:Plus Jakarta Sans,sans-serif;\">");
        if (isSeller) {
            pw.println("            <option value=\"OUT_OF_STOCK\">Kho tạm hết số lượng sách yêu cầu</option>");
            pw.println("            <option value=\"CANNOT_CONTACT\">Không thể liên hệ người mua qua điện thoại</option>");
            pw.println("            <option value=\"CANNOT_DELIVER\">Địa chỉ giao hàng nằm ngoài phạm vi phục vụ</option>");
            pw.println("            <option value=\"CUSTOMER_REQUESTED\">Người mua chủ động gọi điện yêu cầu hủy</option>");
            pw.println("            <option value=\"OTHER\">Lý do khác...</option>");
        } else {
            pw.println("            <option value=\"CHANGE_MIND\">Thay đổi nhu cầu, không muốn mua nữa</option>");
            pw.println("            <option value=\"WRONG_INFO\">Muốn thay đổi địa chỉ hoặc số điện thoại nhận hàng</option>");
            pw.println("            <option value=\"FOUND_CHEAPER\">Tìm thấy tựa sách này ở nơi khác giá tốt hơn</option>");
            pw.println("            <option value=\"WRONG_BOOK\">Đặt nhầm tựa sách hoặc nhầm số lượng</option>");
            pw.println("            <option value=\"OTHER\">Lý do khác...</option>");
        }
        pw.println("          </select>");
        pw.println("        </div>");
        pw.println("        <div id=\"customReasonWrapper\" style=\"display:none; margin-bottom:14px;\">");
        pw.println("          <label style=\"display:block; font-weight:600; font-size:0.88rem; margin-bottom:7px; color:#2b2825;\">Nhập lý do chi tiết:</label>");
        pw.println("          <textarea id=\"customReasonInput\" rows=\"3\" placeholder=\"Nhập lý do hủy của bạn...\" style=\"width:100%; padding:10px 14px; border:1.5px solid #d1cbbf; border-radius:10px; font-size:0.9rem; resize:vertical; font-family:Plus Jakarta Sans,sans-serif;\"></textarea>");
        pw.println("        </div>");
        pw.println("      </div>");
        pw.println("      <div style=\"display:flex; justify-content:flex-end; gap:12px; padding:16px 24px; border-top:1px solid #f1ece4; background:#faf7f2;\">");
        pw.println("        <button type=\"button\" onclick=\"closeCancelModal()\" style=\"padding:9px 20px; border-radius:999px; background:#e5e7eb; color:#374151; font-weight:600; border:0; cursor:pointer; font-size:0.88rem;\">Đóng</button>");
        pw.println("        <button type=\"submit\" style=\"padding:9px 22px; border-radius:999px; background:#dc2626; color:#fff; font-weight:700; border:0; cursor:pointer; font-size:0.88rem;\">&#10005; Xác nhận hủy đơn</button>");
        pw.println("      </div>");
        pw.println("    </form>");
        pw.println("  </div>");
        pw.println("</div>");
    }
}
