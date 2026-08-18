// PijatPro Admin Panel - Dashboard View
const DashboardView = {
    render: function() {
        const stats = PijatProData.stats;
        const formatRupiah = (num) => "Rp " + new Intl.NumberFormat("id-ID").format(num);

        return `
            <!-- Top Stats KPI Grid -->
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-title">Gross Omzet (GMV)</span>
                        <div class="stat-icon-wrapper stat-icon-emerald">💰</div>
                    </div>
                    <div class="stat-value">${formatRupiah(stats.totalGmv)}</div>
                    <div class="stat-footer">
                        <span class="trend-up">↑ +14.2%</span>
                        <span class="trend-text">vs minggu lalu</span>
                    </div>
                </div>

                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-title">Komisi Platform (20%)</span>
                        <div class="stat-icon-wrapper stat-icon-amber">🏦</div>
                    </div>
                    <div class="stat-value">${formatRupiah(stats.platformRevenue)}</div>
                    <div class="stat-footer">
                        <span class="trend-up">↑ +18.5%</span>
                        <span class="trend-text">laba bersih platform</span>
                    </div>
                </div>

                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-title">Pesanan Berjalan</span>
                        <div class="stat-icon-wrapper stat-icon-blue">🛵</div>
                    </div>
                    <div class="stat-value">${stats.activeOrdersNow} <span style="font-size: 14px; font-weight: 600; color: var(--text-muted);">/ ${stats.totalOrdersToday} hari ini</span></div>
                    <div class="stat-footer">
                        <span class="trend-up">★ 98.6%</span>
                        <span class="trend-text">tingkat penyelesaian</span>
                    </div>
                </div>

                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-title">Mitra Terapis Online</span>
                        <div class="stat-icon-wrapper stat-icon-purple">💆‍♂️</div>
                    </div>
                    <div class="stat-value">${stats.onlineTherapists} Mitra</div>
                    <div class="stat-footer">
                        <span class="trend-up">● 38 Ready</span>
                        <span class="trend-text">• 8 Bertugas</span>
                    </div>
                </div>
            </div>

            <!-- Charts & Live Feed Section -->
            <div class="dashboard-grid-2">
                <!-- Revenue Chart Card -->
                <div class="card">
                    <div class="card-header">
                        <div>
                            <h3 class="card-title">Tren Pendapatan & Pesanan Harian (7 Hari Terakhir)</h3>
                            <p class="card-subtitle">Rekapitulasi total GMV dan jumlah transaksi selesai</p>
                        </div>
                        <span class="pill pill-success">Live Sinkron</span>
                    </div>

                    <!-- Interactive SVG Chart -->
                    <div style="height: 220px; width: 100%; display: flex; align-items: flex-end; gap: 18px; padding-top: 20px;">
                        ${this.renderChartBars()}
                    </div>
                </div>

                <!-- Category Breakdown Card -->
                <div class="card">
                    <div class="card-header">
                        <div>
                            <h3 class="card-title">Distribusi Kategori Pijat</h3>
                            <p class="card-subtitle">Persentase order berdasarkan paket</p>
                        </div>
                    </div>

                    <div style="display: flex; flex-direction: column; gap: 14px; margin-top: 8px;">
                        <div>
                            <div style="display: flex; justify-content: space-between; font-size: 13px; font-weight: 700; margin-bottom: 4px;">
                                <span>Pijat Tradisional Jawa</span>
                                <span>48%</span>
                            </div>
                            <div style="height: 8px; background-color: #E2E8F0; border-radius: 4px; overflow: hidden;">
                                <div style="width: 48%; height: 100%; background: var(--primary-emerald); border-radius: 4px;"></div>
                            </div>
                        </div>

                        <div>
                            <div style="display: flex; justify-content: space-between; font-size: 13px; font-weight: 700; margin-bottom: 4px;">
                                <span>Refleksi Kaki & Akupresur</span>
                                <span>24%</span>
                            </div>
                            <div style="height: 8px; background-color: #E2E8F0; border-radius: 4px; overflow: hidden;">
                                <div style="width: 24%; height: 100%; background: var(--accent-amber); border-radius: 4px;"></div>
                            </div>
                        </div>

                        <div>
                            <div style="display: flex; justify-content: space-between; font-size: 13px; font-weight: 700; margin-bottom: 4px;">
                                <span>Deep Tissue & Sport</span>
                                <span>16%</span>
                            </div>
                            <div style="height: 8px; background-color: #E2E8F0; border-radius: 4px; overflow: hidden;">
                                <div style="width: 16%; height: 100%; background: #3B82F6; border-radius: 4px;"></div>
                            </div>
                        </div>

                        <div>
                            <div style="display: flex; justify-content: space-between; font-size: 13px; font-weight: 700; margin-bottom: 4px;">
                                <span>Lulur Spa & Ibu Hamil</span>
                                <span>12%</span>
                            </div>
                            <div style="height: 8px; background-color: #E2E8F0; border-radius: 4px; overflow: hidden;">
                                <div style="width: 12%; height: 100%; background: #8B5CF6; border-radius: 4px;"></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Recent Active Orders Table -->
            <div class="card">
                <div class="card-header">
                    <div>
                        <h3 class="card-title">Aktivitas Pesanan Lapangan Terbaru</h3>
                        <p class="card-subtitle">Status real-time interaksi pelanggan dan mitra terapis</p>
                    </div>
                    <button class="btn btn-secondary btn-sm" onclick="App.navigateTo('orders')">Lihat Semua Order →</button>
                </div>

                <div class="table-container">
                    <table class="custom-table">
                        <thead>
                            <tr>
                                <th>Order ID</th>
                                <th>Pelanggan & Lokasi</th>
                                <th>Mitra Terapis</th>
                                <th>Paket Layanan</th>
                                <th>Total & Komisi 20%</th>
                                <th>Status Order</th>
                                <th>Waktu</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${this.renderRecentOrdersTable()}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    },

    renderChartBars: function() {
        const days = [
            { day: "Sen", value: 68, gmv: "Rp 5.2M" },
            { day: "Sel", value: 74, gmv: "Rp 5.8M" },
            { day: "Rab", value: 82, gmv: "Rp 6.4M" },
            { day: "Kam", value: 78, gmv: "Rp 6.1M" },
            { day: "Jum", value: 95, gmv: "Rp 7.8M" },
            { day: "Sab", value: 130, gmv: "Rp 10.4M" },
            { day: "Min", value: 142, gmv: "Rp 11.2M" }
        ];

        return days.map(d => `
            <div style="flex: 1; display: flex; flex-direction: column; align-items: center; gap: 8px; height: 100%; justify-content: flex-end;">
                <span style="font-size: 11px; font-weight: 700; color: var(--text-muted);">${d.value}</span>
                <div style="width: 100%; max-width: 38px; height: ${d.value * 1.2}px; background: linear-gradient(180deg, var(--primary-mint), var(--primary-emerald)); border-radius: 8px; box-shadow: 0 4px 8px rgba(13, 148, 136, 0.2);"></div>
                <span style="font-size: 12px; font-weight: 700; color: var(--text-main);">${d.day}</span>
            </div>
        `).join("");
    },

    renderRecentOrdersTable: function() {
        const formatRupiah = (num) => "Rp " + new Intl.NumberFormat("id-ID").format(num);

        return PijatProData.liveOrders.map(order => {
            let statusPill = "";
            if (order.status === "ON_THE_WAY") {
                statusPill = `<span class="pill pill-warning">🛵 Menuju Lokasi (~${order.etaMinutes} mnt)</span>`;
            } else if (order.status === "IN_TREATMENT") {
                statusPill = `<span class="pill pill-info">💆‍♂️ Pijat Berlangsung (${order.remainingMinutes} mnt)</span>`;
            } else if (order.status === "COMPLETED") {
                statusPill = `<span class="pill pill-success">✓ Selesai (★ ${order.rating})</span>`;
            }

            return `
                <tr>
                    <td style="font-weight: 700; color: var(--primary-dark);">${order.id}</td>
                    <td>
                        <div style="font-weight: 700;">${order.customerName}</div>
                        <div style="font-size: 11.5px; color: var(--text-muted);">${order.location}</div>
                    </td>
                    <td>
                        <div style="font-weight: 700;">${order.therapistName}</div>
                        <div style="font-size: 11.5px; color: var(--text-muted);">ID: ${order.therapistId}</div>
                    </td>
                    <td>
                        <div style="font-weight: 600;">${order.serviceName}</div>
                        <div style="font-size: 11.5px; color: var(--text-muted);">${order.durationMinutes} Menit • ${order.aroma}</div>
                    </td>
                    <td>
                        <div style="font-weight: 700;">${formatRupiah(order.totalAmount)}</div>
                        <div style="font-size: 11.5px; color: var(--primary-dark); font-weight: 600;">Platform: ${formatRupiah(order.platformFee)}</div>
                    </td>
                    <td>${statusPill}</td>
                    <td style="font-size: 12px; color: var(--text-muted);">${order.createdAt}</td>
                </tr>
            `;
        }).join("");
    }
};
