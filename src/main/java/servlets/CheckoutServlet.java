package servlets;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bittercode.constant.BookStoreConstants;
import com.bittercode.model.UserRole;
import com.bittercode.util.StoreUtil;

public class CheckoutServlet extends HttpServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        doPost(req, res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        PrintWriter pw = res.getWriter();
        res.setContentType("text/html; charset=UTF-8");
        if (!StoreUtil.isLoggedIn(UserRole.CUSTOMER, req.getSession())) {
            RequestDispatcher rd = req.getRequestDispatcher("CustomerLogin.html");
            rd.include(req, res);
            pw.println("<table class=\"tab\"><tr><td>Vui lòng đăng nhập để tiếp tục!</td></tr></table>");
            return;
        }
        try {

            RequestDispatcher rd = req.getRequestDispatcher("payment.html");
            rd.include(req, res);
            StoreUtil.setActiveTab(pw, "cart");

            Object amountObj = req.getSession().getAttribute("amountToPay");
            double amount = 0.0;
            if (amountObj instanceof Double) {
                amount = (Double) amountObj;
            } else if (amountObj != null) {
                try {
                    amount = Double.parseDouble(amountObj.toString());
                } catch (Exception ignored) {}
            }

            pw.println("<script>document.getElementById('checkoutTotal').textContent = 'Tổng thanh toán: "
                    + StoreUtil.formatPrice(amount).replace("'", "\\\\'")
                    + "';</script>");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
