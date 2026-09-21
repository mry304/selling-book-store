package com.bittercode.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.bittercode.constant.ResponseCode;
import com.bittercode.constant.db.BooksDBConstants;
import com.bittercode.model.Book;
import com.bittercode.model.StoreException;
import com.bittercode.service.BookService;
import com.bittercode.util.DBUtil;

public class BookServiceImpl implements BookService {

    private static final String getAllBooksQuery = "SELECT * FROM " + BooksDBConstants.TABLE_BOOK;
    private static final String getBookByIdQuery = "SELECT * FROM " + BooksDBConstants.TABLE_BOOK
            + " WHERE " + BooksDBConstants.COLUMN_BARCODE + " = ?";

    private static final String deleteBookByIdQuery = "DELETE FROM " + BooksDBConstants.TABLE_BOOK + "  WHERE "
            + BooksDBConstants.COLUMN_BARCODE + "=?";

    private static final String addBookQuery = "INSERT INTO " + BooksDBConstants.TABLE_BOOK + " (barcode, name, author, price, quantity) VALUES(?,?,?,?,?)";

    private static final String updateBookQtyByIdQuery = "UPDATE " + BooksDBConstants.TABLE_BOOK + " SET "
            + BooksDBConstants.COLUMN_QUANTITY + "=? WHERE " + BooksDBConstants.COLUMN_BARCODE
            + "=?";

    private static final String updateBookByIdQuery = "UPDATE " + BooksDBConstants.TABLE_BOOK + " SET "
            + BooksDBConstants.COLUMN_NAME + "=? , "
            + BooksDBConstants.COLUMN_AUTHOR + "=?, "
            + BooksDBConstants.COLUMN_PRICE + "=?, "
            + BooksDBConstants.COLUMN_QUANTITY + "=? "
            + "  WHERE " + BooksDBConstants.COLUMN_BARCODE
            + "=?";

