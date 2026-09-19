package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.bittercode.model.Book;
import com.bittercode.model.UserRole;
import com.bittercode.service.BookService;
import com.bittercode.service.impl.BookServiceImpl;
import com.bittercode.util.StoreUtil;

public class ViewBookServlet extends HttpServlet {

    BookService bookService = new BookServiceImpl();

    private static final String[] COVER_COLORS = {
        "#047857", // vibrant emerald
        "#be123c", // royal ruby crimson
        "#1d4ed8", // sapphire cobalt blue
        "#b45309", // golden amber leather
        "#6d28d9", // royal amethyst violet
        "#0f766e", // deep ocean teal
        "#c2410c", // fiery terracotta
        "#1e293b"  // sleek midnight slate
    };

    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        PrintWriter pw = res.getWriter();
        res.setContentType("text/html; charset=UTF-8");

        // Check if customer or seller is logged in, or else return to login page
        if (!StoreUtil.isLoggedIn(UserRole.CUSTOMER, req.getSession()) && !StoreUtil.isLoggedIn(UserRole.SELLER, req.getSession())) {
            RequestDispatcher rd = req.getRequestDispatcher("CustomerLogin.html");
            rd.include(req, res);
            pw.println("<div class='bookshelf-auth-wrap' style='margin-top:-20px; margin-bottom:20px;'><div class='alert alert-warning text-center' style='border-radius:12px;'>Vui lòng đăng nhập để tiếp tục!</div></div>");
            return;
        }

