// Client per le API REST del backend
class APIClient {
    constructor() {
        // Rileva automaticamente il context path dalla URL corrente
        const contextPath = window.location.pathname.split('/')[1];
        if (contextPath && contextPath !== '') {
            this.baseURL = `/${contextPath}/api`;
        } else {
            this.baseURL = '/api';
        }

        // Debug: mostra il path che stiamo usando
        console.log('🔗 [API] Context path rilevato:', window.location.pathname);
        console.log('🔗 [API] Base URL configurato:', this.baseURL);

        this.defaultHeaders = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        };
    }

    // Metodo generico per le chiamate HTTP
    async request(endpoint, options = {}) {
        const url = `${this.baseURL}${endpoint}`;
        const isLoginRequest = endpoint === '/login';

        const config = {
            method: options.method || 'GET',
            headers: { ...this.defaultHeaders, ...options.headers },
            credentials: 'same-origin',
            ...options
        };

        if (config.body && !(config.body instanceof FormData)) {
            config.body = JSON.stringify(config.body);
        }

        try {
            console.log(`🔗 [API] ${config.method} ${url}`);
            console.log('🔗 [API] Full URL:', `http://localhost:8080${url}`);
            if (config.body) {
                console.log('🔗 [API] Request Body:', config.body);
            }

            const response = await fetch(url, config);

            // Log dettagliato della risposta
            console.log(`📡 [API] Response Status: ${response.status}`);
            console.log(`📡 [API] Response Headers:`, [...response.headers.entries()]);

            // Per le richieste di login, 401 è normale se le credenziali sono sbagliate
            if (response.status === 401 && !isLoginRequest) {
                throw new Error('Sessione scaduta. Effettua nuovamente il login.');
            }

            if (!response.ok) {
                let errorMessage = `Errore HTTP ${response.status}`;

                try {
                    const contentType = response.headers.get('content-type');
                    if (contentType && contentType.includes('application/json')) {
                        const errorData = await response.json();
                        errorMessage = errorData.message || errorData.error || errorMessage;
                    } else {
                        const errorText = await response.text();
                        console.log('📡 [API] Response Text:', errorText);

                        if (errorText) {
                            // Se è HTML, estrai solo il titolo
                            const titleMatch = errorText.match(/<title>(.*?)<\/title>/);
                            if (titleMatch) {
                                errorMessage = titleMatch[1];
                            } else if (errorText.includes('401')) {
                                // Per richieste di login con 401
                                if (isLoginRequest) {
                                    errorMessage = 'Username o password non corretti';
                                } else {
                                    errorMessage = 'Accesso non autorizzato';
                                }
                            } else {
                                errorMessage = errorText.substring(0, 100) + '...';
                            }
                        }
                    }
                } catch (parseError) {
                    console.warn('⚠️ [API] Impossibile parsare errore:', parseError);
                    // Fallback per errori di login
                    if (isLoginRequest && response.status === 401) {
                        errorMessage = 'Username o password non corretti';
                    }
                }

                throw new Error(errorMessage);
            }

            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                console.log('📡 [API] Response is not JSON, returning null');
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

    // Metodo per testare manualmente diversi path
    async testPaths() {
        const testPaths = [
            '/aste_online_jakarta_war/api/login',
            '/TIWProjectJS/api/login',
            '/api/login',
            '/TIWProject/api/login'
        ];

        console.log('🧪 [API] Testing possible paths...');

        for (const path of testPaths) {
            try {
                const response = await fetch(`http://localhost:8080${path}`, {
                    method: 'OPTIONS',
                    headers: this.defaultHeaders
                });
                console.log(`✅ [API] Path ${path} - Status: ${response.status}`);
            } catch (error) {
                console.log(`❌ [API] Path ${path} - Error: ${error.message}`);
            }
        }
    }

    // Test specifico per login
    async testLogin() {
        console.log('🧪 [API] Testing login endpoint...');

        try {
            const response = await fetch(`http://localhost:8080${this.baseURL}/login`, {
                method: 'POST',
                headers: this.defaultHeaders,
                body: JSON.stringify({
                    username: 'admin',
                    password: 'admin123'
                })
            });

            console.log('🧪 [API] Login test - Status:', response.status);
            console.log('🧪 [API] Login test - Headers:', [...response.headers.entries()]);

            if (response.ok) {
                const data = await response.json();
                console.log('🧪 [API] Login test - Response:', data);
            } else {
                const errorText = await response.text();
                console.log('🧪 [API] Login test - Error:', errorText);
            }

        } catch (error) {
            console.error('🧪 [API] Login test - Exception:', error);
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

    async testConnection() {
        try {
            await this.request('/health');
            return true;
        } catch (error) {
            console.warn('⚠️ [API] Test connessione fallito:', error);
            return false;
        }
    }

    async uploadFile(endpoint, file, additionalData = {}) {
        const formData = new FormData();
        formData.append('file', file);

        Object.keys(additionalData).forEach(key => {
            formData.append(key, additionalData[key]);
        });

        return this.request(endpoint, {
            method: 'POST',
            body: formData,
            headers: {}
        });
    }
}