    @Override
    public Book getBookById(String bookId) throws StoreException {
        Book book = null;
        Connection con = DBUtil.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement(getBookByIdQuery);
            ps.setString(1, bookId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String bCode = rs.getString(1);
                String bName = rs.getString(2);
                String bAuthor = rs.getString(3);
                int bPrice = rs.getInt(4);
                int bQty = rs.getInt(5);

                book = new Book(bCode, bName, bAuthor, bPrice, bQty);
            }
        } catch (SQLException e) {

        }
        return book;
    }

    @Override
    public List<Book> getAllBooks() throws StoreException {
        List<Book> books = new ArrayList<Book>();
        Connection con = DBUtil.getConnection();

        try {
            PreparedStatement ps = con.prepareStatement(getAllBooksQuery);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String bCode = rs.getString(1);
                String bName = rs.getString(2);
                String bAuthor = rs.getString(3);
                int bPrice = rs.getInt(4);
                int bQty = rs.getInt(5);

                Book book = new Book(bCode, bName, bAuthor, bPrice, bQty);
                books.add(book);
            }
        } catch (SQLException e) {

        }
        return books;
    }

    @Override
    public List<Book> getBooksPage(int page, int pageSize) throws StoreException {
        List<Book> books = new ArrayList<Book>();
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        String query = getAllBooksQuery + " ORDER BY " + BooksDBConstants.COLUMN_NAME + " ASC LIMIT ? OFFSET ?";

        try (PreparedStatement ps = DBUtil.getConnection().prepareStatement(query)) {
            ps.setInt(1, safePageSize);
            ps.setInt(2, (safePage - 1) * safePageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    books.add(new Book(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getInt(5)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    @Override
    public int getBookCount() throws StoreException {
        String query = "SELECT COUNT(*) FROM " + BooksDBConstants.TABLE_BOOK;
        try (PreparedStatement ps = DBUtil.getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public List<Book> getBooksByCategory(String category, int page, int pageSize) throws StoreException {
        String normalized = normalizeCategory(category);
        if ("ALL".equals(normalized)) return getBooksPage(page, pageSize);

        String query;
        if ("BESTSELLER".equals(normalized)) {
            query = "SELECT b.barcode, b.name, b.author, b.price, b.quantity FROM books b "
                    + "JOIN order_details od ON od.book_barcode = b.barcode "
                    + "JOIN orders o ON o.order_id = od.order_id AND o.status = 'COMPLETED' "
                    + "GROUP BY b.barcode, b.name, b.author, b.price, b.quantity "
                    + "ORDER BY SUM(od.quantity) DESC, MAX(o.order_date) DESC LIMIT ? OFFSET ?";
        } else if ("NEW".equals(normalized)) {
            query = "SELECT barcode, name, author, price, quantity FROM books ORDER BY created_at DESC, barcode DESC LIMIT ? OFFSET ?";
        } else {
            query = "SELECT barcode, name, author, price, quantity FROM books WHERE quantity > 0 ORDER BY quantity DESC, created_at DESC LIMIT ? OFFSET ?";
        }
        return executeBookPageQuery(query, page, pageSize);
    }

    @Override
    public int getBookCountByCategory(String category) throws StoreException {
        String normalized = normalizeCategory(category);
        if ("ALL".equals(normalized)) return getBookCount();
        String query;
        if ("BESTSELLER".equals(normalized)) {
            query = "SELECT COUNT(DISTINCT b.barcode) FROM books b JOIN order_details od ON od.book_barcode = b.barcode "
                    + "JOIN orders o ON o.order_id = od.order_id AND o.status = 'COMPLETED'";
        } else if ("NEW".equals(normalized)) {
            query = "SELECT COUNT(*) FROM books";
        } else {
            query = "SELECT COUNT(*) FROM books WHERE quantity > 0";
        }
        try (PreparedStatement ps = DBUtil.getConnection().prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private List<Book> executeBookPageQuery(String query, int page, int pageSize) throws StoreException {
        List<Book> books = new ArrayList<>();
        try (PreparedStatement ps = DBUtil.getConnection().prepareStatement(query)) {
            ps.setInt(1, Math.max(1, pageSize));
            ps.setInt(2, (Math.max(1, page) - 1) * Math.max(1, pageSize));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) books.add(new Book(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getInt(5)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    private String normalizeCategory(String category) {
        if ("BESTSELLER".equalsIgnoreCase(category) || "FEATURED".equalsIgnoreCase(category) || "NEW".equalsIgnoreCase(category)) return category.trim().toUpperCase();
        return "ALL";
    }

    @Override
    public String deleteBookById(String bookId) throws StoreException {
        String response = ResponseCode.FAILURE.name();
        Connection con = DBUtil.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement(deleteBookByIdQuery);
            ps.setString(1, bookId);
            int k = ps.executeUpdate();
            if (k == 1) {
                response = ResponseCode.SUCCESS.name();
            }
        } catch (Exception e) {
            response += " : " + e.getMessage();
            e.printStackTrace();
        }
        return response;
    }

    @Override
    public String addBook(Book book) throws StoreException {
        String responseCode = ResponseCode.FAILURE.name();
        Connection con = DBUtil.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement(addBookQuery);
            ps.setString(1, book.getBarcode());
            ps.setString(2, book.getName());
            ps.setString(3, book.getAuthor());
            ps.setDouble(4, book.getPrice());
            ps.setInt(5, book.getQuantity());
            int k = ps.executeUpdate();
            if (k == 1) {
                responseCode = ResponseCode.SUCCESS.name();
            }
        } catch (Exception e) {
            responseCode += " : " + e.getMessage();
            e.printStackTrace();
        }
        return responseCode;
    }

    @Override
    public String updateBookQtyById(String bookId, int quantity) throws StoreException {
        String responseCode = ResponseCode.FAILURE.name();
        Connection con = DBUtil.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement(updateBookQtyByIdQuery);
            ps.setInt(1, quantity);
            ps.setString(2, bookId);
            ps.executeUpdate();
            responseCode = ResponseCode.SUCCESS.name();
        } catch (Exception e) {
            responseCode += " : " + e.getMessage();
            e.printStackTrace();
        }
        return responseCode;
    }

    @Override
    public List<Book> getBooksByCommaSeperatedBookIds(String commaSeperatedBookIds) throws StoreException {
        List<Book> books = new ArrayList<Book>();
        if (commaSeperatedBookIds == null || commaSeperatedBookIds.trim().isEmpty()) {
            return books;
        }
        String[] ids = commaSeperatedBookIds.split(",");
        List<String> validIds = new ArrayList<>();
        for (String id : ids) {
            String trimmed = id.trim().replace("'", "");
            if (!trimmed.isEmpty()) {
                validIds.add("'" + trimmed + "'");
            }
        }
        if (validIds.isEmpty()) {
            return books;
        }

        Connection con = DBUtil.getConnection();
        try {
            String getBooksByCommaSeperatedBookIdsQuery = "SELECT * FROM " + BooksDBConstants.TABLE_BOOK
                    + " WHERE " + BooksDBConstants.COLUMN_BARCODE + " IN ( " + String.join(",", validIds) + " )";
            PreparedStatement ps = con.prepareStatement(getBooksByCommaSeperatedBookIdsQuery);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String bCode = rs.getString(1);
                String bName = rs.getString(2);
                String bAuthor = rs.getString(3);
                int bPrice = rs.getInt(4);
                int bQty = rs.getInt(5);

                Book book = new Book(bCode, bName, bAuthor, bPrice, bQty);
                books.add(book);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    @Override
    public String updateBook(Book book) throws StoreException {
        String responseCode = ResponseCode.FAILURE.name();
        Connection con = DBUtil.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement(updateBookByIdQuery);
            ps.setString(1, book.getName());
            ps.setString(2, book.getAuthor());
            ps.setDouble(3, book.getPrice());
            ps.setInt(4, book.getQuantity());
            ps.setString(5, book.getBarcode());
            ps.executeUpdate();
            responseCode = ResponseCode.SUCCESS.name();
        } catch (Exception e) {
            responseCode += " : " + e.getMessage();
            e.printStackTrace();
        }
        return responseCode;
    }

}
