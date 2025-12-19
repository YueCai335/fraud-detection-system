<%@ page contentType="text/html; charset=UTF-8" %>

<html>
<head>
<title>Fraud Detection - Login</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link rel="stylesheet" href="<%=request.getContextPath()%>/css/theme.css">
</head>

<body>

<div class="container">
  <div class="row justify-content-center">
    <div class="col-12 col-sm-10 col-md-6 col-lg-4">

      <div class="card shadow-sm mt-5">
        <div class="card-body p-4">

          <h3 class="text-center mb-4">User Login</h3>

          <form method="post" action="<%=request.getContextPath()%>/login">
            <div class="mb-3">
              <label class="form-label">Username</label>
              <input class="form-control" name="username" required />
            </div>

            <div class="mb-3">
              <label class="form-label">Password</label>
              <input class="form-control" name="password" type="password" required />
            </div>

            <button class="btn btn-dark w-100" type="submit">Login</button>
          </form>

          <%
            String err = (String) request.getAttribute("error");
            String msg = (String) request.getAttribute("msg");
            if (err != null) {
          %>
              <div class="alert alert-danger mt-3 mb-0" role="alert">
                <b>Error:</b> <%=err%>
              </div>
          <%
            } else if (msg != null) {
          %>
              <div class="alert alert-success mt-3 mb-0" role="alert">
                <b><%=msg%></b>
              </div>
          <%
            }
          %>

          <div class="text-center mt-3">
            <span>No account yet?</span>
            <a href="<%=request.getContextPath()%>/register">Register here</a>
          </div>

        </div>
      </div>

    </div>
  </div>
</div>

</body>
</html>
