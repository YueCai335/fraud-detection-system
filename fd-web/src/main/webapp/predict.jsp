<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="ec.fraud.soapclient.PredictResponse2" %>
<%
    String username = (String) session.getAttribute("username");
    if (username == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    request.setAttribute("activePage", "predict");

    // Keep user inputs after submit (fallback to defaults)
    String stepVal = request.getParameter("step"); if (stepVal == null || stepVal.trim().isEmpty()) stepVal = "1";
    String typeCodeVal = request.getParameter("type_code"); if (typeCodeVal == null || typeCodeVal.trim().isEmpty()) typeCodeVal = "2";
    String amountVal = request.getParameter("amount"); if (amountVal == null || amountVal.trim().isEmpty()) amountVal = "181.0";
    String oldOrgVal = request.getParameter("oldbalanceOrg"); if (oldOrgVal == null || oldOrgVal.trim().isEmpty()) oldOrgVal = "181.0";
    String newOrgVal = request.getParameter("newbalanceOrig"); if (newOrgVal == null || newOrgVal.trim().isEmpty()) newOrgVal = "0.0";
    String oldDestVal = request.getParameter("oldbalanceDest"); if (oldDestVal == null || oldDestVal.trim().isEmpty()) oldDestVal = "0.0";
    String newDestVal = request.getParameter("newbalanceDest"); if (newDestVal == null || newDestVal.trim().isEmpty()) newDestVal = "0.0";

    // Frontend display (must match your Flask setting)
    double UI_THRESHOLD = 0.25;
%>
<!DOCTYPE html>
<html>
<head>
    <title>Fraud Detection - Single Prediction</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/theme.css">

    <style>
        .app-shell { min-height: 100vh; }
        .content-wrap { padding-top: 16px; padding-bottom: 16px; }
        .section-title { color: #2C3930; font-weight: 800; }

        /* ===== Sidebar (same as About) ===== */
        .sidebar {
            background: #2C3930;
            color: #DCD7C9;
            border-radius: 16px;
            padding: 18px;
            height: calc(100vh - 32px);
        }
        .brand { font-size: 18px; font-weight: 800; margin-bottom: 6px; }
        .brand-accent { color: #A27B5C; font-weight: 900; }
        .user { font-size: 13px; opacity: 0.95; margin-bottom: 18px; }

        .nav-title {
            font-size: 12px;
            font-weight: 800;
            letter-spacing: 0.05em;
            text-transform: uppercase;
            color: #DCD7C9;
            margin-top: 18px;
            margin-bottom: 12px;
        }

        .nav-link-custom {
            display: block;
            padding: 14px 14px;
            border-radius: 10px;
            color: #DCD7C9;
            text-decoration: none;
            margin-bottom: 18px;
            background: rgba(220, 215, 201, 0.06);
            border: 1px solid rgba(162, 123, 92, 0.22);
            font-size: 14px;
        }
        .nav-link-custom:hover {
            background: rgba(220, 215, 201, 0.12);
            border-color: rgba(162, 123, 92, 0.55);
            color: #DCD7C9;
        }
        .nav-link-custom.active {
            background: rgba(162, 123, 92, 0.90);
            color: #2C3930;
            font-weight: 900;
            border-color: rgba(162, 123, 92, 1.0);
        }

        .sidebar-wrap { display: flex; flex-direction: column; height: 100%; }
        .sidebar-footer { margin-top: auto; }

        .btn-logout {
            background: #DCD7C9;
            color: #2C3930;
            border: 2px solid #A27B5C;
            border-radius: 12px;
            font-weight: 800;
            padding: 10px 12px;
        }

        /* ===== Right side accent ===== */
        .accent-bar {
            height: 4px;
            width: 60px;
            background: #A27B5C;
            border-radius: 999px;
            margin-top: 8px;
        }
        .accent-bar.small { width: 42px; height: 3px; }

        .hint-box {
            background: rgba(220, 215, 201, 0.35);
            border: 1px solid rgba(162, 123, 92, 0.35);
            border-left: 6px solid #A27B5C;
            border-radius: 12px;
            padding: 12px 14px;
        }

        .form-label { font-weight: 700; color: #2C3930; }

        /* Bigger, centered Predict button */
        .btn-predict {
            padding: 12px 34px;
            font-weight: 900;
            font-size: 16px;
            border-radius: 12px;
            min-width: 220px;
        }

        .result-title {
            font-weight: 900;
            font-size: 20px;
            letter-spacing: 0.01em;
        }
        .threshold-note {
            font-size: 12px;
            opacity: 0.8;
            margin-top: 10px;
        }
    </style>
</head>

<body>
<div class="container app-shell">
    <div class="row g-3 content-wrap">

        <!-- Left Sidebar (common) -->
        <div class="col-12 col-md-4 col-lg-3">
            <%@ include file="/common/sidebar.jspf" %>
        </div>

        <!-- Right Content -->
        <div class="col-12 col-md-8 col-lg-9">

            <!-- Header card -->
            <div class="card shadow-sm">
                <div class="card-body p-4">
                    <div class="text-muted" style="font-weight:700;letter-spacing:0.02em;">
                        PaySim-Based Fraud Detection System
                    </div>
                    <h4 class="section-title mb-1 mt-1">Single Prediction</h4>
                    <div class="accent-bar"></div>

                    <p class="text-muted mt-3 mb-0">
                        Predict whether a transaction is fraudulent, its risk probability, and model-based Top 3 reasons.
                    </p>
                </div>
            </div>

            <div class="row g-3 mt-0">

                <!-- Block 1: Input form -->
                <div class="col-12">
                    <div class="card shadow-sm">
                        <div class="card-body p-4">
                            <h5 class="section-title mb-2">Transaction Information</h5>
                            <div class="accent-bar small"></div>

                            <div class="hint-box mt-3 mb-3">
                                Use PaySim-style feature fields. Default values are for quick testing.
                            </div>

                            <form method="post" action="<%=request.getContextPath()%>/predict">
                                <div class="row g-3">

                                    <div class="col-12 col-md-4">
                                        <label class="form-label">step</label>
                                        <input class="form-control" name="step" value="<%=stepVal%>"/>
                                    </div>

                                    <div class="col-12 col-md-4">
                                        <label class="form-label">type_code</label>
                                        <input class="form-control" name="type_code" value="<%=typeCodeVal%>"/>
                                    </div>

                                    <div class="col-12 col-md-4">
                                        <label class="form-label">amount</label>
                                        <input class="form-control" name="amount" value="<%=amountVal%>"/>
                                    </div>

                                    <div class="col-12 col-md-6">
                                        <label class="form-label">oldbalanceOrg</label>
                                        <input class="form-control" name="oldbalanceOrg" value="<%=oldOrgVal%>"/>
                                    </div>

                                    <div class="col-12 col-md-6">
                                        <label class="form-label">newbalanceOrig</label>
                                        <input class="form-control" name="newbalanceOrig" value="<%=newOrgVal%>"/>
                                    </div>

                                    <div class="col-12 col-md-6">
                                        <label class="form-label">oldbalanceDest</label>
                                        <input class="form-control" name="oldbalanceDest" value="<%=oldDestVal%>"/>
                                    </div>

                                    <div class="col-12 col-md-6">
                                        <label class="form-label">newbalanceDest</label>
                                        <input class="form-control" name="newbalanceDest" value="<%=newDestVal%>"/>
                                    </div>

                                </div>

                                <div class="mt-4 text-center">
                                    <button class="btn btn-dark btn-predict" type="submit">Predict</button>
                                </div>
                            </form>

                        </div>
                    </div>
                </div>

                <!-- Block 2: Result -->
                <div class="col-12 mb-4">
                    <div class="card shadow-sm">
                        <div class="card-body p-4">
                            <h5 class="section-title mb-2">Prediction Result</h5>
                            <div class="accent-bar small"></div>

                            <div class="mt-3">
<%
    String err = (String) request.getAttribute("error");
    if (err != null) {
%>
                                <div class="alert alert-warning mb-0">
                                    <b>Error:</b> <%= err %>
                                </div>
<%
    } else {
        PredictResponse2 r = (PredictResponse2) request.getAttribute("result");
        if (r == null) {
%>
                                <div class="text-muted">
                                    No result yet. Submit the form to get a prediction.
                                </div>
<%
        } else {
            boolean isFraud = (r.getFraud() == 1);
            int probPercent = (int)Math.round(r.getProbFraud() * 100);
%>
                                <% if (isFraud) { %>
                                    <div class="alert alert-danger mb-0 result-title">
                                        ⚠️ Fraud Risk Detected
                                    </div>
                                <% } else { %>
                                    <div class="alert alert-success mb-0 result-title">
                                        ✓ No Fraud Risk Detected
                                    </div>
                                <% } %>

                                <div class="hint-box mt-3">
                                    <b>Is this transaction likely fraudulent?</b>
                                    <%= isFraud ? "YES" : "NO" %><br/>
                                    <b>Fraud probability:</b> <%= probPercent %>%
                                    <div class="threshold-note">
                                        Threshold used: <%= String.format("%.2f", UI_THRESHOLD) %>
                                    </div>
                                </div>

<%
            if (isFraud) {
%>
                                <div class="mt-3">
                                    <h6 class="section-title mb-2">Top 3 Reasons</h6>
                                    <div class="accent-bar small"></div>

                                    <ol class="mt-3 mb-0">
                                        <li><%= r.getReason1() %></li>
                                        <li><%= r.getReason2() %></li>
                                        <li><%= r.getReason3() %></li>
                                    </ol>
                                </div>
<%
            }
        }
    }
%>
                            </div>

                        </div>
                    </div>
                </div>

            </div>
        </div>

    </div>
</div>
</body>
</html>
