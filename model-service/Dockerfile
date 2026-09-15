FROM python:3.10-slim

WORKDIR /app

# Install dependencies first so the layer is cached across code changes.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .
COPY model/ model/

ENV MODEL_PATH=model/model_proto_rf.pkl \
    FRAUD_THRESHOLD=0.25 \
    PORT=5000

EXPOSE 5000

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
  CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://localhost:5000/health').status==200 else 1)"

# 2 sync workers is plenty for a demo; the model is CPU-bound.
CMD ["sh", "-c", "gunicorn --bind 0.0.0.0:${PORT} --workers 2 --timeout 120 'app:create_app()'"]
