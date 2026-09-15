<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

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

          <form:form modelAttribute="form" method="post" action="${pageContext.request.contextPath}/register">

            <div class="mb-3">
              <label class="form-label">First name</label>
              <form:input path="firstName" cssClass="form-control" required="required"/>
            </div>

            <div class="mb-3">
              <label class="form-label">Last name</label>
              <form:input path="lastName" cssClass="form-control" required="required"/>
            </div>

            <div class="mb-3">
              <label class="form-label">Email</label>
              <form:input path="email" type="email" cssClass="form-control" required="required"/>
            </div>

            <div class="mb-3">
              <label class="form-label">Username</label>
              <form:input path="username" cssClass="form-control" required="required"/>
            </div>

            <div class="mb-3">
              <label class="form-label">Password</label>
              <form:password path="password" cssClass="form-control" required="required"/>
            </div>

            <div class="mb-3">
              <label class="form-label">Confirm password</label>
              <form:password path="password2" cssClass="form-control" required="required"/>
            </div>

            <form:errors path="*" element="div" cssClass="alert alert-danger mb-3" delimiter="<br/>"/>

            <button class="btn btn-dark w-100" type="submit">Register</button>
          </form:form>

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
