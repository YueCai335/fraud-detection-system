<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="ec.fraud.soapclient.PredictResponse2" %>

<html>
<head>
  <title>Fraud Detection - Result</title>
</head>
<body>
<h2>Prediction Result</h2>

<%
  String err = (String) request.getAttribute("error");
  if (err != null) {
%>
    <p><b>Error:</b> <%=err%></p>
    <p><a href="<%=request.getContextPath()%>/predict">Back</a></p>
<%
  } else {
    PredictResponse2 r = (PredictResponse2) request.getAttribute("result");
    if (r == null) {
%>
      <p><b>Error:</b> SOAP returned null result.</p>
      <p><a href="<%=request.getContextPath()%>/predict">Back</a></p>
<%
    } else {
%>
      <p><b>fraud:</b> <%=r.getFraud()%></p>
      <p><b>prob_fraud:</b> <%=r.getProbFraud()%></p>

      <h3>Top 3 reasons (reserved)</h3>
      <ol>
        <li><%=r.getReason1()%></li>
        <li><%=r.getReason2()%></li>
        <li><%=r.getReason3()%></li>
      </ol>

      <p><a href="<%=request.getContextPath()%>/predict">Back</a></p>
<%
    }
  }
%>

</body>
</html>
