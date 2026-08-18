// PijatPro Admin Panel - Live Operations & City Dispatch Map View
const MapView = {
    selectedFilter: "ALL",
    selectedTherapist: null,

    render: function() {
        return `
            <div style="display: flex; gap: 20px; height: calc(100vh - 120px);">
                <!-- Map Area -->
                <div style="flex: 1; display: flex; flex-direction: column;">
                    <!-- Filter Controls -->
                    <div style="display: flex; gap: 10px; margin-bottom: 14px; align-items: center;">
                        <button class="map-filter-btn ${this.selectedFilter === 'ALL' ? 'active' : ''}" onclick="MapView.setFilter('ALL')">Semua Terapis (${PijatProData.therapists.length})</button>
                        <button class="map-filter-btn ${this.selectedFilter === 'AVAILABLE' ? 'active' : ''}" onclick="MapView.setFilter('AVAILABLE')">🟢 Ready Online</button>
                        <button class="map-filter-btn ${this.selectedFilter === 'ON_THE_WAY' ? 'active' : ''}" onclick="MapView.setFilter('ON_THE_WAY')">🟠 Menuju Lokasi</button>
                        <button class="map-filter-btn ${this.selectedFilter === 'IN_TREATMENT' ? 'active' : ''}" onclick="MapView.setFilter('IN_TREATMENT')">🔵 Sedang Terapi</button>
                    </div>

                    <!-- Canvas Map -->
                    <div class="map-canvas-container" style="flex: 1; height: 100%;">
                        <canvas id="cityMapCanvas" style="width: 100%; height: 100%; display: block;"></canvas>

                        <!-- Floating Legend Overlay -->
                        <div style="position: absolute; bottom: 16px; left: 16px; background: rgba(15, 23, 42, 0.85); backdrop-filter: blur(6px); color: white; padding: 10px 16px; border-radius: 12px; font-size: 11.5px; display: flex; gap: 16px; z-index: 10;">
                            <div style="display: flex; align-items: center; gap: 6px;"><span style="width: 10px; height: 10px; border-radius: 50%; background: #10B981;"></span> Ready Online</div>
                            <div style="display: flex; align-items: center; gap: 6px;"><span style="width: 10px; height: 10px; border-radius: 50%; background: #F59E0B;"></span> Menuju Lokasi</div>
                            <div style="display: flex; align-items: center; gap: 6px;"><span style="width: 10px; height: 10px; border-radius: 50%; background: #3B82F6;"></span> Sedang Terapi</div>
                        </div>
                    </div>
                </div>

                <!-- Therapist Info Drawer -->
                <div style="width: 340px; background: white; border: 1px solid var(--border-color); border-radius: 20px; padding: 20px; display: flex; flex-direction: column; overflow-y: auto;">
                    ${this.renderTherapistDrawer()}
                </div>
            </div>
        `;
    },

    setFilter: function(filter) {
        this.selectedFilter = filter;
        App.renderCurrentView();
        setTimeout(() => this.initCanvas(), 50);
    },

    selectTherapist: function(id) {
        this.selectedTherapist = PijatProData.therapists.find(t => t.id === id) || null;
        App.renderCurrentView();
        setTimeout(() => this.initCanvas(), 50);
    },

    renderTherapistDrawer: function() {
        const therapist = this.selectedTherapist || PijatProData.therapists[0];
        const formatRupiah = (num) => "Rp " + new Intl.NumberFormat("id-ID").format(num);

        let statusBadge = `<span class="pill pill-success">🟢 Siap Terima Order</span>`;
        if (therapist.status === "ON_THE_WAY") statusBadge = `<span class="pill pill-warning">🛵 Menuju Lokasi</span>`;
        if (therapist.status === "IN_TREATMENT") statusBadge = `<span class="pill pill-info">💆‍♂️ Pijat Berlangsung</span>`;

        return `
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
                <h3 style="font-size: 15px; font-weight: 800;">Detail Status Terapis</h3>
                ${statusBadge}
            </div>

            <!-- Profile Card -->
            <div style="text-align: center; padding: 16px; background: var(--bg-main); border-radius: 16px; margin-bottom: 16px;">
                <div style="width: 60px; height: 60px; border-radius: 50%; background: linear-gradient(135deg, var(--primary-emerald), var(--primary-dark)); color: white; display: flex; align-items: center; justify-content: center; font-size: 22px; font-weight: 800; margin: 0 auto 10px;">
                    ${therapist.avatar}
                </div>
                <h4 style="font-size: 15px; font-weight: 800;">${therapist.name}</h4>
                <p style="font-size: 12px; color: var(--text-muted);">${therapist.specialty} • ${therapist.badge}</p>
                <div style="display: flex; justify-content: center; gap: 4px; align-items: center; margin-top: 6px; font-size: 12px; font-weight: 700;">
                    <span style="color: var(--accent-amber);">★ ${therapist.rating}</span>
                    <span style="color: var(--text-muted);">(${therapist.completedOrders} order selesai)</span>
                </div>
            </div>

            <!-- Telemetry & Location Info -->
            <div style="display: flex; flex-direction: column; gap: 10px; font-size: 12.5px; margin-bottom: 20px;">
                <div style="display: flex; justify-content: space-between; padding-bottom: 8px; border-bottom: 1px solid var(--border-color);">
                    <span style="color: var(--text-muted);">Baterai Ponsel:</span>
                    <span style="font-weight: 700;">🔋 ${therapist.battery}%</span>
                </div>
                <div style="display: flex; justify-content: space-between; padding-bottom: 8px; border-bottom: 1px solid var(--border-color);">
                    <span style="color: var(--text-muted);">Kecepatan Gerak:</span>
                    <span style="font-weight: 700;">🛵 ${therapist.speedKmH} km/jam</span>
                </div>
                <div style="display: flex; justify-content: space-between; padding-bottom: 8px; border-bottom: 1px solid var(--border-color);">
                    <span style="color: var(--text-muted);">Area Sekarang:</span>
                    <span style="font-weight: 700; text-align: right; max-width: 170px;">📍 ${therapist.currentLocation.area}</span>
                </div>
                <div style="display: flex; justify-content: space-between; padding-bottom: 8px; border-bottom: 1px solid var(--border-color);">
                    <span style="color: var(--text-muted);">Saldo Dompet:</span>
                    <span style="font-weight: 800; color: var(--primary-dark);">${formatRupiah(therapist.walletBalance)}</span>
                </div>
            </div>

            <!-- Quick Action Buttons -->
            <div style="display: flex; flex-direction: column; gap: 8px; margin-top: auto;">
                <button class="btn btn-primary" onclick="App.showToast('Menghubungi nomor WhatsApp ${therapist.name}...')">💬 Hubungi via WhatsApp</button>
                <button class="btn btn-secondary" onclick="App.showToast('Memeriksa riwayat GPS perjalanan...')">📍 Telusuri Jejak GPS</button>
            </div>
        `;
    },

    initCanvas: function() {
        const canvas = document.getElementById("cityMapCanvas");
        if (!canvas) return;
        const ctx = canvas.getContext("2d");

        // High-DPI scaling
        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width * 2;
        canvas.height = rect.height * 2;
        ctx.scale(2, 2);

        const width = rect.width;
        const height = rect.height;

        // Draw Map Background
        ctx.fillStyle = "#EBF1F5";
        ctx.fillRect(0, 0, width, height);

        // Draw Road Grids
        ctx.strokeStyle = "#FFFFFF";
        ctx.lineWidth = 14;

        // Horizontal roads
        ctx.beginPath();
        ctx.moveTo(0, height * 0.25);
        ctx.lineTo(width, height * 0.25);
        ctx.moveTo(0, height * 0.55);
        ctx.lineTo(width, height * 0.55);
        ctx.moveTo(0, height * 0.8);
        ctx.lineTo(width, height * 0.8);
        ctx.stroke();

        // Vertical & diagonal roads
        ctx.beginPath();
        ctx.moveTo(width * 0.25, 0);
        ctx.lineTo(width * 0.25, height);
        ctx.moveTo(width * 0.6, 0);
        ctx.lineTo(width * 0.6, height);
        ctx.moveTo(width * 0.85, 0);
        ctx.lineTo(width * 0.85, height);
        ctx.stroke();

        // Polyline for ON_THE_WAY route
        ctx.strokeStyle = "rgba(13, 148, 136, 0.4)";
        ctx.lineWidth = 8;
        ctx.beginPath();
        ctx.moveTo(width * 0.25, height * 0.25);
        ctx.lineTo(width * 0.6, height * 0.55);
        ctx.lineTo(width * 0.85, height * 0.55);
        ctx.stroke();

        // Dashed route
        ctx.strokeStyle = "#0D9488";
        ctx.lineWidth = 3;
        ctx.setLineDash([8, 6]);
        ctx.beginPath();
        ctx.moveTo(width * 0.25, height * 0.25);
        ctx.lineTo(width * 0.6, height * 0.55);
        ctx.lineTo(width * 0.85, height * 0.55);
        ctx.stroke();
        ctx.setLineDash([]);

        // Draw Area Landmark Labels
        ctx.fillStyle = "#64748B";
        ctx.font = "bold 11px Plus Jakarta Sans";
        ctx.fillText("KUNINGAN", width * 0.28, height * 0.22);
        ctx.fillText("SCBD SUDIRMAN", width * 0.62, height * 0.52);
        ctx.fillText("PONDOK INDAH", width * 0.15, height * 0.76);
        ctx.fillText("MENTENG", width * 0.55, height * 0.15);

        // Draw Therapist Markers
        const markers = [
            { id: "TRP-01", x: width * 0.35, y: height * 0.25, name: "Ibu Sri", status: "AVAILABLE", color: "#10B981" },
            { id: "TRP-02", x: width * 0.6, y: height * 0.55, name: "Bpk. Budi", status: "ON_THE_WAY", color: "#F59E0B" },
            { id: "TRP-03", x: width * 0.25, y: height * 0.8, name: "Ibu Ratna", status: "IN_TREATMENT", color: "#3B82F6" },
            { id: "TRP-04", x: width * 0.75, y: height * 0.25, name: "Bpk. Agus", status: "AVAILABLE", color: "#10B981" },
            { id: "TRP-05", x: width * 0.6, y: height * 0.2, name: "Ibu Dewi", status: "AVAILABLE", color: "#10B981" }
        ];

        const filtered = this.selectedFilter === "ALL" 
            ? markers 
            : markers.filter(m => m.status === this.selectedFilter);

        filtered.forEach(m => {
            // Pulse circle
            ctx.fillStyle = m.color + "33";
            ctx.beginPath();
            ctx.arc(m.x, m.y, 18, 0, Math.PI * 2);
            ctx.fill();

            // Core pin
            ctx.fillStyle = m.color;
            ctx.beginPath();
            ctx.arc(m.x, m.y, 9, 0, Math.PI * 2);
            ctx.fill();
            ctx.strokeStyle = "#FFFFFF";
            ctx.lineWidth = 2.5;
            ctx.stroke();

            // Label tag
            ctx.fillStyle = "#0F172A";
            ctx.font = "bold 11px Plus Jakarta Sans";
            ctx.fillText(m.name, m.x - 18, m.y - 14);
        });
    }
};
