from flask import Flask, request, jsonify, Response, render_template, send_file
from flask_cors import CORS
import joblib
import numpy as np
import shap
import os
import csv
import uuid
import tempfile
from datetime import datetime
from werkzeug.utils import secure_filename

# =========================================================
# 1. Create Flask app (必须最先)
# =========================================================
app = Flask(__name__) # 创建一个可以接收HTTP请求的服务程序。app 就代表整个web服务（一个长期运行的程序，它在等别人通过网络来找它做事。）
CORS(app) # 自动加上 CORS 相关的 HTTP 头，允许浏览器跨域访问，给这个 Flask 应用开个后门，允许前端来访问

# =========================================================
# 2. Load trained sklearn model
# =========================================================
MODEL_PATH = "model_proto_rf.pkl"
print(f"Loading model from {MODEL_PATH} ...")
model = joblib.load(MODEL_PATH)
print("Model loaded.")

# =========================================================
# 2.1 Decision threshold
# =========================================================
FRAUD_THRESHOLD = 0.25
print(f"Fraud threshold = {FRAUD_THRESHOLD}")

# =========================================================
# Batch output folder (temporary files)
# =========================================================
BATCH_OUT_DIR = os.path.join(tempfile.gettempdir(), "fraud_batch_outputs")
os.makedirs(BATCH_OUT_DIR, exist_ok=True)

# =========================================================
# 3. SHAP explainer
# =========================================================
print("Building SHAP TreeExplainer ...")
explainer = shap.TreeExplainer(model)
print("SHAP explainer ready.")

# =========================================================
# 4. Feature vector builder
# =========================================================
FEATURE_NAMES = [
    "step",
    "type_code",
    "amount",
    "oldbalanceOrg",
    "newbalanceOrig",
    "oldbalanceDest",
    "newbalanceDest",
    "balanceDiffOrg",
    "balanceDiffDest",
]

def validate_and_build_X(data):
    """
    Validate incoming JSON dict and build sklearn input X (shape: 1 x n_features).
    Returns:
      (X, None) on success
      (None, error_dict) on failure
    """
    if not isinstance(data, dict):
        return None, {
            "error": "invalid_json",
            "detail": "Request body must be a JSON object (dictionary)."
        }

    # 1) Required fields check
    missing = [k for k in FEATURE_NAMES if k not in data]
    if missing:
        return None, {
            "error": "missing_fields",
            "detail": f"Missing required fields: {missing}",
            "missing": missing
        }

    # 2) Type/number conversion check + NaN/Inf check
    values = []
    bad_fields = {}

    for k in FEATURE_NAMES:
        raw = data.get(k)

        # Empty string / None is invalid
        if raw is None or (isinstance(raw, str) and raw.strip() == ""):
            bad_fields[k] = f"Empty value for '{k}'"
            continue

        try:
            v = float(raw)  # accept int/float/number-like string
        except (TypeError, ValueError):
            bad_fields[k] = f"'{k}' must be a number, got: {raw!r}"
            continue

        # Reject NaN / Inf
        if not np.isfinite(v):
            bad_fields[k] = f"'{k}' must be a finite number (not NaN/Inf), got: {raw!r}"
            continue

        values.append(v)

    if bad_fields:
        return None, {
            "error": "invalid_fields",
            "detail": "Some fields are invalid.",
            "fields": bad_fields
        }

    X = np.array([values], dtype=float)  # shape (1, n_features)
    return X, None

def build_feature_vector(data): #data 是从前端post来的JSON，data = request.get_json()后是字典
    return [
        data["step"],
        data["type_code"],
        data["amount"],
        data["oldbalanceOrg"],
        data["newbalanceOrig"],
        data["oldbalanceDest"],
        data["newbalanceDest"],
        data["balanceDiffOrg"],
        data["balanceDiffDest"],
    ]

# =========================================================
# 5. SHAP explanation helpers
# =========================================================
TYPE_CODE_TEXT = {
    0: "CASH_IN",
    1: "CASH_OUT",
    2: "DEBIT",
    3: "PAYMENT",
    4: "TRANSFER"
}

