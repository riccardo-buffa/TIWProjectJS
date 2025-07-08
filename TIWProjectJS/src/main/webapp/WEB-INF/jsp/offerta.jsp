<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="it.polimi.model.*" %>
<%@ page import="it.polimi.util.DateUtil" %>
<%@ page import="java.util.List" %>
<%
  Utente utente = (Utente) request.getAttribute("utente");
  Asta asta = (Asta) request.getAttribute("asta");
  List<Offerta> offerte = (List<Offerta>) request.getAttribute("offerte");
  Double minimaRichiesta = (Double) request.getAttribute("minimaRichiesta");  // ← CAMBIATO DA BigDecimal a Double!
  String messaggio = (String) request.getAttribute("messaggio");
%>
<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Fai Offerta - Asta #<%= asta.getId() %></title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="header">
  <h1>💰 Fai Offerta</h1>
  <div class="nav-links">
    <a href="home">🏠 Home</a>
    <a href="vendo">📦 Vendo</a>
    <a href="acquisto">🛒 Acquisto</a>
    <a href="login.html">🚪 Logout</a>
  </div>
</div>

<div class="container">
  <div class="user-info">
    Benvenuto, <%= utente.getNomeCompleto() %>
  </div>

  <!-- Messaggio -->
  <% if (messaggio != null) { %>
  <div class="alert <%= messaggio.contains("successo") ? "alert-success" : "alert-error" %>">
    <%= messaggio %>
  </div>
  <% } %>

  <!-- Dettagli articoli -->
  <div class="form-container">
    <h2>📦 Articoli in Asta #<%= asta.getId() %></h2>
    <% for (Articolo art : asta.getArticoli()) { %>
    <div style="border: 1px solid #ddd; padding: 20px; margin-bottom: 20px; border-radius: 8px; background-color: #f9f9f9;">
      <h4 style="color: #2c3e50; margin-bottom: 15px;">
        <%= art.getCodice() %> - <%= art.getNome() %>
      </h4>
      <p style="margin-bottom: 15px; line-height: 1.6;"><%= art.getDescrizione() %></p>
      <% if (art.getImmagine() != null && !art.getImmagine().isEmpty()) { %>
      <div style="text-align: center; margin: 15px 0;">
        <% if (art.getImmagine() != null && !art.getImmagine().isEmpty()) { %>
        <img src="uploads/images/<%= art.getImmagine() %>" alt="<%= art.getNome() %>"
             style="max-width: 200px; height: auto; border-radius: 5px;">
        <% } else { %>
        <div style="width: 200px; height: 150px; background: #f0f0f0; display: flex; align-items: center; justify-content: center; border-radius: 5px;">
          📷 Nessuna immagine
        </div>
        <% } %>
      </div>
      <% } %>
      <p style="font-size: 18px; font-weight: bold; color: #27ae60;">
        💰 Prezzo base: €<%= String.format("%.2f", art.getPrezzo()) %>
      </p>
    </div>
    <% } %>
  </div>

  <!-- Form offerta (solo se asta aperta) -->
  <% if (!asta.isChiusa() && !asta.isScaduta()) { %>
  <div class="form-container">
    <h2>💰 Fai la tua Offerta</h2>

    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px;">
      <div style="background-color: #e8f5e8; padding: 15px; border-radius: 8px;">
        <strong>💵 Offerta Attuale:</strong>
        <p style="font-size: 20px; font-weight: bold; color: #27ae60; margin: 5px 0;">
          €<%= String.format("%.2f", asta.getOffertaMassima()) %>
        </p>
      </div>

      <div style="background-color: #fff3cd; padding: 15px; border-radius: 8px;">
        <strong>📈 Offerta Minima:</strong>
        <p style="font-size: 20px; font-weight: bold; color: #e74c3c; margin: 5px 0;">
          €<%= String.format("%.2f", minimaRichiesta) %>
        </p>
      </div>

      <div style="background-color: #d1ecf1; padding: 15px; border-radius: 8px;">
        <strong>⏰ Scadenza:</strong>
        <p style="font-size: 16px; font-weight: bold; margin: 5px 0;">
          <%= DateUtil.formatDateTime(asta.getScadenza()) %>
        </p>
        <p style="color: #e74c3c; font-weight: bold;">
          <%= DateUtil.getTempoRimanente(asta.getScadenza()) %>
        </p>
      </div>
    </div>

    <form method="post" action="offerta">
      <input type="hidden" name="astaId" value="<%= asta.getId() %>">
      <div class="form-group">
        <label for="importo">💰 La tua Offerta (€):</label>
        <input type="number" step="0.01" id="importo" name="importo"
               min="<%= String.format("%.2f", minimaRichiesta) %>"
               placeholder="<%= String.format("%.2f", minimaRichiesta) %>"
               style="font-size: 18px; padding: 15px;" required>
        <small>L'offerta deve essere almeno €<%= String.format("%.2f", minimaRichiesta) %></small>
      </div>
      <button type="submit" class="btn btn-success"
              style="font-size: 18px; padding: 15px 30px;"
              onclick="return confirm('Confermi di voler fare questa offerta?')">
        🚀 Invia Offerta
      </button>
    </form>
  </div>
  <% } else { %>
  <div class="alert alert-error">
    🚫 <strong>Asta non più disponibile.</strong>
    Questa asta è <%= asta.isChiusa() ? "chiusa" : "scaduta" %>.
    Non è più possibile fare offerte.
  </div>
  <% } %>

  <!-- Lista offerte -->
  <% if (offerte != null && !offerte.isEmpty()) { %>
  <div class="table-container">
    <h3>📊 Cronologia Offerte (<%= offerte.size() %>)</h3>
    <table>
      <tr>
        <th>👤 Offerente</th>
        <th>💵 Importo</th>
        <th>📅 Data/Ora</th>
        <th>🏆 Posizione</th>
      </tr>
      <% for (int i = 0; i < offerte.size(); i++) {
        Offerta offerta = offerte.get(i); %>
      <tr <% if (i == 0) { %>style="background-color: #f0f8ff; font-weight: bold;"<% } %>>
        <td>
          <% if (i == 0) { %>🥇<% } else if (i == 1) { %>🥈<% } else if (i == 2) { %>🥉<% } %>
          <%= offerta.getNomeOfferente() %>
        </td>
        <td>
                        <span style="<% if (i == 0) { %>color: #27ae60; font-size: 18px;<% } %>">
                            €<%= String.format("%.2f", offerta.getImporto()) %>
                        </span>
        </td>
        <td><%= DateUtil.formatDateTime(offerta.getDataOfferta()) %></td>
        <td>
          <% if (i == 0) { %>
          <span class="status-open">🏆 Vincente</span>
          <% } else { %>
          #<%= i + 1 %>
          <% } %>
        </td>
      </tr>
      <% } %>
    </table>
  </div>
  <% } else { %>
  <div class="alert alert-info">
    📭 <strong>Nessuna offerta ancora ricevuta.</strong> Sii il primo a fare un'offerta!
  </div>
  <% } %>

  <div style="text-align: center; margin: 30px 0;">
    <a href="acquisto" class="link-button" style="font-size: 16px;">
      ← Torna alla ricerca aste
    </a>
  </div>
</div>

</body>
</html>