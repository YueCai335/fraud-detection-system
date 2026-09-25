# Databricks ETL — PaySim bronze / silver / gold

A small batch ETL workflow on **Databricks Free Edition** (serverless compute, Unity Catalog)
written in plain PySpark + Spark SQL over Delta tables. It loads PaySim transactions, applies named
data-quality rules, builds summary tables, and proves with reconciliation checks that no row or
cent was lost or double-counted between layers.

> **Data scale:** this runs on a **70,000-row stratified sample** of PaySim, not the full
> 6,362,620-row dataset. The sample keeps **all 8,213 fraud rows** plus 61,787 randomly sampled
> non-fraud rows (`random_state=42`) — the same sampling as the prototype cell in
> `notebooks/Data and Model.ipynb`, but on the raw file so all 11 original columns survive.
> Rebuild it with `scripts/make_sample.py` (the CSV itself is git-ignored).

This module is independent of `fraud-service/` and `model-service/`; nothing there was changed.

## Pipeline

```
 /Volumes/workspace/paysim/raw/paysim_sample_70k.csv
                     │
                     ▼  01_ingest   explicit schema, + ingested_at, source_file
            bronze_transactions        (exactly what arrived, nothing dropped)
                     │
                     ▼  02_validate named data-quality rules
        ┌────────────┴─────────────┐
        ▼                          ▼
silver_transactions      quarantine_transactions
 (passed every rule)      (+ failed_rules: every rule the row broke)
        │
        ▼  03_report   Spark SQL aggregates
gold_summary_by_type_step   (step × type)
gold_summary_by_type        (type only — the readable report)
        │
        ▼  04_checks   reconciliation; raises → Job run fails
audit_checks                (append-only log, one row per check per run)
```

All tables live in `workspace.paysim`. Every table except `audit_checks` is written with
`mode("overwrite")`, so re-running the job replaces data instead of duplicating it.
`audit_checks` is appended on purpose — it is the run history.

| Notebook | Reads | Writes |
|---|---|---|
| [`01_ingest`](notebooks/01_ingest.py) | CSV in the Volume | `bronze_transactions` |
| [`02_validate`](notebooks/02_validate.py) | bronze | `silver_transactions`, `quarantine_transactions` |
| [`03_report`](notebooks/03_report.py) | silver | `gold_summary_by_type_step`, `gold_summary_by_type` |
| [`04_checks`](notebooks/04_checks.py) | bronze, silver, quarantine, gold | `audit_checks` |

Money columns are `DECIMAL(18,2)`, so sums are exact and the reconciliation checks compare with `==`
instead of a floating-point tolerance.

## Data-quality rules (`02_validate`)

| Rule | A row breaks it when |
|---|---|
| `step_not_null` | `step` is null |
| `type_not_null` | `type` is null |
| `amount_not_null` | `amount` is null |
| `nameOrig_not_null` | `nameOrig` is null |
| `nameDest_not_null` | `nameDest` is null |
| `isFraud_not_null` | `isFraud` is null |
| `amount_non_negative` | `amount < 0` |
| `type_valid` | `type` not in CASH_IN, CASH_OUT, DEBIT, PAYMENT, TRANSFER |
| `isFraud_0_or_1` | `isFraud` not in 0, 1 |

A row that breaks several rules lists all of them in `failed_rules`. A null only trips its
`*_not_null` rule (e.g. a missing amount is not also reported as negative).

## Reconciliation checks (`04_checks`)

| Check | Expected | Actual |
|---|---|---|
| `row_reconciliation` | bronze rows | silver rows + quarantine rows |
| `amount_reconciliation` | `SUM(amount)` in silver | `SUM(total_amount)` in `gold_summary_by_type_step` |
| `count_reconciliation` | silver rows | `SUM(transaction_count)` in `gold_summary_by_type_step` |

Each run appends three rows (`check_name, expected, actual, passed, run_at`) to `audit_checks`,
then raises an exception if any check failed, which marks the Job run **Failed**.

## How to run

1. In the SQL Editor:
   ```sql
   CREATE SCHEMA IF NOT EXISTS workspace.paysim;
   CREATE VOLUME IF NOT EXISTS workspace.paysim.raw;
   ```
2. Catalog → `workspace` → `paysim` → `raw` → **Upload to this volume**:
   `paysim_sample_70k.csv` (from `scripts/make_sample.py`) and `test_data/bad_rows.csv`.
3. Workspace → Import the four files in `notebooks/` (they are Databricks source-format `.py`
   files and import as notebooks).
4. Jobs & Pipelines → Create Job `paysim-etl` with four Notebook tasks on Serverless:
   `ingest → validate → report → checks`, each depending on the previous one.
