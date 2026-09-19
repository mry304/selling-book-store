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

public class SellerLoginServlet extends HttpServlet {

    UserService userService = new UserServiceImpl();

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        res.setContentType("text/html; charset=UTF-8");
        String uName = req.getParameter(UsersDBConstants.COLUMN_USERNAME);
        String pWord = req.getParameter(UsersDBConstants.COLUMN_PASSWORD);
        try {
            User user = userService.login(UserRole.SELLER, uName, pWord, req.getSession());
            if (user != null) {
                res.sendRedirect("statistics");
            } else {
                PrintWriter pw = res.getWriter();
                RequestDispatcher rd = req.getRequestDispatcher("SellerLogin.html");
                rd.include(req, res);
                pw.println("<div class='bookshelf-auth-wrap' style='margin-top:-20px; margin-bottom:20px;'><div class='alert alert-danger text-center' style='border-radius:12px;'>Tên đăng nhập hoặc mật khẩu không chính xác. Vui lòng thử lại!</div></div>");
            }

        } catch (Exception e) {
            e.printStackTrace();
            PrintWriter pw = res.getWriter();
            RequestDispatcher rd = req.getRequestDispatcher("SellerLogin.html");
            rd.include(req, res);
            pw.println("<div class='bookshelf-auth-wrap' style='margin-top:-20px; margin-bottom:20px;'><div class='alert alert-danger text-center' style='border-radius:12px;'>Lỗi cơ sở dữ liệu: Không thể kết nối đến MySQL. Vui lòng kiểm tra dịch vụ MySQL!</div></div>");
        }
    }
}