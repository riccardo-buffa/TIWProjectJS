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

            const response = await fetch(url, config);

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
                    // Fallback per errori di login
                    if (isLoginRequest && response.status === 401) {
                        errorMessage = 'Username o password non corretti';
                    }
                }

                throw new Error(errorMessage);
            }

            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                return null;
            }

            const data = await response.json();
            return data;

        } catch (error) {
            console.error(`[API] Errore ${config.method} ${url}:`, error);
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

    async createArticolo(articolo) {
        // Se c'è un file immagine, usa FormData
        if (articolo.immagine instanceof File) {
            const formData = new FormData();
            formData.append('codice', articolo.codice);
            formData.append('nome', articolo.nome);
            formData.append('descrizione', articolo.descrizione);
            formData.append('prezzo', articolo.prezzo.toString());
            formData.append('immagine', articolo.immagine);

            return this.request('/articoli', {
                method: 'POST',
                body: formData,
                headers: {} // Rimuovi Content-Type per FormData
            });
        } else {
            // Altrimenti usa JSON (backward compatibility)
            return this.request('/articoli', {
                method: 'POST',
                body: articolo
            });
        }
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

}