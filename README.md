# PaySim Fraud Detection System

An end-to-end fraud detection system built on the PaySim synthetic transaction dataset.
The system supports single-transaction prediction and batch CSV prediction through a web interface.

## Tech Stack
- Python (scikit-learn, Flask)
- Random Forest classifier
- Java (Servlet, SOAP)
- JSP / WildFly

## Features
- Single transaction fraud prediction with probability
- Batch CSV prediction for multiple transactions
- Python ML inference service (Flask REST)
- Java service integration with SOAP
- Web-based UI for prediction and result display

## Project Structure
- `data_model/` – data processing and model training steps
- `flask_api/` – Flask REST API for ML inference
- `fd-soap/` – Java SOAP integration
- `fd-web/` – JSP/Servlet web interface
- `samples/` – sample CSV files for batch testing
- `project_report.md` – project report with screenshots

## Notes
- Large raw datasets are not included due to GitHub size limits.
- Python virtual environments (`venv/`) are intentionally excluded.

