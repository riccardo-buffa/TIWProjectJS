// Classe principale per l'applicazione SPA Aste Online
class AsteOnlineApp {
    constructor() {
        this.currentUser = null;
        this.currentPage = null;
        this.apiClient = new APIClient();
        this.stateManager = new UserStateManager();

        this.init();
    }

    init() {

        // Event listeners per i pulsanti di navigazione
        document.getElementById('btn-vendo').addEventListener('click', () => this.showVendoPage());
        document.getElementById('btn-acquisto').addEventListener('click', () => this.showAcquistoPage());
        document.getElementById('btn-logout').addEventListener('click', () => this.logout());

        // Event listeners per i form di auth
        document.getElementById('loginForm').addEventListener('submit', (e) => this.handleLogin(e));
        document.getElementById('registerForm').addEventListener('submit', (e) => this.handleRegister(e));

        // Pulsanti per mostrare login/register
        document.getElementById('btn-show-register').addEventListener('click', () => this.showRegisterForm());
        document.getElementById('btn-show-login').addEventListener('click', () => this.showLoginForm());
    }

    // ===== GESTIONE AUTENTICAZIONE =====

    async handleLogin(event) {
        event.preventDefault();

        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;


        try {
            this.showLoading();
            const userData = await this.apiClient.login(username, password);

            this.currentUser = userData;

            // Inizia una nuova sessione
            this.stateManager.startNewSession();

            this.showMainInterface();
            this.determineAndShowInitialPage();

        } catch (error) {
            console.error('Errore di login:', error);
            DOMUtils.showError(error.message);
        } finally {
            this.hideLoading();
        }
    }

    async handleRegister(event) {
        event.preventDefault();

        const formData = new FormData(event.target);
        const userData = {
            username: formData.get('username'),
            password: formData.get('password'),
            confirmPassword: formData.get('confirmPassword'),
            nome: formData.get('nome'),
            cognome: formData.get('cognome'),
            indirizzo: formData.get('indirizzo')
        };

        // Validazione client-side
        const errors = ValidationUtils.validateRegistration(userData);
        if (errors.length > 0) {
            DOMUtils.showError(errors.join('<br>'));
            return;
        }

        try {
            this.showLoading();
            await this.apiClient.register(userData);

            DOMUtils.showSuccess('Registrazione completata! Ora puoi accedere.');
            this.showLoginForm();

        } catch (error) {
            console.error('Errore registrazione:', error);
            DOMUtils.showError(error.message);
        } finally {
            this.hideLoading();
        }
    }

    showLoginForm() {
        document.getElementById('login-form').style.display = 'block';
        document.getElementById('register-form').style.display = 'none';
        document.getElementById('loginForm').reset();
    }

    showRegisterForm() {
        document.getElementById('login-form').style.display = 'none';
        document.getElementById('register-form').style.display = 'block';
        document.getElementById('registerForm').reset();
    }

    // ===== INTERFACCIA PRINCIPALE =====

    showMainInterface() {
        document.getElementById('login-form').style.display = 'none';
        document.getElementById('register-form').style.display = 'none';
        document.getElementById('navigation').style.display = 'block';
        document.getElementById('user-info').style.display = 'block';
        document.getElementById('main-content').style.display = 'block';

        // Aggiorna il nome utente
        document.getElementById('user-name').textContent = this.currentUser.nomeCompleto;
    }

    /**
     * Determina e mostra la pagina iniziale basata sullo stato utente
     */
    determineAndShowInitialPage() {
        const initialPage = this.stateManager.determineInitialPage();

        /*// Marca il primo accesso come completato DOPO aver determinato la pagina
        if (this.stateManager.isFirstAccess()) {
            // Aspetta che l'utente faccia qualche azione prima di marcare come completato
            // Non lo facciamo subito al login
        }*/

        if (initialPage === 'vendo') {
            this.showVendoPage();
        } else {
            this.showAcquistoPage();
        }
    }

    // ===== PAGINA VENDO =====

    async showVendoPage() {
        this.currentPage = 'vendo';
        DOMUtils.highlightActiveNavButton('btn-vendo');

        // Registra navigazione alla pagina vendo
        this.stateManager.recordAction('naviga_vendo');

        try {
            this.showLoading();

            // Carica dati per la pagina Vendo
            const [asteAperte, asteChiuse, articoli] = await Promise.all([
                this.apiClient.getAsteByVenditore(false),
                this.apiClient.getAsteByVenditore(true),
                this.apiClient.getArticoliDisponibili()
            ]);

            const content = this.generateVendoHTML(asteAperte, asteChiuse, articoli);
            document.getElementById('main-content').innerHTML = content;

            // Aggiungi event listeners per i form
            this.setupVendoEventListeners();

        } catch (error) {
            console.error('Errore caricamento pagina Vendo:', error);
            DOMUtils.showError('Errore nel caricamento della pagina Vendo');
        } finally {
            this.hideLoading();
        }
    }

