<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
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

    <style>
      /* Bookshelf Interactive Calendar Revenue Filter */
      .cal-dropdown-container {
        position: relative;
        display: inline-block;
      }
      .bookshelf-cal-btn {
        display: inline-flex;
        align-items: center;
        gap: 10px;
        background: #ffffff;
        border: 1.5px solid rgba(197, 137, 64, 0.28);
        border-radius: var(--radius-pill);
        padding: 5px 16px 5px 6px;
        box-shadow: 0 3px 12px rgba(86, 67, 46, 0.06);
        cursor: pointer;
        transition: all 0.22s cubic-bezier(0.2, 0.8, 0.2, 1);
        outline: none;
        user-select: none;
      }
      .bookshelf-cal-btn:hover {
        border-color: var(--accent-primary);
        box-shadow: 0 6px 18px rgba(197, 137, 64, 0.18);
        transform: translateY(-1px);
      }
      .cal-dropdown-container.open .bookshelf-cal-btn {
        border-color: var(--accent-primary);
        box-shadow: 0 0 0 3px rgba(197, 137, 64, 0.18);
      }
      .cal-btn-icon-box {
        width: 34px;
        height: 34px;
        border-radius: 50%;
        background: linear-gradient(135deg, #FAF7F2, #F4EAD4);
        color: var(--accent-primary);
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 2px 6px rgba(197, 137, 64, 0.15);
      }
      .cal-btn-text-wrap {
        display: flex;
        flex-direction: column;
        text-align: left;
        line-height: 1.18;
      }
      .cal-btn-hint {
        font-size: 0.65rem;
        font-weight: 700;
        color: var(--text-muted);
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }
      .cal-btn-current {
        font-size: 0.85rem;
        font-weight: 700;
        color: var(--text-primary);
        white-space: nowrap;
      }
      .cal-chevron-icon {
        color: var(--accent-primary);
        transition: transform 0.22s ease;
        margin-left: 2px;
      }
      .cal-dropdown-container.open .cal-chevron-icon {
        transform: rotate(180deg);
      }

      /* Popover Card */
      .bookshelf-cal-popover {
        display: none;
        position: absolute;
        top: calc(100% + 8px);
        right: 0;
        z-index: 1050;
        width: 325px;
        background: #ffffff;
        border: 1px solid rgba(197, 137, 64, 0.28);
        border-radius: 20px;
        padding: 14px;
        box-shadow: 0 20px 42px rgba(63, 47, 31, 0.18), 0 5px 12px rgba(63, 47, 31, 0.08);
        backdrop-filter: blur(8px);
        animation: calPopIn 0.22s cubic-bezier(0.16, 1, 0.3, 1);
      }
      @keyframes calPopIn {
        0% { opacity: 0; transform: translateY(-8px) scale(0.97); }
        100% { opacity: 1; transform: translateY(0) scale(1); }
      }
      .cal-dropdown-container.open .bookshelf-cal-popover {
        display: block;
      }

      /* Popover Tabs */
      .cal-nav-tabs {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 4px;
        background: #F5EFE6;
        padding: 4px;
        border-radius: 12px;
        margin-bottom: 12px;
      }
      .cal-tab-btn {
        border: none;
        background: transparent;
        padding: 6px 4px;
        font-size: 0.8rem;
        font-weight: 600;
        color: var(--text-secondary);
        border-radius: 9px;
        cursor: pointer;
        transition: all 0.18s ease;
        text-align: center;
        font-family: inherit;
      }
      .cal-tab-btn:hover:not(.active) {
        background: rgba(197, 137, 64, 0.12);
        color: var(--text-primary);
      }
      .cal-tab-btn.active {
        background: var(--accent-primary);
        color: #ffffff;
        font-weight: 700;
        box-shadow: 0 2px 6px rgba(197, 137, 64, 0.3);
      }

      /* Header Controls in Pane */
      .cal-header-controls {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;
        padding: 0 4px;
      }
      .cal-title-current {
        font-weight: 700;
        font-size: 0.92rem;
        color: var(--text-primary);
      }
      .cal-arrow-btn {
        background: transparent;
        border: 1px solid rgba(197, 137, 64, 0.2);
        border-radius: 8px;
        width: 28px;
        height: 28px;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        color: var(--text-secondary);
        transition: all 0.15s ease;
        font-size: 0.82rem;
      }
      .cal-arrow-btn:hover {
        background: #F5EFE6;
        color: var(--accent-primary);
        border-color: var(--accent-primary);
      }

      /* Day View Grid */
      .cal-day-names-row {
        display: grid;
        grid-template-columns: repeat(7, 1fr);
        text-align: center;
        font-size: 0.72rem;
        font-weight: 700;
        color: var(--text-muted);
        margin-bottom: 6px;
      }
      .cal-day-names-row .weekend {
        color: #ef4444;
      }
      .cal-days-grid {
        display: grid;
        grid-template-columns: repeat(7, 1fr);
        gap: 3px;
      }
      .cal-day-cell {
        height: 33px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 0.82rem;
        font-weight: 600;
        border-radius: 50%;
        cursor: pointer;
        transition: all 0.15s ease;
        color: var(--text-primary);
        border: 1.5px solid transparent;
      }
      .cal-day-cell:hover {
        background: #F5EFE6;
        color: var(--accent-primary);
      }
      .cal-day-cell.other-month {
        color: #D1C9BE;
      }
      .cal-day-cell.today {
        border-color: var(--accent-primary);
        color: var(--accent-hover);
      }
      .cal-day-cell.selected {
        background: var(--accent-primary) !important;
        color: #ffffff !important;
        font-weight: 700;
        box-shadow: 0 3px 8px rgba(197, 137, 64, 0.35);
      }

      /* Month Grid */
      .cal-grid-months {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 7px;
      }
      .cal-month-chip {
        padding: 10px 4px;
        text-align: center;
        font-size: 0.82rem;
        font-weight: 600;
        background: #FAF7F2;
        border: 1px solid rgba(197, 137, 64, 0.16);
        border-radius: 10px;
        cursor: pointer;
        transition: all 0.15s ease;
        color: var(--text-primary);
      }
      .cal-month-chip:hover {
        background: #F4EAD4;
        border-color: var(--accent-primary);
        color: var(--accent-hover);
      }
      .cal-month-chip.selected {
        background: var(--accent-primary) !important;
        color: #fff !important;
        border-color: var(--accent-primary);
        font-weight: 700;
        box-shadow: 0 3px 8px rgba(197, 137, 64, 0.3);
      }

      /* Quarter Grid */
      .cal-grid-quarters {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 9px;
      }
      .cal-quarter-card {
        padding: 12px 8px;
        text-align: center;
        background: #FAF7F2;
        border: 1px solid rgba(197, 137, 64, 0.18);
        border-radius: 12px;
        cursor: pointer;
        transition: all 0.18s ease;
      }
      .cal-quarter-card:hover {
        background: #F4EAD4;
        border-color: var(--accent-primary);
        transform: translateY(-1px);
      }
      .cal-quarter-card.selected {
        background: linear-gradient(135deg, var(--accent-primary), var(--accent-hover)) !important;
        border-color: transparent;
        box-shadow: 0 4px 10px rgba(197, 137, 64, 0.3);
      }
      .cal-quarter-card .q-title {
        font-size: 0.92rem;
        font-weight: 700;
        color: var(--text-primary);
      }
      .cal-quarter-card .q-sub {
        font-size: 0.72rem;
        color: var(--text-secondary);
        margin-top: 2px;
      }
      .cal-quarter-card.selected .q-title,
      .cal-quarter-card.selected .q-sub {
        color: #ffffff !important;
      }

      /* Year Grid */
      .cal-grid-years {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 7px;
      }
      .cal-year-chip {
        padding: 11px 4px;
        text-align: center;
        font-size: 0.86rem;
        font-weight: 600;
        background: #FAF7F2;
        border: 1px solid rgba(197, 137, 64, 0.16);
        border-radius: 10px;
        cursor: pointer;
        transition: all 0.15s ease;
        color: var(--text-primary);
      }
      .cal-year-chip:hover {
        background: #F4EAD4;
        border-color: var(--accent-primary);
        color: var(--accent-hover);
      }
      .cal-year-chip.selected {
        background: var(--accent-primary) !important;
        color: #fff !important;
        border-color: var(--accent-primary);
        font-weight: 700;
        box-shadow: 0 3px 8px rgba(197, 137, 64, 0.3);
      }

      /* Footer Quick Actions */
      .cal-popover-footer {
        margin-top: 12px;
        padding-top: 10px;
        border-top: 1px solid rgba(197, 137, 64, 0.12);
        display: flex;
        justify-content: space-between;
        gap: 8px;
      }
      .cal-preset-btn {
        border: none;
        background: transparent;
        font-size: 0.76rem;
        font-weight: 700;
        color: var(--accent-primary);
        cursor: pointer;
        padding: 5px 8px;
        border-radius: 6px;
        transition: background 0.15s ease, color 0.15s ease;
        font-family: inherit;
      }
      .cal-preset-btn:hover {
        background: #F4EAD4;
        color: var(--accent-hover);
      }
    </style>

    <main class="bookshelf-canvas" style="padding-top: 26px;">
      <div class="bookshelf-page-container" style="max-width: 1340px; margin: 0 auto;">

        <!-- Dashboard Top Bar -->
        <div class="dashboard-topbar-flex">
          <div class="dashboard-title-box">
            <span class="dashboard-pill-tag">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <polyline points="12 6 12 12 16 14"></polyline>
              </svg>
              Phân tích Điều hành
            </span>
            <h1>Thống kê & Hiệu quả Cửa hàng</h1>
            <p>Theo dõi giao dịch trực tiếp, biểu đồ doanh thu và giám sát kho sách.</p>
          </div>

          <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap; position:relative;">
            <!-- Modern Interactive Calendar Revenue Filter -->
            <div class="cal-dropdown-container" id="calDropdownContainer">
              <button type="button" class="bookshelf-cal-btn" id="calTriggerBtn" onclick="toggleCalDropdown(event)" title="Nhấn để chọn mốc thời gian xem doanh thu">
                <div class="cal-btn-icon-box">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                    <line x1="16" y1="2" x2="16" y2="6"></line>
                    <line x1="8" y1="2" x2="8" y2="6"></line>
                    <line x1="3" y1="10" x2="21" y2="10"></line>
                  </svg>
                </div>
                <div class="cal-btn-text-wrap">
                  <span class="cal-btn-hint">Doanh thu theo</span>
                  <span class="cal-btn-current" id="calActiveLabel">Toàn bộ thời gian</span>
                </div>
                <svg class="cal-chevron-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>

              <!-- Popover Card -->
              <div class="bookshelf-cal-popover" id="calPopover">
                <!-- 4 Mode Tabs -->
                <div class="cal-nav-tabs">
                  <button type="button" class="cal-tab-btn active" id="tabModeDay" onclick="switchCalView('day')">Ngày</button>
                  <button type="button" class="cal-tab-btn" id="tabModeMonth" onclick="switchCalView('month')">Tháng</button>
                  <button type="button" class="cal-tab-btn" id="tabModeQuarter" onclick="switchCalView('quarter')">Quý</button>
                  <button type="button" class="cal-tab-btn" id="tabModeYear" onclick="switchCalView('year')">Năm</button>
                </div>

                <!-- Pane 1: Day Calendar -->
                <div class="cal-view-section" id="viewSectionDay">
                  <div class="cal-header-controls">
                    <button type="button" class="cal-arrow-btn" onclick="shiftDayMonth(-1)" title="Tháng trước">&#10094;</button>
                    <div class="cal-title-current" id="dayViewMonthTitle">Tháng 9, 2026</div>
                    <button type="button" class="cal-arrow-btn" onclick="shiftDayMonth(1)" title="Tháng sau">&#10095;</button>
                  </div>
                  <div class="cal-day-names-row">
                    <span>T2</span><span>T3</span><span>T4</span><span>T5</span><span>T6</span><span>T7</span><span class="weekend">CN</span>
                  </div>
                  <div class="cal-days-grid" id="daysGridContainer"></div>
                </div>

                <!-- Pane 2: Month Grid -->
                <div class="cal-view-section" id="viewSectionMonth" style="display:none;">
                  <div class="cal-header-controls">
                    <button type="button" class="cal-arrow-btn" onclick="shiftMonthYear(-1)" title="Năm trước">&#10094;</button>
                    <div class="cal-title-current" id="monthViewYearTitle">Năm 2026</div>
                    <button type="button" class="cal-arrow-btn" onclick="shiftMonthYear(1)" title="Năm sau">&#10095;</button>
                  </div>
                  <div class="cal-grid-months" id="monthsGridContainer"></div>
                </div>

                <!-- Pane 3: Quarter Grid -->
                <div class="cal-view-section" id="viewSectionQuarter" style="display:none;">
                  <div class="cal-header-controls">
                    <button type="button" class="cal-arrow-btn" onclick="shiftQuarterYear(-1)" title="Năm trước">&#10094;</button>
                    <div class="cal-title-current" id="quarterViewYearTitle">Năm 2026</div>
                    <button type="button" class="cal-arrow-btn" onclick="shiftQuarterYear(1)" title="Năm sau">&#10095;</button>
                  </div>
                  <div class="cal-grid-quarters" id="quartersGridContainer"></div>
                </div>

                <!-- Pane 4: Year Grid -->
                <div class="cal-view-section" id="viewSectionYear" style="display:none;">
                  <div class="cal-header-controls">
                    <button type="button" class="cal-arrow-btn" onclick="shiftYearDecade(-6)" title="Trước">&#10094;</button>
                    <div class="cal-title-current" id="yearViewDecadeTitle">2021 - 2026</div>
                    <button type="button" class="cal-arrow-btn" onclick="shiftYearDecade(6)" title="Sau">&#10095;</button>
                  </div>
                  <div class="cal-grid-years" id="yearsGridContainer"></div>
                </div>

                <!-- Quick Presets -->
                <div class="cal-popover-footer">
                  <button type="button" class="cal-preset-btn" onclick="quickSelectToday()">⚡ Hôm nay</button>
                  <button type="button" class="cal-preset-btn" onclick="quickSelectAllTime()">🌐 Toàn bộ thời gian</button>
                </div>
              </div>
            </div>

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
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                  stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="1" x2="12" y2="23"></line>
                  <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
                </svg>
              </div>
            </div>
            <div class="stat-kpi-value" id="kpiRevenueValue">
              <%= String.format("%,.0f đ", (Double)request.getAttribute("totalRevenue")) %>
            </div>
            <div class="stat-kpi-footer">
              <span class="stat-trend-badge trend-up" id="kpiRevenueTrend">▲ Hoạt động</span>
              <span id="kpiRevenueSub">Từ các đơn hoàn tất</span>
            </div>
          </div>

          <!-- Card 2: Total Orders -->
          <div class="stat-kpi-card">
            <div class="stat-kpi-header">
              <span class="stat-kpi-label">Tổng Đơn Hàng</span>
              <div class="stat-kpi-icon-box icon-blue">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                  stroke-linecap="round" stroke-linejoin="round">
                  <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                  <line x1="3" y1="6" x2="21" y2="6"></line>
                  <path d="M16 10a4 4 0 0 1-8 0"></path>
                </svg>
              </div>
            </div>
            <div class="stat-kpi-value" id="kpiOrdersValue">
              <%= request.getAttribute("totalOrders") %>
            </div>
            <div class="stat-kpi-footer">
              <span class="stat-trend-badge trend-up" id="kpiOrdersTrend">▲ Đơn hàng</span>
              <span id="kpiOrdersSub">Khách đã thanh toán</span>
            </div>
          </div>

          <!-- Card 3: Books Sold -->
          <div class="stat-kpi-card">
            <div class="stat-kpi-header">
              <span class="stat-kpi-label">Số Sách Đã Bán</span>
              <div class="stat-kpi-icon-box icon-purple">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                  stroke-linecap="round" stroke-linejoin="round">
                  <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                  <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
                </svg>
              </div>
            </div>
            <div class="stat-kpi-value">
              <span id="kpiBooksSoldValue"><%= request.getAttribute("totalBooksSold") %></span> <span
                  style="font-size:0.9rem; font-weight:600; color:var(--text-secondary);">cuốn</span>
            </div>
            <div class="stat-kpi-footer">
              <span class="stat-trend-badge trend-neutral" id="kpiBooksSoldTrend">&bull; Sản lượng</span>
              <span id="kpiBooksSoldSub">Tổng số bản đã xuất</span>
            </div>
          </div>

          <!-- Card 4: Customers -->
          <div class="stat-kpi-card">
            <div class="stat-kpi-header">
              <span class="stat-kpi-label">Khách Hàng</span>
              <div class="stat-kpi-icon-box icon-amber">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                  stroke-linecap="round" stroke-linejoin="round">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                  <circle cx="9" cy="7" r="4"></circle>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                </svg>
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
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                  stroke-linecap="round" stroke-linejoin="round">
                  <path
                    d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z">
                  </path>
                  <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
                  <line x1="12" y1="22.08" x2="12" y2="12"></line>
                </svg>
              </div>
            </div>
            <div class="stat-kpi-value">
              <%= request.getAttribute("totalStock") %> <span
                  style="font-size:0.9rem; font-weight:600; color:var(--text-secondary);">cuốn</span>
            </div>
            <div class="stat-kpi-footer">
              <span class="stat-trend-badge trend-neutral">
                <%= request.getAttribute("totalTitles") %> Đầu sách
              </span>
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
                <p class="chart-card-subtitle" id="chartRevenueSubtitle">Doanh thu và xu hướng giao dịch theo các mốc thời gian gần nhất</p>
              </div>
              <div style="display:flex; gap:8px;">
                <span id="chartFilterBadge"
                  style="font-size:0.75rem; font-weight:700; background:rgba(197,137,64,0.14); color:var(--accent-hover); padding:4px 10px; border-radius:var(--radius-pill);">
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
              <span
                style="font-size:0.75rem; font-weight:700; background:rgba(16,185,129,0.12); color:#059669; padding:4px 10px; border-radius:var(--radius-pill);">
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

              <div class="store-operations-metrics">
                <% double avgOrderValue=0.0; int ordersCount=(Integer)request.getAttribute("totalOrders"); double
                  revVal=(Double)request.getAttribute("totalRevenue"); if (ordersCount> 0) {
                  avgOrderValue = revVal / ordersCount;
                  }
                  int pendingOrders = (Integer)request.getAttribute("pendingOrders");
                  int confirmedOrders = (Integer)request.getAttribute("confirmedOrders");
                  int actionableOrders = pendingOrders + confirmedOrders;
                  int lowStockTitleCount = (Integer)request.getAttribute("lowStockTitleCount");
                  int slowMovingStockCount = (Integer)request.getAttribute("slowMovingStockCount");
                  double slowMovingStockValue = (Double)request.getAttribute("slowMovingStockValue");
                  %>
                  <div class="store-operation-metric">
                    <div>
                      <div
                        style="font-size:0.8rem; font-weight:700; color:var(--text-secondary); text-transform:uppercase;">
                        Giá trị đơn trung bình (AOV)</div>
                      <div style="font-size:1.3rem; font-weight:800; color:var(--text-primary); margin-top:2px;">
                        <%= String.format("%,.0f đ", avgOrderValue) %>
                      </div>
                    </div>
                    <div class="store-operation-icon metric-aov">💳</div>
                  </div>

                  <a href="orders?status=PENDING" class="store-operation-metric store-operation-link">
                    <div>
                      <div class="store-operation-label">Đơn cần xử lý</div>
                      <div class="store-operation-value <%= actionableOrders > 0 ? "metric-alert" : "metric-good" %>">
                        <%= actionableOrders %> đơn
                      </div>
                      <div class="store-operation-detail"><%= pendingOrders %> chờ xác nhận · <%= confirmedOrders %> chờ giao</div>
                    </div>
                    <div class="store-operation-icon metric-orders">📋</div>
                  </a>

                  <a href="#low-stock-alerts" class="store-operation-metric store-operation-link">
                    <div>
                      <div class="store-operation-label">Cảnh báo tồn kho</div>
                      <div class="store-operation-value <%= lowStockTitleCount > 0 ? "metric-alert" : "metric-good" %>">
                        <%= lowStockTitleCount > 0 ? lowStockTitleCount + " đầu sách" : "Kho ổn định" %>
                      </div>
                      <div class="store-operation-detail"><%= lowStockTitleCount > 0 ? "Tồn kho từ 5 cuốn trở xuống" : "Chưa có đầu sách cần nhập thêm" %></div>
                    </div>
                    <div class="store-operation-icon metric-stock">📦</div>
                  </a>

                  <a href="#slow-moving-stock" class="store-operation-metric store-operation-link">
                    <div>
                      <div class="store-operation-label">Hàng tồn đọng (90 ngày)</div>
                      <div class="store-operation-value <%= slowMovingStockCount > 0 ? "metric-alert" : "metric-good" %>">
                        <%= slowMovingStockCount > 0 ? slowMovingStockCount + " đầu sách" : "Không có" %>
                      </div>
                      <div class="store-operation-detail"><%= String.format("%,.0f đ giá trị tồn", slowMovingStockValue) %></div>
                    </div>
                    <div class="store-operation-icon metric-slow-stock">⏳</div>
                  </a>
              </div>
            </div>

            <div style="margin-top:20px; text-align:center;">
              <a href="storebooks" class="btn-auth-submit"
                style="text-decoration:none; padding:11px 20px; font-size:0.92rem;">
                <span>Quản Lý Kho Sách Cửa Hàng</span> &rarr;
              </a>
            </div>
          </div>
        </div>

        <div class="chart-card" id="slow-moving-stock" style="margin-bottom:28px;">
          <div class="chart-card-header">
            <div>
              <h3 class="chart-card-title">⏳ Hàng Tồn Đọng</h3>
              <p class="chart-card-subtitle">Còn trong kho nhưng không có đơn hoàn tất trong 90 ngày gần nhất</p>
            </div>
            <a href="storebooks" class="nav-pill-btn" style="font-size:0.8rem; padding:6px 14px;">Quản lý kho &rarr;</a>
          </div>

          <div class="table-responsive">
            <table class="table table-hover align-middle" style="margin:0; font-size:0.9rem;">
              <thead style="background:var(--shelf-surface);">
                <tr>
                  <th style="padding:10px 14px;">Tựa sách</th>
                  <th style="padding:10px 14px; text-align:center;">Tồn kho</th>
                  <th style="padding:10px 14px;">Lần bán hoàn tất gần nhất</th>
                  <th style="padding:10px 14px; text-align:right;">Giá trị tồn</th>
                  <th style="padding:10px 14px; text-align:center;">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                <% List<Map<String, Object>> slowMoving = (List<Map<String, Object>>) request.getAttribute("slowMovingStockBooks");
                   if (slowMoving == null || slowMoving.isEmpty()) { %>
                  <tr><td colspan="5" class="text-center py-4" style="color:var(--text-secondary);">🎉 Không có sách tồn đọng theo mốc 90 ngày.</td></tr>
                <% } else { for (Map<String, Object> b : slowMoving) { %>
                  <tr>
                    <td style="vertical-align:middle; padding:12px 14px;">
                      <div style="font-weight:700; color:var(--text-primary);"><%= b.get("name") %></div>
                      <div style="font-size:0.78rem; color:var(--text-secondary);"><%= b.get("author") %></div>
                    </td>
                    <td style="vertical-align:middle; text-align:center; padding:12px 14px;"><strong><%= b.get("quantity") %> cuốn</strong></td>
                    <td style="vertical-align:middle; padding:12px 14px;"><%= b.get("lastSold") != null ? b.get("lastSold") : "Chưa từng bán" %></td>
                    <td style="vertical-align:middle; text-align:right; padding:12px 14px; font-weight:700; color:var(--accent-hover);"><%= String.format("%,.0f đ", (Double)b.get("stockValue")) %></td>
                    <td style="vertical-align:middle; text-align:center; padding:12px 14px;">
                      <form method="post" action="updatebook" style="margin:0;">
                        <input type="hidden" name="bookId" value="<%= b.get("barcode") %>">
                        <button type="submit" class="nav-pill-btn" style="padding:5px 12px; font-size:0.8rem; background:var(--accent-primary); color:#fff; border-color:var(--accent-primary); cursor:pointer;">Xem / sửa</button>
                      </form>
                    </td>
                  </tr>
                <% } } %>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Tables Row: Low Stock Alerts & Recent Orders -->
        <div class="stats-tables-row">
          <!-- Low Stock Alerts Table -->
          <div class="chart-card" id="low-stock-alerts">
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
                  <% List<Map<String, Object>> lowStock = (List<Map<String, Object>>)
                      request.getAttribute("lowStockBooks");
                      if (lowStock == null || lowStock.isEmpty()) {
                      %>
                      <tr>
                        <td colspan="4" class="text-center py-4" style="color:var(--text-secondary);">
                          🎉 <strong>Tuyệt vời!</strong> Tất cả các đầu sách hiện đều có lượng tồn kho dồi dào.
                        </td>
                      </tr>
                      <% } else { for (Map<String, Object> b : lowStock) {
                        %>
                        <tr>
                          <td style="vertical-align:middle; padding:12px 14px;">
                            <div style="font-weight:700; color:var(--text-primary);">
                              <%= b.get("name") %>
                            </div>
                            <div style="font-size:0.78rem; color:var(--text-secondary);">
                              <%= b.get("author") %>
                            </div>
                          </td>
                          <td
                            style="vertical-align:middle; font-weight:700; color:var(--accent-hover); padding:12px 14px;">
                            <%= String.format("%,.0f đ", (Double)b.get("price")) %>
                          </td>
                          <td style="vertical-align:middle; text-align:center; padding:12px 14px;">
                            <span class="badge-low-stock-urgent">
                              Còn <%= b.get("quantity") %> cuốn
                            </span>
                          </td>
                          <td style="vertical-align:middle; text-align:center; padding:12px 14px;">
                            <form method="post" action="updatebook" style="margin:0;">
                              <input type="hidden" name="bookId" value="<%= b.get(" barcode") %>">
                              <button type="submit" class="nav-pill-btn"
                                style="padding:5px 12px; font-size:0.8rem; background:var(--accent-primary); color:#fff; border-color:var(--accent-primary); cursor:pointer;">Cập
                                nhật</button>
                            </form>
                          </td>
                        </tr>
                        <% } } %>
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
                  <% List<Map<String, Object>> recOrders = (List<Map<String, Object>>)
                      request.getAttribute("recentOrders");
                      if (recOrders == null || recOrders.isEmpty()) {
                      %>
                      <tr>
                        <td colspan="4" class="text-center py-4" style="color:var(--text-secondary);">
                          Chưa có đơn hàng nào. Đơn sẽ hiển thị tại đây ngay khi khách đặt sách.
                        </td>
                      </tr>
                      <% } else { for (Map<String, Object> o : recOrders) {
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
                            <div style="font-weight:700; font-family:monospace;">
                              <%= o.get("orderId") %>
                            </div>
                            <div style="font-size:0.76rem; color:var(--text-secondary);">
                              <%= o.get("orderDate") %>
                            </div>
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
                        <% } } %>
                </tbody>
              </table>
            </div>
          </div>
        </div>

      </div>
    </main>

    <!-- Chart.js Configuration Scripts -->
    <script>
      document.addEventListener("DOMContentLoaded", function () {
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

          window.revenueSplineChartInstance = new Chart(ctx, {
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
                    label: function (context) {
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
                    callback: function (value) {
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
        var statusLabels = <%= request.getAttribute("statusLabelsJson") != null ? request.getAttribute("statusLabelsJson") : "[]" %>;
        var statusCounts = <%= request.getAttribute("statusCountsJson") != null ? request.getAttribute("statusCountsJson") : "[]" %>;

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
                    label: function (context) {
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

        // --------------------------------------------------------------------------
        // Interactive Calendar Revenue Filter Logic
        // --------------------------------------------------------------------------
        var calToday = new Date();
        var calCurrentYear = calToday.getFullYear();
        var calCurrentMonth = calToday.getMonth(); // 0-indexed
        var calCurrentDate = calToday.getDate();

        var viewDayYear = calCurrentYear;
        var viewDayMonth = calCurrentMonth;

        var viewMonthYear = calCurrentYear;
        var viewQuarterYear = calCurrentYear;
        var viewYearBase = calCurrentYear;

        var currentSelectedPeriod = {
          type: 'all',
          value: '',
          label: 'Toàn bộ thời gian'
        };

        window.toggleCalDropdown = function (e) {
          if (e) e.stopPropagation();
          var container = document.getElementById('calDropdownContainer');
          if (container) {
            container.classList.toggle('open');
          }
        };

        window.closeCalDropdown = function () {
          var container = document.getElementById('calDropdownContainer');
          if (container) {
            container.classList.remove('open');
          }
        };

        document.addEventListener('click', function (e) {
          var container = document.getElementById('calDropdownContainer');
          if (container && !container.contains(e.target)) {
            window.closeCalDropdown();
          }
        });

        document.addEventListener('keydown', function (e) {
          if (e.key === 'Escape') window.closeCalDropdown();
        });

        window.switchCalView = function (mode) {
          ['day', 'month', 'quarter', 'year'].forEach(function (m) {
            var tab = document.getElementById('tabMode' + m.charAt(0).toUpperCase() + m.slice(1));
            var pane = document.getElementById('viewSection' + m.charAt(0).toUpperCase() + m.slice(1));
            if (tab) tab.classList.toggle('active', m === mode);
            if (pane) pane.style.display = (m === mode) ? 'block' : 'none';
          });
        };

        window.renderDayCalendar = function () {
          var title = document.getElementById('dayViewMonthTitle');
          if (title) title.innerText = 'Tháng ' + (viewDayMonth + 1) + ', ' + viewDayYear;

          var container = document.getElementById('daysGridContainer');
          if (!container) return;
          container.innerHTML = '';

          var firstDayIndex = new Date(viewDayYear, viewDayMonth, 1).getDay();
          var startDay = (firstDayIndex === 0) ? 6 : firstDayIndex - 1;

          var daysInPrevMonth = new Date(viewDayYear, viewDayMonth, 0).getDate();
          var daysInCurrentMonth = new Date(viewDayYear, viewDayMonth + 1, 0).getDate();

          for (var i = startDay - 1; i >= 0; i--) {
            var cell = document.createElement('div');
            cell.className = 'cal-day-cell other-month';
            cell.innerText = daysInPrevMonth - i;
            container.appendChild(cell);
          }

          for (var d = 1; d <= daysInCurrentMonth; d++) {
            (function (dayNum) {
              var cell = document.createElement('div');
              cell.className = 'cal-day-cell';
              cell.innerText = dayNum;

              if (viewDayYear === calCurrentYear && viewDayMonth === calCurrentMonth && dayNum === calCurrentDate) {
                cell.classList.add('today');
              }

              var dateKey = viewDayYear + '-' + String(viewDayMonth + 1).padStart(2, '0') + '-' + String(dayNum).padStart(2, '0');
              if (currentSelectedPeriod.type === 'day' && currentSelectedPeriod.value === dateKey) {
                cell.classList.add('selected');
              }

              cell.onclick = function (e) {
                e.stopPropagation();
                var displayLbl = 'Ngày ' + String(dayNum).padStart(2, '0') + '/' + String(viewDayMonth + 1).padStart(2, '0') + '/' + viewDayYear;
                if (viewDayYear === calCurrentYear && viewDayMonth === calCurrentMonth && dayNum === calCurrentDate) {
                  displayLbl = 'Hôm nay (' + String(dayNum).padStart(2, '0') + '/' + String(viewDayMonth + 1).padStart(2, '0') + ')';
                }
                window.fetchRevenue(displayLbl, 'day', dateKey);
              };

              container.appendChild(cell);
            })(d);
          }

          var totalCells = startDay + daysInCurrentMonth;
          var remaining = (totalCells > 35) ? 42 - totalCells : 35 - totalCells;
          for (var nextD = 1; nextD <= remaining; nextD++) {
            var cell = document.createElement('div');
            cell.className = 'cal-day-cell other-month';
            cell.innerText = nextD;
            container.appendChild(cell);
          }
        };

        window.shiftDayMonth = function (delta) {
          viewDayMonth += delta;
          if (viewDayMonth < 0) {
            viewDayMonth = 11;
            viewDayYear--;
          } else if (viewDayMonth > 11) {
            viewDayMonth = 0;
            viewDayYear++;
          }
          window.renderDayCalendar();
        };

        window.renderMonthGrid = function () {
          var title = document.getElementById('monthViewYearTitle');
          if (title) title.innerText = 'Năm ' + viewMonthYear;

          var container = document.getElementById('monthsGridContainer');
          if (!container) return;
          container.innerHTML = '';

          for (var m = 1; m <= 12; m++) {
            (function (monthNum) {
              var chip = document.createElement('div');
              chip.className = 'cal-month-chip';
              chip.innerText = 'Tháng ' + monthNum;

              var monthKey = monthNum + '-' + viewMonthYear;
              if (currentSelectedPeriod.type === 'month' && currentSelectedPeriod.value === monthKey) {
                chip.classList.add('selected');
              }

              chip.onclick = function (e) {
                e.stopPropagation();
                window.fetchRevenue('Tháng ' + String(monthNum).padStart(2, '0') + '/' + viewMonthYear, 'month', monthKey);
              };

              container.appendChild(chip);
            })(m);
          }
        };

        window.shiftMonthYear = function (delta) {
          viewMonthYear += delta;
          window.renderMonthGrid();
        };

        window.renderQuarterGrid = function () {
          var title = document.getElementById('quarterViewYearTitle');
          if (title) title.innerText = 'Năm ' + viewQuarterYear;

          var container = document.getElementById('quartersGridContainer');
          if (!container) return;
          container.innerHTML = '';

          var quarterData = [
            { q: 1, sub: 'Tháng 1 - Tháng 3' },
            { q: 2, sub: 'Tháng 4 - Tháng 6' },
            { q: 3, sub: 'Tháng 7 - Tháng 9' },
            { q: 4, sub: 'Tháng 10 - Tháng 12' }
          ];

          quarterData.forEach(function (item) {
            var card = document.createElement('div');
            card.className = 'cal-quarter-card';

            var qKey = item.q + '-' + viewQuarterYear;
            if (currentSelectedPeriod.type === 'quarter' && currentSelectedPeriod.value === qKey) {
              card.classList.add('selected');
            }

            card.innerHTML = '<div class="q-title">Quý ' + item.q + '</div><div class="q-sub">' + item.sub + '</div>';

            card.onclick = function (e) {
              e.stopPropagation();
              window.fetchRevenue('Quý ' + item.q + '/' + viewQuarterYear, 'quarter', qKey);
            };

            container.appendChild(card);
          });
        };

        window.shiftQuarterYear = function (delta) {
          viewQuarterYear += delta;
          window.renderQuarterGrid();
        };

        window.renderYearGrid = function () {
          var title = document.getElementById('yearViewDecadeTitle');
          var startY = viewYearBase - 5;
          var endY = viewYearBase;
          if (title) title.innerText = startY + ' - ' + endY;

          var container = document.getElementById('yearsGridContainer');
          if (!container) return;
          container.innerHTML = '';

          for (var y = startY; y <= endY; y++) {
            (function (yearNum) {
              var chip = document.createElement('div');
              chip.className = 'cal-year-chip';
              chip.innerText = yearNum;

              if (currentSelectedPeriod.type === 'year' && currentSelectedPeriod.value === String(yearNum)) {
                chip.classList.add('selected');
              }

              chip.onclick = function (e) {
                e.stopPropagation();
                window.fetchRevenue('Năm ' + yearNum, 'year', String(yearNum));
              };

              container.appendChild(chip);
            })(y);
          }
        };

        window.shiftYearDecade = function (delta) {
          viewYearBase += delta;
          window.renderYearGrid();
        };

        window.fetchRevenue = function (displayLabel, type, value) {
          currentSelectedPeriod = { type: type, value: value, label: displayLabel };

          var activeLabelEl = document.getElementById('calActiveLabel');
          if (activeLabelEl) activeLabelEl.innerText = displayLabel;

          var kpiRevEl = document.getElementById('kpiRevenueValue');
          if (kpiRevEl) kpiRevEl.style.opacity = '0.45';

          fetch('statistics?ajax=revenue&filterType=' + encodeURIComponent(type) + '&filterValue=' + encodeURIComponent(value))
            .then(function (res) { return res.json(); })
            .then(function (data) {
              if (kpiRevEl) {
                kpiRevEl.innerText = data.periodRevenueFormatted;
                kpiRevEl.style.opacity = '1';
              }

              var kpiRevTrend = document.getElementById('kpiRevenueTrend');
              if (kpiRevTrend) kpiRevTrend.innerText = '▲ ' + data.filterLabel;

              var kpiRevSub = document.getElementById('kpiRevenueSub');
              if (kpiRevSub) kpiRevSub.innerText = 'Doanh thu trong kỳ';

              var kpiOrders = document.getElementById('kpiOrdersValue');
              if (kpiOrders) kpiOrders.innerText = data.periodOrders;
              var kpiOrdersSub = document.getElementById('kpiOrdersSub');
              if (kpiOrdersSub) kpiOrdersSub.innerText = 'Đơn hàng trong kỳ';

              var kpiBooks = document.getElementById('kpiBooksSoldValue');
              if (kpiBooks) kpiBooks.innerText = data.periodBooksSold;
              var kpiBooksSub = document.getElementById('kpiBooksSoldSub');
              if (kpiBooksSub) kpiBooksSub.innerText = 'Sách bán trong kỳ';

              var chartSub = document.getElementById('chartRevenueSubtitle');
              if (chartSub) chartSub.innerText = 'Doanh thu và xu hướng giao dịch theo ' + data.filterLabel;

              var chartBadge = document.getElementById('chartFilterBadge');
              if (chartBadge) chartBadge.innerText = '● Doanh thu ' + data.filterLabel;

              if (window.revenueSplineChartInstance) {
                window.revenueSplineChartInstance.data.labels = data.timelineLabels;
                window.revenueSplineChartInstance.data.datasets[0].data = data.timelineAmounts;
                window.revenueSplineChartInstance.update();
              }

              window.renderDayCalendar();
              window.renderMonthGrid();
              window.renderQuarterGrid();
              window.renderYearGrid();

              window.closeCalDropdown();
            })
            .catch(function (err) {
              console.error('Lỗi khi lấy dữ liệu doanh thu:', err);
              if (kpiRevEl) kpiRevEl.style.opacity = '1';
            });
        };

        window.quickSelectToday = function () {
          var d = new Date();
          var dStr = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
          window.fetchRevenue('Hôm nay (' + String(d.getDate()).padStart(2, '0') + '/' + String(d.getMonth() + 1).padStart(2, '0') + ')', 'day', dStr);
        };

        window.quickSelectAllTime = function () {
          window.fetchRevenue('Toàn bộ thời gian', 'all', '');
        };

        // Initialize Calendar Views
        window.renderDayCalendar();
        window.renderMonthGrid();
        window.renderQuarterGrid();
        window.renderYearGrid();
      });
    </script>