FEATURE_TEXT = {
    "step": "Transaction occurred at an unusual time step",
    "type_code": "High-risk transaction type",
    "amount": "Transaction amount is unusually large",
    "oldbalanceOrg": "Sender balance before transaction looks risky",
    "newbalanceOrig": "Sender balance after transaction is suspicious",
    "oldbalanceDest": "Receiver balance before transaction looks unusual",
    "newbalanceDest": "Receiver balance after transaction changed significantly",
    "balanceDiffOrg": "Sender balance dropped sharply",
    "balanceDiffDest": "Receiver balance increased sharply",
}

def _extract_fraud_shap_vector(shap_out, n_features): #不管 SHAP 返回的结构有多复杂，把它变成：每个特征一个数 的数组（向量）
    if hasattr(shap_out, "values"): # hasattr(obj, "values")，检查 obj 有没有 .values 这个属性
        shap_out = shap_out.values  # 如果有，就取出来

    arr = np.array(shap_out) # 取出来的数据，强行转化为numpy数组
    if arr.ndim == 3: #若给出的结果是三维的
        return arr[0, :, 1] # 只要欺诈 class（index=1）那一套贡献，index=0 non-fraud 不想知道理由
    if arr.ndim == 2:
        return arr[0]
    return None

def build_top3_reasons_ml(X_row, y_pred): #y_pred： 0 或者1
    if y_pred == 0:
        return ["No risk detected", "N/A", "N/A"] # non-fraud不需要解释理由，也无需prob

    try:
        shap_out = explainer.shap_values(X_row) # 让 SHAP 解释器给这条样本算贡献
        sv = _extract_fraud_shap_vector(shap_out, len(FEATURE_NAMES)) # 把输出整理成一维向量 sv
        idx = np.argsort(np.abs(sv))[::-1][:3] # 贡献取绝对值（只看影响大小，不看正负），从小到大排序后的索引，反过来，取前三个

        reasons = []
        for i in idx:
            fname = FEATURE_NAMES[i]
            if fname == "type_code":
                t_val = int(X_row[0][FEATURE_NAMES.index("type_code")]) # 从 X_row 二维列表里，取出 type_code 这个特征的“数值”。X_row[0][1]
                t_name = TYPE_CODE_TEXT.get(t_val, "UNKNOWN") #用 t_val 去字典里查对应的文字，查不到，就返回 "UNKNOWN"
                reasons.append(f"High-risk transaction type ({t_name})")
            else:
                reasons.append(FEATURE_TEXT.get(fname, fname))

        return reasons

    except Exception:
        return ["Model explanation unavailable", "N/A", "N/A"]

# =========================================================
# 6. HTML page route (强制返回 text/html)
# =========================================================
@app.route("/") #把 URL 路径 / 绑定到下面这个函数：浏览器访问：http://127.0.0.1:5001/，Flask 就调用 home() 这个函数
# def home():
#     with open("templates/index.html", "r", encoding="utf-8") as f:
#         html = f.read() #把整个HTML文件读成一个字符串
#     return Response(html, mimetype="text/html") #函数的返回值 = HTTP 响应，强制告诉浏览器按照网页渲染
def root():
    # 默认入口：跳到 home（你也可以直接 render home）
    return render_template("home.html")

@app.route("/home")
def home_page():
    return render_template("home.html")

@app.route("/single")
def single_page():
    return render_template("single.html")

@app.route("/batch")
def batch_page():
    return render_template("batch.html")

@app.route("/about")
def about_page():
    return render_template("about.html")

