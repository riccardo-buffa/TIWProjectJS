<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
  String messaggio = (String) request.getAttribute("messaggio");
%>
<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Registrazione Completata - Aste Online</title>
  <link rel="stylesheet" href="css/style.css">
  <meta http-equiv="refresh" content="10;url=home">
  <-- redirect automatico-->
</head>
<body>
<div class="container">
  <div class="login-container" style="text-align: center;">
    <div style="font-size: 4em; margin-bottom: 20px;">🎉</div>
    <h2 style="color: #28a745;">Registrazione Completata!</h2>

    <div class="alert alert-success" style="font-size: 18px;">
      <%= messaggio %>
    </div>

    <div class="alert alert-info" style="text-align: left;">
      <h3>🚀 Ora puoi:</h3>
      <ul>
        <li>📦 <strong>Vendere:</strong> Crea articoli e metti all'asta i tuoi oggetti</li>
        <li>🛒 <strong>Comprare:</strong> Cerca aste interessanti e fai le tue offerte</li>
        <li>🏆 <strong>Vincere:</strong> Aggiudicati gli oggetti che desideri</li>
        <li>📊 <strong>Gestire:</strong> Monitora le tue aste e offerte</li>
      </ul>
    </div>

    <div>
      <a href="home" class="btn btn-success">🏠 Vai alla Home</a>
      <a href="vendo" class="btn">📦 Inizia a Vendere</a>
      <a href="acquisto" class="btn">🛒 Cerca Aste</a>
    </div>

    <div style="color: #6c757d; font-size: 14px; margin-top: 20px;">
      <p>Verrai reindirizzato automaticamente alla home tra 10 secondi...</p>
    </div>
  </div>
</div>
</body>🚀 Ora puoi:</h3>
<ul>
  <li>📦 <strong>Vendere:</strong> Crea articoli e metti all'asta i tuoi oggetti</li>
  <li>🛒 <strong>Comprare:</strong> Cerca aste interessanti e fai le tue offerte</li>
  <li>🏆 <strong>Vincere:</strong> Aggiudicati gli oggetti che desideri</li>
  <li>📊 <strong>Gestire:</strong> Monitora le tue aste e offerte</li>
</ul>
</div>

<div>
  <a href="home" class="btn-link"> Vai alla Home</a>
  <a href="vendo" class="btn-link"> Inizia a Vendere</a>
  <a href="acquisto" class="btn-link"> Cerca Aste</a>
</div>

<div class="countdown">
  <p>Verrai reindirizzato automaticamente alla home tra <span id="countdown">10</span> secondi...</p>
</div>
</html>