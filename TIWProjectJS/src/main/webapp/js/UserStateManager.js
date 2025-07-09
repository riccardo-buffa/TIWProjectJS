/**
 * Gestisce lo stato dell'utente e la persistenza delle azioni
 * Memorizza le informazioni lato client per 30 giorni
 */
class UserStateManager {
    constructor() {
        this.STORAGE_KEY = 'asteOnlineUserState';
        this.EXPIRY_DAYS = 30; // 30 giorni come richiesto
        this.state = this.loadState();
    }

    /**
     * Carica lo stato dell'utente dal localStorage
     */
    loadState() {
        try {
            const stored = localStorage.getItem(this.STORAGE_KEY);
            if (!stored) {
                console.log('📦 [STATE] Nessuno stato precedente trovato - primo accesso');
                return this.createInitialState();
            }

            const parsed = JSON.parse(stored);

            // Verifica scadenza
            if (parsed.expiry && new Date().getTime() > parsed.expiry) {
                console.log('⏰ [STATE] Stato scaduto - reset');
                localStorage.removeItem(this.STORAGE_KEY);
                return this.createInitialState();
            }

            console.log('📦 [STATE] Stato caricato:', {
                isFirstAccess: parsed.isFirstAccess,
                lastAction: parsed.lastAction,
                visitedAuctionsCount: parsed.visitedAuctions ? parsed.visitedAuctions.length : 0
            });

            return parsed;
        } catch (error) {
            console.error('❌ [STATE] Errore caricamento stato:', error);
            return this.createInitialState();
        }
    }

    /**
     * Crea lo stato iniziale per un nuovo utente
     */
    createInitialState() {
        return {
            isFirstAccess: true,
            lastAction: null,
            visitedAuctions: [],
            userPreferences: {},
            expiry: new Date().getTime() + (this.EXPIRY_DAYS * 24 * 60 * 60 * 1000)
        };
    }

    /**
     * Salva lo stato nel localStorage
     */
    saveState() {
        try {
            // Aggiorna la scadenza
            this.state.expiry = new Date().getTime() + (this.EXPIRY_DAYS * 24 * 60 * 60 * 1000);

            localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.state));
            console.log('💾 [STATE] Stato salvato:', {
                isFirstAccess: this.state.isFirstAccess,
                lastAction: this.state.lastAction,
                visitedAuctionsCount: this.state.visitedAuctions.length
            });
        } catch (error) {
            console.error('❌ [STATE] Errore salvataggio stato:', error);
        }
    }

    /**
     * Registra che l'utente ha effettuato il primo accesso
     */
    markFirstAccessComplete() {
        this.state.isFirstAccess = false;
        this.saveState();
        console.log('✅ [STATE] Primo accesso completato');
    }

    /**
     * Registra l'ultima azione dell'utente
     */
    setLastAction(action) {
        console.log('📝 [STATE] Registrazione ultima azione:', action);
        this.state.lastAction = action;
        this.state.lastActionTime = new Date().getTime();
        this.saveState();
    }

    /**
     * Ottiene l'ultima azione dell'utente
     */
    getLastAction() {
        return this.state.lastAction;
    }

    /**
     * Verifica se è il primo accesso
     */
    isFirstAccess() {
        return this.state.isFirstAccess === true;
    }

    /**
     * Aggiunge un'asta alla lista delle aste visitate
     */
    addVisitedAuction(auctionId) {
        if (!auctionId || typeof auctionId !== 'number') {
            console.warn('⚠️ [STATE] ID asta non valido:', auctionId);
            return;
        }

        // Evita duplicati
        if (!this.state.visitedAuctions.includes(auctionId)) {
            this.state.visitedAuctions.push(auctionId);

            // Mantieni solo le ultime 50 aste visitate per performance
            if (this.state.visitedAuctions.length > 50) {
                this.state.visitedAuctions = this.state.visitedAuctions.slice(-50);
            }

            this.saveState();
            console.log('👁️ [STATE] Asta aggiunta alle visitate:', auctionId);
        }
    }

    /**
     * Ottiene la lista delle aste visitate
     */
    getVisitedAuctions() {
        return [...this.state.visitedAuctions]; // Copia per evitare modifiche accidentali
    }

    /**
     * Rimuove un'asta dalla lista delle visitate (es. se è stata chiusa)
     */
    removeVisitedAuction(auctionId) {
        const index = this.state.visitedAuctions.indexOf(auctionId);
        if (index > -1) {
            this.state.visitedAuctions.splice(index, 1);
            this.saveState();
            console.log('🗑️ [STATE] Asta rimossa dalle visitate:', auctionId);
        }
    }

    /**
     * Pulisce le aste visitate che sono state chiuse
     */
    cleanupClosedAuctions(activeAuctionIds) {
        const before = this.state.visitedAuctions.length;
        this.state.visitedAuctions = this.state.visitedAuctions.filter(id =>
            activeAuctionIds.includes(id)
        );

        if (this.state.visitedAuctions.length !== before) {
            this.saveState();
            console.log('🧹 [STATE] Pulizia aste chiuse:',
                `${before} → ${this.state.visitedAuctions.length}`);
        }
    }

    /**
     * Salva una preferenza utente
     */
    setUserPreference(key, value) {
        this.state.userPreferences[key] = value;
        this.saveState();
        console.log('⚙️ [STATE] Preferenza salvata:', key, '=', value);
    }

    /**
     * Ottiene una preferenza utente
     */
    getUserPreference(key, defaultValue = null) {
        return this.state.userPreferences[key] || defaultValue;
    }

    /**
     * Determina quale pagina mostrare all'avvio
     */
    determineInitialPage() {
        console.log('🎯 [STATE] Determinazione pagina iniziale...');

        // Primo accesso → ACQUISTO
        if (this.isFirstAccess()) {
            console.log('🆕 [STATE] Primo accesso → Pagina ACQUISTO');
            return 'acquisto';
        }

        // Ultima azione = creazione asta → VENDO
        else {
            return 'vendo';
        }

        // Altrimenti → ACQUISTO (con aste visitate)
        //console.log('🛒 [STATE] Default → Pagina ACQUISTO');
        //return 'acquisto';
    }

    /**
     * Ottiene statistiche sullo stato
     */
    getStats() {
        return {
            isFirstAccess: this.isFirstAccess(),
            lastAction: this.state.lastAction,
            lastActionTime: this.state.lastActionTime,
            visitedAuctionsCount: this.state.visitedAuctions.length,
            daysUntilExpiry: Math.ceil((this.state.expiry - new Date().getTime()) / (24 * 60 * 60 * 1000))
        };
    }

    /**
     * Reset completo dello stato (per testing o logout)
     */
    reset() {
        localStorage.removeItem(this.STORAGE_KEY);
        this.state = this.createInitialState();
        console.log('🔄 [STATE] Stato resettato');
    }

    /**
     * Esporta lo stato per debugging
     */
    exportState() {
        return JSON.stringify(this.state, null, 2);
    }
}