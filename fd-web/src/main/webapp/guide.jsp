<%@ page contentType="text/html; charset=UTF-8" %>
<%
    String username = (String) session.getAttribute("username");
    if (username == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    // Used by common/sidebar.jspf
    request.setAttribute("activePage", "guide");
%>
<!DOCTYPE html>
<html>
<head>
    <title>Fraud Detection - Guide</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/theme.css">

    <style>
        .app-shell { min-height: 100vh; }
        .content-wrap { padding-top: 16px; padding-bottom: 16px; }

        /* ===== Sidebar (shared style) ===== */
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
        .sidebar-footer { margin-top: auto; padding-bottom: 18px; }

        .btn-logout {
            background: #DCD7C9;
            color: #2C3930;
            border: 2px solid #A27B5C;
            border-radius: 12px;
            font-weight: 800;
            padding: 10px 12px;
        }

        /* ===== Right content ===== */
        .section-title { color: #2C3930; font-weight: 800; }

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
            padding: 14px 16px;
        }
    </style>
</head>

<body>

<div class="container app-shell">
    <div class="row g-3 content-wrap">

        <!-- Sidebar (shared) -->
        <div class="col-12 col-md-4 col-lg-3">
            <%@ include file="/common/sidebar.jspf" %>
        </div>

        <!-- Right Content -->
        <div class="col-12 col-md-8 col-lg-9">

            <!-- Header -->
            <div class="card shadow-sm">
                <div class="card-body p-4">
                    <div class="text-muted" style="font-weight:700;letter-spacing:0.02em;">
                        PaySim-Based Fraud Detection System
                    </div>
                    <h4 class="section-title mb-1 mt-1">Guide</h4>
                    <div class="accent-bar"></div>

                    <p class="text-muted mt-3 mb-0">
                        This page explains how to use the fraud detection system for both single
                        transactions and batch CSV files.
                    </p>
                </div>
            </div>

            <!-- Single Prediction Guide -->
            <div class="card shadow-sm mt-3">
                <div class="card-body p-4">
                    <h5 class="section-title mb-2">Single Prediction Guide</h5>
                    <div class="accent-bar small"></div>

                    <ol class="mt-3 mb-0">
                        <li>Open <b>Single Prediction</b> from the sidebar or the Home page.</li>
                        <li>Manually enter the transaction fields (amount, balances, type, etc.).</li>
                        <li>Click <b>Predict</b> to check whether the transaction is fraud.</li>
                        <li>
                            The result shows:
                            <ul class="mt-2">
                                <li>Fraud decision (YES / NO)</li>
                                <li>Fraud probability</li>
                                <li>Top reasons (shown when fraud is detected)</li>
                            </ul>
                        </li>
                    </ol>
                </div>
            </div>

            <!-- Batch Prediction Guide -->
            <div class="card shadow-sm mt-3 mb-4">
                <div class="card-body p-4">
                    <h5 class="section-title mb-2">Batch CSV Prediction Guide</h5>
                    <div class="accent-bar small"></div>

                    <ol class="mt-3 mb-0">
                        <li>Prepare a CSV file that follows the PaySim transaction format.</li>
                        <li>Open <b>Batch Prediction</b> from the sidebar.</li>
                        <li>Upload the CSV file and click <b>Upload & Predict</b>.</li>
                        <li>The system previews predictions for the first 10 rows.</li>
                        <li>You can download the full prediction result as a CSV file.</li>
                    </ol>
                </div>
            </div>

        </div>
    </div>
</div>

</body>
</html>
