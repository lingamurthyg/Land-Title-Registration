<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Land Title Registry – Login</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: Arial, sans-serif; background: #1a3a5c; display: flex;
               align-items: center; justify-content: center; min-height: 100vh; }
        .card { background: #fff; border-radius: 8px; padding: 40px;
                width: 100%; max-width: 400px; box-shadow: 0 8px 32px rgba(0,0,0,0.3); }
        .logo { text-align: center; margin-bottom: 28px; }
        .logo h1 { color: #1a3a5c; font-size: 22px; margin-top: 8px; }
        .logo p  { color: #666; font-size: 13px; }
        label { display: block; font-size: 13px; font-weight: bold;
                color: #333; margin-bottom: 4px; }
        input[type=text], input[type=password] {
            width: 100%; padding: 10px 12px; border: 1px solid #ccc;
            border-radius: 4px; font-size: 14px; margin-bottom: 18px; }
        input[type=text]:focus, input[type=password]:focus {
            outline: none; border-color: #1a3a5c; }
        button { width: 100%; padding: 12px; background: #1a3a5c; color: #fff;
                 border: none; border-radius: 4px; font-size: 15px;
                 font-weight: bold; cursor: pointer; }
        button:hover { background: #245080; }
        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #999; }
    </style>
</head>
<body>
<div class="card">
    <div class="logo">
        <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
            <rect width="48" height="48" rx="8" fill="#1a3a5c"/>
            <path d="M24 8L8 18v4h4v16h8v-8h8v8h8V22h4v-4L24 8z" fill="#fff"/>
        </svg>
        <h1>Land Title Registry</h1>
        <p>Ministry of Lands – Secure Portal</p>
    </div>

    <%-- WAS FORM-BASED AUTH: action must be j_security_check --%>
    <form method="POST" action="j_security_check">
        <label for="j_username">Username</label>
        <input type="text" id="j_username" name="j_username"
               placeholder="Enter your username" required autofocus/>

        <label for="j_password">Password</label>
        <input type="password" id="j_password" name="j_password"
               placeholder="Enter your password" required/>

        <button type="submit">Sign In</button>
    </form>

    <div class="footer">
        Authenticated via IBM WebSphere JAAS/LDAP &nbsp;|&nbsp; v1.0.0
    </div>
</div>
</body>
</html>