5. Job parameters (passed to each notebook's widgets):

   | Key | Value |
   |---|---|
   | `catalog` | `workspace` |
   | `schema` | `paysim` |
   | `input_path` | `/Volumes/workspace/paysim/raw/paysim_sample_70k.csv` |

6. **Run now**.

## Results (real runs, 2026-09-24)

Job `paysim-etl`, four serverless tasks. Run 1 took 1m 49s (ingest 59s, validate 18s,
report 15s, checks 13s); run 2 took 1m 3s. Both succeeded.

| | Rows | Total amount |
|---|---:|---:|
| Rows in (bronze) | 70,000 | 23,174,418,704.08 |
| Valid (silver) | 70,000 | 23,174,418,704.08 |
| Quarantined | 0 | — |
| Gold (`SUM` over 2,478 step × type rows) | 70,000 | 23,174,418,704.08 |

PaySim is clean simulated data, so nothing is quarantined on the real sample — the bad-rows test
below is what exercises the rules.

**Re-run safety.** The pipeline ran three times against the real sample (one manual notebook run,
then the Job twice). Bronze still holds exactly 70,000 rows (appending would have left 210,000),
and all three runs recorded identical check values in `audit_checks`:

| run_at (UTC) | Run | row_reconciliation | amount_reconciliation | count_reconciliation |
|---|---|---|---|---|
| 03:13:54 | manual notebooks | 70,000 = 70,000 ✅ | 23,174,418,704.08 = 23,174,418,704.08 ✅ | 70,000 = 70,000 ✅ |
| 03:22:55 | Job run 1 | 70,000 = 70,000 ✅ | 23,174,418,704.08 = 23,174,418,704.08 ✅ | 70,000 = 70,000 ✅ |
| 03:24:53 | Job run 2 | 70,000 = 70,000 ✅ | 23,174,418,704.08 = 23,174,418,704.08 ✅ | 70,000 = 70,000 ✅ |

`gold_summary_by_type`:

| type | transaction_count | total_amount | fraud_count | fraud_rate |
|---|---:|---:|---:|---:|
| CASH_IN | 13,414 | 2,269,418,700.73 | 0 | 0.0 |
| CASH_OUT | 25,853 | 9,776,140,714.55 | 4,116 | 0.1592 |
| DEBIT | 401 | 1,875,784.23 | 0 | 0.0 |
| PAYMENT | 21,126 | 275,818,807.51 | 0 | 0.0 |
| TRANSFER | 9,206 | 10,851,164,697.06 | 4,097 | 0.445 |

## Proving the rules catch bad data (separate test run)

[`test_data/bad_rows.csv`](test_data/bad_rows.csv) is a 10-row file of deliberately broken
records. Each row's `nameOrig` says what is wrong with it (`C_TEST_GOOD_*` rows are valid).
It runs through the **same Job** with **Run now with different parameters**, writing to a
separate schema so it never touches the real tables:

| Key | Value |
|---|---|
| `schema` | `paysim_test` |
| `input_path` | `/Volumes/workspace/paysim/raw/bad_rows.csv` |

Result: 10 rows in → 3 silver + 7 quarantine, and all three reconciliation checks passed
(10 = 3 + 7; silver amount 2,900.50 = gold amount 2,900.50; 3 = 3).

| nameOrig | failed_rules |
|---|---|
| C_TEST_BAD_ISFRAUD_2 | isFraud_0_or_1 |
| C_TEST_BAD_MISSING_AMOUNT | amount_not_null |
| C_TEST_BAD_MISSING_NAMEDEST | nameDest_not_null |
| C_TEST_BAD_MISSING_STEP_AND_TYPE | step_not_null, type_not_null |
| C_TEST_BAD_NEGATIVE_AMOUNT | amount_non_negative |
| C_TEST_BAD_THREE_RULES | amount_non_negative, type_valid, isFraud_0_or_1 |
| C_TEST_BAD_TYPE_WIRE | type_valid |

## Screenshots

| | |
|---|---|
| Successful Job run (4 tasks) | [job_run_graph.png](screenshots/job_run_graph.png) |
| Both Job runs succeeded | [job_runs_twice.png](screenshots/job_runs_twice.png) |
| Row counts / totals per layer after re-runs | [layer_counts_after_reruns.png](screenshots/layer_counts_after_reruns.png) |
| `audit_checks` — 3 runs × 3 checks, all passed | [audit_checks.png](screenshots/audit_checks.png) |
| Test run: quarantine with `failed_rules` | [test_quarantine.webp](screenshots/test_quarantine.webp) |
| Test run: `audit_checks` | [test_audit_checks.png](screenshots/test_audit_checks.png) |

## Notes and limitations

- The first test run failed in `01_ingest` with `Path must be absolute`: a tab character had been
  pasted in front of `input_path` in the parameter dialog. Retyping the value fixed it — worth
  knowing when parameters are copy-pasted.
- The CSV is read in Spark's default permissive mode: a value that cannot be parsed into its
  column type (e.g. text in `amount`) becomes null and is quarantined by the matching
  `*_not_null` rule rather than failing the load.
- `audit_checks` keeps growing by three rows per run; it is a log, not a snapshot.
