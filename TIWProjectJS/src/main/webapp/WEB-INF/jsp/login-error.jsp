
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
  String errore = (String) request.getAttribute("errore");
  String username = (String) request.getAttribute("username");
%>
<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Login Fallito - Aste Online</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="container">
  <div class="login-container">
    <h1>Aste Online</h1>

    <div class="alert alert-error">
      <%= errore %>
    </div>

    <form method="post" action="login">
      <div class="form-group">
        <label for="username">Username:</label>
        <input type="text" id="username" name="username"
               value="<%= username != null ? username : "" %>" required>
      </div>

      <div class="form-group">
        <label for="password">Password:</label>
        <input type="password" id="password" name="password" required>
      </div>

      <button type="submit" class="btn" style="width: 100%;">Riprova Login</button>
    </form>

    <div style="margin-top: 30px; padding: 20px; background-color: #f8f9fa; border-radius: 8px;">
      <h4>Account Demo:</h4>
      <p><strong>Username:</strong> admin<br>
        <strong>Password:</strong> admin123</p>
      <p><strong>Username:</strong> utente1<br>
        <strong>Password:</strong> pass123</p>
      <p style="color: #e74c3c; font-size: 14px; margin-top: 10px;">
        <strong>Suggerimento:</strong> Controlla maiuscole/minuscole
      </p>
    </div>
  </div>
</div>
</body>
</html>