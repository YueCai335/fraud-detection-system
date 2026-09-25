"""Rebuild the ~70k-row stratified PaySim sample used by the Databricks ETL.

Same logic as the "Prototype Dataset Construction" cell in notebooks/Data and Model.ipynb:
keep every fraud row, randomly sample non-fraud rows (random_state=42) up to 70,000, shuffle.
The notebook never saved the sample to disk, and by that point it had already dropped
nameOrig / nameDest / isFlaggedFraud / type. Here we sample the RAW file instead, so all
11 original columns survive. Row selection is identical because dropping columns does not
change row order.

Usage:
    python make_sample.py <path/to/30daysData.csv> <output.csv>
"""
import sys

import pandas as pd

TARGET_SIZE = 70000

src, out = sys.argv[1], sys.argv[2]
df = pd.read_csv(src)

df_fraud = df[df["isFraud"] == 1]
df_nonfraud = df[df["isFraud"] == 0]
df_nonfraud_sample = df_nonfraud.sample(n=TARGET_SIZE - len(df_fraud), random_state=42)

df_proto = pd.concat([df_fraud, df_nonfraud_sample], axis=0)
df_proto = df_proto.sample(frac=1.0, random_state=42).reset_index(drop=True)
df_proto.to_csv(out, index=False)

print("source rows:", len(df))
print("sample rows:", len(df_proto))
print(df_proto["isFraud"].value_counts().to_string())
print("total amount:", round(df_proto["amount"].sum(), 2))
