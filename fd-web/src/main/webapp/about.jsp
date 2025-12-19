<%@ page contentType="text/html; charset=UTF-8" %>
<%
    String username = (String) session.getAttribute("username");
    if (username == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    request.setAttribute("activePage", "about");
%>
<!DOCTYPE html>
<html>
<head>
    <title>Fraud Detection - About</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/theme.css">

    <style>
        .app-shell { min-height: 100vh; }
        .content-wrap { padding-top: 16px; padding-bottom: 16px; }

        /* ===== Sidebar (match Batch/Predict/Home style) ===== */
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
        .accent-bar {
            height: 4px;
            width: 60px;
            background: #A27B5C;
            border-radius: 999px;
            margin-top: 8px;
        }
        .accent-bar.small { width: 42px; height: 3px; }

        .section-title { color: #2C3930; font-weight: 800; }
        .mini-label { font-weight: 800; color: #2C3930; }

        .hint-box {
            background: rgba(220, 215, 201, 0.35);
            border: 1px solid rgba(162, 123, 92, 0.35);
            border-left: 6px solid #A27B5C;
            border-radius: 12px;
            padding: 12px 14px;
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

            <div class="card shadow-sm">
                <div class="card-body p-4">
                    <div class="text-muted" style="font-weight:700;letter-spacing:0.02em;">
                        PaySim-Based Fraud Detection System
                    </div>
                    <h4 class="section-title mb-1 mt-1">About</h4>
                    <div class="accent-bar"></div>
                </div>
            </div>

            <div class="row g-3 mt-0">
                <div class="col-12">
                    <div class="card shadow-sm">
                        <div class="card-body p-4">
                            <h5 class="section-title mb-2">New Features</h5>
                            <div class="accent-bar small"></div>

                            <ul class="mt-3 mb-0">
                                <li>
                                    <span class="mini-label">Provide</span>
                                    model-based Top 3 reasons (RandomForest, single prediction)
                                </li>
                                <li class="mt-2">
                                    <span class="mini-label">Allow</span>
                                    batch transaction prediction (enterprise-oriented)
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div class="col-12">
                    <div class="card shadow-sm">
                        <div class="card-body p-4">
                            <h5 class="section-title mb-2">Tech Stack</h5>
                            <div class="accent-bar small"></div>

                            <ul class="mt-3 mb-0">
                                <li><span class="mini-label">UI</span>: JSP, Bootstrap 5, theme.css</li>
                                <li class="mt-2"><span class="mini-label">Web</span>: Java Servlets, WildFly, Maven</li>
                                <li class="mt-2"><span class="mini-label">API</span>: Python Flask (REST)</li>
                                <li class="mt-2"><span class="mini-label">ML</span>: scikit-learn RandomForest</li>
                                <li class="mt-2"><span class="mini-label">DB</span>: MySQL (XAMPP, users only)</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div class="col-12 mb-4">
                    <div class="card shadow-sm">
                        <div class="card-body p-4">
                            <h5 class="section-title mb-2">Team / Version</h5>
                            <div class="accent-bar small"></div>

                            <div class="row mt-3">
                                <div class="col-12 col-md-6">
                                    <div class="hint-box">
                                        <b>Team members</b><br/><br/>
                                        Yue Cai (ID: 169076223)<br/>
                                        Nilufar Hossain (ID: 245844000)
                                    </div>
                                </div>

                                <div class="col-12 col-md-6 mt-3 mt-md-0">
                                    <div class="hint-box">
                                        <b>Version</b><br/><br/>
                                        v1.0<br/>
                                        Fall 2025
                                    </div>
                                </div>
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
