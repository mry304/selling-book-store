<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, java.util.Map" %>

<!-- Include Admin Left Sidebar -->
<jsp:include page="SellerHome.html" />

<script>
    // Ensure Statistics tab is marked active
    if (typeof activeTab !== 'undefined') {
        activeTab = 'statistics';
    }
    var statTab = document.getElementById('statistics');
    if (statTab) {
        statTab.classList.add('active');
    }
</script>

<!-- Chart.js CDN -->
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>

<main class="bookshelf-canvas" style="padding-top: 26px;">
  <div class="bookshelf-page-container" style="max-width: 1340px; margin: 0 auto;">

    <!-- Dashboard Top Bar -->
    <div class="dashboard-topbar-flex">
      <div class="dashboard-title-box">
        <span class="dashboard-pill-tag">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
          Phân tích Điều hành
        </span>
        <h1>Thống kê & Hiệu quả Cửa hàng</h1>
        <p>Theo dõi giao dịch trực tiếp, biểu đồ doanh thu và giám sát kho sách.</p>
      </div>

      <div style="display:flex; gap:12px; align-items:center; flex-wrap:wrap;">
        <span style="background:#fff; border:1px solid rgba(197,137,64,0.25); border-radius:var(--radius-pill); padding:8px 16px; font-size:0.85rem; font-weight:600; color:var(--text-secondary); box-shadow:0 2px 8px rgba(0,0,0,0.03);">
          📅 <%= new java.text.SimpleDateFormat("EEEE, dd/MM/yyyy", new java.util.Locale("vi", "VN")).format(new java.util.Date()) %>
        </span>
        <a href="storebooks" class="nav-pill-btn" style="background:#fff; font-size:0.86rem; padding:8px 18px;">Danh mục</a>
        <a href="addbook" class="nav-pill-btn" style="background:var(--accent-primary); color:#fff; border-color:var(--accent-primary); font-size:0.86rem; padding:8px 18px;">+ Thêm sách</a>
      </div>
    </div>

    <!-- KPI Cards Grid -->
    <div class="stats-kpi-grid">
      <!-- Card 1: Revenue -->
      <div class="stat-kpi-card">
        <div class="stat-kpi-header">
          <span class="stat-kpi-label">Tổng Doanh Thu</span>
          <div class="stat-kpi-icon-box icon-emerald">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="1" x2="12" y2="23"></line><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path></svg>
          </div>
        </div>
        <div class="stat-kpi-value">
          <%= String.format("%,.0f đ", (Double)request.getAttribute("totalRevenue")) %>
        </div>
        <div class="stat-kpi-footer">
          <span class="stat-trend-badge trend-up">▲ Hoạt động</span>
          <span>Từ các đơn hoàn tất</span>
        </div>
      </div>

      <!-- Card 2: Total Orders -->
      <div class="stat-kpi-card">
        <div class="stat-kpi-header">
          <span class="stat-kpi-label">Tổng Đơn Hàng</span>
          <div class="stat-kpi-icon-box icon-blue">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path><line x1="3" y1="6" x2="21" y2="6"></line><path d="M16 10a4 4 0 0 1-8 0"></path></svg>
          </div>
        </div>
        <div class="stat-kpi-value">
          <%= request.getAttribute("totalOrders") %>
        </div>
        <div class="stat-kpi-footer">
          <span class="stat-trend-badge trend-up">▲ Đơn hàng</span>
          <span>Khách đã thanh toán</span>
        </div>
      </div>

      <!-- Card 3: Books Sold -->
      <div class="stat-kpi-card">
        <div class="stat-kpi-header">
          <span class="stat-kpi-label">Số Sách Đã Bán</span>
          <div class="stat-kpi-icon-box icon-purple">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
          </div>
        </div>
        <div class="stat-kpi-value">
          <%= request.getAttribute("totalBooksSold") %> <span style="font-size:0.9rem; font-weight:600; color:var(--text-secondary);">cuốn</span>
        </div>
        <div class="stat-kpi-footer">
          <span class="stat-trend-badge trend-neutral">&bull; Sản lượng</span>
          <span>Tổng số bản đã xuất</span>
        </div>
      </div>

      <!-- Card 4: Customers -->
      <div class="stat-kpi-card">
        <div class="stat-kpi-header">
          <span class="stat-kpi-label">Khách Hàng</span>
          <div class="stat-kpi-icon-box icon-amber">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
          </div>
        </div>
        <div class="stat-kpi-value">
          <%= request.getAttribute("totalCustomers") %>
        </div>
        <div class="stat-kpi-footer">
          <span class="stat-trend-badge trend-up">▲ Độc giả</span>
          <span>Tài khoản đã đăng ký</span>
        </div>
      </div>

      <!-- Card 5: Inventory Stock -->
      <div class="stat-kpi-card">
        <div class="stat-kpi-header">
          <span class="stat-kpi-label">Tồn Kho Hiện Tại</span>
          <div class="stat-kpi-icon-box icon-teal">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>
          </div>
        </div>
        <div class="stat-kpi-value">
          <%= request.getAttribute("totalStock") %> <span style="font-size:0.9rem; font-weight:600; color:var(--text-secondary);">cuốn</span>
        </div>
        <div class="stat-kpi-footer">
          <span class="stat-trend-badge trend-neutral"><%= request.getAttribute("totalTitles") %> Đầu sách</span>
          <span>Đang bày bán trong kho</span>
        </div>
      </div>
    </div>

    <!-- Charts Row 1: Revenue Timeline & Order Status -->
    <div class="stats-charts-row">
      <!-- Area Chart: Revenue Trend -->
      <div class="chart-card">
        <div class="chart-card-header">
          <div>
            <h3 class="chart-card-title">Biểu đồ Tăng trưởng Doanh thu</h3>
            <p class="chart-card-subtitle">Doanh thu và xu hướng giao dịch theo các mốc thời gian gần nhất</p>
          </div>
          <div style="display:flex; gap:8px;">
            <span style="font-size:0.75rem; font-weight:700; background:rgba(197,137,64,0.14); color:var(--accent-hover); padding:4px 10px; border-radius:var(--radius-pill);">
              ● Doanh thu (đ)
            </span>
          </div>
        </div>
        <div class="chart-box-relative">
          <canvas id="revenueSplineChart"></canvas>
        </div>
      </div>

      <!-- Doughnut Chart: Order Status Breakdown -->
      <div class="chart-card">
        <div class="chart-card-header">
          <div>
            <h3 class="chart-card-title">Trạng thái Đơn hàng</h3>
            <p class="chart-card-subtitle">Tỷ lệ xử lý & hoàn tất</p>
          </div>
        </div>
        <div class="chart-box-relative" style="display:flex; align-items:center; justify-content:center;">
          <canvas id="orderStatusDoughnutChart" style="max-height: 270px;"></canvas>
        </div>
      </div>
    </div>

    <!-- Charts Row 2: Top Selling Books Bar Chart & Quick Metrics -->
    <div class="stats-charts-row" style="grid-template-columns: 1.6fr 1fr;">
      <!-- Bar Chart: Bestselling Books -->
      <div class="chart-card">
        <div class="chart-card-header">
          <div>
            <h3 class="chart-card-title">Top 5 Tựa Sách Bán Chạy Nhất</h3>
            <p class="chart-card-subtitle">Những đầu sách có số lượng tiêu thụ cao nhất</p>
          </div>
          <span style="font-size:0.75rem; font-weight:700; background:rgba(16,185,129,0.12); color:#059669; padding:4px 10px; border-radius:var(--radius-pill);">
            Sản Phẩm Xuất Sắc Nhất
          </span>
        </div>
        <div class="chart-box-relative">
          <canvas id="topBooksBarChart"></canvas>
        </div>
      </div>

      <!-- Quick Metrics & Conversion Health Card -->
      <div class="chart-card" style="justify-content: space-between;">
        <div>
          <div class="chart-card-header">
            <div>
              <h3 class="chart-card-title">Chỉ số Vận hành Cửa hàng</h3>
              <p class="chart-card-subtitle">Tỷ số hiệu quả kinh doanh</p>
            </div>
          </div>

          <div style="display:flex; flex-direction:column; gap:16px; margin-top:10px;">
            <% 
              double avgOrderValue = 0.0;
              int ordersCount = (Integer)request.getAttribute("totalOrders");
              double revVal = (Double)request.getAttribute("totalRevenue");
              if (ordersCount > 0) {
                  avgOrderValue = revVal / ordersCount;
              }
            %>
            <div style="padding:14px 18px; background:var(--bg-board); border-radius:12px; border:1px solid rgba(197,137,64,0.18); display:flex; justify-content:space-between; align-items:center;">
              <div>
                <div style="font-size:0.8rem; font-weight:700; color:var(--text-secondary); text-transform:uppercase;">Giá trị đơn trung bình (AOV)</div>
                <div style="font-size:1.3rem; font-weight:800; color:var(--text-primary); margin-top:2px;">
                  <%= String.format("%,.0f đ", avgOrderValue) %>
                </div>
              </div>
              <div style="font-size:1.5rem;">💳</div>
            </div>

            <div style="padding:14px 18px; background:var(--bg-board); border-radius:12px; border:1px solid rgba(197,137,64,0.18); display:flex; justify-content:space-between; align-items:center;">
              <div>
                <div style="font-size:0.8rem; font-weight:700; color:var(--text-secondary); text-transform:uppercase;">Tình trạng kho hàng</div>
                <div style="font-size:1.3rem; font-weight:800; color:#059669; margin-top:2px;">
                  <%= (Integer)request.getAttribute("totalStock") > 0 ? "Còn Hàng & Sẵn Sàng" : "Cần Nhập Thêm" %>
                </div>
              </div>
              <div style="font-size:1.5rem;">📦</div>
            </div>

            <div style="padding:14px 18px; background:var(--bg-board); border-radius:12px; border:1px solid rgba(197,137,64,0.18); display:flex; justify-content:space-between; align-items:center;">
              <div>
                <div style="font-size:0.8rem; font-weight:700; color:var(--text-secondary); text-transform:uppercase;">Hệ thống Quản trị</div>
                <div style="font-size:1.1rem; font-weight:700; color:var(--accent-hover); margin-top:2px;">
                  Đồng Bộ Dữ Liệu Trực Tiếp
                </div>
              </div>
              <div style="font-size:1.5rem;">⚡</div>
            </div>
          </div>
        </div>

        <div style="margin-top:20px; text-align:center;">
          <a href="storebooks" class="btn-auth-submit" style="text-decoration:none; padding:11px 20px; font-size:0.92rem;">
            <span>Quản Lý Kho Sách Cửa Hàng</span> &rarr;
          </a>
        </div>
      </div>
    </div>

    <!-- Tables Row: Low Stock Alerts & Recent Orders -->
    <div class="stats-tables-row">
      <!-- Low Stock Alerts Table -->
      <div class="chart-card">
        <div class="chart-card-header">
          <div>
            <h3 class="chart-card-title">⚠️ Cảnh Báo Sắp Hết Hàng (≤ 5 cuốn)</h3>
            <p class="chart-card-subtitle">Các đầu sách cần bổ sung số lượng trong kho</p>
          </div>
          <a href="addbook" class="nav-pill-btn" style="font-size:0.8rem; padding:6px 14px;">+ Nhập thêm</a>
        </div>

        <div class="table-responsive">
          <table class="table table-hover align-middle" style="margin:0; font-size:0.9rem;">
            <thead style="background:var(--shelf-surface);">
              <tr>
                <th style="padding:10px 14px;">Tựa sách</th>
                <th style="padding:10px 14px;">Đơn giá</th>
                <th style="padding:10px 14px; text-align:center;">Tồn kho</th>
                <th style="padding:10px 14px; text-align:center;">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <%
                List<Map<String, Object>> lowStock = (List<Map<String, Object>>) request.getAttribute("lowStockBooks");
                if (lowStock == null || lowStock.isEmpty()) {
              %>
                <tr>
                  <td colspan="4" class="text-center py-4" style="color:var(--text-secondary);">
                    🎉 <strong>Tuyệt vời!</strong> Tất cả các đầu sách hiện đều có lượng tồn kho dồi dào.
                  </td>
                </tr>
              <%
                } else {
                  for (Map<String, Object> b : lowStock) {
              %>
                <tr>
                  <td style="vertical-align:middle; padding:12px 14px;">
                    <div style="font-weight:700; color:var(--text-primary);"><%= b.get("name") %></div>
                    <div style="font-size:0.78rem; color:var(--text-secondary);"><%= b.get("author") %></div>
                  </td>
                  <td style="vertical-align:middle; font-weight:700; color:var(--accent-hover); padding:12px 14px;">
                    <%= String.format("%,.0f đ", (Double)b.get("price")) %>
                  </td>
                  <td style="vertical-align:middle; text-align:center; padding:12px 14px;">
                    <span class="badge-low-stock-urgent">
                      Còn <%= b.get("quantity") %> cuốn
                    </span>
                  </td>
                  <td style="vertical-align:middle; text-align:center; padding:12px 14px;">
                    <form method="post" action="updatebook" style="margin:0;">
                      <input type="hidden" name="bookId" value="<%= b.get("barcode") %>">
                      <button type="submit" class="nav-pill-btn" style="padding:5px 12px; font-size:0.8rem; background:var(--accent-primary); color:#fff; border-color:var(--accent-primary); cursor:pointer;">Cập nhật</button>
                    </form>
                  </td>
                </tr>
              <%
                  }
                }
              %>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Recent Orders Table -->
      <div class="chart-card">
        <div class="chart-card-header">
          <div>
            <h3 class="chart-card-title">🕒 Đơn Hàng Gần Đây</h3>
            <p class="chart-card-subtitle">Các giao dịch mua sách mới nhất</p>
          </div>
          <a href="orders" class="nav-pill-btn" style="font-size:0.8rem; padding:6px 14px;">Tất cả đơn &rarr;</a>
        </div>

        <div class="table-responsive">
          <table class="table table-hover align-middle" style="margin:0; font-size:0.9rem;">
            <thead style="background:var(--shelf-surface);">
              <tr>
                <th style="padding:10px 14px;">Mã đơn</th>
                <th style="padding:10px 14px;">Khách hàng</th>
                <th style="padding:10px 14px;">Tổng tiền</th>
                <th style="padding:10px 14px; text-align:center;">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              <%
                List<Map<String, Object>> recOrders = (List<Map<String, Object>>) request.getAttribute("recentOrders");
                if (recOrders == null || recOrders.isEmpty()) {
              %>
                <tr>
                  <td colspan="4" class="text-center py-4" style="color:var(--text-secondary);">
                    Chưa có đơn hàng nào. Đơn sẽ hiển thị tại đây ngay khi khách đặt sách.
                  </td>
                </tr>
              <%
                } else {
                  for (Map<String, Object> o : recOrders) {
                    String st = (String) o.get("status");
                    String badgeClass = "status-paid";
                    String stVi = "ĐÃ THANH TOÁN";
                    if ("PROCESSING".equalsIgnoreCase(st)) {
                        badgeClass = "status-processing";
                        stVi = "ĐANG XỬ LÝ";
                    } else if ("SHIPPED".equalsIgnoreCase(st)) {
                        stVi = "ĐÃ GIAO HÀNG";
                    } else if ("CANCELLED".equalsIgnoreCase(st)) {
                        stVi = "ĐÃ HỦY";
                    }
              %>
                <tr>
                  <td style="vertical-align:middle; padding:12px 14px;">
                    <div style="font-weight:700; font-family:monospace;"><%= o.get("orderId") %></div>
                    <div style="font-size:0.76rem; color:var(--text-secondary);"><%= o.get("orderDate") %></div>
                  </td>
                  <td style="vertical-align:middle; padding:12px 14px; font-weight:600;">
                    <%= o.get("username") %>
                  </td>
                  <td style="vertical-align:middle; font-weight:700; color:#059669; padding:12px 14px;">
                    <%= String.format("%,.0f đ", (Double)o.get("totalAmount")) %>
                  </td>
                  <td style="vertical-align:middle; text-align:center; padding:12px 14px;">
                    <span class="badge-order-status-pill <%= badgeClass %>">
                      <%= stVi %>
                    </span>
                  </td>
                </tr>
              <%
                  }
                }
              %>
            </tbody>
          </table>
        </div>
      </div>
    </div>

  </div>
