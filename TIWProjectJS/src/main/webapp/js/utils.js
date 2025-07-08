// Utility functions per l'applicazione Aste Online

class DateUtils {
    // Formatta data nel formato dd-MM-yyyy HH:mm
    static formatDateTime(dateString) {
        if (!dateString) return 'N/A';
        
        const date = new Date(dateString);
        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        
        return `${day}-${month}-${year} ${hours}:${minutes}`;
    }

    // Calcola tempo rimanente fino alla scadenza
    static getTempoRimanente(scadenzaString) {
        if (!scadenzaString) return 'N/A';
        
        const now = new Date();
        const scadenza = new Date(scadenzaString);
        
        if (now > scadenza) return 'Scaduta';
        
        const diff = scadenza - now;
        const giorni = Math.floor(diff / (1000 * 60 * 60 * 24));
        const ore = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        
        if (giorni > 0) {
            return `${giorni} giorni, ${ore} ore`;
        } else {
            return `${ore} ore`;
        }
    }

    // Controlla se una data è scaduta
    static isScaduta(scadenzaString) {
        if (!scadenzaString) return false;
        return new Date() > new Date(scadenzaString);
    }
}

class DOMUtils {
    // Mostra un messaggio di errore
    static showError(message, container = null) {
        DOMUtils.showMessage(message, 'error-message', container);
    }

    // Mostra un messaggio di successo
    static showSuccess(message, container = null) {
        DOMUtils.showMessage(message, 'success-message', container);
    }

    // Mostra un messaggio generico
    static showMessage(message, className, container = null) {
        // Rimuovi messaggi esistenti
        const existing = document.querySelectorAll('.error-message, .success-message');
        existing.forEach(el => el.remove());
        
        const messageEl = document.createElement('div');
        messageEl.className = className;
        messageEl.innerHTML = message;
        
        const targetContainer = container || 
                               document.getElementById('main-content') || 
                               document.querySelector('.container');
        
        if (targetContainer) {
            targetContainer.insertBefore(messageEl, targetContainer.firstChild);
            
            // Rimuovi dopo 5 secondi
            setTimeout(() => {
                if (messageEl.parentNode) {
                    messageEl.remove();
                }
            }, 5000);
        }
    }

    // Mostra indicatore di caricamento
    static showLoading(container) {
        const loadingEl = document.createElement('div');
        loadingEl.className = 'loading';
        loadingEl.innerHTML = '⏳ Caricamento in corso...';
        
        if (container) {
            container.innerHTML = '';
            container.appendChild(loadingEl);
        }
    }

    // Nascondi indicatore di caricamento
    static hideLoading(container) {
        if (container) {
            const loading = container.querySelector('.loading');
            if (loading) loading.remove();
        }
    }

    // Conferma azione con l'utente
    static confirm(message) {
        return window.confirm(message);
    }

    // Evidenzia pulsante di navigazione attivo
    static highlightActiveNavButton(activeId) {
        // Rimuovi classe active da tutti i pulsanti
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        
        // Aggiungi classe active al pulsante corrente
        const activeBtn = document.getElementById(activeId);
        if (activeBtn) {
            activeBtn.classList.add('active');
        }
    }
}

class ValidationUtils {
    // Valida un articolo
    static validateArticolo(articolo) {
        const errors = [];
        
        if (!articolo.codice || articolo.codice.trim().length === 0) {
            errors.push('Il codice è obbligatorio');
        }
        
        if (!articolo.nome || articolo.nome.trim().length === 0) {
            errors.push('Il nome è obbligatorio');
        }
        
        if (!articolo.descrizione || articolo.descrizione.trim().length === 0) {
            errors.push('La descrizione è obbligatoria');
        }
        
        if (!articolo.prezzo || articolo.prezzo <= 0) {
            errors.push('Il prezzo deve essere maggiore di zero');
        }
        
        return errors;
    }