# =========================================================
# 7. Modern REST API， 定义 API /api/predict：返回 JSON 结果（给程序用）
# =========================================================
@app.route("/api/predict", methods=["POST"])  # 定义一个只接受 POST 的 API：/api/predict
def api_predict():
    data = request.get_json(force=True, silent=True) #把 HTTP 请求 body 里的 JSON，读成dictionary，存于data变量

    X, err = validate_and_build_X(data)
    if err is not None:
        # 400 Bad Request: client sent invalid data
        return jsonify(err), 400

    try:
        prob_fraud = float(model.predict_proba(X)[0][1])
        y_pred = 1 if prob_fraud >= FRAUD_THRESHOLD else 0

        return jsonify({
            "fraud": y_pred,
            "prob_fraud": prob_fraud,
            "threshold": FRAUD_THRESHOLD,
            "top3_reasons": build_top3_reasons_ml(X, y_pred)
        })
    except Exception as e:
        # 500 Internal Server Error: model/runtime problem
        return jsonify({
            "error": "prediction_failed",
            "detail": str(e)
        }), 500

@app.route("/api/predict_batch", methods=["POST"])
def api_predict_batch():
    if "file" not in request.files:
        return jsonify({"error": "missing_file", "detail": "No file uploaded. Field name must be 'file'."}), 400

    f = request.files["file"]
    if not f or f.filename.strip() == "":
        return jsonify({"error": "empty_file", "detail": "Uploaded file is empty or missing filename."}), 400

    filename = secure_filename(f.filename)
    if not filename.lower().endswith(".csv"):
        return jsonify({"error": "invalid_file_type", "detail": "Only .csv files are supported."}), 400

    # Read CSV content
    try:
        raw = f.read().decode("utf-8-sig")
    except Exception:
        return jsonify({"error": "read_failed", "detail": "Failed to read file as UTF-8 CSV."}), 400

    reader = csv.DictReader(raw.splitlines())
    if reader.fieldnames is None:
        return jsonify({"error": "invalid_csv", "detail": "CSV must include a header row."}), 400

    # User only uploads 7 columns; diffs are computed in backend
    required_input = [
        "step",
        "type_code",
        "amount",
        "oldbalanceOrg",
        "newbalanceOrig",
        "oldbalanceDest",
        "newbalanceDest",
    ]

    missing_cols = [c for c in required_input if c not in reader.fieldnames]
    if missing_cols:
        return jsonify({
            "error": "missing_columns",
            "detail": f"CSV missing required columns: {missing_cols}",
            "missing": missing_cols
        }), 400

    rows = list(reader)
    if len(rows) == 0:
        return jsonify({"error": "no_rows", "detail": "CSV has a header but contains no data rows."}), 400

    # Build X matrix (model expects 9 features including 2 diffs)
    X_list = []
    row_errors = []

    for i, row in enumerate(rows):
        bad = {}

        def read_num(col):
            raw_val = row.get(col)
            if raw_val is None or (isinstance(raw_val, str) and raw_val.strip() == ""):
                bad[col] = f"Empty value for '{col}'"
                return None
            try:
                v = float(raw_val)
            except (TypeError, ValueError):
                bad[col] = f"'{col}' must be a number, got: {raw_val!r}"
                return None
            if not np.isfinite(v):
                bad[col] = f"'{col}' must be finite (not NaN/Inf), got: {raw_val!r}"
                return None
            return v

        step = read_num("step")
        type_code = read_num("type_code")
        amount = read_num("amount")
        old_org = read_num("oldbalanceOrg")
        new_org = read_num("newbalanceOrig")
        old_dest = read_num("oldbalanceDest")
        new_dest = read_num("newbalanceDest")

        if bad:
            row_errors.append({"row_index": i, "fields": bad})
            X_list.append([0.0] * len(FEATURE_NAMES))  # placeholder
            continue

        # compute derived features
        balanceDiffOrg = old_org - new_org
        balanceDiffDest = new_dest - old_dest

        # IMPORTANT: must match FEATURE_NAMES order used by model
        vec = [
            step,
            type_code,
            amount,
            old_org,
            new_org,
            old_dest,
            new_dest,
            balanceDiffOrg,
            balanceDiffDest,
        ]
        X_list.append(vec)

    X = np.array(X_list, dtype=float)  # shape (n, 9)

    # Predict probabilities
    try:
        probs = model.predict_proba(X)[:, 1].astype(float)  # fraud class prob
    except Exception as e:
        return jsonify({"error": "prediction_failed", "detail": str(e)}), 500

    # Build output CSV path
    file_id = str(uuid.uuid4())
    out_path = os.path.join(BATCH_OUT_DIR, f"batch_result_{file_id}.csv")

    # Generate output rows + preview (top 10)
    preview = []

    out_fieldnames = reader.fieldnames[:] + ["fraud", "prob_fraud", "threshold", "reason1", "reason2", "reason3"]

    with open(out_path, "w", newline="", encoding="utf-8") as out_f:
        writer = csv.DictWriter(out_f, fieldnames=out_fieldnames)
        writer.writeheader()

        for i, row in enumerate(rows):
            p = float(probs[i])
            y_pred = 1 if p >= FRAUD_THRESHOLD else 0

            # If this row had parse errors, override with a safe "failed" record
            if any(err["row_index"] == i for err in row_errors):
                y_pred = -1
                p = -1.0
                reasons = ["Invalid row data", "N/A", "N/A"]
            else:
                # Only compute SHAP reasons if predicted fraud; else return "No risk detected"
                # Note: SHAP per-row is expensive; this is fine for moderate files.
                X_row = X[i:i+1, :]
                reasons = build_top3_reasons_ml(X_row, 1 if y_pred == 1 else 0)

            out_row = dict(row)
            out_row["fraud"] = y_pred
            out_row["prob_fraud"] = f"{p:.6f}" if p >= 0 else str(p)
            out_row["threshold"] = f"{FRAUD_THRESHOLD:.2f}"
            out_row["reason1"] = reasons[0] if len(reasons) > 0 else ""
            out_row["reason2"] = reasons[1] if len(reasons) > 1 else ""
            out_row["reason3"] = reasons[2] if len(reasons) > 2 else ""

            writer.writerow(out_row)

            # Preview only first 10
            if i < 10:
                prob_percent = int(round(p * 100)) if p >= 0 else -1
                preview.append({
                    "row_index": i,  # 0-based
                    "csv_row": i + 2,  # 1-based data row in file (header is row 1)
                    "step": row.get("step"),
                    "type_code": row.get("type_code"),
                    "amount": row.get("amount"),
                    "oldbalanceOrg": row.get("oldbalanceOrg"),
                    "newbalanceOrig": row.get("newbalanceOrig"),
                    "oldbalanceDest": row.get("oldbalanceDest"),
                    "newbalanceDest": row.get("newbalanceDest"),
                    "fraud": y_pred,
                    "prob_percent": prob_percent,
                    "threshold": FRAUD_THRESHOLD
                })

    # Return summary + preview + download handle
    # (We only return preview; full data is in downloadable CSV)
    resp = {
        "file_id": file_id,
        "total_rows": len(rows),
        "preview": preview
    }

    # If there are row-level errors, include a short summary (not full dump)
    if row_errors:
        resp["row_errors_count"] = len(row_errors)
        resp["detail"] = "Some rows contain invalid values and were marked as fraud=-1, prob_fraud=-1.0 in the output CSV."

    return jsonify(resp)

@app.route("/download/<file_id>", methods=["GET"])
def download_batch(file_id):
    # Basic safety check: only UUID format
    try:
        uuid.UUID(file_id)
    except Exception:
        return jsonify({"error": "invalid_file_id", "detail": "Invalid download id."}), 400

    path = os.path.join(BATCH_OUT_DIR, f"batch_result_{file_id}.csv")
    if not os.path.exists(path):
        return jsonify({"error": "not_found", "detail": "File not found or expired."}), 404

    # Download as attachment
    return send_file(
        path,
        as_attachment=True,
        download_name=f"batch_predictions_{file_id}.csv",
        mimetype="text/csv"
    )

# =========================================================
# 8. Run， 启动整个 Web 服务，FLASK服务
# =========================================================
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, debug=True)
