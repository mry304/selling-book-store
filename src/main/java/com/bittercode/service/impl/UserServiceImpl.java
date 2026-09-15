package com.bittercode.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.servlet.http.HttpSession;

import com.bittercode.constant.ResponseCode;
import com.bittercode.constant.db.UsersDBConstants;
import com.bittercode.model.StoreException;
import com.bittercode.model.User;
import com.bittercode.model.UserRole;
import com.bittercode.service.UserService;
import com.bittercode.util.DBUtil;

public class UserServiceImpl implements UserService {

    private static final String registerUserQuery = "INSERT INTO " + UsersDBConstants.TABLE_USERS + " ("
            + UsersDBConstants.COLUMN_USERNAME + ", "
            + UsersDBConstants.COLUMN_PASSWORD + ", "
            + UsersDBConstants.COLUMN_FIRSTNAME + ", "
            + UsersDBConstants.COLUMN_LASTNAME + ", "
            + UsersDBConstants.COLUMN_ADDRESS + ", "
            + UsersDBConstants.COLUMN_PHONE + ", "
            + UsersDBConstants.COLUMN_MAILID + ", "
            + UsersDBConstants.COLUMN_USERTYPE + ") VALUES(?,?,?,?,?,?,?,?)";

    private static final String loginUserQuery = "SELECT * FROM " + UsersDBConstants.TABLE_USERS + " WHERE ("
            + UsersDBConstants.COLUMN_USERNAME + "=? OR " + UsersDBConstants.COLUMN_MAILID + "=?) AND "
            + UsersDBConstants.COLUMN_PASSWORD + "=? AND "
            + UsersDBConstants.COLUMN_USERTYPE + "=?";

    @Override
    public User login(UserRole role, String email, String password, HttpSession session) throws StoreException {
        User user = null;
        String userType = UserRole.SELLER.equals(role) ? "1" : "2";

        Connection con = DBUtil.getConnection();
        try (PreparedStatement ps = con.prepareStatement(loginUserQuery)) {

            ps.setString(1, email);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, userType);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user = new User();
                    user.setUsername(rs.getString(UsersDBConstants.COLUMN_USERNAME));
                    user.setFirstName(rs.getString(UsersDBConstants.COLUMN_FIRSTNAME));
                    user.setLastName(rs.getString(UsersDBConstants.COLUMN_LASTNAME));
                    user.setPhone(rs.getLong(UsersDBConstants.COLUMN_PHONE));
                    user.setEmailId(rs.getString(UsersDBConstants.COLUMN_MAILID));
                    user.setPassword(password);

                    if (session != null) {
                        session.setAttribute(role.toString(), user.getEmailId());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new StoreException("Lỗi đăng nhập: " + e.getMessage());
        }
        return user;
    }

    @Override
    public boolean isLoggedIn(UserRole role, HttpSession session) {
        if (session == null) return false;
        if (role == null) role = UserRole.CUSTOMER;
        return session.getAttribute(role.toString()) != null;
    }

    @Override
    public boolean logout(HttpSession session) {
        if (session != null) {
            session.removeAttribute(UserRole.CUSTOMER.toString());
            session.removeAttribute(UserRole.SELLER.toString());
            session.invalidate();
        }
        return true;
    }

    @Override
    public String register(UserRole role, User user) throws StoreException {
        String responseMessage = ResponseCode.FAILURE.name();
        int userType = UserRole.SELLER.equals(role) ? 1 : 2;

        Connection con = DBUtil.getConnection();
        try (PreparedStatement ps = con.prepareStatement(registerUserQuery)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFirstName());
            ps.setString(4, user.getLastName());
            ps.setString(5, user.getAddress());
            ps.setString(6, user.getPhone() != null ? String.valueOf(user.getPhone()) : "0");
            ps.setString(7, user.getEmailId());
            ps.setInt(8, userType);

            int k = ps.executeUpdate();
            if (k > 0) {
                responseMessage = ResponseCode.SUCCESS.name();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg != null && (msg.contains("Duplicate") || msg.contains("PRIMARY"))) {
                responseMessage = "User already registered with this username or email !!";
            } else {
                responseMessage = ResponseCode.FAILURE.name() + " : " + msg;
            }
        }
        return responseMessage;
    }
}