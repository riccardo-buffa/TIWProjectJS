<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="it.polimi.model.*" %>
<%@ page import="it.polimi.util.DateUtil" %>
<%@ page import="java.util.List" %>
<%
  Utente utente = (Utente) request.getAttribute("utente");
  List<Asta> asteAperte = (List<Asta>) request.getAttribute("asteAperte");
  List<Asta> asteChiuse = (List<Asta>) request.getAttribute("asteChiuse");
  List<Articolo> articoliDisponibili = (List<Articolo>) request.getAttribute("articoliDisponibili");
%>
<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Vendo - Aste Online</title>
  <link rel="stylesheet" href="main/webapp/CSS/style.css">
</head>
<body>
<%@ include file="common/header.jsp" %>

<div class="container">
  <div class="user-info">
    Benvenuto, <%= utente.getNomeCompleto() %>
  </div>

  <!-- Form per creare nuovo articolo -->
  <div class="form-container">
    <h2>🆕 Crea Nuovo Articolo</h2>
    <form method="post" action="crea-articolo" enctype="multipart/form-data">
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
        <textarea id="descrizione" name="descrizione" rows="3" placeholder="Descrizione dettagliata dell'articolo..." required></textarea>
      </div>
      <div class="form-group">
        <label for="immagine">📷 Immagine:</label>
        <input type="file" id="immagine" name="immagine" accept=".jpg,.jpeg,.png,.gif">
        <small style="color: #666;">Formati supportati: JPG, PNG, GIF (max 10MB) - Opzionale</small>
      </div>
      <div class="form-group">
        <label for="prezzo">💰 Prezzo (€):</label>
        <input type="number" step="0.01" id="prezzo" name="prezzo" min="0.01" placeholder="0.00" required>
      </div>
      <button type="submit" class="btn btn-success">🚀 Crea Articolo</button>
    </form>
  </div>

  <!-- Form per creare nuova asta -->
  <% if (articoliDisponibili != null && !articoliDisponibili.isEmpty()) { %>
  <div class="form-container">
    <h2>🎯 Crea Nuova Asta</h2>
    <p style="color: #666; margin-bottom: 20px;">
      Seleziona uno o più articoli e imposta i parametri dell'asta.
    </p>

    <form method="post" action="crea-asta">
      <!-- Selezione Articoli -->
      <div class="form-group">
        <label>📦 Seleziona Articoli da Mettere all'Asta:</label>
        <div class="checkbox-list">
          <% for (Articolo articolo : articoliDisponibili) { %>
          <div class="checkbox-item">
            <input type="checkbox"
                   id="art<%= articolo.getId() %>"
                   name="articoli"
                   value="<%= articolo.getId() %>">
            <label for="art<%= articolo.getId() %>">
              <strong><%= articolo.getCodice() %> - <%= articolo.getNome() %></strong>
              <br>💰 €<%= String.format("%.2f", articolo.getPrezzo()) %>
              <br><small style="color: #666;"><%= articolo.getDescrizione() %></small>
            </label>
          </div>
          <% } %>
        </div>
      </div>

      <!-- Parametri Asta -->
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-top: 20px;">
        <div class="form-group">
          <label for="rialzo">📈 Rialzo Minimo (€):</label>
          <input type="number" id="rialzo" name="rialzo" min="1" value="10" required>
          <small>Importo minimo che deve essere aggiunto a ogni offerta</small>
        </div>

        <div class="form-group">
          <label for="scadenza">⏰ Scadenza Asta:</label>
          <input type="text"
                 id="scadenza"
                 name="scadenza"
                 placeholder="dd-MM-yyyy HH:mm"
                 pattern="\d{2}-\d{2}-\d{4} \d{2}:\d{2}"
                 title="Formato: 10-07-2025 23:59"
                 required>
          <small>Formato: <strong>dd-MM-yyyy HH:mm</strong> (es. 10-07-2025 23:59)</small>
        </div>
      </div>

      <!-- Pulsante Crea Asta -->
      <div style="text-align: center; margin-top: 30px;">
        <button type="submit" class="btn btn-success" style="font-size: 18px; padding: 15px 30px;">
          🚀 Crea Asta
        </button>
      </div>
    </form>
  </div>
  <% } else { %>
  <div class="alert alert-info">
    📦 <strong>Nessun articolo disponibile per creare aste.</strong>
    <br>Crea prima alcuni articoli usando il form sopra!
  </div>
  <% } %>

  <!-- Lista aste aperte -->
  <% if (asteAperte != null && !asteAperte.isEmpty()) { %>
  <div class="table-container">
    <h3>🟢 Le Mie Aste Aperte</h3>
    <table>
      <tr>
        <th>Articoli</th>
        <th>Prezzo Iniziale</th>
        <th>Offerta Massima</th>
        <th>Tempo Rimanente</th>
        <th>Azioni</th>
      </tr>
      <% for (Asta asta : asteAperte) { %>
      <tr>
        <td>
          <% for (Articolo art : asta.getArticoli()) { %>
          <strong><%= art.getCodice() %></strong> - <%= art.getNome() %><br>
          <% } %>
        </td>
        <td>€<%= String.format("%.2f", asta.getPrezzoIniziale()) %></td>
        <td>
          <strong style="color: #27ae60;">
            €<%= String.format("%.2f", asta.getOffertaMassima()) %>
          </strong>
        </td>
        <td>
          <span class="<%= asta.isScaduta() ? "status-closed" : "status-open" %>">
            <%= DateUtil.getTempoRimanente(asta.getScadenza()) %>
          </span>
        </td>
        <td>
          <a href="dettaglio-asta?id=<%= asta.getId() %>" class="link-button">📋 Dettagli</a>
        </td>
      </tr>
      <% } %>
    </table>
  </div>
  <% } %>

  <!-- Lista aste chiuse -->
  <% if (asteChiuse != null && !asteChiuse.isEmpty()) { %>
  <div class="table-container">
    <h3>🔴 Le Mie Aste Chiuse (<%= asteChiuse.size() %>)</h3>
    <table>
      <tr>
        <th>📦 Articoli</th>
        <th>💰 Prezzo Iniziale</th>
        <th>💸 Prezzo Finale</th>
        <th>🏆 Stato</th>
        <th>📅 Data Chiusura</th>
        <th>⚙️ Azioni</th>
      </tr>
      <% for (Asta asta : asteChiuse) { %>
      <tr style="<%= asta.getVincitoreId() != null ? "background-color: #f0f8ff;" : "background-color: #fff5f5;" %>">
        <!-- Articoli -->
        <td>
          <% for (Articolo art : asta.getArticoli()) { %>
          <div style="margin-bottom: 8px;">
            <strong><%= art.getCodice() %></strong><br>
            <span style="font-size: 14px;"><%= art.getNome() %></span>
          </div>
          <% } %>
        </td>

        <!-- Prezzo Iniziale -->
        <td>
          <strong style="color: #3498db;">€<%= String.format("%.2f", asta.getPrezzoIniziale()) %></strong><br>
          <small style="color: #666;">Rialzo: €<%= asta.getRialzoMinimo() %></small>
        </td>

        <!-- Prezzo Finale -->
        <td>
          <% if (asta.getPrezzoFinale() != null) { %>
          <strong style="color: #27ae60; font-size: 16px;">€<%= String.format("%.2f", asta.getPrezzoFinale()) %></strong><br>
          <%
            double guadagno = asta.getPrezzoFinale() - asta.getPrezzoIniziale();
            String guadagnoColor = guadagno >= 0 ? "#27ae60" : "#e74c3c";
          %>
          <small style="color: <%= guadagnoColor %>;">
            <%= guadagno >= 0 ? "+" : "" %>€<%= String.format("%.2f", guadagno) %>
          </small>
          <% } else { %>
          <span style="color: #e74c3c; font-weight: bold;">Nessuna offerta</span>
          <% } %>
        </td>

        <!-- Stato -->
        <td>
          <% if (asta.getVincitoreId() != null) { %>
          <div style="background: linear-gradient(135deg, #27ae60, #229954); color: white; padding: 8px 12px; border-radius: 15px; text-align: center;">
            <strong>🏆 VENDUTO</strong>
          </div>
          <% } else { %>
          <div style="background: linear-gradient(135deg, #e74c3c, #c0392b); color: white; padding: 8px 12px; border-radius: 15px; text-align: center;">
            <strong>❌ NON VENDUTO</strong>
          </div>
          <% } %>
        </td>

        <!-- Data Chiusura -->
        <td>
          <strong><%= DateUtil.formatDateTime(asta.getScadenza()) %></strong><br>
          <%
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime scadenza = asta.getScadenza();
            long giorniPassati = java.time.temporal.ChronoUnit.DAYS.between(scadenza, now);
          %>
          <small style="color: #666;">
            <%= giorniPassati == 0 ? "Oggi" : giorniPassati + (giorniPassati == 1 ? " giorno fa" : " giorni fa") %>
          </small>
        </td>

        <!-- Azioni -->
        <td>
          <a href="dettaglio-asta?id=<%= asta.getId() %>" class="link-button">📋 Dettagli</a>
        </td>
      </tr>
      <% } %>
    </table>
  </div>
  <% } %>

  <% if ((asteAperte == null || asteAperte.isEmpty()) && (asteChiuse == null || asteChiuse.isEmpty())) { %>
  <div class="alert alert-info">
    🎯 <strong>Nessuna asta creata ancora.</strong>
    <br>Crea alcuni articoli e poi la tua prima asta!
  </div>
  <% } %>
</div>

</body>
</html>