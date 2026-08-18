/**
 * PijatIn Landing Page Interactive Controller
 */

// Services Data Catalog
const SERVICES = [
  {
    id: "tradisional",
    name: "Pijat Tradisional Jawa",
    icon: "💆‍♂️",
    desc: "Pijatan relaksasi seluruh tubuh dengan teknik urut Jawa turun-temurun untuk melancarkan peredaran darah, meredakan otot pegal, dan masuk angin.",
    prices: {
      60: 120000,
      90: 160000,
      120: 200000
    }
  },
  {
    id: "refleksi",
    name: "Refleksi Kaki & Akupresur",
    icon: "🦶",
    desc: "Titik akupresur telapak kaki dan betis untuk merangsang organ vital, mengatasi lelah berdiri lama, dan memulihkan stamina harian.",
    prices: {
      60: 110000,
      90: 150000,
      120: 190000
    }
  },
  {
    id: "deep-tissue",
    name: "Deep Tissue & Sport Massage",
    icon: "💪",
    desc: "Tekanan intensif terfokus untuk simpul otot kaku (knot), pemulihan pasca olahraga berat/fitness, dan mengatasi leher kaku.",
    prices: {
      60: 140000,
      90: 190000,
      120: 240000
    }
  },
  {
    id: "scrub-spa",
    name: "Body Scrub & Spa Aromaterapi",
    icon: "🌿",
    desc: "Kombinasi pijat relaksasi dengan lulur herbal alami untuk mengangkat sel kulit mati, mencerahkan kulit, dan menenangkan pikiran.",
    prices: {
      60: 150000,
      90: 200000,
      120: 250000
    }
  },
  {
    id: "ibu-hamil",
    name: "Pijat Ibu Hamil / Pasca Lahir",
    icon: "🤰",
    desc: "Teknik pijatan lembut bersertifikasi khusus untuk mengurangi pegal punggung dan kaki bengkak pada ibu hamil atau relaksasi pasca melahirkan.",
    prices: {
      60: 140000,
      90: 180000,
      120: 230000
    }
  },
  {
    id: "totok-wajah",
    name: "Pijat Relaksasi & Totok Wajah",
    icon: "✨",
    desc: "Relaksasi pundak, leher, kepala disertai totok titik aura wajah untuk melancarkan sirkulasi wajah, mengurangi stres & sakit kepala.",
    prices: {
      60: 130000,
      90: 170000,
      120: 210000
    }
  }
];

const DURATIONS = [
  { minutes: 60, label: "60 Menit", sub: "Pilihan Kilat" },
  { minutes: 90, label: "90 Menit", sub: "Paling Populer ⭐", popular: true },
  { minutes: 120, label: "120 Menit", sub: "Relaksasi Total" }
];

let selectedServiceId = "tradisional";
let selectedDuration = 90;

document.addEventListener("DOMContentLoaded", () => {
  initNavbar();
  renderServiceButtons();
  renderDurationButtons();
  updateCalculationDisplay();
});

// Mobile Navbar Toggle
function initNavbar() {
  const mobileMenuBtn = document.getElementById("mobileMenuBtn");
  const mobileMenu = document.getElementById("mobileMenu");
  const menuIcon = document.getElementById("menuIcon");
  const navbar = document.getElementById("navbar");

  if (mobileMenuBtn && mobileMenu) {
    mobileMenuBtn.addEventListener("click", () => {
      const isHidden = mobileMenu.classList.contains("hidden");
      if (isHidden) {
        mobileMenu.classList.remove("hidden");
        menuIcon.classList.remove("fa-bars");
        menuIcon.classList.add("fa-xmark");
      } else {
        mobileMenu.classList.add("hidden");
        menuIcon.classList.remove("fa-xmark");
        menuIcon.classList.add("fa-bars");
      }
    });

    // Close on link click
    document.querySelectorAll(".mobile-nav-link").forEach((link) => {
      link.addEventListener("click", () => {
        mobileMenu.classList.add("hidden");
        menuIcon.classList.remove("fa-xmark");
        menuIcon.classList.add("fa-bars");
      });
    });
  }

  // Scroll effect
  window.addEventListener("scroll", () => {
    if (window.scrollY > 20) {
      navbar?.classList.add("shadow-md");
    } else {
      navbar?.classList.remove("shadow-md");
    }
  });
}

