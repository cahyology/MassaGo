// PijatPro Admin Panel - KYC & Therapist Onboarding View
const KycView = {
    selectedApplicant: null,

    render: function() {
        return `
            <div class="card">
                <div class="card-header">
                    <div>
                        <h3 class="card-title">Antrean Verifikasi Dokumen Mitra Baru (KYC Desk)</h3>
                        <p class="card-subtitle">Validasi KTP, SKCK Kepolisian, dan Sertifikat Pelatihan Keahlian BNSP</p>
                    </div>
                    <span class="pill pill-warning">${PijatProData.kycApplicants.filter(k => k.status === 'PENDING').length} Menunggu Review</span>
                </div>

                <div class="table-container">
                    <table class="custom-table">
                        <thead>
                            <tr>
                                <th>No. KYC</th>
                                <th>Nama Calon Mitra</th>
                                <th>NIK & Domisili</th>
                                <th>Pengalaman & Keahlian</th>
                                <th>Dokumen Diunggah</th>
                                <th>Status</th>
                                <th>Aksi Admin</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${this.renderApplicantsTable()}
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- KYC Document Viewer Modal -->
            <div id="kycModal" class="modal-overlay">
                <div class="modal-container">
                    ${this.renderModalContent()}
                </div>
            </div>
        `;
    },

    renderApplicantsTable: function() {
        return PijatProData.kycApplicants.map(app => {
            let statusPill = `<span class="pill pill-warning">⏳ Menunggu Verifikasi</span>`;
            if (app.status === "APPROVED") statusPill = `<span class="pill pill-success">✓ Disetujui (Aktif)</span>`;
            if (app.status === "REJECTED") statusPill = `<span class="pill pill-error">✕ Ditolak</span>`;

            return `
                <tr>
                    <td style="font-weight: 700; color: var(--primary-dark);">${app.id}</td>
                    <td>
                        <div style="font-weight: 700;">${app.name}</div>
                        <div style="font-size: 11.5px; color: var(--text-muted);">${app.phone} • ${app.gender}</div>
                    </td>
                    <td>
                        <div style="font-family: monospace; font-size: 12px; font-weight: 600;">${app.nik}</div>
                        <div style="font-size: 11.5px; color: var(--text-muted);">${app.city}</div>
                    </td>
                    <td>
                        <div style="font-weight: 600;">${app.experienceYears} Tahun Pengalaman</div>
                        <div style="font-size: 11.5px; color: var(--text-muted);">${app.specialties.join(", ")}</div>
                    </td>
                    <td>
                        <span style="font-size: 12px; font-weight: 600; color: var(--primary-emerald); cursor: pointer;" onclick="KycView.openModal('${app.id}')">
                            📎 4 Dokumen Terlampir
                        </span>
                    </td>
                    <td>${statusPill}</td>
                    <td>
                        <button class="btn btn-secondary btn-sm" onclick="KycView.openModal('${app.id}')">
                            🔍 Periksa Dokumen
                        </button>
                    </td>
                </tr>
            `;
        }).join("");
    },

    openModal: function(id) {
        this.selectedApplicant = PijatProData.kycApplicants.find(k => k.id === id) || null;
        const modal = document.getElementById("kycModal");
        if (modal) {
            modal.querySelector(".modal-container").innerHTML = this.renderModalContent();
            modal.classList.add("active");
        }
    },

    closeModal: function() {
        const modal = document.getElementById("kycModal");
        if (modal) modal.classList.remove("active");
    },

    renderModalContent: function() {
        const app = this.selectedApplicant;
        if (!app) return "";

        return `
            <div class="modal-header">
                <div>
                    <h3 class="modal-title">Verifikasi Dokumen: ${app.name}</h3>
                    <p style="font-size: 12px; color: var(--text-muted);">Nomor Registrasi: ${app.id} • NIK: ${app.nik}</p>
                </div>
                <button class="modal-close" onclick="KycView.closeModal()">✕</button>
            </div>

            <!-- Documents Preview Grid -->
            <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; margin-bottom: 20px;">
                <div style="border: 1px solid var(--border-color); border-radius: 14px; padding: 12px; background: #F8FAFC;">
                    <div style="display: flex; justify-content: space-between; font-size: 12px; font-weight: 700; margin-bottom: 6px;">
                        <span>🪪 KTP Asli</span>
                        <span style="color: var(--status-success);">Terbaca Jelas</span>
                    </div>
                    <div style="height: 90px; background: #E2E8F0; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 12px; color: var(--text-muted);">
                        [Pratinjau KTP: ${app.name}]
                    </div>
                </div>

                <div style="border: 1px solid var(--border-color); border-radius: 14px; padding: 12px; background: #F8FAFC;">
                    <div style="display: flex; justify-content: space-between; font-size: 12px; font-weight: 700; margin-bottom: 6px;">
                        <span>📜 SKCK Kepolisian</span>
                        <span style="color: var(--status-success);">Berlaku s/d 2027</span>
                    </div>
                    <div style="height: 90px; background: #E2E8F0; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 12px; color: var(--text-muted);">
                        [Pratinjau SKCK Kepolisian]
                    </div>
                </div>

                <div style="border: 1px solid var(--border-color); border-radius: 14px; padding: 12px; background: #F8FAFC;">
                    <div style="display: flex; justify-content: space-between; font-size: 12px; font-weight: 700; margin-bottom: 6px;">
                        <span>🎓 Sertifikat BNSP / Pelatihan</span>
                        <span style="color: var(--status-success);">Terverifikasi</span>
                    </div>
                    <div style="height: 90px; background: #E2E8F0; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 12px; color: var(--text-muted);">
                        [Pratinjau Sertifikat Keahlian]
                    </div>
                </div>

                <div style="border: 1px solid var(--border-color); border-radius: 14px; padding: 12px; background: #F8FAFC;">
                    <div style="display: flex; justify-content: space-between; font-size: 12px; font-weight: 700; margin-bottom: 6px;">
                        <span>📸 Pas Foto Mitra</span>
                        <span style="color: var(--status-success);">Latar Belakang Rapi</span>
                    </div>
                    <div style="height: 90px; background: #E2E8F0; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 12px; color: var(--text-muted);">
                        [Pas Foto Resmi]
                    </div>
                </div>
            </div>

            <!-- Notes & Evaluation -->
            <div style="background: var(--bg-main); padding: 14px; border-radius: 14px; margin-bottom: 20px; font-size: 12.5px;">
                <strong>Catatan Sistem:</strong> ${app.notes}
            </div>

            <!-- Approval Actions -->
            <div style="display: flex; justify-content: flex-end; gap: 10px;">
                <button class="btn btn-danger" onclick="KycView.rejectApplicant('${app.id}')">✕ Tolak Pendaftaran</button>
                <button class="btn btn-success" onclick="KycView.approveApplicant('${app.id}')">✓ Setujui Akun Mitra & Aktifkan</button>
            </div>
        `;
    },

    approveApplicant: function(id) {
        const app = PijatProData.kycApplicants.find(k => k.id === id);
        if (app) {
            app.status = "APPROVED";
            App.showToast(`Akun mitra ${app.name} (${app.id}) berhasil disetujui & diaktifkan!`);
            this.closeModal();
            App.renderCurrentView();
        }
    },

    rejectApplicant: function(id) {
        const app = PijatProData.kycApplicants.find(k => k.id === id);
        if (app) {
            app.status = "REJECTED";
            App.showToast(`Pendaftaran mitra ${app.name} telah ditolak.`);
            this.closeModal();
            App.renderCurrentView();
        }
    }
};
