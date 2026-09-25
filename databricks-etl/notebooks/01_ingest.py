# Databricks notebook source
# MAGIC %md
# MAGIC # 01 · Ingest (raw CSV → bronze)
# MAGIC
# MAGIC Reads the raw PaySim CSV from a Unity Catalog Volume **with an explicit schema**,
# MAGIC adds two lineage columns (`ingested_at`, `source_file`) and writes the Delta table
# MAGIC `bronze_transactions`. No rows are dropped or fixed here — bronze is "exactly what arrived".

# COMMAND ----------

# Parameters. When this notebook runs as a Job task, the Job's parameters fill these in;
# when run by hand, edit them in the widget bar at the top of the notebook.
dbutils.widgets.text("catalog", "workspace")
dbutils.widgets.text("schema", "paysim")
dbutils.widgets.text("input_path", "/Volumes/workspace/paysim/raw/paysim_sample_70k.csv")

catalog = dbutils.widgets.get("catalog")
schema = dbutils.widgets.get("schema")
input_path = dbutils.widgets.get("input_path")

spark.sql(f"CREATE SCHEMA IF NOT EXISTS {catalog}.{schema}")
print(f"reading {input_path} -> {catalog}.{schema}")

# COMMAND ----------

from pyspark.sql import functions as F
from pyspark.sql.types import DecimalType, IntegerType, StringType, StructField, StructType

# Explicit schema instead of inferSchema: types never depend on what the file happens to
# contain, and Spark does not scan the file twice. Money columns are DECIMAL so sums are
# exact (no floating-point drift), which lets the reconciliation checks compare with ==.
MONEY = DecimalType(18, 2)
RAW_SCHEMA = StructType([
    StructField("step", IntegerType()),
    StructField("type", StringType()),
    StructField("amount", MONEY),
    StructField("nameOrig", StringType()),
    StructField("oldbalanceOrg", MONEY),
    StructField("newbalanceOrig", MONEY),
    StructField("nameDest", StringType()),
    StructField("oldbalanceDest", MONEY),
    StructField("newbalanceDest", MONEY),
    StructField("isFraud", IntegerType()),
    StructField("isFlaggedFraud", IntegerType()),
])

# COMMAND ----------

bronze = (
    spark.read
    .option("header", "true")
    .schema(RAW_SCHEMA)
    .csv(input_path)
    .withColumn("ingested_at", F.current_timestamp())
    # _metadata.file_path is the Unity Catalog way to get the file each row came from
    .withColumn("source_file", F.col("_metadata.file_path"))
)

# overwrite: re-running replaces the table instead of appending a second copy
(bronze.write
    .mode("overwrite")
    .option("overwriteSchema", "true")
    .saveAsTable(f"{catalog}.{schema}.bronze_transactions"))

# COMMAND ----------

result = spark.table(f"{catalog}.{schema}.bronze_transactions").agg(
    F.count("*").alias("row_count"),
    F.sum("amount").alias("total_amount"),
).first()
print(f"bronze_transactions rows:   {result['row_count']:,}")
print(f"bronze_transactions amount: {result['total_amount']:,}")
