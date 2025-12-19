<%@ page contentType="text/html; charset=UTF-8" %>
<%
    String username = (String) session.getAttribute("username");
    if (username == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    request.setAttribute("activePage", "home");
%>
<html>
<head>
    <title>Fraud Detection - Home</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/theme.css">

    <style>
        .app-shell { min-height: 100vh; }
        .content-wrap { padding-top: 16px; padding-bottom: 16px; }

        .section-title { color: #2C3930; font-weight: 800; }

        /* ===== Sidebar (match Batch/Predict style) ===== */
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

        /* Lift logout slightly (not stuck to bottom, but not glued to About) */
        .sidebar-footer { margin-top: auto; padding-bottom: 18px; }

        .btn-logout {
            background: #DCD7C9;
            color: #2C3930;
            border: 2px solid #A27B5C;
            border-radius: 12px;
            font-weight: 800;
            padding: 10px 12px;
        }

        /* ===== Right side ===== */
        .accent-bar {
            height: 4px;
            width: 60px;
            background: #A27B5C;
            border-radius: 999px;
            margin-top: 8px;
        }

        .hint-box {
            background: rgba(220, 215, 201, 0.35);
            border: 1px solid rgba(162, 123, 92, 0.35);
            border-left: 6px solid #A27B5C;
            border-radius: 12px;
            padding: 14px 16px;
        }

        .action-card { border-radius: 16px; }
        .action-card .card-body { padding: 32px; }
        .action-card .section-title { font-size: 22px; }
        .action-card .btn {
            padding-top: 14px;
            padding-bottom: 14px;
            font-weight: 800;
        }

        .table thead th {
            background: rgba(162, 123, 92, 0.18);
            color: #2C3930;
            border-bottom: 1px solid rgba(162, 123, 92, 0.40);
        }
    </style>
</head>

<body>

<div class="container app-shell">
    <div class="row g-3 content-wrap">

        <!-- Left Sidebar (shared) -->
        <div class="col-12 col-md-4 col-lg-3">
            <%@ include file="/common/sidebar.jspf" %>
        </div>

        <!-- Right Content -->
        <div class="col-12 col-md-8 col-lg-9">

            <div class="card shadow-sm">
                <div class="card-body p-4">
                    <div class="text-muted" style="font-weight: 700; letter-spacing: 0.02em;">
                        PaySim-Based Fraud Detection System
                    </div>

                    <h4 class="section-title mb-1 mt-1">Home</h4>
                    <div class="accent-bar"></div>

                    <p class="text-muted mt-3 mb-0">
                        Predict fraud risk for PaySim-style transactions using a deployed RandomForest model,
                        supporting both single input and batch CSV analysis with explainable results.
                    </p>
                </div>
            </div>

            <div class="row g-4 mt-2">
                <div class="col-12 col-lg-6">
                    <div class="card shadow-sm h-100 action-card">
                        <div class="card-body">
                            <h4 class="section-title mb-3">Single Prediction</h4>

                            <div class="hint-box mb-4">
                                Manually enter transaction fields, predict fraud (YES/NO), and see the top reasons behind the decision.
                            </div>

                            <a class="btn btn-dark btn-lg w-100" href="<%=request.getContextPath()%>/predict">
                                Start Single Prediction
                            </a>
                        </div>
                    </div>
                </div>

                <div class="col-12 col-lg-6">
                    <div class="card shadow-sm h-100 action-card">
                        <div class="card-body">
                            <h4 class="section-title mb-3">Batch CSV Prediction</h4>

                            <div class="hint-box mb-4">
                                Upload a CSV file to predict fraud for multiple transactions, preview results, and download the full output.
                            </div>

                            <a class="btn btn-dark btn-lg w-100" href="<%=request.getContextPath()%>/batch">
                                Start Batch Prediction
                            </a>
                        </div>
                    </div>
                </div>
            </div>

            <div class="card shadow-sm mt-4 mb-4">
                <div class="card-body p-4">
                    <h5 class="section-title mb-1">Transaction Data Overview</h5>
                    <div class="accent-bar"></div>

                    <p class="text-muted mt-3">
                        The system predicts fraud based on simulated PaySim transaction fields.
                        Each record represents a transaction at a specific time step.
                    </p>

                    <div class="table-responsive mt-3">
                        <table class="table table-sm align-middle mb-0">
                            <thead>
                                <tr>
                                    <th style="width: 200px;">Field</th>
                                    <th>Description</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td><b>step</b></td>
                                    <td>Simulation time step (e.g., hourly index in the PaySim timeline).</td>
                                </tr>
                                <tr>
                                    <td><b>type_code</b></td>
                                    <td>
                                        Encoded transaction type:
                                        <br/>
                                        <code>0 = CASH_IN</code>,
                                        <code>1 = CASH_OUT</code>,
                                        <code>2 = DEBIT</code>,
                                        <code>3 = PAYMENT</code>,
                                        <code>4 = TRANSFER</code>
                                    </td>
                                </tr>
                                <tr><td><b>amount</b></td><td>Transaction amount.</td></tr>
                                <tr><td><b>oldbalanceOrg</b></td><td>Sender balance before the transaction.</td></tr>
                                <tr><td><b>newbalanceOrig</b></td><td>Sender balance after the transaction.</td></tr>
                                <tr><td><b>oldbalanceDest</b></td><td>Receiver balance before the transaction.</td></tr>
                                <tr><td><b>newbalanceDest</b></td><td>Receiver balance after the transaction.</td></tr>
                            </tbody>
                        </table>
                    </div>

                </div>
            </div>

        </div>
    </div>
</div>

</body>
</html>
