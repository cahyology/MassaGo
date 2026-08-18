// PijatPro Admin Panel - Catalog & Promo Management View
const CatalogView = {
    render: function() {
        const formatRupiah = (num) => "Rp " + new Intl.NumberFormat("id-ID").format(num);

        return `
            <div style="display: flex; flex-direction: column; gap: 24px;">
                <!-- Services Catalog Management -->
                <div class="card">
                    <div class="card-header">
                        <div>
                            <h3 class="card-title">Katalog Paket Layanan Pijat & Spa</h3>
                            <p class="card-subtitle">Pengaturan tarif durasi 60/90/120 menit dan status aktif di aplikasi customer</p>
                        </div>
                        <button class="btn btn-primary btn-sm" onclick="App.showToast('Membuka form penambahan paket layanan baru...')">+ Tambah Paket Baru</button>
                    </div>

                    <div class="table-container">
                        <table class="custom-table">
                            <thead>
                                <tr>
                                    <th>Paket Layanan</th>
                                    <th>Kategori</th>
                                    <th>Tarif 60 Mnt</th>
                                    <th>Tarif 90 Mnt</th>
                                    <th>Tarif 120 Mnt</th>
                                    <th>Total Order</th>
                                    <th>Status</th>
                                    <th>Aksi</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${this.renderServicesTable()}
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Voucher & Promo Management -->
                <div class="card">
                    <div class="card-header">
                        <div>
                            <h3 class="card-title">Manajemen Kode Voucher Promo Diskon</h3>
                            <p class="card-subtitle">Pengaturan kuota pemakaian dan diskon promo pelanggan</p>
                        </div>
                        <button class="btn btn-primary btn-sm" onclick="CatalogView.createVoucherPrompt()">+ Buat Voucher Baru</button>
                    </div>

                    <div class="table-container">
                        <table class="custom-table">
                            <thead>
                                <tr>
                                    <th>Kode Promo</th>
                                    <th>Besaran Diskon</th>
                                    <th>Min. Belanja</th>
                                    <th>Total Digunakan</th>
                                    <th>Status</th>
                                    <th>Aksi</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${this.renderVouchersTable()}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;
    },

    renderServicesTable: function() {
        const formatRupiah = (num) => num > 0 ? "Rp " + new Intl.NumberFormat("id-ID").format(num) : "-";

        return PijatProData.services.map(srv => `
            <tr>
                <td>
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <span style="font-size: 22px;">${srv.icon}</span>
                        <div>
                            <div style="font-weight: 700;">${srv.name}</div>
                            <div style="font-size: 11.5px; color: var(--text-muted);">${srv.id}</div>
                        </div>
                    </div>
                </td>
                <td><span class="pill pill-emerald">${srv.category}</span></td>
                <td style="font-weight: 600;">${formatRupiah(srv.price60)}</td>
                <td style="font-weight: 700; color: var(--primary-dark);">${formatRupiah(srv.price90)}</td>
                <td style="font-weight: 600;">${formatRupiah(srv.price120)}</td>
                <td style="font-size: 12.5px; color: var(--text-muted);">${srv.ordersCount} order</td>
                <td><span class="pill pill-success">✓ Aktif</span></td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick="App.showToast('Membuka editor tarif paket ${srv.name}...')">✏️ Edit</button>
                </td>
            </tr>
        `).join("");
    },

    renderVouchersTable: function() {
        const formatRupiah = (num) => "Rp " + new Intl.NumberFormat("id-ID").format(num);

        return PijatProData.vouchers.map(v => `
            <tr>
                <td>
                    <span style="font-family: monospace; font-weight: 800; color: var(--primary-dark); background: var(--primary-light); padding: 4px 8px; border-radius: 6px;">
                        ${v.code}
                    </span>
                </td>
                <td style="font-weight: 700; color: var(--accent-amber-dark);">${v.discount}</td>
                <td>${formatRupiah(v.minSpend)}</td>
                <td style="font-weight: 600;">${v.usedCount} kali</td>
                <td><span class="pill pill-success">✓ ${v.status}</span></td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick="App.showToast('Menonaktifkan voucher ${v.code}...')">Nonaktifkan</button>
                </td>
            </tr>
        `).join("");
    },

    createVoucherPrompt: function() {
        const code = prompt("Masukkan Kode Voucher Baru (contoh: MERDEKA50):");
        if (code) {
            PijatProData.vouchers.push({
                code: code.toUpperCase(),
                discount: "Potongan Rp 25.000",
                minSpend: 120000,
                usedCount: 0,
                status: "AKTIF"
            });
            App.showToast(`Voucher ${code.toUpperCase()} berhasil dibuat!`);
            App.renderCurrentView();
        }
    }
};