// Render Service Selection Buttons
function renderServiceButtons() {
  const container = document.getElementById("serviceButtonsContainer");
  if (!container) return;

  container.innerHTML = SERVICES.map((service) => {
    const isActive = service.id === selectedServiceId;
    return `
      <button 
        type="button"
        onclick="selectService('${service.id}')"
        class="service-btn ${isActive ? 'active' : ''} p-3 rounded-2xl border border-slate-200 text-left transition-all hover:border-emerald-400 bg-slate-50 flex items-center gap-3 cursor-pointer group"
      >
        <div class="icon-box w-10 h-10 rounded-xl bg-white text-slate-800 flex items-center justify-center text-lg shadow-xs group-hover:scale-105 transition-transform">
          ${service.icon}
        </div>
        <div class="flex-1 min-w-0">
          <p class="font-bold text-xs sm:text-sm text-slate-900 truncate leading-tight">${service.name}</p>
          <p class="text-[11px] text-slate-500 truncate mt-0.5">Mulai Rp ${(service.prices[60]).toLocaleString("id-ID")}</p>
        </div>
      </button>
    `;
  }).join("");
}

// Render Duration Selection Buttons
function renderDurationButtons() {
  const container = document.getElementById("durationButtonsContainer");
  if (!container) return;

  container.innerHTML = DURATIONS.map((dur) => {
    const isActive = dur.minutes === selectedDuration;
    return `
      <button 
        type="button"
        onclick="selectDuration(${dur.minutes})"
        class="duration-btn ${isActive ? 'active' : ''} p-3 sm:p-4 rounded-2xl border border-slate-200 text-center transition-all hover:border-emerald-400 bg-slate-50 cursor-pointer"
      >
        <p class="font-outfit font-extrabold text-sm sm:text-base text-slate-900 leading-tight">${dur.label}</p>
        <p class="text-[10px] sm:text-xs text-slate-500 mt-1">${dur.sub}</p>
      </button>
    `;
  }).join("");
}

// Selection handlers
window.selectService = function(serviceId) {
  selectedServiceId = serviceId;
  renderServiceButtons();
  updateCalculationDisplay();
};

window.selectDuration = function(durationMinutes) {
  selectedDuration = durationMinutes;
  renderDurationButtons();
  updateCalculationDisplay();
};

// Update Calculation Card
function updateCalculationDisplay() {
  const service = SERVICES.find((s) => s.id === selectedServiceId) || SERVICES[0];
  const price = service.prices[selectedDuration] || service.prices[90];

  const calcServiceName = document.getElementById("calcServiceName");
  const calcServiceDesc = document.getElementById("calcServiceDesc");
  const calcDurationText = document.getElementById("calcDurationText");
  const calcPriceDisplay = document.getElementById("calcPriceDisplay");
  const btnOrderNow = document.getElementById("btnOrderNow");

  if (calcServiceName) calcServiceName.textContent = service.name;
  if (calcServiceDesc) calcServiceDesc.textContent = service.desc;
  if (calcDurationText) calcDurationText.textContent = `${selectedDuration} Menit`;
  if (calcPriceDisplay) calcPriceDisplay.textContent = `Rp ${price.toLocaleString("id-ID")}`;

  if (btnOrderNow) {
    const waText = encodeURIComponent(
      `Halo Admin PijatIn, saya ingin memesan layanan:\n\n` +
      `📌 Layanan: ${service.name}\n` +
      `⏱️ Durasi: ${selectedDuration} Menit\n` +
      `💰 Tarif: Rp ${price.toLocaleString("id-ID")}\n\n` +
      `Mohon info ketersediaan terapis terdekat ke alamat saya. Terima kasih!`
    );
    btnOrderNow.href = `https://wa.me/6281298765432?text=${waText}`;
  }
}
