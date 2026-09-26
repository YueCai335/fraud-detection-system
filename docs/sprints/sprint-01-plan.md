# Sprint 1 — real-MySQL verification of the async batch worker

**2026-09-26 → 2026-10-09** (two weeks) · solo · board: GitHub Project *fraud-detection-system*

## Sprint Goal

Verify MySQL 8.4 transaction behaviour for idempotent batch submission, concurrent job claiming,
and checkpoint recovery — the parts of the async batch pipeline whose correctness depends on the
database, not on application logic.

## Why this, and not the other backlog items

The 48 JUnit tests run against H2 with the real Flyway migrations, and `scripts/smoke.sh` already
exercises a real MySQL 8.4 end to end on both GitHub Actions and Jenkins. So this is not "the
database has never been tested" — it is narrower and more specific:

- `BatchJobWorker` claims jobs with `@Lock(PESSIMISTIC_WRITE)` + a `-2` lock timeout, which
  Hibernate turns into `SELECT … FOR UPDATE SKIP LOCKED`. H2 accepts the statement; whether it
  behaves like MySQL under contention is not something an H2 test can answer.
- The smoke test is single-threaded, so **no test anywhere covers two workers racing for the same
  job** — the exact scenario `SKIP LOCKED` exists for.

Redis, MongoDB and SQS stay in the backlog on purpose. Each adds a service to deploy, test and
maintain, and there is no evidence yet that any of them solves a problem this system currently has.
Producing that evidence — for example, what polling `/batch/jobs/{id}/status` every three seconds
actually costs the database — is its own piece of work, and is not part of this sprint.

## Capacity

Two weeks with large uninterrupted blocks available (confirmed 2026-09-26). Capacity is not the
binding constraint this sprint; scope is deliberately kept at two items anyway, because the value
is in what the tests find, not in how many cards reach Done.

## Sprint backlog

### 1. A MySQL test harness with Testcontainers

**Problem** — integration tests run on H2. It is a different database with different locking,
different type coercion and its own MySQL-compatibility mode; a test passing there does not prove
the same code passes on the database the app actually ships with.

**Value** — the DB-dependent tests start proving something about production. The harness is also
what makes item 2 possible at all.

**Acceptance criteria**
1. A reusable base class (or Spring test slice) starts a MySQL container matching the compose
   version (8.4) and runs the real Flyway migrations against it.
2. The container is started once per test run, not once per test class.
3. The existing fast H2 tests still run and still pass — this adds a tier, it does not replace one.
4. `./mvnw verify` runs both tiers locally and in CI (GitHub Actions and the Jenkins stage) with no
   manual setup beyond a working Docker daemon.
5. CI time stays under control; if the MySQL tier adds more than ~2 minutes, say so in the review.

**Tasks** — (a) dependency + base class + one migration smoke test; (b) move the batch-job flow
tests that depend on DB behaviour onto it; (c) make it work inside the Jenkins Maven stage and
record timings for both pipelines.

Task (c) is not just "add it to the pipeline". That stage runs inside a `maven:3.9-eclipse-temurin-17`
container started by Jenkins, and the controller having a Docker socket says nothing about the
agent having one: the agent needs its own access to the daemon, the mapped MySQL port has to be
reachable from inside that container rather than from the host, and the containers Testcontainers
starts (including its reaper) have to be cleaned up when the stage ends. This is the same class of
problem as the `localhost` mix-up in build #1 — see [ci-jenkins.md](../ci-jenkins.md) — and
Testcontainers documents the patterns for running inside a container.

### 2. Three boundary behaviours verified on real MySQL

**Problem** — these three behaviours have different levels of cover today, and none of it is on
MySQL. Idempotent resubmit and crash recovery have H2 tests, but only for the serial path: the
`DataIntegrityViolationException` branch in `BatchJobService.submit` re-queries **inside the same
transaction**, which is exactly the kind of thing a database gets to decide. Concurrent claim has
no test at all — the smoke test is single-threaded — even though `SKIP LOCKED` exists for it.

**Value** — these are the failure modes an interviewer asks about, and the ones that actually bite
in production. Finding a difference is a result; finding none is also a result, and a defensible
sentence to say out loud.

**Acceptance criteria**

Every test below runs against a real MySQL with real connections and real transactions. The model
call may be stubbed — isolating it does not weaken what is being verified about the database.

1. **Idempotency, serial and concurrent** — two submits with the same explicit `Idempotency-Key`,
   and separately two submits of the same file with no key (the `sha256:` path), both return the
   same job id and leave exactly one `batch_jobs` row. The concurrent case must actually overlap,
   so that the losing submit hits `uk_batch_jobs_user_key` and takes the
   `DataIntegrityViolationException` branch — a serial resubmit returns early from the initial
   lookup and never exercises it. That branch queries inside the failed transaction; if MySQL
   refuses, this criterion is met by the test proving it, and the fix becomes its own card.
2. **Concurrent claim, two scenarios** — driven by a synchronisation barrier with a timeout, not by
   sleeps, so the transactions are known to overlap:
   - one claimable job, two claimers: exactly one wins, the other gets nothing;
   - two claimable jobs, claimer A holding its row lock in an open transaction: claimer B skips A
     and claims B instead of blocking — the property `SKIP LOCKED` is there to provide.
3. **Resume without rescoring** — after an interrupted run, the retry scores only the rows that were
   not committed. Asserted on **what the stubbed model actually received**, plus the final
   prediction count; `UNIQUE(batch_id, csv_row)` prevents duplicate rows but cannot prove that a
   row was not scored twice.
4. **Stale reclaim** — with the original worker stopped, a RUNNING job whose heartbeat is older than
   the cutoff becomes claimable again, and a repeated scan does not disturb a job that is still
   progressing. Out of scope: `reclaimStale` currently takes no lock, so the behaviour of two
   reclaimers racing is undefined by design — if a test shows a real race, it goes to the backlog
   as its own item rather than being fixed inside this card.
5. Any behavioural difference found between H2 and MySQL is written down in the review, with the
   fix or the explicit decision not to fix.

**Tasks** — (a) concurrent + serial idempotency tests; (b) the two claim-contention tests;
(c) resume-without-rescoring test; (d) stale reclaim test; (e) write up what differed.

## Definition of Done

A card is Done only when all of these hold:

- its acceptance criteria are met — a criterion that fails is not Done because it was written up;
- tests pass locally and **both** CI pipelines are green **on the same commit**;
- it shipped as its own branch and pull request, and the PR checks passed;
- documentation touched by the change is updated;
- it can be demonstrated — a command someone else can run, or log output pasted into the PR.

"The code is written" is not Done. A defect the tests uncover does not block the card that found
it: record it in the review and file it as its own backlog item, with the card's own criteria still
having to hold.

## Board

Columns: `Backlog` → `Sprint` → `In Progress` → `In Review` → `Done`. Cards move when the state
actually changes, including staying put: a card sitting in In Progress for three days is fine if
the reason is recorded.

## Review and retrospective

At the end of the sprint, `docs/sprints/sprint-01-review-retro.md` records two separate things:

- **Review** (the product): what was delivered against each acceptance criterion, what the tests
  found, what was demonstrated and to whom. Self-review, AI review and human review are labelled as
  what they are.
- **Retro** (the process): where estimates and reality diverged and why, plus **one** concrete
  change to try in sprint 2.
