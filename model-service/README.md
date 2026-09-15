# model-service

Python inference service for the PaySim fraud model (scikit-learn RandomForest + SHAP explanations).

| Endpoint | Body | Returns |
|---|---|---|
| `GET /health` | – | `{status, model_path, threshold, features}` |
| `POST /predict` | one transaction (9 numeric features) | `{fraud, prob_fraud, reason1..3, threshold}` |
| `POST /batch_predict` | `{"records": [ ... ]}` | `{"results": [{input, fraud, prob_fraud, reason1..3, threshold}]}` |

Feature order: `step, type_code, amount, oldbalanceOrg, newbalanceOrig, oldbalanceDest, newbalanceDest, balanceDiffOrg, balanceDiffDest`.
`type_code` is the label-encoded transaction type: `0=CASH_IN 1=CASH_OUT 2=DEBIT 3=PAYMENT 4=TRANSFER`.

## Run locally

```bash
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements-dev.txt
pytest
python app.py            # http://localhost:5000
```

## Configuration

| Env var | Default | Meaning |
|---|---|---|
| `MODEL_PATH` | `model/model_proto_rf.pkl` | pickled sklearn estimator |
| `FRAUD_THRESHOLD` | `0.25` | probability at or above which a transaction is labelled fraud |
| `PORT` | `5000` | listen port |

The model was trained in [`../notebooks/Data and Model.ipynb`](../notebooks/) with scikit-learn 1.7.2; `requirements.txt` pins that version so the pickle loads cleanly.
