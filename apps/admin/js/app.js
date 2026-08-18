// PijatPro Admin Panel - Main Application Controller
const App = {
    currentRoute: "dashboard",
    audioCtx: null,

    init: function() {
        this.startClock();
        this.bindEvents();
        this.navigateTo("dashboard");
    },

    navigateTo: function(route) {
        this.currentRoute = route;

        // Update nav items
        document.querySelectorAll(".nav-item").forEach(item => {
            if (item.dataset.route === route) {
                item.classList.add("active");
            } else {
                item.classList.remove("active");
            }
        });

        // Update Page Title
        const titleMap = {
            dashboard: "Ringkasan Operasional & KPI",
            map: "Peta Operasional & Dispatch Kota",
            kyc: "Verifikasi Dokumen Mitra (KYC Desk)",
            orders: "Manajemen Pesanan & Sengketa",
            finance: "Keuangan & Persetujuan Tarik Dana",
            catalog: "Katalog Layanan & Voucher Promo",
            sos: "Pusat Tanggap Darurat SOS 24/7"
        };
        document.getElementById("pageTitle").textContent = titleMap[route] || "PijatPro Admin";

        this.renderCurrentView();
    },

    renderCurrentView: function() {
        const container = document.getElementById("viewContainer");
        if (!container) return;

        switch (this.currentRoute) {
            case "dashboard":
                container.innerHTML = DashboardView.render();
                break;
            case "map":
                container.innerHTML = MapView.render();
                setTimeout(() => MapView.initCanvas(), 50);
                break;
            case "kyc":
                container.innerHTML = KycView.render();
                break;
            case "orders":
                container.innerHTML = OrdersView.render();
                break;
            case "finance":
                container.innerHTML = FinanceView.render();
                break;
            case "catalog":
                container.innerHTML = CatalogView.render();
                break;
            case "sos":
                container.innerHTML = SosView.render();
                break;
            default:
                container.innerHTML = DashboardView.render();
        }
    },

    bindEvents: function() {
        document.querySelectorAll(".nav-item").forEach(item => {
            item.addEventListener("click", (e) => {
                e.preventDefault();
                const route = item.dataset.route;
                if (route) this.navigateTo(route);
            });
        });
    },

    startClock: function() {
        const clockEl = document.getElementById("topbarClock");
        const update = () => {
            const now = new Date();
            const options = { hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false, timeZone: "Asia/Jakarta" };
            if (clockEl) clockEl.textContent = now.toLocaleTimeString("id-ID", options) + " WIB";
        };
        update();
        setInterval(update, 1000);
    },

    showToast: function(message) {
        this.playChimeTone();
        const container = document.getElementById("toastContainer");
        if (!container) return;

        const toast = document.createElement("div");
        toast.className = "toast";
        toast.innerHTML = `<span>🔔</span><span>${message}</span>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = "0";
            toast.style.transition = "opacity 0.3s ease";
            setTimeout(() => toast.remove(), 300);
        }, 3500);
    },

    playChimeTone: function() {
        try {
            const ctx = new (window.AudioContext || window.webkitAudioContext)();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = "sine";
            osc.frequency.setValueAtTime(587.33, ctx.currentTime); // D5
            osc.frequency.exponentialRampToValueAtTime(880, ctx.currentTime + 0.15); // A5
            gain.gain.setValueAtTime(0.15, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.3);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start();
            osc.stop(ctx.currentTime + 0.3);
        } catch (_) {}
    },

    playSirenTone: function() {
        try {
            const ctx = new (window.AudioContext || window.webkitAudioContext)();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = "sawtooth";
            osc.frequency.setValueAtTime(440, ctx.currentTime);
            osc.frequency.linearRampToValueAtTime(880, ctx.currentTime + 0.25);
            osc.frequency.linearRampToValueAtTime(440, ctx.currentTime + 0.5);
            gain.gain.setValueAtTime(0.2, ctx.currentTime);
            gain.gain.linearRampToValueAtTime(0.01, ctx.currentTime + 0.6);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start();
            osc.stop(ctx.currentTime + 0.6);
        } catch (_) {}
    }
};

window.addEventListener("DOMContentLoaded", () => {
    App.init();
});