    generateVendoHTML(asteAperte, asteChiuse, articoli) {
        return `
            <!-- Form per creare nuovo articolo -->
            <div class="form-container">
                <h2>Crea Nuovo Articolo</h2>
                <form id="form-articolo" enctype="multipart/form-data">
                    <div class="form-group">
                        <label for="codice">Codice:</label>
                        <input type="text" id="codice" name="codice" placeholder="Es. ART001" required>
                    </div>
                    <div class="form-group">
                        <label for="nome">Nome:</label>
                        <input type="text" id="nome" name="nome" placeholder="Es. iPhone 14 Pro" required>
                    </div>
                    <div class="form-group">
                        <label for="descrizione">Descrizione:</label>
                        <textarea id="descrizione" name="descrizione" rows="3" placeholder="Descrizione dettagliata..." required></textarea>
                    </div>
                    <div class="form-group">
                        <label for="immagine">Immagine (opzionale):</label>
                        <input type="file" id="immagine" name="immagine" accept="image/*">
                        <small style="color: #666;">Formati supportati: JPG, PNG, GIF, WEBP (max 10MB)</small>
                        <div id="image-preview" style="margin-top: 10px;"></div>
                    </div>
                    <div class="form-group">
                        <label for="prezzo">Prezzo (€):</label>
                        <input type="number" step="0.01" id="prezzo" name="prezzo" min="0.01" placeholder="0.00" required>
                    </div>
                    <button type="submit" class="btn btn-success">Crea Articolo</button>
                </form>
            </div>

            <!-- Form per creare nuova asta -->
            ${articoli.length > 0 ? `
            <div class="form-container">
                <h2>Crea Nuova Asta</h2>
                <form id="form-asta">
                    <div class="form-group">
                        <label>Seleziona Articoli:</label>
                        <div class="checkbox-list">
                            ${articoli.map(art => `
                                <div class="checkbox-item">
                                    <input type="checkbox" id="art${art.id}" name="articoli" value="${art.id}">
                                    <label for="art${art.id}">
                                        <strong>${art.codice} - ${art.nome}</strong>
                                        <br>${FormatUtils.formatPrice(art.prezzo)}
                                        <br><small>${FormatUtils.truncateString(art.descrizione, 60)}</small>
                                    </label>
                                </div>
                            `).join('')}
                        </div>
                    </div>
                    
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
                        <div class="form-group">
                            <label for="rialzo">Rialzo Minimo (€):</label>
                            <input type="number" id="rialzo" name="rialzo" min="1" value="10" required>
                        </div>
                        <div class="form-group">
                            <label for="scadenza">Scadenza:</label>
                            <input type="text" id="scadenza" name="scadenza" 
                                   placeholder="dd-MM-yyyy HH:mm" required>
                            <small>Formato: dd-MM-yyyy HH:mm (es. 15-01-2025 18:00)</small>
                        </div>
                    </div>
                    
                    <button type="submit" class="btn btn-success">Crea Asta</button>
                </form>
            </div>
            ` : '<div class="alert alert-info">Crea prima alcuni articoli per poter creare aste</div>'}

            <!-- Lista aste aperte -->
            ${this.generateAsteTable(asteAperte, 'Aste Aperte', true)}
            
            <!-- Lista aste chiuse -->
            ${this.generateAsteTable(asteChiuse, 'Aste Chiuse', false)}
        `;
    }

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

    async handleCreaArticolo(event) {
        event.preventDefault();

        const formData = new FormData(event.target);
        const articolo = {
            codice: formData.get('codice'),
            nome: formData.get('nome'),
            descrizione: formData.get('descrizione'),
            prezzo: parseFloat(formData.get('prezzo'))
        };

        // Validazione
        const errors = ValidationUtils.validateArticolo(articolo);
        if (errors.length > 0) {
            DOMUtils.showError(errors.join('<br>'));
            return;
        }

        try {
            this.showLoading();
            await this.apiClient.createArticolo(articolo);

            // Registra l'azione
            this.stateManager.recordAction('crea_articolo', {
                codice: articolo.codice,
                nome: articolo.nome
            });

            DOMUtils.showSuccess('Articolo creato con successo!');
            this.showVendoPage(); // Ricarica la pagina
        } catch (error) {
            console.error('Errore creazione articolo:', error);
            DOMUtils.showError(error.message);
        } finally {
            this.hideLoading();
        }
    }

    async handleCreaAsta(event) {
        event.preventDefault();

        const formData = new FormData(event.target);
        const articoliSelezionati = Array.from(document.querySelectorAll('input[name="articoli"]:checked'))
            .map(cb => parseInt(cb.value));

        const asta = {
            articoli: articoliSelezionati,
            rialzoMinimo: parseInt(formData.get('rialzo')),
            scadenza: formData.get('scadenza')
        };

        // Validazione
        const errors = ValidationUtils.validateAsta(asta);
        if (errors.length > 0) {
            DOMUtils.showError(errors.join('<br>'));
            return;
        }

        try {
            this.showLoading();
            await this.apiClient.createAsta(asta);

            this.stateManager.recordAction('crea_asta', {
                articoliCount: articoliSelezionati.length,
                rialzoMinimo: asta.rialzoMinimo,
                scadenza: asta.scadenza
            });

            if (this.stateManager.isFirstAccess()) {
                this.stateManager.markFirstAccessComplete();
            }

            DOMUtils.showSuccess('Asta creata con successo!');
            this.showVendoPage(); // Ricarica la pagina
        } catch (error) {
            console.error('Errore creazione asta:', error);
            DOMUtils.showError(error.message);
        } finally {
            this.hideLoading();
        }
    }

