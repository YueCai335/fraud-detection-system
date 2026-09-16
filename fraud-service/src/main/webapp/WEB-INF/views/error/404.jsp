<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>Fraud Detection - Not found</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/theme.css">
</head>
<body>
<div class="container">
    <div class="card shadow-sm mt-5" style="max-width: 560px; margin: 0 auto;">
        <div class="card-body p-4">
            <h4>Not found</h4>
            <p class="text-muted">That job does not exist, or it belongs to another user.</p>
            <a class="btn btn-dark" href="<%=request.getContextPath()%>/batch">Back to batch page</a>
        </div>
    </div>
</div>
</body>
</html>
