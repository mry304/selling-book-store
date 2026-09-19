package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bittercode.constant.BookStoreConstants;
import com.bittercode.model.Book;
import com.bittercode.model.UserRole;
import com.bittercode.service.BookService;
import com.bittercode.service.impl.BookServiceImpl;
import com.bittercode.util.StoreUtil;

public class ReceiptServlet extends HttpServlet {
    BookService bookService = new BookServiceImpl();

    //NOT_IN_USED
    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        PrintWriter pw = res.getWriter();
        res.setContentType("text/html; charset=UTF-8");
        if (!StoreUtil.isLoggedIn(UserRole.CUSTOMER, req.getSession())) {
            RequestDispatcher rd = req.getRequestDispatcher("CustomerLogin.html");
            rd.include(req, res);
            pw.println("<table class=\"tab\"><tr><td>Vui lòng đăng nhập để tiếp tục!</td></tr></table>");
            return;
        }
        try {
            List<Book> books = bookService.getAllBooks();
            int i = 0;
            RequestDispatcher rd = req.getRequestDispatcher("CustomerHome.html");
            rd.include(req, res);
            StoreUtil.setActiveTab(pw, "cart");
            pw.println("<div class=\"tab\">Trạng thái đơn hàng của bạn như bên dưới:</div>");
            pw.println(
                    "<div class=\"tab\">\r\n" + "		<table>\r\n" + "			<tr>\r\n" + "				\r\n"
                            + "				<th>Mã sách</th>\r\n" + "				<th>Tựa sách</th>\r\n"
                            + "				<th>Tác giả</th>\r\n" + "				<th>Đơn giá</th>\r\n"
                            + "				<th>Số lượng</th><br/>\r\n" + "				<th>Thành tiền</th><br/>\r\n"
                            + "			</tr>");
            double total = 0.0;
            for (Book book : books) {
                double bPrice = book.getPrice();
                String bCode = book.getBarcode();
                String bName = book.getName();
                String bAuthor = book.getAuthor();
                int bQty = book.getQuantity();
                i = i + 1;

                String qt = "qty" + Integer.toString(i);
                int quantity = Integer.parseInt(req.getParameter(qt));
                try {
                    String check1 = "checked" + Integer.toString(i);
                    String getChecked = req.getParameter(check1);
                    if (bQty < quantity) {
                        pw.println(
                                "</table><div class=\"tab\" style='color:red;'>Vui lòng chọn số lượng nhỏ hơn hoặc bằng số lượng sách có sẵn</div>");
                        break;
                    }

                    if (getChecked.equals("pay")) {
                        pw.println("<tr><td>" + bCode + "</td>");
                        pw.println("<td>" + bName + "</td>");
                        pw.println("<td>" + bAuthor + "</td>");
                        pw.println("<td>" + StoreUtil.formatPrice(bPrice) + "</td>");
                        pw.println("<td>" + quantity + "</td>");
                        double amount = bPrice * quantity;
                        total = total + amount;
                        pw.println("<td>" + StoreUtil.formatPrice(amount) + "</td></tr>");
                        bQty = bQty - quantity;
                        System.out.println(bQty);
                        bookService.updateBookQtyById(bCode, bQty);
                    }
                } catch (Exception e) {
                }
            }
            pw.println("</table><br/><div class='tab'>Tổng tiền đã thanh toán: " + StoreUtil.formatPrice(total) + "</div>");
//            String fPay = req.getParameter("f_pay");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
