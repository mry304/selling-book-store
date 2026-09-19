package servlets;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bittercode.constant.BookStoreConstants;
import com.bittercode.service.UserService;
import com.bittercode.service.impl.UserServiceImpl;
import com.bittercode.util.StoreUtil;

public class LogoutServlet extends HttpServlet {

    UserService authService = new UserServiceImpl();

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        PrintWriter pw = res.getWriter();
        res.setContentType("text/html; charset=UTF-8");
        try {

            // Save user's current cart before session invalidation
            String username = StoreUtil.getCustomerUsername(req.getSession());
            if (username != null) {
                StoreUtil.saveUserCart(req.getSession(), username);
            }

            boolean logout = authService.logout(req.getSession());

            RequestDispatcher rd = req.getRequestDispatcher("CustomerLogin.html");
            rd.include(req, res);
//            StoreUtil.setActiveTab(pw, "logout");
            if (logout) {
                pw.println("<div class='bookshelf-auth-wrap' style='margin-top:-20px; margin-bottom:20px;'><div class='alert alert-success text-center' style='border-radius:12px;'>Đăng xuất thành công!</div></div>");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}