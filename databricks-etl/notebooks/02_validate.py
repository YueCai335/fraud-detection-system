# Databricks notebook source
# MAGIC %md
# MAGIC # 02 · Validate (bronze → silver + quarantine)
# MAGIC
# MAGIC Applies named data-quality rules to every bronze row.
# MAGIC Rows that pass every rule → `silver_transactions`.
# MAGIC Rows that fail one or more → `quarantine_transactions`, with a `failed_rules` column
# MAGIC listing **every** rule the row broke (not just the first one).

# COMMAND ----------

dbutils.widgets.text("catalog", "workspace")
dbutils.widgets.text("schema", "paysim")

catalog = dbutils.widgets.get("catalog")
schema = dbutils.widgets.get("schema")

# COMMAND ----------

from pyspark.sql import functions as F

VALID_TYPES = ["CASH_IN", "CASH_OUT", "DEBIT", "PAYMENT", "TRANSFER"]

# Each rule: (name, condition that is TRUE when the row BREAKS the rule).
# A null in a comparison (e.g. null < 0) is not true, so a missing amount only trips
# amount_not_null, not amount_non_negative as well.
RULES = [
    ("step_not_null",       F.col("step").isNull()),
    ("type_not_null",       F.col("type").isNull()),
    ("amount_not_null",     F.col("amount").isNull()),
    ("nameOrig_not_null",   F.col("nameOrig").isNull()),
    ("nameDest_not_null",   F.col("nameDest").isNull()),
    ("isFraud_not_null",    F.col("isFraud").isNull()),
    ("amount_non_negative", F.col("amount") < 0),
    ("type_valid",          ~F.col("type").isin(VALID_TYPES)),
    ("isFraud_0_or_1",      ~F.col("isFraud").isin(0, 1)),
]

# COMMAND ----------

bronze = spark.table(f"{catalog}.{schema}.bronze_transactions")

# For each rule, F.when(...) gives the rule name if broken, otherwise null.
# array_compact drops the nulls, leaving e.g. ["amount_non_negative", "type_valid"].
checked = bronze.withColumn(
    "failed_rules",
    F.array_compact(F.array(*[F.when(broken, F.lit(name)) for name, broken in RULES])),
)

silver = checked.filter(F.size("failed_rules") == 0).drop("failed_rules")
quarantine = checked.filter(F.size("failed_rules") > 0)

(silver.write.mode("overwrite").option("overwriteSchema", "true")
    .saveAsTable(f"{catalog}.{schema}.silver_transactions"))
(quarantine.write.mode("overwrite").option("overwriteSchema", "true")
    .saveAsTable(f"{catalog}.{schema}.quarantine_transactions"))

# COMMAND ----------

bronze_n = spark.table(f"{catalog}.{schema}.bronze_transactions").count()
silver_n = spark.table(f"{catalog}.{schema}.silver_transactions").count()
quarantine_t = spark.table(f"{catalog}.{schema}.quarantine_transactions")
quarantine_n = quarantine_t.count()

print(f"bronze_transactions:     {bronze_n:,}")
print(f"silver_transactions:     {silver_n:,}")
print(f"quarantine_transactions: {quarantine_n:,}")

# One row can break several rules, so these counts can add up to more than quarantine_n.
print("\nrows per failed rule:")
(quarantine_t
    .select(F.explode("failed_rules").alias("rule"))
    .groupBy("rule").count()
    .orderBy("rule")
    .show(truncate=False))