    // ===== PAGINA ACQUISTO =====

    async showAcquistoPage() {
        this.currentPage = 'acquisto';
        DOMUtils.highlightActiveNavButton('btn-acquisto');

        // Registra navigazione alla pagina acquisto
        this.stateManager.recordAction('naviga_acquisto');

        try {
            this.showLoading();

            // Carica le aste vinte
            const asteVinte = await this.apiClient.getAsteVinte();

            // Carica le aste visitate dall'ultima sessione
            let asteVisitate = [];
            const visitedIds = this.stateManager.getVisitedAuctionsFromLastSession();

            if (visitedIds.length > 0) {
                try {
                    const promises = visitedIds.map(id => this.apiClient.getAstaById(id));
                    const results = await Promise.allSettled(promises);

                    asteVisitate = results
                        .filter(result => result.status === 'fulfilled' && result.value)
                        .map(result => result.value)
                        .filter(asta => !asta.chiusa && !DateUtils.isScaduta(asta.scadenza));


                } catch (error) {
                    console.warn('Errore caricamento aste visitate:', error);
                    asteVisitate = [];
                }
            }

            const content = this.generateAcquistoHTML(null, asteVinte, asteVisitate);
            document.getElementById('main-content').innerHTML = content;

            // Aggiungi event listeners
            this.setupAcquistoEventListeners();

        } catch (error) {
            console.error('Errore caricamento pagina Acquisto:', error);
            DOMUtils.showError('Errore nel caricamento della pagina Acquisto');
        } finally {
            this.hideLoading();
        }
    }

    generateAcquistoHTML(risultatiRicerca, asteVinte, asteVisitate) {
        return `
            <!-- Form di ricerca -->
            <div class="form-container">
                <h2>Cerca Aste</h2>
                <form id="form-ricerca">
                    <div class="form-group">
                        <label for="parola-chiave">Parola chiave:</label>
                        <input type="text" id="parola-chiave" name="parolaChiave" 
                               placeholder="Cerca per nome o descrizione..." required>
                    </div>
                    <button type="submit" class="btn">Cerca</button>
                </form>
            </div>

            <!-- Risultati ricerca -->
            <div id="risultati-ricerca">
                ${risultatiRicerca ? this.generateRisultatiRicerca(risultatiRicerca) : ''}
            </div>

            <!-- Aste visitate dall'ultima sessione -->
            ${asteVisitate.length > 0 ? `
                <div class="table-container">
                    <h3>Aste Visitate nell'Ultimo Accesso (${asteVisitate.length})</h3>
                    <div class="alert alert-info">
                        Queste sono le aste che hai visitato durante il tuo ultimo accesso e che sono ancora aperte.
                    </div>
                    ${this.generateAsteRicercaTable(asteVisitate)}
                </div>
            ` : ''}

            <!-- Aste vinte -->
            ${asteVinte.length > 0 ? `
                <div class="table-container">
                    <h3>Le Mie Aste Vinte (${asteVinte.length})</h3>
                    ${this.generateAsteVinteTable(asteVinte)}
                </div>
            ` : ''}

            ${!risultatiRicerca && asteVinte.length === 0 && asteVisitate.length === 0 ? `
                <div class="alert alert-info">
                    <strong>Benvenuto nella sezione Acquisto!</strong><br>
                    • Usa il campo di ricerca per trovare aste interessanti<br>
                    • Le aste che visiti verranno mostrate qui al prossimo accesso<br>
                    • Le aste che vinci appariranno nella sezione dedicata
                </div>
            ` : ''}
        `;
    }

    setupAcquistoEventListeners() {
        // Form ricerca
        const formRicerca = document.getElementById('form-ricerca');
        if (formRicerca) {
            formRicerca.addEventListener('submit', (e) => this.handleRicercaAste(e));
        }
    }

    async handleRicercaAste(event) {
        event.preventDefault();

        const parolaChiave = document.getElementById('parola-chiave').value.trim();

        if (!parolaChiave) {
            DOMUtils.showError('Inserisci una parola chiave per la ricerca');
            return;
        }

        try {
            this.showLoading();
            const aste = await this.apiClient.searchAste(parolaChiave);

            // Registra l'azione di ricerca
            this.stateManager.recordAction('ricerca_aste', {
                query: parolaChiave,
                risultati: aste.length
            });

            const risultatiHTML = this.generateRisultatiRicerca(aste, parolaChiave);
            document.getElementById('risultati-ricerca').innerHTML = risultatiHTML;

            // Marca il primo accesso come completato se necessario
            if (this.stateManager.isFirstAccess()) {
                this.stateManager.markFirstAccessComplete();
            }

        } catch (error) {
            console.error('Errore ricerca:', error);
            DOMUtils.showError('Errore nella ricerca: ' + error.message);
        } finally {
            this.hideLoading();
        }
    }

    // ===== DETTAGLIO ASTA =====

