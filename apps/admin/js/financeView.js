// PijatPro Admin Panel - Finance & Payout Desk
const FinanceView = {
    render: function() {
        const formatRupiah = (num) => "Rp " + new Intl.NumberFormat("id-ID").format(num);

        return `
            <!-- Top Finance Summary -->
            <div class="stats-grid" style="grid-template-columns: repeat(3, 1fr);">
                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-title">Total Pendapatan Mitra (80%)</span>
                        <div class="stat-icon-wrapper stat-icon-emerald">💼</div>
                    </div>
                    <div class="stat-value">${formatRupiah(PijatProData.stats.therapistEarnings)}</div>
                    <div class="stat-footer">
                        <span class="trend-up">Siap Dicairkan</span>
                    </div>
                </div>

                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-title">Komisi Bersih Platform (20%)</span>
                        <div class="stat-icon-wrapper stat-icon-amber">📈</div>
                    </div>
                    <div class="stat-value">${formatRupiah(PijatProData.stats.platformRevenue)}</div>
                    <div class="stat-footer">
                        <span class="trend-up">Margin Bersih</span>
                    </div>
                </div>

                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-title">Antrean Penarikan (Withdraw)</span>
                        <div class="stat-icon-wrapper stat-icon-blue">🏦</div>
                    </div>
                    <div class="stat-value">${PijatProData.withdrawals.filter(w => w.status === 'PENDING').length} Permintaan</div>
                    <div class="stat-footer">
                        <span style="color: var(--status-warning); font-weight: 700;">Butuh Persetujuan Segera</span>
                    </div>
                </div>
            </div>

            <!-- Withdrawal Approval Table -->
            <div class="card">
                <div class="card-header">
                    <div>
                        <h3 class="card-title">Antrean Pencairan Dana Mitra Terapis (Auto-Disbursement Desk)</h3>
                        <p class="card-subtitle">Persetujuan transfer saldo dompet ke rekening bank mitra terapis secara instan</p>
                    </div>
                    <button class="btn btn-secondary btn-sm" onclick="App.showToast('Mengekspor laporan keuangan format Excel/CSV...')">📥 Ekspor Laporan</button>
                </div>

                <div class="table-container">
                    <table class="custom-table">
                        <thead>
                            <tr>
                                <th>No. Transaksi</th>
                                <th>Nama Mitra Terapis</th>
                                <th>Nominal Penarikan</th>
                                <th>Bank / E-Wallet Tujuan</th>
                                <th>Nomor Rekening & Nama Pemilik</th>
                                <th>Waktu Pengajuan</th>
                                <th>Status</th>
                                <th>Aksi Admin</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${this.renderWithdrawalsTable()}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    },

    renderWithdrawalsTable: function() {
        const formatRupiah = (num) => "Rp " + new Intl.NumberFormat("id-ID").format(num);

        return PijatProData.withdrawals.map(wd => {
            let statusPill = `<span class="pill pill-warning">⏳ Menunggu Transfer</span>`;
            if (wd.status === "TRANSFERRED") statusPill = `<span class="pill pill-success">✓ Berhasil Ditransfer</span>`;
            if (wd.status === "REJECTED") statusPill = `<span class="pill pill-error">✕ Ditolak</span>`;

            let actionButtons = "";
            if (wd.status === "PENDING") {
                actionButtons = `
                    <div style="display: flex; gap: 6px;">
                        <button class="btn btn-success btn-sm" onclick="FinanceView.approveWithdrawal('${wd.id}')">✓ Setujui & Transfer</button>
                        <button class="btn btn-danger btn-sm" onclick="FinanceView.rejectWithdrawal('${wd.id}')">✕</button>
                    </div>
                `;
            } else {
                actionButtons = `<span style="font-size: 12px; color: var(--text-muted);">Selesai diproses</span>`;
            }

            return `
                <tr>
                    <td style="font-weight: 700; color: var(--primary-dark);">${wd.id}</td>
                    <td>
                        <div style="font-weight: 700;">${wd.therapistName}</div>
                        <div style="font-size: 11.5px; color: var(--text-muted);">ID: ${wd.therapistId}</div>
                    </td>
                    <td>
                        <div style="font-weight: 800; font-size: 14px; color: var(--text-main);">${formatRupiah(wd.amount)}</div>
                    </td>
                    <td>
                        <span class="pill pill-emerald">${wd.bankName}</span>
                    </td>
                    <td>
                        <div style="font-family: monospace; font-weight: 700;">${wd.accountNumber}</div>
                        <div style="font-size: 11.5px; color: var(--text-muted);">a.n. ${wd.accountHolder}</div>
                    </td>
                    <td style="font-size: 12px; color: var(--text-muted);">${wd.requestedAt}</td>
                    <td>${statusPill}</td>
                    <td>${actionButtons}</td>
                </tr>
            `;
        }).join("");
    },

    approveWithdrawal: function(id) {
        const wd = PijatProData.withdrawals.find(w => w.id === id);
        if (wd) {
            wd.status = "TRANSFERRED";
            App.showToast(`Pencairan dana ${wd.id} sebesar Rp ${new Intl.NumberFormat("id-ID").format(wd.amount)} ke ${wd.bankName} (${wd.accountNumber}) BERHASIL ditransfer!`);
            App.renderCurrentView();
        }
    },

    rejectWithdrawal: function(id) {
        const wd = PijatProData.withdrawals.find(w => w.id === id);
        if (wd) {
            wd.status = "REJECTED";
            App.showToast(`Permintaan pencairan dana ${wd.id} telah ditolak.`);
            App.renderCurrentView();
        }
    }
};
