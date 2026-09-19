package servlets;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bittercode.constant.BookStoreConstants;
import com.bittercode.constant.db.UsersDBConstants;
import com.bittercode.model.User;
import com.bittercode.model.UserRole;
import com.bittercode.service.UserService;
import com.bittercode.service.impl.UserServiceImpl;
import com.bittercode.util.StoreUtil;

public class CustomerLoginServlet extends HttpServlet {

    UserService authService = new UserServiceImpl();

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        res.setContentType("text/html; charset=UTF-8");
        String uName = req.getParameter(UsersDBConstants.COLUMN_USERNAME);
        String pWord = req.getParameter(UsersDBConstants.COLUMN_PASSWORD);

        try {
            User user = authService.login(UserRole.CUSTOMER, uName, pWord, req.getSession());
            if (user != null) {
                // Restore customer cart items from persistent storage (database & cache)
                StoreUtil.restoreUserCart(req.getSession(), user.getUsername());
                res.sendRedirect("viewbook");
            } else {
                PrintWriter pw = res.getWriter();
                RequestDispatcher rd = req.getRequestDispatcher("CustomerLogin.html");
                rd.include(req, res);
                pw.println("<div class='bookshelf-auth-wrap' style='margin-top:-20px; margin-bottom:20px;'><div class='alert alert-danger text-center' style='border-radius:12px;'>Tên đăng nhập hoặc mật khẩu không chính xác. Vui lòng thử lại!</div></div>");
            }
        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter pw = res.getWriter();
            RequestDispatcher rd = req.getRequestDispatcher("CustomerLogin.html");
            rd.include(req, res);
            pw.println("<div class='bookshelf-auth-wrap' style='margin-top:-20px; margin-bottom:20px;'><div class='alert alert-danger text-center' style='border-radius:12px;'>Đã xảy ra lỗi trong quá trình đăng nhập. Vui lòng thử lại!</div></div>");
        }
    }

}