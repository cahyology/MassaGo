// PijatPro Admin Panel - Orders & Operations View
const OrdersView = {
    selectedOrder: null,

    render: function() {
        const formatRupiah = (num) => "Rp " + new Intl.NumberFormat("id-ID").format(num);

        return `
            <div class="card">
                <div class="card-header">
                    <div>
                        <h3 class="card-title">Manajemen & Monitoring Pesanan Lapangan</h3>
                        <p class="card-subtitle">Pelacakan alur pengerjaan terapi dan mitigasi sengketa pelanggan</p>
                    </div>
                    <div style="display: flex; gap: 8px;">
                        <input type="text" placeholder="Cari ID, Pelanggan, atau Terapis..." style="padding: 8px 14px; border: 1px solid var(--border-color); border-radius: 10px; font-size: 13px; outline: none;" />
                    </div>
                </div>

                <div class="table-container">
                    <table class="custom-table">
                        <thead>
                            <tr>
                                <th>Order ID</th>
                                <th>Pelanggan</th>
                                <th>Mitra Terapis</th>
                                <th>Paket & Durasi</th>
                                <th>Metode Bayar</th>
                                <th>Total & Bagi Hasil 80/20</th>
                                <th>Status</th>
                                <th>Aksi</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${this.renderOrdersTable()}
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Order Detail Modal -->
            <div id="orderModal" class="modal-overlay">
                <div class="modal-container">
                    ${this.renderOrderModalContent()}
                </div>
            </div>
        `;
    },

    renderOrdersTable: function() {
        const formatRupiah = (num) => "Rp " + new Intl.NumberFormat("id-ID").format(num);

        return PijatProData.liveOrders.map(order => {
            let statusPill = `<span class="pill pill-warning">🛵 Menuju Lokasi</span>`;
            if (order.status === "IN_TREATMENT") statusPill = `<span class="pill pill-info">💆‍♂️ Pijat Berlangsung</span>`;
            if (order.status === "COMPLETED") statusPill = `<span class="pill pill-success">✓ Selesai</span>`;

            return `
                <tr>
                    <td style="font-weight: 700; color: var(--primary-dark);">${order.id}</td>
                    <td>
                        <div style="font-weight: 700;">${order.customerName}</div>
                        <div style="font-size: 11.5px; color: var(--text-muted);">${order.customerPhone}</div>
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
                        <span class="pill pill-emerald" style="font-size: 11px;">${order.paymentMethod}</span>
                    </td>
                    <td>
                        <div style="font-weight: 700;">${formatRupiah(order.totalAmount)}</div>
                        <div style="font-size: 11.5px; color: var(--primary-dark);">Mitra 80%: ${formatRupiah(order.therapistShare)}</div>
                    </td>
                    <td>${statusPill}</td>
                    <td>
                        <button class="btn btn-secondary btn-sm" onclick="OrdersView.openModal('${order.id}')">
                            Detail Order →
                        </button>
                    </td>
                </tr>
            `;
        }).join("");
    },

    openModal: function(id) {
        this.selectedOrder = PijatProData.liveOrders.find(o => o.id === id) || null;
        const modal = document.getElementById("orderModal");
        if (modal) {
            modal.querySelector(".modal-container").innerHTML = this.renderOrderModalContent();
            modal.classList.add("active");
        }
    },

    closeModal: function() {
        const modal = document.getElementById("orderModal");
        if (modal) modal.classList.remove("active");
    },

    renderOrderModalContent: function() {
        const order = this.selectedOrder;
        if (!order) return "";
        const formatRupiah = (num) => "Rp " + new Intl.NumberFormat("id-ID").format(num);

        return `
            <div class="modal-header">
                <div>
                    <h3 class="modal-title">Rincian Pesanan: ${order.id}</h3>
                    <p style="font-size: 12px; color: var(--text-muted);">Dibuat pada: ${order.createdAt}</p>
                </div>
                <button class="modal-close" onclick="OrdersView.closeModal()">✕</button>
            </div>

            <div style="display: flex; flex-direction: column; gap: 14px; font-size: 13px; margin-bottom: 20px;">
                <div style="background: var(--bg-main); padding: 14px; border-radius: 14px;">
                    <div style="font-weight: 800; margin-bottom: 6px;">📍 Lokasi Pelanggan:</div>
                    <div>${order.customerName} (${order.customerPhone})</div>
                    <div style="color: var(--text-muted); font-size: 12px;">${order.location}</div>
                    <div style="color: var(--primary-dark); font-size: 11.5px; margin-top: 4px; font-weight: 600;">Catatan: ${order.notes}</div>
                </div>

                <div style="background: var(--bg-main); padding: 14px; border-radius: 14px;">
                    <div style="font-weight: 800; margin-bottom: 6px;">💆‍♂️ Layanan & Mitra Bertugas:</div>
                    <div>${order.serviceName} (${order.durationMinutes} Menit)</div>
                    <div style="color: var(--text-muted); font-size: 12px;">Aroma: ${order.aroma}</div>
                    <div style="margin-top: 4px; font-weight: 700;">Terapis: ${order.therapistName} (${order.therapistId})</div>
                </div>

                <div style="border: 1px solid var(--border-color); padding: 14px; border-radius: 14px;">
                    <div style="font-weight: 800; margin-bottom: 8px;">💰 Rincian Pembagian Hasil (80 / 20):</div>
                    <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
                        <span style="color: var(--text-muted);">Total Bayar Pelanggan:</span>
                        <span style="font-weight: 800;">${formatRupiah(order.totalAmount)}</span>
                    </div>
                    <div style="display: flex; justify-content: space-between; margin-bottom: 4px; color: var(--primary-dark); font-weight: 700;">
                        <span>Hak Bersih Mitra Terapis (80%):</span>
                        <span>${formatRupiah(order.therapistShare)}</span>
                    </div>
                    <div style="display: flex; justify-content: space-between; color: var(--accent-amber-dark); font-weight: 700;">
                        <span>Komisi Operasional Platform (20%):</span>
                        <span>${formatRupiah(order.platformFee)}</span>
                    </div>
                </div>
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 10px;">
                <button class="btn btn-secondary" onclick="App.showToast('Menghubungi pelanggan ${order.customerName}...')">📞 Hubungi Pelanggan</button>
                <button class="btn btn-primary" onclick="OrdersView.reassignOrder('${order.id}')">🔄 Alihkan ke Terapis Lain (Re-assign)</button>
            </div>
        `;
    },

    reassignOrder: function(orderId) {
        App.showToast(`Mencari mitra terdekat alternatif untuk mengalihkan order ${orderId}...`);
        this.closeModal();
    }
};
