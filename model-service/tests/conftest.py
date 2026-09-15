import os
import sys

import pytest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app import FraudModel, create_app  # noqa: E402

MODEL_PATH = os.path.join(os.path.dirname(__file__), "..", "model", "model_proto_rf.pkl")


@pytest.fixture(scope="session")
def model():
    return FraudModel(MODEL_PATH, threshold=0.25)


@pytest.fixture(scope="session")
def client(model):
    app = create_app(model)
    app.testing = True
    return app.test_client()
