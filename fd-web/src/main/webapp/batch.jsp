<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*" %>
<%
    String username = (String) session.getAttribute("username");
    if (username == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    request.setAttribute("activePage", "batch");

    // Keep consistent with Flask threshold used in your app.py
    double UI_THRESHOLD = 0.25;
%>
<!DOCTYPE html>
<html>
<head>
    <title>Fraud Detection - Batch Prediction</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/theme.css">

    <style>
        .app-shell { min-height: 100vh; }
        .content-wrap { padding-top: 16px; padding-bottom: 16px; }
        .section-title { color: #2C3930; font-weight: 800; }

        /* ===== Sidebar (same as Predict/About) ===== */
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

        .btn-upload {
            padding: 14px 40px;
            font-weight: 900;
            font-size: 17px;
            border-radius: 12px;
            min-width: 320px;
        }

        .mono { font-family: Consolas, monospace; }

        .table thead th {
            background: rgba(162, 123, 92, 0.18);
            color: #2C3930;
            border-bottom: 1px solid rgba(162, 123, 92, 0.40);
            white-space: nowrap;
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

        <!-- Left Sidebar -->
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
                    <h4 class="section-title mb-1 mt-1">Batch CSV Prediction</h4>
                    <div class="accent-bar"></div>

                    <p class="text-muted mt-3 mb-0">
                        Upload a CSV file to run bulk predictions. Preview shows the first 10 rows only.
                    </p>
                </div>
            </div>

            <div class="row g-3 mt-0">

                <!-- Card 1: Upload -->
                <div class="col-12">
                    <div class="card shadow-sm">
                        <div class="card-body p-4">
                            <h5 class="section-title mb-2">Upload CSV</h5>
                            <div class="accent-bar small"></div>

                            <div class="alert alert-warning mt-3 mb-0">
                                <b>Attention:</b> CSV columns must strictly match the PaySim-style format, otherwise parsing may fail.
                            </div>

                            <!-- Sample CSV download -->
                            <div class="mt-3">
                                <a class="btn btn-outline-secondary"
                                   href="<%=request.getContextPath()%>/sample/sample_paysim.csv"
                                   download>
                                    Download Sample CSV
                                </a>
                            </div>

                            <form class="mt-3"
                                  method="post"
                                  action="<%=request.getContextPath()%>/batch"
                                  enctype="multipart/form-data">

                                <div class="row g-3">

                                    <div class="col-12">
                                        <label class="form-label" style="font-weight:700;color:#2C3930;">
                                            Select CSV file
                                        </label>

                                        <!-- Hidden native file input -->
                                        <input
                                                type="file"
                                                id="csvFileInput"
                                                name="csvFile"
                                                accept=".csv"
                                                required
                                                style="display:none"
                                        />

                                        <!-- Custom English button -->
                                        <div class="d-flex align-items-center gap-3">
                                            <button
                                                    type="button"
                                                    class="btn btn-outline-secondary"
                                                    onclick="document.getElementById('csvFileInput').click();">
                                                Choose CSV File
                                            </button>

                                            <span id="csvFileName" class="text-muted">
                                                No file selected
                                            </span>
                                        </div>
                                    </div>

                                    <!-- Button on its own row -->
                                    <div class="col-12 text-center mt-2">
                                        <button class="btn btn-dark btn-upload" type="submit">
                                            Upload & Predict
                                        </button>
                                    </div>

                                </div>
                            </form>

<%
    String err = (String) request.getAttribute("error");
    String okMsg = (String) request.getAttribute("okMsg");
    if (err != null) {
%>
                            <!-- CHANGED: warning -> danger -->
                            <div class="alert alert-danger mt-3 mb-0" role="alert">
                                <b>Error:</b> <%= err %>
                                <div class="mt-2">
                                    Please download and compare with the sample file above.
                                </div>
                            </div>
<%
    } else if (okMsg != null) {
%>
                            <div class="alert alert-success mt-3 mb-0">
                                <b><%= okMsg %></b>
                            </div>
<%
    }
%>

                        </div>
                    </div>
                </div>

                <!-- Card 2: Preview -->
                <div class="col-12">
                    <div class="card shadow-sm">
                        <div class="card-body p-4">
                            <h5 class="section-title mb-2">Preview (Top 10)</h5>
                            <div class="accent-bar small"></div>

                            <div class="text-muted mt-3">
                                Fraud is shown as YES/NO. Probability is shown as a percentage.
                                <div class="threshold-note">
                                    Threshold used: <%= String.format("%.2f", UI_THRESHOLD) %>
                                </div>
                            </div>

<%
    Object previewObj = request.getAttribute("previewRows");
    if (previewObj == null) {
%>
                            <div class="hint-box mt-3">
                                No results yet. Upload a CSV file to see predictions here.
                            </div>
<%
    } else {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> rows = (List<Map<String, String>>) previewObj;

        if (rows.isEmpty()) {
%>
                            <div class="hint-box mt-3">
                                No rows were parsed from CSV.
                            </div>
<%
        } else {
%>
                            <div class="table-responsive mt-3">
                                <table class="table table-sm align-middle mb-0">
                                    <thead>
                                        <tr>
                                            <th>#</th>
                                            <th>CSV Row</th>
                                            <th>step</th>
                                            <th>type_code</th>
                                            <th>amount</th>
                                            <th>oldbalanceOrg</th>
                                            <th>newbalanceOrig</th>
                                            <th>oldbalanceDest</th>
                                            <th>newbalanceDest</th>
                                            <th>Fraud?</th>
                                            <th>Probability</th>
                                        </tr>
                                    </thead>
                                    <tbody>
<%
            int i = 1;
            for (Map<String, String> r : rows) {

                String fraudStr = r.get("fraud");
                boolean isFraud = "1".equals(fraudStr);

                int probPercent = 0;
                try {
                    double p = Double.parseDouble(r.get("prob_fraud"));
                    probPercent = (int)Math.round(p * 100);
                } catch (Exception ignore) {
                    probPercent = 0;
                }

                String rowNum = r.get("row_num");
                if (rowNum == null) rowNum = "-";
%>
                                        <tr>
                                            <td><%= i %></td>
                                            <td class="mono"><%= rowNum %></td>
                                            <td class="mono"><%= r.get("step") %></td>
                                            <td class="mono"><%= r.get("type_code") %></td>
                                            <td class="mono"><%= r.get("amount") %></td>
                                            <td class="mono"><%= r.get("oldbalanceOrg") %></td>
                                            <td class="mono"><%= r.get("newbalanceOrig") %></td>
                                            <td class="mono"><%= r.get("oldbalanceDest") %></td>
                                            <td class="mono"><%= r.get("newbalanceDest") %></td>

                                            <td>
                                                <% if (isFraud) { %>
                                                    <span class="badge text-bg-danger">YES</span>
                                                <% } else { %>
                                                    <span class="badge text-bg-success">NO</span>
                                                <% } %>
                                            </td>

                                            <td class="mono"><%= probPercent %>%</td>
                                        </tr>
<%
                i++;
            }
%>
                                    </tbody>
                                </table>
                            </div>
<%
        }
    }
%>

                        </div>
                    </div>
                </div>

                <!-- Card 3: Download -->
                <div class="col-12 mb-4">
                    <div class="card shadow-sm">
                        <div class="card-body p-4">
                            <h5 class="section-title mb-2">Download Full Result CSV</h5>
                            <div class="accent-bar small"></div>

<%
    String downloadToken = (String) request.getAttribute("downloadToken");
    if (downloadToken == null) {
%>
                            <div class="hint-box mt-3">
                                Download will be available after you upload and predict.
                            </div>
<%
    } else {
%>
                            <div class="hint-box mt-3">
                                Your full result file is ready.
                            </div>

                            <div class="mt-3">
                                <a class="btn btn-dark"
                                   href="<%=request.getContextPath()%>/batch-download?token=<%=downloadToken%>">
                                    Download full prediction CSV
                                </a>
                            </div>
<%
    }
%>

                        </div>
                    </div>
                </div>

            </div>
        </div>

    </div>
</div>

<script>
    const fileInput = document.getElementById("csvFileInput");
    const fileNameSpan = document.getElementById("csvFileName");

    fileInput.addEventListener("change", function () {
        if (fileInput.files.length > 0) {
            fileNameSpan.textContent = fileInput.files[0].name;
        } else {
            fileNameSpan.textContent = "No file selected";
        }
    });
</script>

</body>
</html>
