// Classe principale per l'applicazione SPA
class AsteOnlineApp {
    constructor() {
        this.currentUser = null;
        this.currentPage = null;
        this.lastAction = null;
        this.visitedAuctions = [];
        
        // Carica dati da localStorage se disponibili
        this.loadUserPreferences();
        
        this.init();
    }

    init() {
        console.log('🚀 Inizializzazione Aste Online SPA');
        
        // Event listeners per i pulsanti di navigazione
        document.getElementById('btn-vendo').addEventListener('click', () => this.showVendoPage());
        document.getElementById('btn-acquisto').addEventListener('click', () => this.showAcquistoPage());
        document.getElementById('btn-logout').addEventListener('click', () => this.logout());
        
        // Event listener per il form di login
        document.getElementById('loginForm').addEventListener('submit', (e) => this.handleLogin(e));
        
        // Controlla se l'utente ha già effettuato l'accesso
        if (this.currentUser) {
            this.showMainInterface();
            this.decidePaginaIniziale();
        }
    }

    // Gestione del login
    async handleLogin(event) {
        event.preventDefault();
        
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        
        console.log('🔐 Tentativo di login per:', username);
        
        try {
            const response = await fetch('/TIWProject/api/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password })
            });
            
            if (response.ok) {
                const userData = await response.json();
                this.currentUser = userData;
                console.log('✅ Login successful:', userData);
                
                this.showMainInterface();
                this.decidePaginaIniziale();
            } else {
                const error = await response.text();
                this.showError('❌ ' + error);
            }
        } catch (error) {
            console.error('❌ Errore di login:', error);
            this.showError('❌ Errore di connessione al server');
        }
    }

    // Decide quale pagina mostrare al login
    decidePaginaIniziale() {
        if (!this.currentUser) return;
        
        // Se è il primo accesso, mostra Acquisto
        if (!this.lastAction) {
            console.log('🆕 Primo accesso - Mostro pagina Acquisto');
            this.showAcquistoPage();
            return;
        }
        
        // Se l'ultima azione è stata creare un'asta, mostra Vendo
        if (this.lastAction === 'crea_asta') {
            console.log('📦 Ultima azione: crea asta - Mostro pagina Vendo');
            this.showVendoPage();
        } else {
            // Altrimenti mostra Acquisto con le aste visitate
            console.log('🛒 Mostro pagina Acquisto con aste visitate');
            this.showAcquistoPage();
        }
    }

    // Mostra l'interfaccia principale
    showMainInterface() {
        document.getElementById('login-form').style.display = 'none';
        document.getElementById('navigation').style.display = 'block';
        document.getElementById('user-info').style.display = 'block';
        document.getElementById('main-content').style.display = 'block';
        
        // Aggiorna il nome utente
        document.getElementById('user-name').textContent = this.currentUser.nomeCompleto;
    }

    // Mostra la pagina Vendo
    async showVendoPage() {
        console.log('📦 Caricamento pagina Vendo');
        this.currentPage = 'vendo';
        
        try {
            // Carica dati per la pagina Vendo
            const [asteAperte, asteChiuse, articoli] = await Promise.all([
                this.fetchAsteByVenditore(false),
                this.fetchAsteByVenditore(true),
                this.fetchArticoliDisponibili()
            ]);
            
            const content = this.generateVendoHTML(asteAperte, asteChiuse, articoli);
            document.getElementById('main-content').innerHTML = content;
            
            // Aggiungi event listeners per i form
            this.setupVendoEventListeners();
            
        } catch (error) {
            console.error('❌ Errore caricamento pagina Vendo:', error);
            this.showError('Errore nel caricamento della pagina Vendo');
        }
    }

    // Mostra la pagina Acquisto
    async showAcquistoPage() {
        console.log('🛒 Caricamento pagina Acquisto');
        this.currentPage = 'acquisto';
        
        try {
            // Carica le aste vinte
            const asteVinte = await this.fetchAsteVinte();
            
            // Carica le aste visitate se presenti
            let asteVisitate = [];
            if (this.visitedAuctions.length > 0) {
                asteVisitate = await this.fetchAsteByIds(this.visitedAuctions);
            }
            
            const content = this.generateAcquistoHTML(null, asteVinte, asteVisitate);
            document.getElementById('main-content').innerHTML = content;
            
            // Aggiungi event listeners
            this.setupAcquistoEventListeners();
            
        } catch (error) {
            console.error('❌ Errore caricamento pagina Acquisto:', error);
            this.showError('Errore nel caricamento della pagina Acquisto');
        }
    }

    // Genera HTML per la pagina Vendo
    generateVendoHTML(asteAperte, asteChiuse, articoli) {
        return `
            <!-- Form per creare nuovo articolo -->
            <div class="form-container">
                <h2>🆕 Crea Nuovo Articolo</h2>
                <form id="form-articolo">
                    <div class="form-group">
                        <label for="codice">📋 Codice:</label>
                        <input type="text" id="codice" name="codice" placeholder="Es. ART001" required>
                    </div>
                    <div class="form-group">
                        <label for="nome">🏷️ Nome:</label>
                        <input type="text" id="nome" name="nome" placeholder="Es. iPhone 14 Pro" required>
                    </div>
                    <div class="form-group">
                        <label for="descrizione">📝 Descrizione:</label>
                        <textarea id="descrizione" name="descrizione" rows="3" placeholder="Descrizione dettagliata..." required></textarea>
                    </div>
                    <div class="form-group">
                        <label for="prezzo">💰 Prezzo (€):</label>
                        <input type="number" step="0.01" id="prezzo" name="prezzo" min="0.01" placeholder="0.00" required>
                    </div>
                    <button type="submit" class="btn btn-success">🚀 Crea Articolo</button>
                </form>
            </div>

            <!-- Form per creare nuova asta -->
            ${articoli.length > 0 ? `
            <div class="form-container">
                <h2>🎯 Crea Nuova Asta</h2>
                <form id="form-asta">
                    <div class="form-group">
                        <label>📦 Seleziona Articoli:</label>
                        <div class="checkbox-list">
                            ${articoli.map(art => `
                                <div class="checkbox-item">
                                    <input type="checkbox" id="art${art.id}" name="articoli" value="${art.id}">
                                    <label for="art${art.id}">
                                        <strong>${art.codice} - ${art.nome}</strong>
                                        <br>💰 €${art.prezzo.toFixed(2)}
                                        <br><small>${art.descrizione}</small>
                                    </label>
                                </div>
                            `).join('')}
                        </div>
                    </div>
                    
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
                        <div class="form-group">
                            <label for="rialzo">📈 Rialzo Minimo (€):</label>
                            <input type="number" id="rialzo" name="rialzo" min="1" value="10" required>
                        </div>
                        <div class="form-group">
                            <label for="scadenza">⏰ Scadenza:</label>
                            <input type="text" id="scadenza" name="scadenza" 
                                   placeholder="dd-MM-yyyy HH:mm" required>
                        </div>
                    </div>
                    
                    <button type="submit" class="btn btn-success">🚀 Crea Asta</button>
                </form>
            </div>
            ` : '<div class="alert alert-info">📦 Crea prima alcuni articoli per poter creare aste</div>'}

            <!-- Lista aste aperte -->
            ${this.generateAsteTable(asteAperte, 'Aste Aperte', true)}
            
            <!-- Lista aste chiuse -->
            ${this.generateAsteTable(asteChiuse, 'Aste Chiuse', false)}
        `;
    }

    // Genera HTML per la pagina Acquisto
    generateAcquistoHTML(risultatiRicerca, asteVinte, asteVisitate) {
        return `
            <!-- Form di ricerca -->
            <div class="form-container">
                <h2>🔍 Cerca Aste</h2>
                <form id="form-ricerca">
                    <div class="form-group">
                        <label for="parola-chiave">Parola chiave:</label>
                        <input type="text" id="parola-chiave" name="parolaChiave" 
                               placeholder="Cerca per nome o descrizione..." required>
                    </div>
                    <button type="submit" class="btn">🔍 Cerca</button>
                </form>
            </div>

            <!-- Risultati ricerca -->
            <div id="risultati-ricerca">
                ${risultatiRicerca ? this.generateRisultatiRicerca(risultatiRicerca) : ''}
            </div>

            <!-- Aste visitate -->
            ${asteVisitate.length > 0 ? `
                <div class="table-container">
                    <h3>👁️ Aste Visitate Ancora Aperte</h3>
                    ${this.generateAsteRicercaTable(asteVisitate)}
                </div>
            ` : ''}

            <!-- Aste vinte -->
            ${asteVinte.length > 0 ? `
                <div class="table-container">
                    <h3>🏆 Le Mie Aste Vinte</h3>
                    ${this.generateAsteVinteTable(asteVinte)}
                </div>
            ` : ''}
        `;
    }

    // Genera tabella delle aste per la pagina Vendo
    generateAsteTable(aste, titolo, aperte) {
        if (!aste || aste.length === 0) return '';
        
        return `
            <div class="table-container">
                <h3>${aperte ? '🟢' : '🔴'} ${titolo}</h3>
                <table>
                    <tr>
                        <th>Articoli</th>
                        <th>Prezzo ${aperte ? 'Iniziale' : 'Finale'}</th>
                        <th>Offerta Massima</th>
                        <th>${aperte ? 'Tempo Rimanente' : 'Stato'}</th>
                        <th>Azioni</th>
                    </tr>
                    ${aste.map(asta => `
                        <tr>
                            <td>
                                ${asta.articoli.map(art => `<strong>${art.codice}</strong> - ${art.nome}`).join('<br>')}
                            </td>
                            <td>€${asta.prezzoIniziale.toFixed(2)}</td>
                            <td>€${asta.offertaMassima.toFixed(2)}</td>
                            <td>
                                ${aperte ? 
                                    `<span class="status-${asta.scaduta ? 'closed' : 'open'}">${this.getTempoRimanente(asta.scadenza)}</span>` :
                                    `<span class="${asta.vincitoreId ? 'status-open' : 'status-closed'}">${asta.vincitoreId ? 'VENDUTO' : 'NON VENDUTO'}</span>`
                                }
                            </td>
                            <td>
                                <button class="link-button" onclick="app.mostraDettaglioAsta(${asta.id})">📋 Dettagli</button>
                                ${aperte && asta.scaduta && !asta.chiusa ? 
                                    `<button class="btn btn-danger btn-sm" onclick="app.chiudiAsta(${asta.id})">🔒 Chiudi</button>` : ''
                                }
                            </td>
                        </tr>
                    `).join('')}
                </table>
            </div>
        `;
    }

    // Setup event listeners per la pagina Vendo
    setupVendoEventListeners() {
        // Form creazione articolo
        const formArticolo = document.getElementById('form-articolo');
        if (formArticolo) {
            formArticolo.addEventListener('submit', (e) => this.handleCreaArticolo(e));
        }

        // Form creazione asta
        const formAsta = document.getElementById('form-asta');
        if (formAsta) {
            formAsta.addEventListener('submit', (e) => this.handleCreaAsta(e));
        }
    }

    // Setup event listeners per la pagina Acquisto
    setupAcquistoEventListeners() {
        // Form ricerca
        const formRicerca = document.getElementById('form-ricerca');
        if (formRicerca) {
            formRicerca.addEventListener('submit', (e) => this.handleRicercaAste(e));
        }
    }

    // Gestione creazione articolo
    async handleCreaArticolo(event) {
        event.preventDefault();
        
        const formData = new FormData(event.target);
        const articolo = {
            codice: formData.get('codice'),
            nome: formData.get('nome'),
            descrizione: formData.get('descrizione'),
            prezzo: parseFloat(formData.get('prezzo'))
        };
        
        try {
            const response = await fetch('/TIWProject/api/articoli', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(articolo)
            });
            
            if (response.ok) {
                this.showSuccess('✅ Articolo creato con successo!');
                this.showVendoPage(); // Ricarica la pagina
            } else {
                const error = await response.text();
                this.showError('❌ ' + error);
            }
        } catch (error) {
            console.error('❌ Errore creazione articolo:', error);
            this.showError('❌ Errore di connessione');
        }
    }

    // Gestione creazione asta
    async handleCreaAsta(event) {
        event.preventDefault();
        
        const formData = new FormData(event.target);
        const articoliSelezionati = Array.from(document.querySelectorAll('input[name="articoli"]:checked'))
            .map(cb => parseInt(cb.value));
        
        if (articoliSelezionati.length === 0) {
            this.showError('❌ Seleziona almeno un articolo');
            return;
        }
        
        const asta = {
            articoli: articoliSelezionati,
            rialzoMinimo: parseInt(formData.get('rialzo')),
            scadenza: formData.get('scadenza')
        };
        
        try {
            const response = await fetch('/TIWProject/api/aste', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(asta)
            });
            
            if (response.ok) {
                this.lastAction = 'crea_asta';
                this.saveUserPreferences();
                this.showSuccess('✅ Asta creata con successo!');
                this.showVendoPage(); // Ricarica la pagina
            } else {
                const error = await response.text();
                this.showError('❌ ' + error);
            }
        } catch (error) {
            console.error('❌ Errore creazione asta:', error);
            this.showError('❌ Errore di connessione');
        }
    }

    // Gestione ricerca aste
    async handleRicercaAste(event) {
        event.preventDefault();
        
        const parolaChiave = document.getElementById('parola-chiave').value;
        
        try {
            const response = await fetch(`/TIWProject/api/aste/search?q=${encodeURIComponent(parolaChiave)}`);
            
            if (response.ok) {
                const aste = await response.json();
                const risultatiHTML = this.generateRisultatiRicerca(aste);
                document.getElementById('risultati-ricerca').innerHTML = risultatiHTML;
            } else {
                this.showError('❌ Errore nella ricerca');
            }
        } catch (error) {
            console.error('❌ Errore ricerca:', error);
            this.showError('❌ Errore di connessione');
        }
    }

    // Mostra dettaglio asta
    async mostraDettaglioAsta(astaId) {
        console.log('📋 Caricamento dettaglio asta:', astaId);
        
        // Aggiungi alle aste visitate se siamo nella pagina acquisto
        if (this.currentPage === 'acquisto') {
            if (!this.visitedAuctions.includes(astaId)) {
                this.visitedAuctions.push(astaId);
                this.saveUserPreferences();
            }
        }
        
        try {
            const response = await fetch(`/TIWProject/api/aste/${astaId}`);
            if (response.ok) {
                const asta = await response.json();
                const dettaglioHTML = this.generateDettaglioAsta(asta);
                document.getElementById('main-content').innerHTML = dettaglioHTML;
                this.setupDettaglioEventListeners(astaId);
            }
        } catch (error) {
            console.error('❌ Errore caricamento dettaglio:', error);
            this.showError('❌ Errore nel caricamento');
        }
    }

    // API calls
    async fetchAsteByVenditore(chiuse) {
        const response = await fetch(`/TIWProject/api/aste/venditore?chiuse=${chiuse}`);
        return response.ok ? await response.json() : [];
    }

    async fetchArticoliDisponibili() {
        const response = await fetch('/TIWProject/api/articoli/disponibili');
        return response.ok ? await response.json() : [];
    }

    async fetchAsteVinte() {
        const response = await fetch('/TIWProject/api/aste/vinte');
        return response.ok ? await response.json() : [];
    }

    // Utility functions
    getTempoRimanente(scadenza) {
        const now = new Date();
        const scadenzaDate = new Date(scadenza);
        
        if (now > scadenzaDate) return 'Scaduta';
        
        const diff = scadenzaDate - now;
        const giorni = Math.floor(diff / (1000 * 60 * 60 * 24));
        const ore = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        
        return giorni > 0 ? `${giorni} giorni, ${ore} ore` : `${ore} ore`;
    }

    // Gestione localStorage per preferenze utente
    saveUserPreferences() {
        const preferences = {
            lastAction: this.lastAction,
            visitedAuctions: this.visitedAuctions,
            currentUser: this.currentUser
        };
        
        // Salva per un mese
        const expiryDate = new Date();
        expiryDate.setMonth(expiryDate.getMonth() + 1);
        
        localStorage.setItem('asteOnlinePrefs', JSON.stringify({
            data: preferences,
            expiry: expiryDate.getTime()
        }));
    }

    loadUserPreferences() {
        const stored = localStorage.getItem('asteOnlinePrefs');
        if (!stored) return;
        
        try {
            const parsed = JSON.parse(stored);
            
            // Controlla se non è scaduto
            if (new Date().getTime() > parsed.expiry) {
                localStorage.removeItem('asteOnlinePrefs');
                return;
            }
            
            this.lastAction = parsed.data.lastAction;
            this.visitedAuctions = parsed.data.visitedAuctions || [];
            this.currentUser = parsed.data.currentUser;
            
        } catch (error) {
            console.error('❌ Errore caricamento preferenze:', error);
            localStorage.removeItem('asteOnlinePrefs');
        }
    }

    // Utility per messaggi
    showError(message) {
        this.showMessage(message, 'alert-error');
    }

    showSuccess(message) {
        this.showMessage(message, 'alert-success');
    }

    showMessage(message, className) {
        const existing = document.querySelector('.alert');
        if (existing) existing.remove();
        
        const alert = document.createElement('div');
        alert.className = `alert ${className}`;
        alert.innerHTML = message;
        
        const container = document.getElementById('main-content') || document.querySelector('.container');
        container.insertBefore(alert, container.firstChild);
        
        // Rimuovi dopo 5 secondi
        setTimeout(() => alert.remove(), 5000);
    }

    // Logout
    logout() {
        if (confirm('Sei sicuro di voler uscire?')) {
            localStorage.removeItem('asteOnlinePrefs');
            location.reload();
        }
    }
}

// Inizializza l'applicazione
let app;
document.addEventListener('DOMContentLoaded', () => {
    app = new AsteOnlineApp();
});