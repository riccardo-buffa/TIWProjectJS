<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="it.polimi.model.Utente" %>
<%
    Utente utente = (Utente) session.getAttribute("utente");
    String errore = (String) request.getAttribute("errore");
    String codice = (String) request.getAttribute("codice");
    String nome = (String) request.getAttribute("nome");
    String descrizione = (String) request.getAttribute("descrizione");
    Double prezzo = (Double) request.getAttribute("prezzo");
%>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Errore Creazione Articolo - Aste Online</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<%@ include file="common/header.jsp" %>

<div class="container">
    <div class="user-info">
        Benvenuto, <%= utente.getNomeCompleto() %>
    </div>

    <div class="alert alert-error">
        <%= errore %>
    </div>

    <!-- Form per ricreare articolo -->
    <div class="form-container">
        <h2>🆕 Crea Nuovo Articolo</h2>
        <form method="post" action="crea-articolo" enctype="multipart/form-data">
            <div class="form-group">
                <label for="codice">📋 Codice:</label>
                <input type="text" id="codice" name="codice"
                       value="<%= codice != null ? codice : "" %>"
                       placeholder="Es. ART001" required>
            </div>

            <div class="form-group">
                <label for="nome">🏷️ Nome:</label>
                <input type="text" id="nome" name="nome"
                       value="<%= nome != null ? nome : "" %>"
                       placeholder="Es. iPhone 14 Pro" required>
            </div>

            <div class="form-group">
                <label for="descrizione">📝 Descrizione:</label>
                <textarea id="descrizione" name="descrizione" rows="3"
                          placeholder="Descrizione dettagliata dell'articolo..." required><%= descrizione != null ? descrizione : "" %></textarea>
            </div>

            <div class="form-group">
                <label for="immagine">📷 Immagine:</label>
                <input type="file" id="immagine" name="immagine"
                       accept=".jpg,.jpeg,.png,.gif">
                <small>Formati supportati: JPG, PNG, GIF (max 10MB)</small>
            </div>

            <div class="form-group">
                <label for="prezzo">💰 Prezzo (€):</label>
                <input type="number" step="0.01" id="prezzo" name="prezzo"
                       value="<%= prezzo != null ? prezzo : "" %>"
                       min="0.01" placeholder="0.00" required>
            </div>

            <button type="submit" class="btn btn-success">🚀 Crea Articolo</button>
        </form>
    </div>

    <div style="text-align: center; margin: 30px 0;">
        <a href="vendo" class="link-button">← Torna alla pagina Vendo</a>
    </div>
</div>

</body>
</html>