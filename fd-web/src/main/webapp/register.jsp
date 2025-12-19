<%@ page contentType="text/html; charset=UTF-8" %>

<html>
<head>
  <title>Fraud Detection - Register</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link rel="stylesheet" href="<%=request.getContextPath()%>/css/theme.css">
</head>

<body>

<div class="container">
  <div class="row justify-content-center">
    <div class="col-12 col-sm-10 col-md-7 col-lg-5">

      <div class="card shadow-sm mt-5">
        <div class="card-body p-4">

          <h3 class="text-center mb-4">User Registration</h3>

          <form method="post" action="<%=request.getContextPath()%>/register">

            <div class="mb-3">
              <label class="form-label">First name</label>
              <input class="form-control" name="first_name" required />
            </div>

            <div class="mb-3">
              <label class="form-label">Last name</label>
              <input class="form-control" name="last_name" required />
            </div>

            <div class="mb-3">
              <label class="form-label">Email</label>
              <input class="form-control" name="email" type="email" required />
            </div>

            <div class="mb-3">
              <label class="form-label">Username</label>
              <input class="form-control" name="username" required />
            </div>

            <div class="mb-3">
              <label class="form-label">Password</label>
              <input class="form-control" name="password" type="password" required />
            </div>

            <div class="mb-3">
              <label class="form-label">Confirm password</label>
              <input class="form-control" name="password2" type="password" required />
            </div>

            <button class="btn btn-dark w-100" type="submit">Register</button>
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

              <div class="d-grid mt-3">
                <a class="btn btn-dark" href="<%=request.getContextPath()%>/login">
                  Go to Login
                </a>
              </div>
          <%
            }
          %>

          <div class="text-center mt-3">
            <span>Already have an account?</span>
            <a href="<%=request.getContextPath()%>/login">Login here</a>
          </div>

        </div>
      </div>

    </div>
  </div>
</div>

</body>
</html>
