---
title:  'CP630 project proposal<br> PaySim-Based Fraud Detection System'
author: Yue Cai, Nilufar Hossain
date: 2025-11-25
---
# CP630 Project Proposal  
## PaySim-Based Fraud Detection System  
**Members:** Yue Cai, Nilufar Hossain  
**Date:** Nov 25, 2025

## Introduction

Billions of transactions happen every day, while the risk of fraud increases simultaneously. With detailed transaction data such as time, amount, and receiver account, machine learning can be a useful tool to discover patterns behind fraud cases and build a fraud detection application.  
In this project, we use a synthetic financial transaction dataset (from PaySim) to train and compare different machine learning models and build a fraud detection system. The final model is deployed as an enterprise application using Flask REST API, Java SOAP, WildFly, and JSP.  Users can input transaction data and get prediction results, with short explanations by web interface, and they can even get batch prediction by uploading a CSV file.


## Problem solving and algorithms

### 1.  Data Source and Dataset Design
We use the PaySim synthetic financial transaction dataset. Each record represents a transaction with features such as type, amount, balance changes, and a label (1 = fraud, 0 = not fraud). Fraud detection is a binary classification task with strong class imbalance.  
The original dataset contains over 6.3 million transactions in a 30-day period, suitable for large-scale data mining.
For initial prototyping, we applied stratified sampling, keeping all 8,213 fraud cases and randomly sampling non-fraud cases, resulting in a subset of about 70,000 rows.
Later after the Lesson 12 and Lab 5, we plan to explore distributed training using big data tools __(Spark/Hadoop)__ on the full dataset.

### 2. Data Pre-processing
- Removed irrelevant columns: nameOrig, nameDest, isFlaggedFraud
- Label-encoded type into type_code
- Added two engineered features: (__new feature__).
    balanceDiffOrg = oldbalanceOrg − newbalanceOrig  
    balanceDiffDest = newbalanceDest − oldbalanceDest

### 3. Models and Evaluation
We tested three scikit-learn classification algorithms: 
- Decision Tree
- Random Forest 
- K-Nearest Neighbors (KNN)

The models were evaluated using Accuracy, Precision, Recall, and F1-score, with a focus on Recall and F1 due to class imbalance.
 Random Forest achieved the best overall performance and also provides feature importance, which allows basic explanation of fraud. Therefore, it was selected as our final model for deployment.


## Proposed System Design

### 1. System Components
- __Signup and Login pages__
 Allow user registration and login, using session tracking to restrict access to fraud detection pages.

- __Single Transaction Prediction page__
 A JSP form where the user can enter one transaction’s details, including amount, transaction type, old and new account balances, and step (time). The data is submitted to a Java Servlet, which calls the backend service to get the fraud prediction.

- __Prediction Result page (with Explainable AI)__
 Displays whether the transaction is fraud or not. If it is fraud, the system also shows a confidence score and the top three reasons (__new feature__).

- __Batch Prediction page & Result page__
Users can upload a CSV file with many transactions. The system runs batch prediction and returns a summary showing how many are fraud and highlights the suspicious records in a table. (__new feature__).

- __About / Help page__
 Provides basic guidance on how to use the system, background of the PaySim dataset, and project purpose.

### 2. System architecture 
Our machine learning model is trained using Python (scikit-learn) and saved as model.pkl.
 It is deployed through a Flask REST API, which handles both single transaction prediction and batch detection. We also plan to extend the system to support big data processing using Spark or Hadoop when training on the full dataset later.
A Java SOAP web service (JAX-WS) calls the Flask API and integrates the prediction service into our enterprise application running on WildFly.
 The frontend is developed using JSP and Servlet, allowing users to input transaction data and view prediction results through a web interface.
This architecture follows Java-based enterprise computing while keeping flexibility for Python-based model training and future scalability.

### 3. Platform and tools to be used in the project. 
- Java EE (Servlet, JSP, SOAP, WAR deployment on WildFly)
- Python Flask (for ML model hosting)
- scikit-learn (for training machine learning models)
- Maven, Jenkins (for build and deployment)
- HTML / CSS / JavaScript (for frontend user interface)
- (Optional, for later extension) Hadoop / Spark

## Project plan and schedule
  

| Task ID | Description   |  Due date | Lead   |  
| :----:  | :------------ | :-----:   | :------: |  
|  1      | Dataset understanding and feature engineering | week 12 | Yue | 
|  2      | Train and compare ML models (DT, RF, KNN) | Week 12 | Nilufar|
|  3      | Select best model and deploy Flask API  | Week 12 | All  |
|  4      | Create SOAP service and connect to Flask  | Week 13 | Yue  |
|  5      | Design JSP pages and prediction UI     | Week 13 | Nilufar  |
|  6      | Implement batch prediction feature     | Week 13 | Yue  | 
|  7      | Deploy web app on WildFly using Maven/Jenkins    | Week 14 | Nilufar  | 
|  8      | Final testing, documentation, and presentation     | Week 14 | All  | 
 

## References

1. Dataset: https://www.kaggle.com/datasets/ealaxi/paysim1
2. Jenkins – https://www.jenkins.io/
3. Apache Karaf – https://karaf.apache.org/