        try {
            // Post-Redirect-Get pattern: If POST with selectedBookId, update cart and redirect to clean GET viewbook
            if ("POST".equalsIgnoreCase(req.getMethod()) && req.getParameter("selectedBookId") != null) {
                StoreUtil.updateCartItems(req);
                res.sendRedirect("viewbook");
                return;
            }

            // Add or Remove items from the cart, if requested
            StoreUtil.updateCartItems(req);

            // Read All available books from the database
            List<Book> books = bookService.getAllBooks();

            // Select matching header based on logged in role
            String homeHeader = StoreUtil.isLoggedIn(UserRole.SELLER, req.getSession()) ? "SellerHome.html" : "CustomerHome.html";
            RequestDispatcher rd = req.getRequestDispatcher(homeHeader);
            rd.include(req, res);

            HttpSession session = req.getSession();

            // Set Available Books tab as active and render cart count badge
            StoreUtil.setActiveTab(pw, "books", session);

            // Featured Book data (spotlight the first book if available)
            String featuredTitle = "The Last Thing He Told Me";
            String featuredAuthor = "Laura Dave";
            String featuredColor = "#255e50";
            if (books != null && !books.isEmpty()) {
                Book fBook = books.get(0);
                featuredTitle = fBook.getName();
                featuredAuthor = fBook.getAuthor();
                featuredColor = getBookColor(fBook.getBarcode());
            }

            pw.println("<main class=\"bookshelf-canvas\" style=\"padding-top:20px;\">");
            pw.println("  <section class=\"bookshelf-board\" aria-label=\"Online Book Store Shelves\">");
            pw.println("    <div class=\"bookshelf-stage\">");

            // --- Upper Shelf (Hero / Featured / Author / Audio Player) ---
            pw.println("      <section class=\"top-shelf\">");
            pw.println("        <div class=\"hero-copy\">");
            pw.println("          <h1>Sách Mới &amp;<br>Thịnh Hành</h1>");
            pw.println("          <p>Khám phá thế giới tri thức từ các tác giả nổi tiếng.</p>");
            pw.println("          <div class=\"pill-search-input\">");
            pw.println("            <span>&#9906;</span>");
            pw.println("            <input type=\"search\" id=\"bookSearchInput\" placeholder=\"Tìm kiếm tựa sách, tác giả, chủ đề...\" onkeyup=\"filterBooks()\">");
            pw.println("          </div>");
            pw.println("        </div>");

            pw.println("        <div class=\"book-standing-3d featured-book\" style=\"--book-color:" + featuredColor + "\">");
            pw.println("          <span class=\"book-kicker\">Tác Phẩm Nổi Bật</span>");
            pw.println("          <span class=\"book-title\">" + escapeHtml(featuredTitle) + "</span>");
            pw.println("          <span class=\"book-author\">" + escapeHtml(featuredAuthor) + "</span>");
            pw.println("        </div>");

            pw.println("        <span class=\"vertical-label\">Tác Giả Nổi Bật</span>");
            pw.println("        <article class=\"author-card\">");
            pw.println("          <div class=\"author-card__header\">Stephen King<strong>Tuyển Tập</strong></div>");
            pw.println("          <div class=\"author-card__portrait\"></div>");
            pw.println("        </article>");

            // Secondary Featured Book (spotlight the second book if available)
            String secondTitle = "C++ Primer";
            String secondAuthor = "Stanley Lippman";
            String secondColor = "#1c3d5a";
            if (books != null && books.size() > 1) {
                Book sBook = books.get(1);
                secondTitle = sBook.getName();
                secondAuthor = sBook.getAuthor();
                secondColor = getBookColor(sBook.getBarcode());
            }

            pw.println("        <span class=\"vertical-label\">Biên Tập Viên Chọn</span>");
            pw.println("        <div class=\"book-standing-3d featured-book-secondary\" style=\"--book-color:" + secondColor + "\">");
            pw.println("          <span class=\"book-kicker\">Gợi Ý Hàng Đầu</span>");
            pw.println("          <span class=\"book-title\">" + escapeHtml(secondTitle) + "</span>");
            pw.println("          <span class=\"book-author\">" + escapeHtml(secondAuthor) + "</span>");
            pw.println("        </div>");
            pw.println("      </section>");

            // --- 3D Shelf Divider ---
            pw.println("      <div class=\"shelf-divider\"></div>");

            // --- Lower Shelf (Recent Bestsellers Grid) ---
            pw.println("      <section class=\"bottom-shelf-layout\">");
            pw.println("        <span class=\"vertical-label\">Sách Bán Chạy Nhất</span>");
            pw.println("        <div class=\"bottom-shelf-books\" id=\"bookshelfContainer\">");

            if (books != null && !books.isEmpty()) {
                for (Book book : books) {
                    pw.println(this.addBookToCard(session, book));
                }
            } else {
                pw.println("<p style='grid-column: 1/-1; padding: 40px; text-align: center; color: var(--text-secondary);'>Hiện chưa có cuốn sách nào trong danh mục.</p>");
            }

            pw.println("        </div>");
            pw.println("      </section>");

            // --- Bottom Checkout Bar ---
            pw.println("      <footer class=\"bottom-shelf-footer\">");
            pw.println("        <span class=\"shelf-note\">Hiển thị các đầu sách tuyển chọn trực tiếp trên kệ</span>");
            pw.println("        <form action=\"cart\" method=\"post\">");
            pw.println("          <button type=\"submit\" class=\"btn-checkout-shelf\" name=\"cart\">Xem giỏ hàng &amp; Thanh toán &rarr;</button>");
            pw.println("        </form>");
            pw.println("      </footer>");

            pw.println("    </div>");
            pw.println("  </section>");
            pw.println("</main>");

            // Instant Client-Side Filter Script
            pw.println("<script>");
            pw.println("function filterBooks() {");
            pw.println("  var query = document.getElementById('bookSearchInput').value.toLowerCase();");
            pw.println("  var cards = document.querySelectorAll('.book-listing');");
            pw.println("  cards.forEach(function(card) {");
            pw.println("    var text = card.textContent.toLowerCase();");
            pw.println("    card.style.display = text.indexOf(query) !== -1 ? 'grid' : 'none';");
            pw.println("  });");
            pw.println("}");
            pw.println("</script>");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String addBookToCard(HttpSession session, Book book) {
        String bCode = book.getBarcode();
        int bQty = book.getQuantity();
        String bookColor = getBookColor(bCode);

        // Quantity of current book in cart
        int cartItemQty = 0;
        if (session.getAttribute("qty_" + bCode) != null) {
            cartItemQty = (int) session.getAttribute("qty_" + bCode);
        }

        // Button / Stepper to add/remove item from cart
        String button = "";
        if (bQty > 0) {
            if (cartItemQty == 0) {
                button = "<form action=\"viewbook\" method=\"post\" style='margin:0;'>"
                        + "<input type='hidden' name='selectedBookId' value='" + bCode + "'>"
                        + "<input type='hidden' name='qty_" + bCode + "' value='1'/>"
                        + "<button type='submit' class=\"btn-pill-buy\" name='addToCart'>Chọn mua</button>"
                        + "</form>";
            } else {
                button = "<form method='post' action='viewbook' class='cart-stepper'>"
                        + "<button type='submit' name='removeFromCart' class=\"stepper-btn minus\" title=\"Bớt một\">&minus;</button>"
                        + "<input type='hidden' name='selectedBookId' value='" + bCode + "'/>"
                        + "<span class='stepper-qty'>" + cartItemQty + "</span>"
                        + "<button type='submit' name='addToCart' class=\"stepper-btn plus\" title=\"Thêm một\">&plus;</button>"
                        + "</form>";
            }
        } else {
            button = "<span class=\"badge-out-of-stock\">Tạm hết hàng</span>";
        }

        String[] kickers = {"BÁN CHẠY", "MỚI NHẤT", "GỢI Ý", "NỔI BẬT", "ĐẶC SẮC"};
        String kicker = kickers[Math.abs(bCode.hashCode()) % kickers.length];

        return "<article class=\"book-listing\">"
                + "  <div class=\"book-standing-3d\" style=\"--book-color:" + bookColor + "\">"
                + "    <div class=\"book-sheen-glare\"></div>"
                + "    <span class=\"book-kicker\">" + kicker + "</span>"
                + "    <span class=\"book-title\">" + escapeHtml(book.getName()) + "</span>"
                + "    <span class=\"book-author\">" + escapeHtml(book.getAuthor()) + "</span>"
                + "  </div>"
                + "  <div class=\"listing-copy\">"
                + "    <div style=\"display:flex; align-items:center; gap:8px; margin-bottom:4px;\">"
                + "      <div class=\"stars\">&#9733;&#9733;&#9733;&#9733;&#9733;</div>"
                + "      <span class=\"badge-tag-star\" style=\"font-size:0.62rem; padding:1px 6px;\">4.9</span>"
                + "    </div>"
                + "    <h3 title=\"" + escapeHtml(book.getName()) + "\">" + escapeHtml(book.getName()) + "</h3>"
                + "    <p class=\"author-text\">" + escapeHtml(book.getAuthor()) + "</p>"
                + "    <div class=\"price-tag\">" + StoreUtil.formatPrice(book.getPrice()) + "</div>"
                + button
                + "  </div>"
                + "</article>";
    }

    private String getBookColor(String barcode) {
        if (barcode == null || barcode.isEmpty()) {
            return COVER_COLORS[0];
        }
        int index = Math.abs(barcode.hashCode()) % COVER_COLORS.length;
        return COVER_COLORS[index];
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }
}
