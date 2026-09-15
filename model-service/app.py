"""
Fraud model inference service.

A small Flask app that wraps the trained RandomForest (scikit-learn) model
and exposes three endpoints:

    GET  /health          liveness + model metadata
    POST /predict         score one transaction
    POST /batch_predict   score a list of transactions

The scoring logic is intentionally unchanged from the original course
project so that the Spring Boot migration can be verified against the
same outputs. Only configuration, logging and the app-factory wrapper
were added.
"""

import logging
import os
from typing import Any

import joblib
import numpy as np
import shap
from flask import Flask, jsonify, request

log = logging.getLogger("model-service")

# ---------------------------------------------------------------------------
# Feature definition (order matters — it must match the training notebook)
# ---------------------------------------------------------------------------
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

TYPE_CODE_TEXT = {
    0: "CASH_IN",
    1: "CASH_OUT",
    2: "DEBIT",
    3: "PAYMENT",
    4: "TRANSFER",
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


class FraudModel:
    """Loads the pickled sklearn model once and scores feature rows."""

    def __init__(self, model_path: str, threshold: float):
        log.info("Loading model from %s", model_path)
        self.model = joblib.load(model_path)
        self.threshold = threshold
        self.model_path = model_path
        log.info("Building SHAP TreeExplainer")
        self.explainer = shap.TreeExplainer(self.model)
        log.info("Model ready (threshold=%.2f)", threshold)

    # -- feature handling ---------------------------------------------------

    @staticmethod
    def build_feature_vector(data: dict[str, Any]) -> list[float]:
        return [float(data[name]) for name in FEATURE_NAMES]

    @staticmethod
    def missing_fields(data: dict[str, Any]) -> list[str]:
        return [f for f in FEATURE_NAMES if f not in data]

    # -- scoring ------------------------------------------------------------

    def probabilities(self, X: np.ndarray) -> list[float | None]:
        if hasattr(self.model, "predict_proba"):
            return self.model.predict_proba(X)[:, 1].tolist()
        return [None] * len(X)

    def decide(self, X_row: np.ndarray, prob: float | None) -> int:
        if prob is None:
            return int(self.model.predict(X_row)[0])
        return 1 if float(prob) >= self.threshold else 0

    def score_rows(self, X: np.ndarray) -> list[dict[str, Any]]:
        probas = self.probabilities(X)
        out = []
        for i, prob in enumerate(probas):
            row = X[i : i + 1]
            y = self.decide(row, prob)
            reasons = self.top3_reasons(row, y)
            out.append(
                {
                    "fraud": int(y),
                    "prob_fraud": float(prob) if prob is not None else None,
                    "reason1": reasons[0],
                    "reason2": reasons[1],
                    "reason3": reasons[2],
                    "threshold": self.threshold,
                }
            )
        return out

    # -- explanations -------------------------------------------------------

    @staticmethod
    def _extract_fraud_shap_vector(shap_out, n_features: int):
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

    @staticmethod
    def _reason_text(fname: str, X_row: np.ndarray) -> str:
        if fname == "type_code":
            t_val = int(float(X_row[0][FEATURE_NAMES.index("type_code")]))
            return f"High-risk transaction type ({TYPE_CODE_TEXT.get(t_val, 'UNKNOWN')})"
        return FEATURE_TEXT.get(fname, fname)

    def top3_reasons(self, X_row: np.ndarray, y_pred: int) -> list[str]:
        if int(y_pred) == 0:
            return ["No risk detected", "N/A", "N/A"]
        try:
            sv = self._extract_fraud_shap_vector(
                self.explainer.shap_values(X_row), len(FEATURE_NAMES)
            )
            if sv is None:
                return ["Model explanation unavailable", "N/A", "N/A"]

            pos_idx = [i for i in range(len(sv)) if sv[i] > 0]
            pos_idx.sort(key=lambda i: abs(sv[i]), reverse=True)

            reasons = [self._reason_text(FEATURE_NAMES[i], X_row) for i in pos_idx[:3]]
            if not reasons:
                top_idx = np.argsort(np.abs(sv))[::-1][:3]
                reasons = [self._reason_text(FEATURE_NAMES[i], X_row) for i in top_idx]

            while len(reasons) < 3:
                reasons.append("N/A")
            return reasons[:3]
        except Exception:  # noqa: BLE001 — explanation must never break scoring
            log.exception("SHAP explanation failed")
            return ["Model explanation unavailable", "N/A", "N/A"]


# ---------------------------------------------------------------------------
# Flask application factory
# ---------------------------------------------------------------------------
def create_app(model: FraudModel | None = None) -> Flask:
    logging.basicConfig(
        level=os.getenv("LOG_LEVEL", "INFO"),
        format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    )
    app = Flask(__name__)

    if model is None:
        model = FraudModel(
            model_path=os.getenv("MODEL_PATH", "model/model_proto_rf.pkl"),
            threshold=float(os.getenv("FRAUD_THRESHOLD", "0.25")),
        )
    app.config["MODEL"] = model

    @app.get("/health")
    def health():
        return jsonify(
            {
                "status": "UP",
                "model_path": os.path.basename(model.model_path),
                "threshold": model.threshold,
                "features": FEATURE_NAMES,
            }
        )

    @app.post("/predict")
    def predict():
        data = request.get_json(force=True, silent=True)
        if not isinstance(data, dict):
            return jsonify({"error": "Request body must be a JSON object"}), 400

        missing = model.missing_fields(data)
        if missing:
            return jsonify({"error": "Missing required fields", "missing_fields": missing}), 400

        try:
            X = np.array([model.build_feature_vector(data)], dtype=float)
        except (TypeError, ValueError):
            return jsonify({"error": "All feature values must be numeric"}), 400

        return jsonify(model.score_rows(X)[0])

    @app.post("/batch_predict")
    def batch_predict():
        data = request.get_json(force=True, silent=True) or {}
        records = data.get("records", [])
        if not records:
            return jsonify({"error": "No records provided"}), 400

        for idx, rec in enumerate(records):
            missing = model.missing_fields(rec)
            if missing:
                return (
                    jsonify(
                        {
                            "error": f"Missing required fields in record {idx}",
                            "missing_fields": missing,
                        }
                    ),
                    400,
                )

        try:
            X = np.array([model.build_feature_vector(r) for r in records], dtype=float)
        except (TypeError, ValueError):
            return jsonify({"error": "All feature values must be numeric"}), 400

        scored = model.score_rows(X)
        results = [{"input": rec, **res} for rec, res in zip(records, scored)]
        return jsonify({"results": results})

    return app


if __name__ == "__main__":
    create_app().run(host="0.0.0.0", port=int(os.getenv("PORT", "5000")))
