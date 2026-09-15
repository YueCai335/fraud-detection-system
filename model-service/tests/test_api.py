"""
Contract tests for the model service.

The two example transactions are shared with the Spring Boot service's
tests, so both sides of the HTTP boundary agree on the contract.
"""

# First row of the PaySim dataset: an ordinary PAYMENT.
LEGIT = {
    "step": 1, "type_code": 3, "amount": 9839.64,
    "oldbalanceOrg": 170136.0, "newbalanceOrig": 160296.36,
    "oldbalanceDest": 0.0, "newbalanceDest": 0.0,
    "balanceDiffOrg": 9839.64, "balanceDiffDest": 0.0,
}

# CASH_OUT that empties the sender's account into a previously empty
# destination — the textbook PaySim fraud pattern.
FRAUD = {
    "step": 100, "type_code": 1, "amount": 10000.0,
    "oldbalanceOrg": 10000.0, "newbalanceOrig": 0.0,
    "oldbalanceDest": 0.0, "newbalanceDest": 10000.0,
    "balanceDiffOrg": 10000.0, "balanceDiffDest": 10000.0,
}


def test_health(client):
    res = client.get("/health")
    assert res.status_code == 200
    body = res.get_json()
    assert body["status"] == "UP"
    assert body["threshold"] == 0.25
    assert len(body["features"]) == 9


def test_predict_fraud_transfer(client):
    res = client.post("/predict", json=FRAUD)
    assert res.status_code == 200
    body = res.get_json()
    assert body["fraud"] == 1
    assert body["prob_fraud"] >= 0.25
    assert body["reason1"] not in ("", "N/A", "No risk detected")


def test_predict_legit_payment(client):
    res = client.post("/predict", json=LEGIT)
    assert res.status_code == 200
    body = res.get_json()
    assert body["fraud"] == 0
    assert 0.0 <= body["prob_fraud"] < 0.25
    assert body["reason1"] == "No risk detected"


def test_predict_missing_fields(client):
    res = client.post("/predict", json={"step": 1})
    assert res.status_code == 400
    assert "amount" in res.get_json()["missing_fields"]


def test_predict_non_numeric(client):
    res = client.post("/predict", json={**LEGIT, "amount": "abc"})
    assert res.status_code == 400


def test_batch_predict(client):
    res = client.post("/batch_predict", json={"records": [LEGIT, FRAUD]})
    assert res.status_code == 200
    results = res.get_json()["results"]
    assert [r["fraud"] for r in results] == [0, 1]
    assert results[1]["input"] == FRAUD


def test_batch_predict_empty(client):
    res = client.post("/batch_predict", json={"records": []})
    assert res.status_code == 400


def test_batch_matches_single(client):
    """Batch scoring must give exactly the same numbers as single scoring."""
    single = client.post("/predict", json=FRAUD).get_json()
    batch = client.post("/batch_predict", json={"records": [FRAUD]}).get_json()["results"][0]
    for key in ("fraud", "prob_fraud", "reason1", "reason2", "reason3"):
        assert single[key] == batch[key]
