package servlets;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bittercode.constant.ResponseCode;
import com.bittercode.constant.db.UsersDBConstants;
import com.bittercode.model.User;
import com.bittercode.model.UserRole;
import com.bittercode.service.UserService;
import com.bittercode.service.impl.UserServiceImpl;

public class CustomerRegisterServlet extends HttpServlet {

    private UserService userService = new UserServiceImpl();

    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        // If accessed directly via GET, redirect to register page
        if ("GET".equalsIgnoreCase(req.getMethod())) {
            res.sendRedirect("CustomerRegister.html");
            return;
        }

        String uName   = req.getParameter(UsersDBConstants.COLUMN_USERNAME);
        String pWord   = req.getParameter(UsersDBConstants.COLUMN_PASSWORD);
        String fName   = req.getParameter(UsersDBConstants.COLUMN_FIRSTNAME);
        String lName   = req.getParameter(UsersDBConstants.COLUMN_LASTNAME);
        String addr    = req.getParameter(UsersDBConstants.COLUMN_ADDRESS);
        String phNoRaw = req.getParameter(UsersDBConstants.COLUMN_PHONE);
        String mailId  = req.getParameter(UsersDBConstants.COLUMN_MAILID);

        // Validate required fields before any processing
        if (mailId == null || mailId.trim().isEmpty()
                || pWord == null || pWord.trim().isEmpty()
                || fName == null || fName.trim().isEmpty()
                || lName == null || lName.trim().isEmpty()) {
            redirectWithError(res, "Vui lòng điền đầy đủ các thông tin bắt buộc.");
            return;
        }

        // Sanitize phone number: strip non-digits so "+84 912 345 678" -> 84912345678
        long phone = 0;
        if (phNoRaw != null) {
            String digitsOnly = phNoRaw.replaceAll("[^0-9]", "");
            if (!digitsOnly.isEmpty()) {
                try {
                    phone = Long.parseLong(digitsOnly);
                } catch (NumberFormatException ex) {
                    phone = 0;
                }
            }
        }

        User user = new User();
        user.setEmailId(mailId.trim());
        user.setUsername((uName != null && !uName.trim().isEmpty()) ? uName.trim() : mailId.trim());
        user.setFirstName(fName.trim());
        user.setLastName(lName.trim());
        user.setPassword(pWord);
        user.setPhone(phone);
        user.setAddress(addr != null ? addr.trim() : "");

        try {
            String respCode = userService.register(UserRole.CUSTOMER, user);
            System.out.println("Registration response: " + respCode);
            if (ResponseCode.SUCCESS.name().equalsIgnoreCase(respCode)) {
                // SUCCESS: redirect to login page with registered flag
                res.sendRedirect("CustomerLogin.html?registered=true");
            } else {
                // FAILURE: redirect back to register page with error message
                String friendlyMsg = (respCode != null && respCode.contains("already registered"))
                        ? "Email hoặc tên đăng nhập này đã được sử dụng. Vui lòng chọn tài khoản khác!"
                        : "Đăng ký thất bại. Vui lòng kiểm tra lại thông tin và thử lại.";
                redirectWithError(res, friendlyMsg);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            redirectWithError(res, "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.");
        }
    }

    private void redirectWithError(HttpServletResponse res, String message) throws IOException {
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8.toString());
        res.sendRedirect("CustomerRegister.html?error=" + encoded);
    }
}