</main>

<!-- Chart.js Configuration Scripts -->
<script>
document.addEventListener("DOMContentLoaded", function() {
    // 1. Spline Area Chart: Revenue Trend
    var revDates = <%= request.getAttribute("revenueDatesJson") %>;
    var revAmounts = <%= request.getAttribute("revenueAmountsJson") %>;

    var revCanvas = document.getElementById('revenueSplineChart');
    if (revCanvas) {
        var ctx = revCanvas.getContext('2d');
        
        // Golden gradient fill
        var gradient = ctx.createLinearGradient(0, 0, 0, 260);
        gradient.addColorStop(0, 'rgba(197, 137, 64, 0.45)');
        gradient.addColorStop(0.7, 'rgba(197, 137, 64, 0.12)');
        gradient.addColorStop(1, 'rgba(197, 137, 64, 0.0)');

        new Chart(ctx, {
            type: 'line',
            data: {
                labels: revDates,
                datasets: [{
                    label: 'Revenue (đ)',
                    data: revAmounts,
                    borderColor: '#C58940',
                    borderWidth: 3,
                    pointBackgroundColor: '#AE7634',
                    pointBorderColor: '#ffffff',
                    pointBorderWidth: 2,
                    pointRadius: 5,
                    pointHoverRadius: 7,
                    tension: 0.38,
                    fill: true,
                    backgroundColor: gradient
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {
                    intersect: false,
                    mode: 'index',
                },
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        backgroundColor: 'rgba(28, 24, 21, 0.92)',
                        titleFont: { family: 'Plus Jakarta Sans', size: 13 },
                        bodyFont: { family: 'Plus Jakarta Sans', size: 12 },
                        padding: 12,
                        cornerRadius: 10,
                        callbacks: {
                            label: function(context) {
                                return ' Doanh thu: ' + context.parsed.y.toLocaleString('vi-VN') + ' đ';
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: {
                            font: { family: 'Plus Jakarta Sans', weight: '600' },
                            color: '#7C746B'
                        }
                    },
                    y: {
                        beginAtZero: true,
                        grid: {
                            color: 'rgba(197, 137, 64, 0.1)'
                        },
                        ticks: {
                            font: { family: 'Plus Jakarta Sans' },
                            color: '#7C746B',
                            callback: function(value) {
                                if (value >= 1000000) return (value / 1000000) + 'M đ';
                                if (value >= 1000) return (value / 1000) + 'k đ';
                                return value + ' đ';
                            }
                        }
                    }
                }
            }
        });
    }

    // 2. Doughnut Chart: Order Status Distribution
    var statusLabels = <%= request.getAttribute("statusLabelsJson") %>;
    var statusCounts = <%= request.getAttribute("statusCountsJson") %>;

    var statusCanvas = document.getElementById('orderStatusDoughnutChart');
    if (statusCanvas) {
        new Chart(statusCanvas, {
            type: 'doughnut',
            data: {
                labels: statusLabels,
                datasets: [{
                    data: statusCounts,
                    backgroundColor: [
                        '#10B981', // Emerald PAID
                        '#3B82F6', // Blue PROCESSING
                        '#F59E0B', // Amber SHIPPED
                        '#8B5CF6', // Purple COMPLETED
                        '#EF4444'  // Red CANCELLED
                    ],
                    borderWidth: 3,
                    borderColor: '#ffffff',
                    hoverOffset: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '68%',
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            font: { family: 'Plus Jakarta Sans', weight: '600', size: 12 },
                            color: '#2B2825',
                            padding: 14,
                            usePointStyle: true
                        }
                    },
                    tooltip: {
                        backgroundColor: 'rgba(28, 24, 21, 0.92)',
                        cornerRadius: 10,
                        padding: 10
                    }
                }
            }
        });
    }

    // 3. Horizontal Bar Chart: Top 5 Bestsellers
    var topNames = <%= request.getAttribute("topNamesJson") %>;
    var topSales = <%= request.getAttribute("topSalesJson") %>;

    var topCanvas = document.getElementById('topBooksBarChart');
    if (topCanvas) {
        new Chart(topCanvas, {
            type: 'bar',
            data: {
                labels: topNames,
                datasets: [{
                    label: 'Số lượng bán (cuốn)',
                    data: topSales,
                    backgroundColor: 'rgba(197, 137, 64, 0.85)',
                    hoverBackgroundColor: '#AE7634',
                    borderRadius: 8,
                    borderSkipped: false,
                    barThickness: 24
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: 'rgba(28, 24, 21, 0.92)',
                        cornerRadius: 10,
                        padding: 10,
                        callbacks: {
                            label: function(context) {
                                return ' Đã bán: ' + context.parsed.x + ' cuốn';
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        beginAtZero: true,
                        grid: { color: 'rgba(197, 137, 64, 0.08)' },
                        ticks: {
                            font: { family: 'Plus Jakarta Sans' },
                            color: '#7C746B',
                            stepSize: 1
                        }
                    },
                    y: {
                        grid: { display: false },
                        ticks: {
                            font: { family: 'Plus Jakarta Sans', weight: '600', size: 12 },
                            color: '#2B2825'
                        }
                    }
                }
            }
        });
    }
});
</script>
