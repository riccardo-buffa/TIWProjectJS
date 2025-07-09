// Client per le API REST del backend
class APIClient {
    constructor() {
        this.baseURL = '/TIWProjectJS/api';
        this.defaultHeaders = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        };
    }

    // Metodo generico per le chiamate HTTP
    async request(endpoint, options = {}) {
        const url = `${this.baseURL}${endpoint}`;

        const config = {
            method: options.method || 'GET',
            headers: { ...this.defaultHeaders, ...options.headers },
            credentials: 'same-origin', // Include cookies di sessione
            ...options
        };

        // Se c'è un body e non è FormData, convertilo in JSON
        if (config.body && !(config.body instanceof FormData)) {
            config.body = JSON.stringify(config.body);
        }

        try {
            console.log(`🔗 [API] ${config.method} ${url}`);

            const response = await fetch(url, config);

            // Se la risposta è 401, l'utente deve rifare login
            if (response.status === 401) {
                throw new Error('Sessione scaduta. Effettua nuovamente il login.');
            }

            // Se la risposta non è ok, prova a estrarre il messaggio di errore
            if (!response.ok) {
                let errorMessage = `Errore HTTP ${response.status}`;

                try {
                    const contentType = response.headers.get('content-type');
                    if (contentType && contentType.includes('application/json')) {
                        const errorData = await response.json();
                        errorMessage = errorData.message || errorData.error || errorMessage;
                    } else {
                        const errorText = await response.text();
                        if (errorText) errorMessage = errorText;
                    }
                } catch (parseError) {
                    console.warn('⚠️ [API] Impossibile parsare errore:', parseError);
                }

                throw new Error(errorMessage);
            }

            // Se la risposta è vuota, ritorna null
            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                return null;
            }

            const data = await response.json();
            console.log(`✅ [API] Risposta ricevuta:`, data);
            return data;

        } catch (error) {
            console.error(`❌ [API] Errore ${config.method} ${url}:`, error);
            throw error;
        }
    }

    // ===== AUTENTICAZIONE =====

    async login(username, password) {
        return this.request('/login', {
            method: 'POST',
            body: { username, password }
        });
    }

    async register(userData) {
        return this.request('/register', {
            method: 'POST',
            body: userData
        });
    }

    async logout() {
        return this.request('/logout', {
            method: 'POST'
        });
    }

    async getCurrentUser() {
        return this.request('/utenti/current');
    }

    // ===== ARTICOLI =====

    async createArticolo(articolo) {
        return this.request('/articoli', {
            method: 'POST',
            body: articolo
        });
    }

    async getArticoliDisponibili() {
        return this.request('/articoli/disponibili');
    }

    async getArticoliByIds(ids) {
        if (!ids || ids.length === 0) return [];
        const idsParam = ids.join(',');
        return this.request(`/articoli?ids=${idsParam}`);
    }

    // ===== ASTE =====

    async createAsta(asta) {
        return this.request('/aste', {
            method: 'POST',
            body: asta
        });
    }

    async getAsteByVenditore(chiuse = false) {
        return this.request(`/aste/venditore?chiuse=${chiuse}`);
    }

    async searchAste(parolaChiave) {
        const encodedQuery = encodeURIComponent(parolaChiave);
        return this.request(`/aste/search?q=${encodedQuery}`);
    }

    async getAstaById(id) {
        return this.request(`/aste/${id}`);
    }

    async getAsteVinte() {
        return this.request('/aste/vinte');
    }

    async getAsteByIds(ids) {
        if (!ids || ids.length === 0) return [];
        const idsParam = ids.join(',');
        return this.request(`/aste?ids=${idsParam}`);
    }

    async chiudiAsta(astaId) {
        return this.request(`/aste/${astaId}/chiudi`, {
            method: 'POST'
        });
    }

    // ===== OFFERTE =====

    async createOfferta(offerta) {
        return this.request('/offerte', {
            method: 'POST',
            body: offerta
        });
    }

    async getOfferteByAsta(astaId) {
        return this.request(`/offerte/asta/${astaId}`);
    }

    async getOffertaMassima(astaId) {
        return this.request(`/offerte/asta/${astaId}/massima`);
    }

    // ===== UTENTI =====

    async getUtenteById(id) {
        return this.request(`/utenti/${id}`);
    }

    // ===== UTILITY =====

    // Metodo per testare la connettività dell'API
    async testConnection() {
        try {
            await this.request('/health');
            return true;
        } catch (error) {
            console.warn('⚠️ [API] Test connessione fallito:', error);
            return false;
        }
    }

    // Metodo per gestire upload di file (se necessario in futuro)
    async uploadFile(endpoint, file, additionalData = {}) {
        const formData = new FormData();
        formData.append('file', file);

        // Aggiungi dati aggiuntivi
        Object.keys(additionalData).forEach(key => {
            formData.append(key, additionalData[key]);
        });

        return this.request(endpoint, {
            method: 'POST',
            body: formData,
            headers: {} // Rimuovi Content-Type per FormData
        });
    }
}