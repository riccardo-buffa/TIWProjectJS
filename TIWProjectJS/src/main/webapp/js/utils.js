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
        const minuti = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));

        if (giorni > 0) {
            return `${giorni} giorni, ${ore} ore`;
        } else if (ore > 0) {
            return `${ore} ore, ${minuti} minuti`;
        } else {
            return `${minuti} minuti`;
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
        DOMUtils.showMessage(message, 'alert alert-error', container);
    }

    // Mostra un messaggio di successo
    static showSuccess(message, container = null) {
        DOMUtils.showMessage(message, 'alert alert-success', container);
    }

    // Mostra un messaggio generico
    static showMessage(message, className, container = null) {
        // Rimuovi messaggi esistenti
        const existing = document.querySelectorAll('.alert');
        existing.forEach(el => el.remove());

        const messageEl = document.createElement('div');
        messageEl.className = className;
        messageEl.innerHTML = message;

        const targetContainer = container ||
            document.getElementById('alert-container') ||
            document.getElementById('main-content') ||
            document.querySelector('.container');

        if (targetContainer) {
            if (targetContainer.id === 'alert-container') {
                targetContainer.appendChild(messageEl);
            } else {
                targetContainer.insertBefore(messageEl, targetContainer.firstChild);
            }

            // Rimuovi dopo 5 secondi
            setTimeout(() => {
                if (messageEl.parentNode) {
                    messageEl.remove();
                }
            }, 5000);
        }
    }

    // Evidenzia pulsante di navigazione attivo
    static highlightActiveNavButton(activeId) {
        // Rimuovi classe active da tutti i pulsanti
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.classList.remove('active');
            btn.style.background = '';
        });

        // Aggiungi stile al pulsante corrente
        const activeBtn = document.getElementById(activeId);
        if (activeBtn) {
            activeBtn.classList.add('active');
            activeBtn.style.background = 'rgba(255, 255, 255, 0.3)';
        }
    }
}

class ValidationUtils {
    // Valida un articolo
    static validateArticolo(articolo) {
        const errors = [];

        if (!articolo.codice || articolo.codice.trim().length === 0) {
            errors.push('Il codice è obbligatorio');
        } else if (articolo.codice.trim().length > 20) {
            errors.push('Il codice non può superare 20 caratteri');
        }

        if (!articolo.nome || articolo.nome.trim().length === 0) {
            errors.push('Il nome è obbligatorio');
        } else if (articolo.nome.trim().length > 100) {
            errors.push('Il nome non può superare 100 caratteri');
        }

        if (!articolo.descrizione || articolo.descrizione.trim().length === 0) {
            errors.push('La descrizione è obbligatoria');
        } else if (articolo.descrizione.trim().length > 500) {
            errors.push('La descrizione non può superare 500 caratteri');
        }

        if (!articolo.prezzo || articolo.prezzo <= 0) {
            errors.push('Il prezzo deve essere maggiore di zero');
        } else if (articolo.prezzo > 999999.99) {
            errors.push('Il prezzo non può superare €999,999.99');
        }

        return errors;
    }

    // Valida un'asta
    static validateAsta(asta) {
        const errors = [];

        if (!asta.articoli || asta.articoli.length === 0) {
            errors.push('Seleziona almeno un articolo');
        } else if (asta.articoli.length > 10) {
            errors.push('Non puoi selezionare più di 10 articoli per asta');
        }

        if (!asta.rialzoMinimo || asta.rialzoMinimo < 1) {
            errors.push('Il rialzo minimo deve essere almeno €1');
        } else if (asta.rialzoMinimo > 10000) {
            errors.push('Il rialzo minimo non può superare €10,000');
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

                // Controlla che la scadenza non sia troppo lontana (max 1 anno)
                const maxDate = new Date();
                maxDate.setFullYear(maxDate.getFullYear() + 1);
                if (scadenzaDate > maxDate) {
                    errors.push('La scadenza non può essere oltre 1 anno nel futuro');
                }
            }
        }

        return errors;
    }

    // Valida registrazione utente
    static validateRegistration(userData) {
        const errors = [];

        if (!userData.username || userData.username.trim().length < 3) {
            errors.push('Username deve essere di almeno 3 caratteri');
        } else if (userData.username.trim().length > 50) {
            errors.push('Username non può superare 50 caratteri');
        } else if (!/^[a-zA-Z0-9._]+$/.test(userData.username)) {
            errors.push('Username può contenere solo lettere, numeri, punti e underscore');
        }

        if (!userData.password || userData.password.length < 6) {
            errors.push('Password deve essere di almeno 6 caratteri');
        } else if (userData.password.length > 100) {
            errors.push('Password non può superare 100 caratteri');
        }

        if (userData.password !== userData.confirmPassword) {
            errors.push('Le password non corrispondono');
        }

        if (!userData.nome || userData.nome.trim().length === 0) {
            errors.push('Nome è obbligatorio');
        } else if (userData.nome.trim().length > 50) {
            errors.push('Nome non può superare 50 caratteri');
        }

        if (!userData.cognome || userData.cognome.trim().length === 0) {
            errors.push('Cognome è obbligatorio');
        } else if (userData.cognome.trim().length > 50) {
            errors.push('Cognome non può superare 50 caratteri');
        }

        if (!userData.indirizzo || userData.indirizzo.trim().length === 0) {
            errors.push('Indirizzo è obbligatorio');
        } else if (userData.indirizzo.trim().length > 200) {
            errors.push('Indirizzo non può superare 200 caratteri');
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


    // Tronca una stringa se troppo lunga
    static truncateString(str, maxLength) {
        if (!str) return '';
        if (str.length <= maxLength) return str;
        return str.substring(0, maxLength) + '...';
    }
}


// Aggiungi stili CSS per le animazioni se non presenti
if (!document.querySelector('#animation-styles')) {
    const style = document.createElement('style');
    style.id = 'animation-styles';
    style.textContent = `
        @keyframes shake {
            0%, 100% { transform: translateX(0); }
            25% { transform: translateX(-5px); }
            75% { transform: translateX(5px); }
        }
        
        @keyframes bounce {
            0%, 20%, 60%, 100% { transform: translateY(0); }
            40% { transform: translateY(-10px); }
            80% { transform: translateY(-5px); }
        }
        
        .nav-btn {
            background: rgba(255,255,255,0.1);
            border: none;
            color: white;
            padding: 10px 20px;
            margin: 0 5px;
            border-radius: 25px;
            cursor: pointer;
            transition: all 0.3s ease;
        }
        
        .nav-btn:hover {
            background: rgba(255,255,255,0.2);
            transform: translateY(-2px);
        }
        
        .nav-btn.active {
            background: rgba(255,255,255,0.3);
            font-weight: bold;
        }
        
        .loading-spinner {
            display: inline-block;
            animation: spin 1s linear infinite;
        }
        
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    `;
    document.head.appendChild(style);
}

// Esporta le utility se si usa un module system
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        DateUtils,
        DOMUtils,
        ValidationUtils,
        FormatUtils
    };
}