// PijatPro Admin Panel - SOS Emergency Response Console
const SosView = {
    render: function() {
        return `
            <div style="display: flex; flex-direction: column; gap: 20px;">
                <!-- SOS Emergency Alert Banner -->
                <div style="background: linear-gradient(135deg, #7F1D1D, #991B1B); border-radius: 20px; padding: 24px; color: white; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 10px 25px rgba(239, 68, 68, 0.25);">
                    <div style="display: flex; align-items: center; gap: 18px;">
                        <div style="width: 56px; height: 56px; border-radius: 50%; background: #EF4444; display: flex; align-items: center; justify-content: center; font-size: 28px; animation: pulse 1s infinite;">
                            🚨
                        </div>
                        <div>
                            <h2 style="font-size: 20px; font-weight: 800; letter-spacing: -0.5px;">Pusat Tanggap Darurat & Keselamatan Mitra (24/7 Safety Desk)</h2>
                            <p style="font-size: 13px; color: #FCA5A5; margin-top: 2px;">Menerima sinyal darurat langsung dari tombol SOS aplikasi mitra terapis dan pelanggan secara real-time</p>
                        </div>
                    </div>

                    <button class="btn btn-danger" style="padding: 12px 20px; font-size: 13.5px;" onclick="SosView.triggerSimulatedEmergency()">
                        ⚠️ Simulasi Sinyal SOS Darurat Masuk
                    </button>
                </div>

                <!-- Emergency Dispatch & Contact Grid -->
                <div class="dashboard-grid-2">
                    <!-- Emergency Response Steps -->
                    <div class="card">
                        <div class="card-header">
                            <h3 class="card-title">SOP Penanganan Insiden Darurat</h3>
                            <span class="pill pill-emerald">SOP Terstandar</span>
                        </div>

                        <div style="display: flex; flex-direction: column; gap: 12px; font-size: 13px;">
                            <div style="display: flex; gap: 12px; align-items: flex-start;">
                                <span style="background: var(--primary-light); color: var(--primary-dark); font-weight: 800; width: 26px; height: 26px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">1</span>
                                <div>
                                    <strong>Deteksi Koordinat GPS Otomatis:</strong> Sistem mengunci titik koordinat latitude/longitude terapis saat tombol ditekan.
                                </div>
                            </div>
                            <div style="display: flex; gap: 12px; align-items: flex-start;">
                                <span style="background: var(--primary-light); color: var(--primary-dark); font-weight: 800; width: 26px; height: 26px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">2</span>
                                <div>
                                    <strong>Panggilan Verifikasi 2 Arah:</strong> Operator admin langsung menelepon nomor ponsel mitra terapis.
                                </div>
                            </div>
                            <div style="display: flex; gap: 12px; align-items: flex-start;">
                                <span style="background: var(--primary-light); color: var(--primary-dark); font-weight: 800; width: 26px; height: 26px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">3</span>
                                <div>
                                    <strong>Penerjunan Tim Lapangan & Keamanan:</strong> Menghubungi satpam gedung / aparat setempat terdekat untuk perlindungan fisik.
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Hotline Contacts -->
                    <div class="card">
                        <div class="card-header">
                            <h3 class="card-title">Hotline Koordinasi Keamanan</h3>
                        </div>
                        <div style="display: flex; flex-direction: column; gap: 10px; font-size: 13px;">
                            <div style="display: flex; justify-content: space-between; padding: 10px; background: var(--bg-main); border-radius: 10px;">
                                <span>👮 Kepolisian (Polri Call Center)</span>
                                <strong>110</strong>
                            </div>
                            <div style="display: flex; justify-content: space-between; padding: 10px; background: var(--bg-main); border-radius: 10px;">
                                <span>🚑 Ambulans Gawat Darurat</span>
                                <strong>118 / 119</strong>
                            </div>
                            <div style="display: flex; justify-content: space-between; padding: 10px; background: var(--bg-main); border-radius: 10px;">
                                <span>🛡️ Tim Reaksi Cepat PijatPro</span>
                                <strong>0800-1122-3344</strong>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Past SOS Incidents Log Table -->
                <div class="card">
                    <div class="card-header">
                        <h3 class="card-title">Riwayat Insiden & Penanganan SOS</h3>
                    </div>
                    <div class="table-container">
                        <table class="custom-table">
                            <thead>
                                <tr>
                                    <th>No. Kasus</th>
                                    <th>Mitra Terapis</th>
                                    <th>Titik Lokasi Kejadian</th>
                                    <th>Waktu Kejadian</th>
                                    <th>Status Penanganan</th>
                                    <th>Hasil Investigasi</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${this.renderSosLogTable()}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;
    },

    renderSosLogTable: function() {
        return PijatProData.sosLogs.map(log => `
            <tr>
                <td style="font-weight: 700; color: var(--status-error);">${log.id}</td>
                <td>
                    <div style="font-weight: 700;">${log.therapistName}</div>
                    <div style="font-size: 11.5px; color: var(--text-muted);">${log.therapistPhone}</div>
                </td>
                <td style="font-size: 12.5px;">📍 ${log.location}</td>
                <td style="font-size: 12px; color: var(--text-muted);">${log.time}</td>
                <td><span class="pill pill-success">✓ Selesai Teratasi</span></td>
                <td style="font-size: 12px; color: var(--text-muted);">${log.note}</td>
            </tr>
        `).join("");
    },

    triggerSimulatedEmergency: function() {
        App.playSirenTone();
        alert("🚨 PERINGATAN DARURAT SOS!\n\nMitra Terapis: Ibu Sri Rahayu (ID: TRP-01)\nLokasi: Apartemen Sudirman Tower Lt. 12\n\nSistem telah mengunci koordinat GPS. Hubungi mitra segera!");
        PijatProData.sosLogs.unshift({
            id: "SOS-" + Math.floor(100 + Math.random() * 900),
            therapistName: "Ibu Sri Rahayu",
            therapistPhone: "+62 812-3456-7890",
            location: "Apartemen Sudirman Tower Lt. 12, Jakarta Selatan",
            lat: -6.2088,
            lng: 106.8456,
            time: "Baru saja",
            status: "RESOLVED",
            note: "Simulasi uji kesiapsiagaan operator admin selesai dengan respon cepat."
        });
        App.renderCurrentView();
    }
};
