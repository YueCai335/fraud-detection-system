# Databricks notebook source
# MAGIC %md
# MAGIC # 03 · Report (silver → gold)
# MAGIC
# MAGIC Builds two summary tables with Spark SQL:
# MAGIC - `gold_summary_by_type_step` — one row per (step, type)
# MAGIC - `gold_summary_by_type` — one row per type, the easy-to-read version

# COMMAND ----------

dbutils.widgets.text("catalog", "workspace")
dbutils.widgets.text("schema", "paysim")

catalog = dbutils.widgets.get("catalog")
schema = dbutils.widgets.get("schema")
silver = f"{catalog}.{schema}.silver_transactions"

# COMMAND ----------

# Spark SQL does the aggregation; the DataFrame writer does a full overwrite.
# fraud_rate = share of transactions in the group that are fraud (0.0 – 1.0).
by_type_step = spark.sql(f"""
    SELECT step,
           type,
           COUNT(*)                          AS transaction_count,
           SUM(amount)                       AS total_amount,
           SUM(isFraud)                      AS fraud_count,
           ROUND(SUM(isFraud) / COUNT(*), 4) AS fraud_rate
    FROM {silver}
    GROUP BY step, type
""")

(by_type_step.write.mode("overwrite").option("overwriteSchema", "true")
    .saveAsTable(f"{catalog}.{schema}.gold_summary_by_type_step"))

# COMMAND ----------

by_type = spark.sql(f"""
    SELECT type,
           COUNT(*)                          AS transaction_count,
           SUM(amount)                       AS total_amount,
           SUM(isFraud)                      AS fraud_count,
           ROUND(SUM(isFraud) / COUNT(*), 4) AS fraud_rate
    FROM {silver}
    GROUP BY type
""")

(by_type.write.mode("overwrite").option("overwriteSchema", "true")
    .saveAsTable(f"{catalog}.{schema}.gold_summary_by_type"))

# COMMAND ----------

print(f"gold_summary_by_type_step rows: {spark.table(f'{catalog}.{schema}.gold_summary_by_type_step').count():,}")
spark.table(f"{catalog}.{schema}.gold_summary_by_type").orderBy("type").show(truncate=False)
