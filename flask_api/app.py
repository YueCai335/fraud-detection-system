from flask import Flask, request, jsonify
import joblib
import numpy as np
import shap

# =========================================================
# 1. Create Flask app
# =========================================================
app = Flask(__name__)

# =========================================================
# 2. Load trained sklearn model
# =========================================================
MODEL_PATH = "model_proto_rf.pkl"
print(f"Loading model from {MODEL_PATH} ...")
model = joblib.load(MODEL_PATH)
print("Model loaded.")

# =========================================================
# 2.1 Decision threshold (demo-friendly)
# =========================================================
FRAUD_THRESHOLD = 0.25
print(f"Fraud threshold = {FRAUD_THRESHOLD}")

# =========================================================
# 3. SHAP explainer (ML-based local explanations)
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

def build_feature_vector(data):
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
# 5. Human text
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

def _extract_fraud_shap_vector(shap_out, n_features):
    if hasattr(shap_out, "values"):
        shap_out = shap_out.values

    if isinstance(shap_out, list):
        arr = shap_out[1] if len(shap_out) >= 2 else shap_out[0]
        arr = np.array(arr)
        if arr.ndim == 2 and arr.shape[0] == 1 and arr.shape[1] == n_features:
            return arr[0]
        return arr.reshape(-1)[:n_features]

    arr = np.array(shap_out)

    if arr.ndim == 2 and arr.shape[0] == 1 and arr.shape[1] == n_features:
        return arr[0]

    if arr.ndim == 3 and arr.shape[0] == 1 and arr.shape[1] == n_features:
        cls_idx = 1 if arr.shape[2] > 1 else 0
        return arr[0, :, cls_idx]

    if arr.ndim == 1 and arr.shape[0] == n_features:
        return arr

    return None

def build_top3_reasons_ml(X_row, y_pred):
    if int(y_pred) == 0:
        return ["No risk detected", "N/A", "N/A"]

    try:
        shap_out = explainer.shap_values(X_row)
        sv = _extract_fraud_shap_vector(shap_out, len(FEATURE_NAMES))
        if sv is None:
            return ["Model explanation unavailable", "N/A", "N/A"]

        pos_idx = [i for i in range(len(sv)) if sv[i] > 0]
        pos_idx.sort(key=lambda i: abs(sv[i]), reverse=True)

        reasons = []
        for i in pos_idx:
            fname = FEATURE_NAMES[i]

            if fname == "type_code":
                # read current type_code from the input row
                t_idx = FEATURE_NAMES.index("type_code")
                t_val = int(float(X_row[0][t_idx]))
                t_name = TYPE_CODE_TEXT.get(t_val, "UNKNOWN")
                reasons.append(f"High-risk transaction type ({t_name})")
            else:
                reasons.append(FEATURE_TEXT.get(fname, fname))

            if len(reasons) == 3:
                break

        if not reasons:
            top_idx = np.argsort(np.abs(sv))[::-1][:3]
            for i in top_idx:
                fname = FEATURE_NAMES[i]
                if fname == "type_code":
                    t_idx = FEATURE_NAMES.index("type_code")
                    t_val = int(float(X_row[0][t_idx]))
                    t_name = TYPE_CODE_TEXT.get(t_val, "UNKNOWN")
                    reasons.append(f"High-risk transaction type ({t_name})")
                else:
                    reasons.append(FEATURE_TEXT.get(fname, fname))

        while len(reasons) < 3:
            reasons.append("N/A")
        return reasons[:3]

    except Exception:
        return ["Model explanation unavailable", "N/A", "N/A"]

# =========================================================
# 6. Single transaction prediction: /predict
# =========================================================
@app.route("/predict", methods=["POST"])
def predict():
    data = request.get_json(force=True)

    required_fields = [
        "step", "type_code", "amount",
        "oldbalanceOrg", "newbalanceOrig",
        "oldbalanceDest", "newbalanceDest",
        "balanceDiffOrg", "balanceDiffDest",
    ]

    missing = [f for f in required_fields if f not in data]
    if missing:
        return jsonify({
            "error": "Missing required fields",
            "missing_fields": missing
        }), 400

    X = np.array([build_feature_vector(data)], dtype=float)

    if hasattr(model, "predict_proba"):
        prob_fraud = float(model.predict_proba(X)[0][1])
    else:
        prob_fraud = None

    if prob_fraud is None:
        y_pred = int(model.predict(X)[0])
    else:
        y_pred = 1 if prob_fraud >= FRAUD_THRESHOLD else 0

    reasons = build_top3_reasons_ml(X, y_pred)

    return jsonify({
        "fraud": int(y_pred),
        "prob_fraud": prob_fraud,
        "reason1": reasons[0],
        "reason2": reasons[1],
        "reason3": reasons[2],
        "threshold": FRAUD_THRESHOLD
    })

# =========================================================
# 7. Batch prediction: /batch_predict
# =========================================================
@app.route("/batch_predict", methods=["POST"])
def batch_predict():
    data = request.get_json(force=True)
    records = data.get("records", [])

    if not records:
        return jsonify({"error": "No records provided"}), 400

    X = np.array([build_feature_vector(rec) for rec in records], dtype=float)

    if hasattr(model, "predict_proba"):
        probas = model.predict_proba(X)[:, 1].tolist()
    else:
        probas = [None] * len(records)

    results = []
    for i, rec in enumerate(records):
        prob_i = probas[i]
        if prob_i is None:
            y_pred_i = int(model.predict(X[i:i+1])[0])
        else:
            y_pred_i = 1 if float(prob_i) >= FRAUD_THRESHOLD else 0

        reasons = build_top3_reasons_ml(X[i:i+1], y_pred_i)

        results.append({
            "input": rec,
            "fraud": int(y_pred_i),
            "prob_fraud": float(prob_i) if prob_i is not None else None,
            "reason1": reasons[0],
            "reason2": reasons[1],
            "reason3": reasons[2],
            "threshold": FRAUD_THRESHOLD
        })

    return jsonify({"results": results})

# =========================================================
# 8. Run
# =========================================================
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