    async mostraDettaglioAsta(astaId) {

        // Registra la visita all'asta (importante per la lista delle aste visitate)
        this.stateManager.recordAction('visita_asta', {
            astaId: astaId,
            fromPage: this.currentPage
        });

        try {
            this.showLoading();

            const [asta, offerte] = await Promise.all([
                this.apiClient.getAstaById(astaId),
                this.apiClient.getOfferteByAsta(astaId)
            ]);

            const dettaglioHTML = this.generateDettaglioAsta(asta, offerte);
            document.getElementById('main-content').innerHTML = dettaglioHTML;
            //this.setupDettaglioEventListeners(astaId);

            // Marca il primo accesso come completato se necessario
            if (this.stateManager.isFirstAccess()) {
                this.stateManager.markFirstAccessComplete();
            }

        } catch (error) {
            console.error('Errore caricamento dettaglio:', error);
            DOMUtils.showError('Errore nel caricamento: ' + error.message);
        } finally {
            this.hideLoading();
        }
    }
    // ===== GENERAZIONE HTML =====

    generateAsteTable(aste, titolo, aperte) {
        if (!aste || aste.length === 0) return '';

        return `
            <div class="table-container">
                <h3>${aperte ? '🟢' : '🔴'} ${titolo} (${aste.length})</h3>
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
                            <td>${FormatUtils.formatPrice(asta.prezzoIniziale)}</td>
                            <td><strong style="color: #27ae60;">${FormatUtils.formatPrice(asta.offertaMassima)}</strong></td>
                            <td>
                                ${aperte ?
            `<span class="status-${DateUtils.isScaduta(asta.scadenza) ? 'closed' : 'open'}">${DateUtils.getTempoRimanente(asta.scadenza)}</span>` :
            `<span class="${asta.vincitoreId ? 'status-open' : 'status-closed'}">${asta.vincitoreId ? 'VENDUTO' : 'NON VENDUTO'}</span>`
        }
                            </td>
                            <td>
                                <button class="link-button" onclick="app.mostraDettaglioAsta(${asta.id})">📋 Dettagli</button>
                                ${aperte && DateUtils.isScaduta(asta.scadenza) && !asta.chiusa ?
            `<br><button class="btn btn-danger" style="font-size: 12px; padding: 6px 12px; margin-top: 5px;" onclick="app.chiudiAsta(${asta.id})">🔒 Chiudi</button>` : ''
        }
                            </td>
                        </tr>
                    `).join('')}
                </table>
            </div>
        `;
    }

    generateRisultatiRicerca(aste, parolaChiave = '') {
        if (!aste || aste.length === 0) {
            return parolaChiave ? `
                <div class="alert alert-info">
                    <strong>Nessuna asta trovata</strong> per la parola chiave: "${parolaChiave}"
                    <br>Prova con termini diversi o controlla l'ortografia.
                </div>
            ` : '';
        }

        return `
            <div class="table-container">
                <h3>Risultati Ricerca${parolaChiave ? ` per: "${parolaChiave}"` : ''} (${aste.length})</h3>
                ${this.generateAsteRicercaTable(aste)}
            </div>
        `;
    }

    generateAsteRicercaTable(aste) {
        return `
            <table>
                <tr>
                    <th>Articoli</th>
                    <th>Prezzo Attuale</th>
                    <th>Tempo Rimanente</th>
                    <th>Stato</th>
                    <th>Azioni</th>
                </tr>
                ${aste.map(asta => `
                    <tr>
                        <td>
                            ${asta.articoli.map(art => `
                                <div style="margin-bottom: 8px;">
                                    <strong>${art.codice} - ${art.nome}</strong><br>
                                    <small>${FormatUtils.truncateString(art.descrizione, 50)}</small>
                                </div>
                            `).join('')}
                        </td>
                        <td>
                            <strong>${FormatUtils.formatPrice(asta.offertaMassima || asta.prezzoIniziale)}</strong>
                            <br><small>Rialzo min: ${FormatUtils.formatPrice(asta.rialzoMinimo)}</small>
                        </td>
                        <td>
                            <span class="status-${DateUtils.isScaduta(asta.scadenza) ? 'closed' : 'open'}">
                                ${DateUtils.getTempoRimanente(asta.scadenza)}
                            </span>
                        </td>
                        <td>
                            ${asta.chiusa ?
            `<span class="status-closed">Chiusa</span>` :
            DateUtils.isScaduta(asta.scadenza) ?
                `<span class="status-closed">Scaduta</span>` :
                `<span class="status-open">Aperta</span>`
        }
                        </td>
                        <td>
                            ${!asta.chiusa && !DateUtils.isScaduta(asta.scadenza) ?
            `<button class="btn btn-success" onclick="app.mostraFormOfferta(${asta.id})" style="font-size: 12px;">Fai Offerta</button>` :
            `<span style="color: #888;">Non disponibile</span>`
        }
                            <br><button class="link-button" onclick="app.mostraDettaglioAsta(${asta.id})" style="font-size: 11px; margin-top: 5px;">Dettagli</button>
                        </td>
                    </tr>
                `).join('')}
            </table>
        `;
    }

    generateAsteVinteTable(asteVinte) {
        return `
            <table>
                <tr>
                    <th>Articoli</th>
                    <th>Prezzo Pagato</th>
                    <th>Data Aggiudicazione</th>
                    <th>Azioni</th>
                </tr>
                ${asteVinte.map(asta => `
                    <tr style="background-color: #f0f8ff; border-left: 4px solid #27ae60;">
                        <td>
                            ${asta.articoli.map(art => `
                                <div style="margin-bottom: 8px;">
                                    <strong style="color: #2c3e50;">${art.codice} - ${art.nome}</strong><br>
                                    <small style="color: #666;">${FormatUtils.truncateString(art.descrizione, 60)}</small><br>
                                    <small style="color: #888;">Valore base: ${FormatUtils.formatPrice(art.prezzo)}</small>
                                </div>
                            `).join('')}
                        </td>
                        <td>
                            <div style="text-align: center;">
                                <span style="background: linear-gradient(135deg, #27ae60, #229954); color: white; padding: 8px 12px; border-radius: 15px; font-weight: bold; display: inline-block;">
                                    ${FormatUtils.formatPrice(asta.prezzoFinale)}
                                </span>
                            </div>
                        </td>
                        <td>
                            <strong style="color: #2c3e50;">${DateUtils.formatDateTime(asta.scadenza)}</strong>
                        </td>
                        <td>
                            <button class="link-button" onclick="app.mostraDettaglioAsta(${asta.id})" style="font-size: 12px;">
                                Dettagli Completi
                            </button>
                            <div style="font-size: 10px; color: #666; text-align: center; margin-top: 5px;">
                                <em>Asta #${asta.id}</em>
                            </div>
                        </td>
                    </tr>
                `).join('')}
            </table>
        `;
    }

    generateDettaglioAsta(asta, offerte) {
        const isVenditore = asta.venditoreId === this.currentUser.id;
        const isVincitore = asta.vincitoreId === this.currentUser.id;
        const isPartecipante = offerte.some(o => o.offerenteId === this.currentUser.id);

        return `
            <div class="form-container">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                    <h2>Dettagli Asta #${asta.id}</h2>
                    <div>
                        ${isVenditore ? '<span style="background: #3498db; color: white; padding: 4px 8px; border-radius: 12px; font-size: 12px;">TUA ASTA</span>' : ''}
                        ${isVincitore ? '<span style="background: #27ae60; color: white; padding: 4px 8px; border-radius: 12px; font-size: 12px;">HAI VINTO</span>' : ''}
                        ${isPartecipante && !isVincitore ? '<span style="background: #f39c12; color: white; padding: 4px 8px; border-radius: 12px; font-size: 12px;">HAI PARTECIPATO</span>' : ''}
                    </div>
                </div>

                <!-- Badge stato asta -->
                <div style="text-align: center; margin-bottom: 20px;">
                    ${asta.chiusa ?
            (asta.vincitoreId ?
                    '<div style="background: linear-gradient(135deg, #27ae60, #229954); color: white; padding: 15px 25px; border-radius: 25px; display: inline-block; font-size: 18px; font-weight: bold;">ASTA CONCLUSA - VENDUTA</div>' :
                    '<div style="background: linear-gradient(135deg, #e74c3c, #c0392b); color: white; padding: 15px 25px; border-radius: 25px; display: inline-block; font-size: 18px; font-weight: bold;">ASTA CONCLUSA - NON VENDUTA</div>'
            ) :
            DateUtils.isScaduta(asta.scadenza) ?
                `<div style="background: linear-gradient(135deg, #f39c12, #e67e22); color: white; padding: 15px 25px; border-radius: 25px; display: inline-block; font-size: 18px; font-weight: bold;">
                                ASTA SCADUTA
                                ${isVenditore ? `<button onclick="app.chiudiAsta(${asta.id})" class="btn btn-danger" style="margin-left: 15px; font-size: 14px; padding: 8px 15px;">Chiudi Asta</button>` : ''}
                            </div>` :
                '<div style="background: linear-gradient(135deg, #27ae60, #229954); color: white; padding: 15px 25px; border-radius: 25px; display: inline-block; font-size: 18px; font-weight: bold;">ASTA ATTIVA</div>'
        }
                </div>

                <!-- Articoli -->
                <div class="form-group">
                    <label>Articoli in Asta:</label>
                    ${asta.articoli.map(art => `
                        <div style="border: 1px solid #ddd; padding: 15px; margin-bottom: 15px; border-radius: 8px; background-color: #f9f9f9;">
                            <h4 style="color: #2c3e50; margin-bottom: 10px;">
                                ${art.codice} - ${art.nome}
                            </h4>
                            <p style="margin-bottom: 10px;">${art.descrizione}</p>
                            ${art.immagine ? `
                                <div style="text-align: center; margin: 10px 0;">
                                    <img src="uploads/images/${art.immagine}" alt="${art.nome}"
                                         style="max-width: 300px; height: auto; border-radius: 5px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                                </div>
                            ` : ''}
                            <p><strong>Prezzo base: ${FormatUtils.formatPrice(art.prezzo)}</strong></p>
                        </div>
                    `).join('')}
                </div>

                <!-- Informazioni asta -->
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-top: 20px;">
                    <div class="form-group">
                        <label>Prezzo Iniziale:</label>
                        <p style="font-size: 18px; font-weight: bold; color: #3498db;">${FormatUtils.formatPrice(asta.prezzoIniziale)}</p>
                    </div>
                    <div class="form-group">
                        <label>Rialzo Minimo:</label>
                        <p style="font-size: 18px; font-weight: bold; color: #e74c3c;">${FormatUtils.formatPrice(asta.rialzoMinimo)}</p>
                    </div>
                    <div class="form-group">
                        <label>Scadenza:</label>
                        <p style="font-size: 16px; font-weight: bold;">${DateUtils.formatDateTime(asta.scadenza)}</p>
                        ${!asta.chiusa && !DateUtils.isScaduta(asta.scadenza) ?
            `<p style="color: #e74c3c; font-weight: bold;">${DateUtils.getTempoRimanente(asta.scadenza)}</p>` : ''
        }
                    </div>
                    <div class="form-group">
                        <label>Offerta Attuale:</label>
                        <p style="font-size: 18px; font-weight: bold; color: #27ae60;">${FormatUtils.formatPrice(asta.offertaMassima || asta.prezzoIniziale)}</p>
                    </div>
                </div>
            </div>

            <!-- Risultato asta se chiusa -->
            ${asta.chiusa && asta.vincitoreId ? `
                <div class="form-container">
                    <div style="background: linear-gradient(135deg, #d4edda, #c3e6cb); padding: 20px; border-radius: 10px; border-left: 5px solid #28a745;">
                        <h4 style="color: #155724; margin-bottom: 15px;">Asta Aggiudicata</h4>
                        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px;">
                            <div>
                                <h5 style="color: #155724;">Vincitore:</h5>
                                <p style="font-size: 18px; font-weight: bold; color: #2c3e50;">Utente #${asta.vincitoreId}</p>
                            </div>
                            <div>
                                <h5 style="color: #155724;">Prezzo Finale:</h5>
                                <p style="font-size: 24px; font-weight: bold; color: #27ae60;">${FormatUtils.formatPrice(asta.prezzoFinale)}</p>
                            </div>
                        </div>
                    </div>
                </div>
            ` : ''}

            <!-- Lista offerte -->
            ${offerte && offerte.length > 0 ? `
                <div class="table-container">
                    <h3>Cronologia Offerte (${offerte.length})</h3>
                    <table>
                        <tr>
                            <th>Pos.</th>
                            <th>Offerente</th>
                            <th>Importo</th>
                            <th>Data/Ora</th>
                            <th>Status</th>
                        </tr>
                        ${offerte.map((offerta, i) => `
                            <tr style="${i === 0 ? 'background-color: #f0f8ff; font-weight: bold;' : ''} ${offerta.offerenteId === this.currentUser.id ? 'border-left: 4px solid #3498db;' : ''}">
                                <td style="text-align: center;">
                                    ${i === 0 ? '' : i === 1 ? '' : i === 2 ? '' : `#${i + 1}`}
                                </td>
                                <td>
                                    ${offerta.nomeOfferente || `Utente #${offerta.offerenteId}`}
                                    ${offerta.offerenteId === this.currentUser.id ?
            '<span style="background: #3498db; color: white; padding: 2px 6px; border-radius: 10px; font-size: 10px; margin-left: 5px;">TU</span>' : ''
        }
                                </td>
                                <td>
                                    <span style="${i === 0 ? 'color: #27ae60; font-size: 18px;' : ''}">
                                        ${FormatUtils.formatPrice(offerta.importo)}
                                    </span>
                                </td>
                                <td>${DateUtils.formatDateTime(offerta.dataOfferta)}</td>
                                <td>
                                    ${i === 0 && asta.chiusa ?
            '<span class="status-open">VINCENTE</span>' :
            i === 0 && !asta.chiusa ?
                '<span class="status-open">IN TESTA</span>' :
                `<span style="color: #666;">#${i + 1}</span>`
        }
                                </td>
                            </tr>
                        `).join('')}
                    </table>
                </div>
            ` : `
                <div class="alert alert-info">
                    <strong>Nessuna offerta ancora ricevuta.</strong>
                    ${!asta.chiusa && !DateUtils.isScaduta(asta.scadenza) ? 'Sii il primo a fare un\'offerta!' : ''}
                </div>
            `}

            <!-- Pulsante per fare offerta -->
            ${!asta.chiusa && !DateUtils.isScaduta(asta.scadenza) && !isVenditore ? `
                <div style="text-align: center; margin: 30px 0;">
                    <button onclick="app.mostraFormOfferta(${asta.id})" class="btn btn-success" style="font-size: 18px; padding: 15px 30px;">
                        Fai la tua Offerta
                    </button>
                </div>
            ` : ''}

            <!-- Pulsanti di navigazione -->
            <div style="text-align: center; margin: 30px 0;">
                <button onclick="app.tornaAllaPaginaPrecedente()" class="link-button" style="font-size: 16px; margin-right: 15px;">
                    ← Torna Indietro
                </button>
                <button onclick="app.showAcquistoPage()" class="link-button" style="font-size: 16px;">
                    Vai ad Acquisto
                </button>
            </div>
        `;
    }

    /*setupDettaglioEventListeners(astaId) {
        // Gli event listener sono gestiti tramite onclick inline per semplicità
    }*/

    // ===== FORM OFFERTA =====

    async mostraFormOfferta(astaId) {

        try {
            this.showLoading();

            const [asta, offerte] = await Promise.all([
                this.apiClient.getAstaById(astaId),
                this.apiClient.getOfferteByAsta(astaId)
            ]);

            if (asta.chiusa || DateUtils.isScaduta(asta.scadenza)) {
                DOMUtils.showError('Asta non più disponibile per offerte');
                return;
            }

            if (asta.venditoreId === this.currentUser.id) {
                DOMUtils.showError('Non puoi fare offerte sulle tue aste');
                return;
            }

            const minimaRichiesta = (asta.offertaMassima || asta.prezzoIniziale) + asta.rialzoMinimo;
            const formHTML = this.generateFormOfferta(asta, offerte, minimaRichiesta);

            document.getElementById('main-content').innerHTML = formHTML;
            this.setupFormOffertaEventListeners(astaId, minimaRichiesta);

        } catch (error) {
            console.error('Errore caricamento form offerta:', error);
            DOMUtils.showError('Errore nel caricamento: ' + error.message);
        } finally {
            this.hideLoading();
        }
    }

    generateFormOfferta(asta, offerte, minimaRichiesta) {
        return `
            <div class="form-container">
                <h2>Fai Offerta - Asta #${asta.id}</h2>
                
                <!-- Articoli in asta -->
                <div class="form-group">
                    <label>Articoli in Asta:</label>
                    ${asta.articoli.map(art => `
                        <div style="border: 1px solid #ddd; padding: 15px; margin-bottom: 15px; border-radius: 8px; background-color: #f9f9f9;">
                            <h4 style="color: #2c3e50; margin-bottom: 10px;">
                                ${art.codice} - ${art.nome}
                            </h4>
                            <p style="margin-bottom: 10px;">${art.descrizione}</p>
                            <p style="font-size: 16px; font-weight: bold; color: #27ae60;">
                                Prezzo base: ${FormatUtils.formatPrice(art.prezzo)}
                            </p>
                        </div>
                    `).join('')}
                </div>

                <!-- Informazioni offerta -->
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px;">
                    <div style="background-color: #e8f5e8; padding: 15px; border-radius: 8px;">
                        <strong>Offerta Attuale:</strong>
                        <p style="font-size: 20px; font-weight: bold; color: #27ae60; margin: 5px 0;">
                            ${FormatUtils.formatPrice(asta.offertaMassima || asta.prezzoIniziale)}
                        </p>
                    </div>
                    <div style="background-color: #fff3cd; padding: 15px; border-radius: 8px;">
                        <strong>Offerta Minima:</strong>
                        <p style="font-size: 20px; font-weight: bold; color: #e74c3c; margin: 5px 0;">
                            ${FormatUtils.formatPrice(minimaRichiesta)}
                        </p>
                    </div>
                    <div style="background-color: #d1ecf1; padding: 15px; border-radius: 8px;">
                        <strong>Scadenza:</strong>
                        <p style="font-size: 16px; font-weight: bold; margin: 5px 0;">
                            ${DateUtils.formatDateTime(asta.scadenza)}
                        </p>
                        <p style="color: #e74c3c; font-weight: bold;">
                            ${DateUtils.getTempoRimanente(asta.scadenza)}
                        </p>
                    </div>
                </div>

                <!-- Form offerta -->
                <form id="form-offerta">
                    <div class="form-group">
                        <label for="importo-offerta">La tua Offerta (€):</label>
                        <input type="number" step="0.01" id="importo-offerta" name="importo"
                               min="${minimaRichiesta.toFixed(2)}" 
                               placeholder="${minimaRichiesta.toFixed(2)}"
                               style="font-size: 18px; padding: 15px;" required>
                        <small>L'offerta deve essere almeno ${FormatUtils.formatPrice(minimaRichiesta)}</small>
                    </div>
                    <button type="submit" class="btn btn-success" style="font-size: 18px; padding: 15px 30px;">
                        Invia Offerta
                    </button>
                </form>
            </div>

            <!-- Cronologia offerte -->
            ${offerte && offerte.length > 0 ? `
                <div class="table-container">
                    <h3>Cronologia Offerte (${offerte.length})</h3>
                    <table>
                        <tr>
                            <th>Offerente</th>
                            <th>Importo</th>
                            <th>Data/Ora</th>
                            <th>Posizione</th>
                        </tr>
                        ${offerte.map((offerta, i) => `
                            <tr ${i === 0 ? 'style="background-color: #f0f8ff; font-weight: bold;"' : ''}>
                                <td>
                                    ${i === 0 ? '' : i === 1 ? '' : i === 2 ? '' : ''}
                                    ${offerta.nomeOfferente || `Utente #${offerta.offerenteId}`}
                                    ${offerta.offerenteId === this.currentUser.id ?
            '<span style="background: #3498db; color: white; padding: 2px 6px; border-radius: 10px; font-size: 10px; margin-left: 5px;">TU</span>' : ''
        }
                                </td>
                                <td>
                                    <span style="${i === 0 ? 'color: #27ae60; font-size: 18px;' : ''}">
                                        ${FormatUtils.formatPrice(offerta.importo)}
                                    </span>
                                </td>
                                <td>${DateUtils.formatDateTime(offerta.dataOfferta)}</td>
                                <td>
                                    ${i === 0 ?
            '<span class="status-open">Vincente</span>' :
            `#${i + 1}`
        }
                                </td>
                            </tr>
                        `).join('')}
                    </table>
                </div>
            ` : `
                <div class="alert alert-info">
                    <strong>Nessuna offerta ancora ricevuta.</strong> Sii il primo a fare un'offerta!
                </div>
            `}

            <div style="text-align: center; margin: 30px 0;">
                <button onclick="app.mostraDettaglioAsta(${asta.id})" class="link-button" style="font-size: 16px;">
                    ← Torna ai dettagli asta
                </button>
            </div>
        `;
    }

    setupFormOffertaEventListeners(astaId, minimaRichiesta) {
        const formOfferta = document.getElementById('form-offerta');
        if (formOfferta) {
            formOfferta.addEventListener('submit', (e) => this.handleInviaOfferta(e, astaId, minimaRichiesta));
        }
    }

    async handleInviaOfferta(event, astaId, minimaRichiesta) {
        event.preventDefault();

        const importo = parseFloat(document.getElementById('importo-offerta').value);

        // Validazione client-side
        if (importo < minimaRichiesta) {
            DOMUtils.showError(`L'offerta deve essere almeno ${FormatUtils.formatPrice(minimaRichiesta)}`);
            return;
        }

        if (!confirm(`Confermi di voler fare un'offerta di ${FormatUtils.formatPrice(importo)}?`)) {
            return;
        }

        try {
            this.showLoading();

            await this.apiClient.createOfferta({
                astaId: astaId,
                importo: importo
            });

            // Registra l'azione di fare offerta
            this.stateManager.setLastAction('fai_offerta');

            DOMUtils.showSuccess('Offerta inviata con successo!');

            // Ricarica il form per mostrare la nuova offerta
            setTimeout(() => {
                this.mostraFormOfferta(astaId);
            }, 1500);

        } catch (error) {
            console.error('Errore invio offerta:', error);
            DOMUtils.showError(error.message);
        } finally {
            this.hideLoading();
        }
    }

    // ===== CHIUSURA ASTA =====

    async chiudiAsta(astaId) {
        if (!confirm('Sei sicuro di voler chiudere questa asta?')) {
            return;
        }

        try {
            this.showLoading();
            await this.apiClient.chiudiAsta(astaId);

            // Registra l'azione di chiusura asta
            this.stateManager.setLastAction('chiudi_asta');

            DOMUtils.showSuccess('Asta chiusa con successo!');

            // Ricarica i dettagli dell'asta
            setTimeout(() => {
                this.mostraDettaglioAsta(astaId);
            }, 1500);

        } catch (error) {
            console.error('Errore chiusura asta:', error);
            DOMUtils.showError(error.message);
        } finally {
            this.hideLoading();
        }
    }

    // ===== UTILITY E NAVIGAZIONE =====

    tornaAllaPaginaPrecedente() {
        if (this.currentPage === 'vendo') {
            this.showVendoPage();
        } else {
            this.showAcquistoPage();
        }
    }

    showLoading() {
        document.getElementById('loading').style.display = 'block';
    }

    hideLoading() {
        document.getElementById('loading').style.display = 'none';
    }

    // ===== LOGOUT =====

    logout() {
        if (confirm('Sei sicuro di voler uscire?')) {
            //this.stateManager.reset();
            this.currentUser = null;
            this.currentPage = null;

            // Reset dell'interfaccia
            document.getElementById('navigation').style.display = 'none';
            document.getElementById('user-info').style.display = 'none';
            document.getElementById('main-content').style.display = 'none';
            document.getElementById('main-content').innerHTML = '';

            this.showLoginForm();

            console.log('Logout completato e stato resettato');
        }
    }


    /**
     * Funzione per forzare il reset dello stato (solo per testing)
     */
    resetState() {
        this.stateManager.reset();
    }

    /**
     * Funzione per simulare azioni per testing
     */
    simulateAction(action) {
        this.stateManager.setLastAction(action);
    }

    /**
     * Funzione per aggiungere asta visitata manualmente (testing)
     */
    addTestVisitedAuction(auctionId) {
        this.stateManager.addVisitedAuction(auctionId);
    }
}

// Inizializza l'applicazione
let app;
document.addEventListener('DOMContentLoaded', () => {
    app = new AsteOnlineApp();

    // Esporta funzioni di debug nel global scope per la console
    window.resetState = () => app.resetState();
    window.simulateAction = (action) => app.simulateAction(action);
    window.addTestVisitedAuction = (id) => app.addTestVisitedAuction(id);
});