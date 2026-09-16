<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*" %>
<%@ page import="com.yuecai.fraud.prediction.PredictionResult" %>
<%@ page import="com.yuecai.fraud.batch.BatchJobResponse" %>
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
                    <h4 class="section-title mb-1 mt-1">Batch Job</h4>
                    <div class="accent-bar"></div>

                    <p class="text-muted mt-3 mb-0">
                        Progress of one background scoring job. This page refreshes itself while the job is running.
                    </p>
                </div>
            </div>

            <div class="row g-3 mt-0">

                <!-- Card 1: Status -->
                <div class="col-12">
                    <div class="card shadow-sm">
                        <div class="card-body p-4">
                            <h5 class="section-title mb-2">Job <span class="mono"><c:out value="${job.id}"/></span></h5>
                            <div class="accent-bar small"></div>

                            <div class="mt-3">
                                <b>File:</b> <c:out value="${job.originalFilename}"/> &nbsp;
                                <b>Status:</b>
                                <span id="statusBadge" class="badge ${job.status == 'SUCCEEDED' ? 'text-bg-success' : job.status == 'FAILED' ? 'text-bg-danger' : job.status == 'RUNNING' ? 'text-bg-primary' : 'text-bg-secondary'}">
                                    <c:out value="${job.status}"/>
                                </span>
                                &nbsp; <b>Attempt:</b> <span id="attempts" class="mono"><c:out value="${job.attempts}"/></span> / <c:out value="${job.maxAttempts}"/>
                            </div>

                            <div class="progress mt-3" role="progressbar" style="height: 22px;">
                                <div id="progressBar" class="progress-bar" style="width: ${job.progressPercent}%; background:#A27B5C;">
                                    <span id="progressText">${job.progressPercent}%</span>
                                </div>
                            </div>
                            <div class="text-muted mt-2">
                                Rows: <span id="processedRows" class="mono"><c:out value="${job.processedRows}"/></span>
                                / <span id="totalRows" class="mono"><c:out value="${job.totalRows == null ? '?' : job.totalRows}"/></span>
                                &nbsp;·&nbsp; Flagged as fraud: <span id="fraudCount" class="mono"><c:out value="${job.fraudCount}"/></span>
                                &nbsp;·&nbsp; Skipped lines: <span id="skippedRows" class="mono"><c:out value="${job.skippedRows}"/></span>
                            </div>

                            <c:if test="${not empty job.lastError}">
                            <div class="alert alert-warning mt-3 mb-0">
                                <b>Last error:</b> <c:out value="${job.lastError}"/>
                            </div>
                            </c:if>

                            <div class="mt-3 d-flex gap-2">
                                <c:if test="${job.status == 'SUCCEEDED'}">
                                <a class="btn btn-dark" href="<%=request.getContextPath()%>/batch/jobs/<c:out value='${job.id}'/>/download">
                                    Download result CSV
                                </a>
                                </c:if>
                                <c:if test="${job.status == 'FAILED'}">
                                <form method="post" action="<%=request.getContextPath()%>/batch/jobs/<c:out value='${job.id}'/>/retry">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                    <button class="btn btn-outline-secondary" type="submit">Retry (resumes from row <c:out value="${job.processedRows}"/>)</button>
                                </form>
                                </c:if>
                                <a class="btn btn-outline-secondary" href="<%=request.getContextPath()%>/batch">Back to batch page</a>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Card 2: Preview -->
                <c:if test="${job.status == 'SUCCEEDED'}">
                <div class="col-12 mb-4">
                    <div class="card shadow-sm">
                        <div class="card-body p-4">
                            <h5 class="section-title mb-2">Preview (Top 10)</h5>
                            <div class="accent-bar small"></div>
                            <div class="text-muted mt-3">Fraud is shown as YES/NO. Probability is shown as a percentage. The full result is in the CSV.</div>
                            <div class="table-responsive mt-3">
                                <table class="table table-sm align-middle mb-0">
                                    <thead>
                                        <tr>
                                            <th>CSV Row</th><th>step</th><th>type</th><th>amount</th>
                                            <th>oldbalanceOrg</th><th>newbalanceOrig</th><th>oldbalanceDest</th><th>newbalanceDest</th>
                                            <th>Fraud?</th><th>Probability</th>
                                        </tr>
                                    </thead>
                                    <tbody>
<%
    @SuppressWarnings("unchecked")
    List<PredictionResult> rows = (List<PredictionResult>) request.getAttribute("previewRows");
    if (rows != null) {
        for (PredictionResult r : rows) {
%>
                                        <tr>
                                            <td class="mono"><%= r.csvRow() %></td>
                                            <td class="mono"><%= r.step() %></td>
                                            <td class="mono"><%= r.type() %></td>
                                            <td class="mono"><%= r.amount() %></td>
                                            <td class="mono"><%= r.oldbalanceOrg() %></td>
                                            <td class="mono"><%= r.newbalanceOrig() %></td>
                                            <td class="mono"><%= r.oldbalanceDest() %></td>
                                            <td class="mono"><%= r.newbalanceDest() %></td>
                                            <td><% if (r.fraud()) { %><span class="badge text-bg-danger">YES</span><% } else { %><span class="badge text-bg-success">NO</span><% } %></td>
                                            <td class="mono"><%= r.probPercent() %>%</td>
                                        </tr>
<%
        }
    }
%>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
                </c:if>

            </div>
        </div>

    </div>
</div>

<c:if test="${job.status == 'PENDING' || job.status == 'RUNNING'}">
<script>
    // Poll the job until it finishes, then reload so the server renders the final state.
    (function () {
        const url = "<%=request.getContextPath()%>/batch/jobs/<c:out value='${job.id}'/>/status";
        function tick() {
            fetch(url, {credentials: "same-origin"})
                .then(r => r.ok ? r.json() : Promise.reject(r.status))
                .then(j => {
                    document.getElementById("statusBadge").textContent = j.status;
                    document.getElementById("attempts").textContent = j.attempts;
                    document.getElementById("processedRows").textContent = j.processedRows;
                    document.getElementById("totalRows").textContent = j.totalRows == null ? "?" : j.totalRows;
                    document.getElementById("fraudCount").textContent = j.fraudCount;
                    document.getElementById("skippedRows").textContent = j.skippedRows;
                    document.getElementById("progressBar").style.width = j.progressPercent + "%";
                    document.getElementById("progressText").textContent = j.progressPercent + "%";
                    if (j.status === "SUCCEEDED" || j.status === "FAILED") {
                        window.location.reload();
                    } else {
                        setTimeout(tick, 3000);
                    }
                })
                .catch(() => setTimeout(tick, 5000));
        }
        setTimeout(tick, 3000);
    })();
</script>
</c:if>

</body>
</html>
