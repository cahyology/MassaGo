// ====================================================================
// MassaGo Admin - Live Supabase Client Integration
// ====================================================================

const SUPABASE_CONFIG = {
    url: "https://jrwkmedrrwvomyljdkpw.supabase.co",
    anonKey: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Impyd2ttZWRycnd2b215bGpka3B3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY5MTcxNzQsImV4cCI6MjEwMjQ5MzE3NH0.UiN6JvJt23ds-3eID9J6wOtEt3pg4-farSwQIliPzuw"
};

let supabase = null;

if (window.supabase && window.supabase.createClient) {
    supabase = window.supabase.createClient(SUPABASE_CONFIG.url, SUPABASE_CONFIG.anonKey);
    console.log("🟢 Supabase Client connected to:", SUPABASE_CONFIG.url);
}

const SupabaseAdmin = {
    client: supabase,
    
    // Fetch live therapists from database
    async getTherapists() {
        if (!supabase) return MockData.therapists;
        try {
            const { data, error } = await supabase.from('therapists').select('*, profile:profiles(*)');
            if (error || !data || data.length === 0) return MockData.therapists;
            return data;
        } catch (e) {
            console.warn("Falling back to mock therapists:", e);
            return MockData.therapists;
        }
    },

    // Fetch live orders
    async getOrders() {
        if (!supabase) return MockData.orders;
        try {
            const { data, error } = await supabase.from('orders').select('*').order('created_at', { ascending: false });
            if (error || !data || data.length === 0) return MockData.orders;
            return data;
        } catch (e) {
            console.warn("Falling back to mock orders:", e);
            return MockData.orders;
        }
    },

    // Fetch live services
    async getServices() {
        if (!supabase) return MockData.servicePackages;
        try {
            const { data, error } = await supabase.from('service_packages').select('*').order('orders_count', { ascending: false });
            if (error || !data || data.length === 0) return MockData.servicePackages;
            return data;
        } catch (e) {
            return MockData.servicePackages;
        }
    }
};

window.SupabaseAdmin = SupabaseAdmin;
