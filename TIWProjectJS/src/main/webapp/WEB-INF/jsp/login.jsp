<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Login - Aste Online</title>
</head>
<body>
<div class="login-container">
  <h2> Aste Online</h2>

  <form method="post" action="login">
    <div class="form-group">
      <label for="username">Username:</label>
      <input type="text" id="username" name="username" required>
    </div>

    <div class="form-group">
      <label for="password">Password:</label>
      <input type="password" id="password" name="password" required>
    </div>

    <button type="submit">Accedi</button>
  </form>

  <% if (request.getAttribute("error") != null) { %>
  <div class="error">${error}</div>
  <% } %>

  <div class="demo-info">
    <h4>Account Demo</h4>
    <p>Username: <code>mario.rossi</code></p>
    <p>Password: <code>password123</code></p>
  </div>
</div>
</body>
</html>
