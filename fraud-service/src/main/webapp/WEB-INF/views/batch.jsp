<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*" %>
<%@ page import="com.yuecai.fraud.prediction.PredictionResult" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
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
            <%@ include file="common/sidebar.jspf" %>
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
                                   href="<%=request.getContextPath()%>/samples/sample_transactions.csv"
                                   download>
                                    Download Sample CSV
                                </a>
                            </div>

                            <form class="mt-3"
                                  method="post"
                                  action="<%=request.getContextPath()%>/batch"
                                  enctype="multipart/form-data">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

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

                            <c:if test="${not empty error}">
                            <div class="alert alert-danger mt-3 mb-0" role="alert">
                                <b>Error:</b> <c:out value="${error}"/>
                            </div>
                            </c:if>

                            <div class="hint-box mt-3">
                                Files are scored in the background. After upload you are taken to the job page,
                                which shows progress and offers the result CSV when done.
                            </div>

                        </div>
                    </div>
                </div>

                <!-- Card 2: My jobs -->
                <div class="col-12 mb-4">
                    <div class="card shadow-sm">
                        <div class="card-body p-4">
                            <h5 class="section-title mb-2">My Batch Jobs</h5>
                            <div class="accent-bar small"></div>

                            <c:choose>
                            <c:when test="${empty jobs}">
                            <div class="hint-box mt-3">No jobs yet. Upload a CSV to start one.</div>
                            </c:when>
                            <c:otherwise>
                            <div class="table-responsive mt-3">
                                <table class="table table-sm align-middle mb-0">
                                    <thead>
                                        <tr>
                                            <th>Submitted</th>
                                            <th>File</th>
                                            <th>Status</th>
                                            <th>Rows</th>
                                            <th>Fraud</th>
                                            <th>Attempts</th>
                                            <th></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach var="j" items="${jobs}">
                                        <tr>
                                            <td class="mono"><c:out value="${j.createdAt}"/></td>
                                            <td><c:out value="${j.originalFilename}"/></td>
                                            <td>
                                                <span class="badge ${j.status == 'SUCCEEDED' ? 'text-bg-success' : j.status == 'FAILED' ? 'text-bg-danger' : j.status == 'RUNNING' ? 'text-bg-primary' : 'text-bg-secondary'}">
                                                    <c:out value="${j.status}"/>
                                                </span>
                                            </td>
                                            <td class="mono"><c:out value="${j.processedRows}"/><c:if test="${j.totalRows != null}"> / <c:out value="${j.totalRows}"/></c:if></td>
                                            <td class="mono"><c:out value="${j.fraudCount}"/></td>
                                            <td class="mono"><c:out value="${j.attempts}"/></td>
                                            <td><a href="<%=request.getContextPath()%>/batch/jobs/<c:out value='${j.id}'/>">Open</a></td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                            </c:otherwise>
                            </c:choose>

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