    // Valida un'asta
    static validateAsta(asta) {
        const errors = [];
        
        if (!asta.articoli || asta.articoli.length === 0) {
            errors.push('Seleziona almeno un articolo');
        }
        
        if (!asta.rialzoMinimo || asta.rialzoMinimo < 1) {
            errors.push('Il rialzo minimo deve essere almeno 1€');
        }
        
        if (!asta.scadenza) {
            errors.push('La scadenza è obbligatoria');
        } else {
            // Controlla formato data
            const dateRegex = /^\d{2}-\d{2}-\d{4} \d{2}:\d{2}$/;
            if (!dateRegex.test(asta.scadenza)) {
                errors.push('Formato scadenza non valido (dd-MM-yyyy HH:mm)');
            } else {
                // Controlla che la scadenza sia nel futuro
                const [datePart, timePart] = asta.scadenza.split(' ');
                const [day, month, year] = datePart.split('-');
                const [hours, minutes] = timePart.split(':');
                const scadenzaDate = new Date(year, month - 1, day, hours, minutes);
                
                if (scadenzaDate <= new Date()) {
                    errors.push('La scadenza deve essere nel futuro');
                }
            }
        }
        
        return errors;
    }

    // Valida un'offerta
    static validateOfferta(offerta, minimaRichiesta) {
        const errors = [];
        
        if (!offerta.importo || offerta.importo <= 0) {
            errors.push("L'importo dell'offerta è obbligatorio");
        }
        
        if (offerta.importo < minimaRichiesta) {
            errors.push(`L'offerta deve essere almeno €${minimaRichiesta.toFixed(2)}`);
        }
        
        return errors;
    }
}

class FormatUtils {
    // Formatta un prezzo in euro
    static formatPrice(price) {
        if (price === null || price === undefined) return '€0.00';
        return `€${parseFloat(price).toFixed(2)}`;
    }

    // Formatta un numero
    static formatNumber(number) {
        if (number === null || number === undefined) return '0';
        return number.toLocaleString('it-IT');
    }

    // Tronca una stringa se troppo lunga
    static truncateString(str, maxLength) {
        if (!str) return '';
        if (str.length <= maxLength) return str;
        return str.substring(0, maxLength) + '...';
    }

    // Genera un badge di stato
    static generateStatusBadge(isActive, text) {
        const className = isActive ? 'status-open' : 'status-closed';
        const emoji = isActive ? '🟢' : '🔴';
        return `<span class="${className}">${emoji} ${text}</span>`;
    }
}

class StorageUtils {
    // Salva dati con scadenza
    static saveWithExpiry(key, data, expiryDays = 30) {
        const expiryDate = new Date();
        expiryDate.setDate(expiryDate.getDate() + expiryDays);
        
        const item = {
            data: data,
            expiry: expiryDate.getTime()
        };
        
        try {
            localStorage.setItem(key, JSON.stringify(item));
            return true;
        } catch (error) {
            console.warn('⚠️ Impossibile salvare in localStorage:', error);
            return false;
        }
    }

    // Carica dati verificando scadenza
    static loadWithExpiry(key) {
        try {
            const itemStr = localStorage.getItem(key);
            if (!itemStr) return null;
            
            const item = JSON.parse(itemStr);
            const now = new Date().getTime();
            
            if (now > item.expiry) {
                localStorage.removeItem(key);
                return null;
            }
            
            return item.data;
        } catch (error) {
            console.warn('⚠️ Errore caricamento da localStorage:', error);
            localStorage.removeItem(key);
            return null;
        }
    }

    // Rimuovi dati
    static remove(key) {
        try {
            localStorage.removeItem(key);
        } catch (error) {
            console.warn('⚠️ Errore rimozione da localStorage:', error);
        }
    }
}

// Esporta le utility se si usa un module system
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        DateUtils,
        DOMUtils,
        ValidationUtils,
        FormatUtils,
        StorageUtils
    };
}