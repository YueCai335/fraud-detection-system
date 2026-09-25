# Databricks notebook source
# MAGIC %md
# MAGIC # 04 · Reconciliation checks
# MAGIC
# MAGIC Proves nothing was lost or double-counted between layers:
# MAGIC - **row_reconciliation** — bronze rows == silver rows + quarantine rows
# MAGIC - **amount_reconciliation** — SUM(total_amount) in gold_summary_by_type_step == SUM(amount) in silver
# MAGIC - **count_reconciliation** — SUM(transaction_count) in gold_summary_by_type_step == silver rows
# MAGIC
# MAGIC Every result is appended to `audit_checks` (a log, so append is intended here).
# MAGIC If any check fails the notebook raises an exception, which marks the Job run as **Failed**.

# COMMAND ----------

dbutils.widgets.text("catalog", "workspace")
dbutils.widgets.text("schema", "paysim")

catalog = dbutils.widgets.get("catalog")
schema = dbutils.widgets.get("schema")

def t(name):
    return f"{catalog}.{schema}.{name}"

# COMMAND ----------

from datetime import datetime, timezone
from decimal import Decimal

from pyspark.sql import functions as F
from pyspark.sql.types import BooleanType, DecimalType, StringType, StructField, StructType, TimestampType

bronze_rows = spark.table(t("bronze_transactions")).count()
silver = spark.table(t("silver_transactions"))
silver_rows = silver.count()
quarantine_rows = spark.table(t("quarantine_transactions")).count()
gold = spark.table(t("gold_summary_by_type_step"))

# Amounts are DECIMAL, so the sums are exact and can be compared with ==.
# coalesce(..., 0) keeps an empty table from producing null instead of 0.
silver_amount = silver.agg(F.coalesce(F.sum("amount"), F.lit(0))).first()[0]
gold_amount, gold_count = gold.agg(
    F.coalesce(F.sum("total_amount"), F.lit(0)),
    F.coalesce(F.sum("transaction_count"), F.lit(0)),
).first()

# (check_name, expected, actual)
checks = [
    ("row_reconciliation",    bronze_rows,   silver_rows + quarantine_rows),
    ("amount_reconciliation", silver_amount, gold_amount),
    ("count_reconciliation",  silver_rows,   gold_count),
]

# COMMAND ----------

run_at = datetime.now(timezone.utc)
AUDIT_SCHEMA = StructType([
    StructField("check_name", StringType()),
    StructField("expected", DecimalType(38, 2)),
    StructField("actual", DecimalType(38, 2)),
    StructField("passed", BooleanType()),
    StructField("run_at", TimestampType()),
])
rows = [
    (name, Decimal(expected), Decimal(actual), expected == actual, run_at)
    for name, expected, actual in checks
]
audit = spark.createDataFrame(rows, AUDIT_SCHEMA)

# append, not overwrite: audit_checks is a history of every run
audit.write.mode("append").saveAsTable(t("audit_checks"))
audit.show(truncate=False)

# COMMAND ----------

failed = [name for name, _, _, passed, _ in rows if not passed]
if failed:
    raise Exception(f"Reconciliation failed: {failed}")
print("All reconciliation checks passed.")
