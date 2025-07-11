/**
 * Gestisce lo stato dell'utente e la persistenza delle azioni
 * Memorizza le informazioni lato client per 30 giorni
 */
class UserStateManager {
    constructor() {
        this.STORAGE_KEY = 'asteOnlineUserState';
        this.EXPIRY_DAYS = 30;
        this.state = this.loadState();
    }

    /**
     * Carica lo stato dell'utente dal localStorage
     */
    loadState() {
        try {
            const stored = localStorage.getItem(this.STORAGE_KEY);
            if (!stored) {
                return this.createInitialState();
            }

            const parsed = JSON.parse(stored);

            // Verifica scadenza
            if (parsed.expiry && new Date().getTime() > parsed.expiry) {
                localStorage.removeItem(this.STORAGE_KEY);
                return this.createInitialState();
            }

            // Migrazione per vecchi stati senza actionHistory o currentSessionActions
            if (!parsed.actionHistory) {
                parsed.actionHistory = [];
            }
            if (!parsed.currentSessionActions) {
                parsed.currentSessionActions = [];
            }

            return parsed;
        } catch (error) {
            console.error('Errore caricamento stato:', error);
            return this.createInitialState();
        }
    }

    /**
     * Crea lo stato iniziale per un nuovo utente
     */
    createInitialState() {
        return {
            isFirstAccess: true,
            actionHistory: [],  // Lista di tutte le azioni dell'ultimo accesso
            currentSessionActions: [], // Azioni della sessione corrente
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
        } catch (error) {
            console.error('Errore salvataggio stato:', error);
        }
    }

    /**
     * Registra che l'utente ha effettuato il primo accesso
     */
    markFirstAccessComplete() {
        this.state.isFirstAccess = false;
        // Salva le azioni della sessione corrente come storia
        this.state.actionHistory = [...this.state.currentSessionActions];
        this.saveState();
    }

    /**
     * Registra un'azione dell'utente
     */
    recordAction(actionType, data = {}) {
        const action = {
            type: actionType,
            timestamp: new Date().getTime(),
            data: data
        };

        // Assicurati che currentSessionActions esista
        if (!this.state.currentSessionActions) {
            this.state.currentSessionActions = [];
        }

        // Aggiungi all'elenco delle azioni della sessione corrente
        this.state.currentSessionActions.push(action);

        this.saveState();
    }

    /**
     * Ottiene l'ultima azione dalla storia (non dalla sessione corrente)
     */
    getLastAction() {
        if (!this.state.actionHistory || this.state.actionHistory.length === 0) {
            return null;
        }
        return this.state.actionHistory[this.state.actionHistory.length - 1];
    }

    /**
     * Verifica se è il primo accesso
     */
    isFirstAccess() {
        return this.state.isFirstAccess === true;
    }

    /**
     * Aggiunge un'asta alla lista delle aste visitate (per retrocompatibilità)
     */
    addVisitedAuction(auctionId) {
        if (!auctionId || typeof auctionId !== 'number') {
            return;
        }

        // Registra anche come azione
        this.recordAction('visita_asta', { astaId: auctionId });
    }

    /**
     * Ottiene la lista delle aste visitate dall'ultima sessione
     */
    getVisitedAuctionsFromLastSession() {
        const visitedAuctions = [];

        // Controlla che actionHistory esista
        if (!this.state.actionHistory || !Array.isArray(this.state.actionHistory)) {
            return visitedAuctions;
        }

        // Estrai gli ID delle aste visitate dalla storia delle azioni
        for (const action of this.state.actionHistory) {
            if (action.type === 'visita_asta' && action.data && action.data.astaId) {
                if (!visitedAuctions.includes(action.data.astaId)) {
                    visitedAuctions.push(action.data.astaId);
                }
            }
        }

        return visitedAuctions;
    }

    /**
     * Determina quale pagina mostrare all'avvio
     */
    determineInitialPage() {

        // Primo accesso → ACQUISTO
        if (this.isFirstAccess()) {
            return 'acquisto';
        }

        // Controlla l'ultima azione dalla storia
        const lastAction = this.getLastAction();

        if (!lastAction) {
            return 'acquisto';
        }


        // Se l'ultima azione è stata creare un'asta → VENDO
        if (lastAction.type === 'crea_asta') {
            return 'vendo';
        }

        // Altrimenti → ACQUISTO
        return 'acquisto';
    }

    /**
     * Inizia una nuova sessione (chiamato al login)
     */
    startNewSession() {

        // Assicurati che gli array esistano
        if (!this.state.currentSessionActions) {
            this.state.currentSessionActions = [];
        }
        if (!this.state.actionHistory) {
            this.state.actionHistory = [];
        }

        // Salva le azioni della sessione precedente nella storia
        if (this.state.currentSessionActions.length > 0) {
            this.state.actionHistory = [...this.state.currentSessionActions];
        }

        // Reset delle azioni della sessione corrente
        this.state.currentSessionActions = [];

        this.saveState();
    }

    /**
     * Termina la sessione corrente (chiamato al logout)
     */
    endSession() {

        // Assicurati che gli array esistano
        if (!this.state.currentSessionActions) {
            this.state.currentSessionActions = [];
        }
        if (!this.state.actionHistory) {
            this.state.actionHistory = [];
        }

        // Salva le azioni correnti nella storia prima del logout
        if (this.state.currentSessionActions.length > 0) {
            this.state.actionHistory = [...this.state.currentSessionActions];
        }

        this.saveState();
    }
    /**
     * Ottiene statistiche sullo stato
     */
    getStats() {
        return {
            isFirstAccess: this.isFirstAccess(),
            lastAction: this.getLastAction(),
            actionHistoryCount: this.state.actionHistory ? this.state.actionHistory.length : 0,
            currentSessionActionsCount: this.state.currentSessionActions ? this.state.currentSessionActions.length : 0,
            visitedAuctionsLastSession: this.getVisitedAuctionsFromLastSession().length,
            daysUntilExpiry: Math.ceil((this.state.expiry - new Date().getTime()) / (24 * 60 * 60 * 1000))
        };
    }

    /**
     * Ottiene un riepilogo delle azioni per tipo
     */
    getActionsSummary() {
        const summary = {};

        if (!this.state.actionHistory || !Array.isArray(this.state.actionHistory)) {
            return summary;
        }

        for (const action of this.state.actionHistory) {
            if (!summary[action.type]) {
                summary[action.type] = 0;
            }
            summary[action.type]++;
        }

        return summary;
    }

    /**
     * Reset completo dello stato (per testing o logout completo)
     */
    reset() {
        localStorage.removeItem(this.STORAGE_KEY);
        this.state = this.createInitialState();
    }
}