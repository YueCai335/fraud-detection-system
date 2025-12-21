from flask import Flask, request, jsonify, Response
from flask_cors import CORS
import joblib
import numpy as np
import shap

# =========================================================
# 1. Create Flask app (必须最先)
# =========================================================
app = Flask(__name__)
CORS(app)

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

def _extract_fraud_shap_vector(shap_out, n_features):
    if hasattr(shap_out, "values"):
        shap_out = shap_out.values

    arr = np.array(shap_out)
    if arr.ndim == 3:
        return arr[0, :, 1]
    if arr.ndim == 2:
        return arr[0]
    return None

def build_top3_reasons_ml(X_row, y_pred):
    if y_pred == 0:
        return ["No risk detected", "N/A", "N/A"]

    try:
        shap_out = explainer.shap_values(X_row)
        sv = _extract_fraud_shap_vector(shap_out, len(FEATURE_NAMES))
        idx = np.argsort(np.abs(sv))[::-1][:3]

        reasons = []
        for i in idx:
            fname = FEATURE_NAMES[i]
            if fname == "type_code":
                t_val = int(X_row[0][FEATURE_NAMES.index("type_code")])
                t_name = TYPE_CODE_TEXT.get(t_val, "UNKNOWN")
                reasons.append(f"High-risk transaction type ({t_name})")
            else:
                reasons.append(FEATURE_TEXT.get(fname, fname))

        return reasons

    except Exception:
        return ["Model explanation unavailable", "N/A", "N/A"]

# =========================================================
# 6. HTML page route (强制返回 text/html)
# =========================================================
@app.route("/")
def home():
    with open("templates/index.html", "r", encoding="utf-8") as f:
        html = f.read()
    return Response(html, mimetype="text/html")

# =========================================================
# 7. Modern REST API
# =========================================================
@app.route("/api/predict", methods=["POST"])
def api_predict():
    data = request.get_json(force=True)

    X = np.array([build_feature_vector(data)], dtype=float)
    prob_fraud = float(model.predict_proba(X)[0][1])
    y_pred = 1 if prob_fraud >= FRAUD_THRESHOLD else 0

    return jsonify({
        "fraud": y_pred,
        "prob_fraud": prob_fraud,
        "threshold": FRAUD_THRESHOLD,
        "top3_reasons": build_top3_reasons_ml(X, y_pred)
    })

# =========================================================
# 8. Run
# =========================================================
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, debug=True)
