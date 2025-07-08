<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="it.polimi.model.Utente" %>
<%@ page import="java.util.List" %>
<%
    Utente utente = (Utente) session.getAttribute("utente");
    String errore = (String) request.getAttribute("errore");
    List<Integer> articoliSelezionati = (List<Integer>) request.getAttribute("articoliSelezionati");
%>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Errore Creazione Asta - Aste Online</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<%@ include file="common/header.jsp" %>

<div class="container">
    <div class="user-info">
        Benvenuto, <%= utente.getNomeCompleto() %>
    </div>

    <!-- Messaggio di errore -->
    <div class="alert alert-error">
        <h3>❌ Impossibile creare l'asta</h3>
        <p><%= errore %></p>
    </div>

    <!-- Informazioni aggiuntive -->
    <div class="alert alert-info">
        <h4>💡 Informazioni importanti:</h4>
        <ul>
            <li><strong>Un articolo può essere in una sola asta alla volta</strong></li>
            <li>Se vuoi creare una nuova asta con un articolo già inserito, devi prima chiudere l'asta precedente</li>
            <li>Gli articoli già venduti non possono essere inseriti in nuove aste</li>
            <li>Puoi controllare lo stato dei tuoi articoli nella sezione "Le Mie Aste"</li>
        </ul>
    </div>

    <!-- Suggerimenti -->
    <div class="form-container">
        <h3>🔧 Cosa puoi fare:</h3>

        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; margin-top: 20px;">

            <!-- Opzione 1: Torna alla pagina vendo -->
            <div style="background: linear-gradient(135deg, #3498db, #2980b9); color: white; padding: 20px; border-radius: 10px; text-align: center;">
                <h4 style="margin: 0 0 15px 0;">📦 Gestisci le tue aste</h4>
                <p style="margin-bottom: 15px; font-size: 14px;">
                    Visualizza lo stato delle tue aste attive e gestisci i tuoi articoli
                </p>
                <a href="vendo" style="background: white; color: #3498db; padding: 10px 20px; border-radius: 5px; text-decoration: none; font-weight: bold;">
                    Vai a "Vendo"
                </a>
            </div>

            <!-- Opzione 2: Crea nuovo articolo -->
            <div style="background: linear-gradient(135deg, #27ae60, #229954); color: white; padding: 20px; border-radius: 10px; text-align: center;">
                <h4 style="margin: 0 0 15px 0;">🆕 Crea nuovo articolo</h4>
                <p style="margin-bottom: 15px; font-size: 14px;">
                    Aggiungi un nuovo articolo da mettere all'asta
                </p>
                <a href="vendo#crea-articolo" style="background: white; color: #27ae60; padding: 10px 20px; border-radius: 5px; text-decoration: none; font-weight: bold;">
                    Nuovo Articolo
                </a>
            </div>

            <!-- Opzione 3: Cerca aste -->
            <div style="background: linear-gradient(135deg, #f39c12, #e67e22); color: white; padding: 20px; border-radius: 10px; text-align: center;">
                <h4 style="margin: 0 0 15px 0;">🛒 Partecipa ad aste</h4>
                <p style="margin-bottom: 15px; font-size: 14px;">
                    Cerca aste interessanti e fai le tue offerte
                </p>
                <a href="acquisto" style="background: white; color: #f39c12; padding: 10px 20px; border-radius: 5px; text-decoration: none; font-weight: bold;">
                    Cerca Aste
                </a>
            </div>
        </div>
    </div>

    <!-- Debug info (solo se ci sono articoli selezionati) -->
    <% if (articoliSelezionati != null && !articoliSelezionati.isEmpty()) { %>
    <div class="form-container">
        <h4>🔍 Dettagli tecnici (per sviluppatori)</h4>
        <p><strong>Articoli selezionati:</strong> <%= articoliSelezionati %></p>
        <p><strong>Numero articoli:</strong> <%= articoliSelezionati.size() %></p>
    </div>
    <% } %>

    <!-- Pulsanti di navigazione -->
    <div style="text-align: center; margin: 30px 0;">
        <a href="vendo" class="btn" style="margin-right: 15px; text-decoration: none;">
            ← Torna alla pagina Vendo
        </a>
        <a href="home" class="btn btn-success" style="text-decoration: none;">
            🏠 Vai alla Home
        </a>
    </div>
</div>

</body>
</html>