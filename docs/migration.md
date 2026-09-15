# Migrating the middle tier from JAX-WS SOAP to Spring Boot REST

The original course project (commit `b8b6c98`, "Initial project upload") had three tiers:

```
fd-web  (JSP + Servlets + raw JDBC, WildFly)
   │  SOAP (JAX-WS, wsimport-generated client)
   ▼
fd-soap (JAX-WS @WebService, HttpURLConnection to Flask, WildFly)
   │  HTTP/JSON
   ▼
flask_api (Flask + scikit-learn + SHAP)
```

This document records what was replaced, why, and how the behaviour was kept the same.

## Why migrate

| Problem in the legacy system | What it caused |
|---|---|
| Two WARs on WildFly (`fd-web`, `fd-soap`) plus XAMPP MySQL, all configured by hand | Nobody could start the system from a clean machine; no reproducible environment |
| SOAP between two modules that lived on the same server | XML/WSDL overhead and a generated client checked in as `.class` files, for a call that is really just JSON in/JSON out |
| `HttpURLConnection` with no timeouts; errors returned as `fraud = -1` | A slow model service hung the request thread; the UI showed "-1" instead of an error |
| Batch = one SOAP call per CSV row, results written to a temp file tracked in the HTTP session | O(n) network round-trips; downloads broke on restart; no record of what was scored |
| Login in a servlet comparing SHA-256 hex against a hand-made `user` table, no CSRF protection | Weak password storage; every servlet re-implemented the session check |
| No tests, no build pipeline | Regressions only found by clicking through the UI |

## What changed

| Legacy | Now | Notes |
|---|---|---|
| `fd-soap` JAX-WS `FraudService.predict()` | `ModelServiceClient` (Spring `RestClient`) | Connect/read timeouts; every failure becomes `ModelServiceException` → HTTP 503 with a problem detail |
| `fd-web` servlets (`PredictServlet`, `BatchServlet`, `LoginServlet`, …) | Spring MVC controllers in `web/` + REST controllers in `api/` | Same URLs for the pages (`/login`, `/predict`, `/batch`, …) |
| JSP `session.getAttribute("username")` checks in every page and servlet | Spring Security filter chain | Form login for pages, HTTP Basic for `/api/**`; CSRF tokens on all forms |
| `DBUtil` + `PreparedStatement` + SHA-256 hex | Spring Data JPA `UserRepository` + `DelegatingPasswordEncoder` (`{bcrypt}`) | Registration validated with Bean Validation |
| Hand-created XAMPP `project.user` table | Flyway `V1__init.sql`, `V2__seed_demo_user.sql` | Hibernate runs in `validate` mode, so the entity model and the schema cannot drift |
| Batch: per-row SOAP call, temp file per upload, token in session | One chunked call to `/batch_predict`; rows persisted with a `batch_id`; CSV regenerated from the DB on download | Also exposed as `POST /api/v1/predictions/batch` |
| No API documentation | springdoc-openapi (`/swagger-ui.html`, `/v3/api-docs`) | |
| No tests | 8 pytest contract tests, 27 JUnit tests (Mockito, `MockRestServiceServer`, MockMvc on H2 + Flyway), compose smoke test in CI | |
| Manual WildFly deploys | Dockerfiles + `docker compose up`, GitHub Actions | |

Deliberately **not** changed:

- `model-service/app.py` scoring logic and the pickled model — the point of the migration was the
  Java tier. The Flask code was only wrapped in an app factory, given `/health`, input validation and
  environment-based configuration.
- The JSP pages. They were moved under `WEB-INF/views` and edited only where the servlet contract
  changed (CSRF hidden fields, the result object, `<c:out>` for output escaping).

## How "same results" was verified

1. **Feature derivation is identical.** The legacy `FraudSoapClient` computed
   `balanceDiffOrg = oldbalanceOrg − newbalanceOrig` and `balanceDiffDest = newbalanceDest − oldbalanceDest`
   before calling SOAP. `ModelFeatures.of(...)` does exactly that and `ModelFeaturesTest` pins it.
2. **The wire format to the model is unchanged.** `ModelServiceClientTest` asserts the JSON body
   sent to `/predict` (`type_code`, both diffs) with `MockRestServiceServer`.
3. **Shared examples across the language boundary.** `model-service/tests/test_api.py` and
   `fraud-service/src/test/java/.../TestFixtures.java` use the same two transactions (a PaySim
   `PAYMENT` scored 0.13, a `CASH_OUT` scored 0.39) so both sides assert the same contract.
4. **Batch equals single.** `test_batch_matches_single` in the Python tests guarantees the batch
   endpoint returns the same numbers as the single endpoint — which is what lets the Java side
   switch from per-row calls to one batch call without changing results.
5. **End-to-end smoke test in CI.** The compose job scores the sample CSV through the real stack
   and checks the totals (`8` rows, `4` flagged).

## What a further iteration could add

- Extract the ~100 lines of sidebar CSS duplicated in each JSP into `theme.css` (or move the pages
  to Thymeleaf).
- Retry/circuit breaker on the model call (Resilience4j) and a request-id passed through to the
  model service for log correlation.
- Deploy: the Spring Boot WAR runs as-is on ECS/Elastic Beanstalk with RDS MySQL; the model service
  fits ECS or Lambda + container image.
