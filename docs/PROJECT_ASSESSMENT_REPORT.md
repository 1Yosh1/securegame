# SecureGame — Software Engineering and Requirements Review

**Reviewed:** 7 October 2026
**Basis:** The course material PDFs under `SW/`, the checked-out source, tests, and build configuration. Course slides establish software-engineering principles and topics, but are not a project acceptance specification; requirements marked “not evidenced” below must be confirmed against the professor's actual rubric. This is a source review, not a security certification or a grading decision.

## Executive conclusion

SecureGame is a working Spring Boot/Java training-game prototype with meaningful domain code, tests, persistence abstractions, and a Vaadin Flow landing route alongside its static game UI. It demonstrates several course concepts in real implementation, particularly interface-based polymorphism, dependency injection, composition, encapsulation, and separation of concerns. It is **not possible to claim that every course or grading requirement is met** from slides alone: course lecture files explain practices and topics, not project acceptance criteria. Core engineering elements are present, but important quality, documentation, security, and production-readiness gaps remain.

A notable review blocker discovered during verification was that recent workspace edits changed the controller API/tests but left tests uncompilable. I fixed the stale reset-password test call and strengthened focused regression tests. This review also corrected misleading design comments, made anomaly-signal registration genuinely extensible, hardened profile ownership and score bounds, and added authenticated boundaries to selected APIs. Classroom REST and STOMP now share one typed room-session service, enforce teacher/member ownership, and use per-user queues guarded by an inbound subscription interceptor. All 97 tests pass across 17 suites; full run and package build used Java 27, not the declared Java 21 baseline.

## What the course materials emphasize and how the project compares

| Course material theme | Current evidence | Assessment |
|---|---|---|
| Requirements analysis and validation (`6 - Requirement Engineering.pdf`) | The repository includes a useful assessment/trace report, but there is no confirmed signed-off requirements baseline or acceptance-to-test mapping located. | **Partial / needs owner confirmation.** A set of course lecture PDFs is not itself the project's detailed contract. |
| Software qualities: correctness, reliability, maintainability (`2 - Sw Qualities.pdf`) | Separate controllers/services/repositories, regression tests, input bounds in several endpoints, DTO allowlists, and H2-backed integration tests. | **Good foundation, incomplete evidence.** No measured availability, live database test, or complete API/error contract coverage. |
| SE principles: separation, modularity, abstraction, incremental change, high cohesion/low coupling (`3 - SE-Principles.pdf`) | MVC/service/repository layers, focused scoring strategies, immutable records, constructor injection. The anomaly combiner now accepts an injected list of signal abstractions instead of hard-coding five constructor arguments. | **Mostly demonstrated in the backend.** Some controllers still contain multiple responsibilities; mutable string/object maps and in-memory state increase coupling. |
| UML and system modeling (`7 - modeling.pdf`; course introduction) | Two existing source-aligned review reports include architecture and current-state algorithm diagrams. | **Partial.** Diagrams document selected flows, not a comprehensive validated use-case/class/sequence/activity model. |
| Testing, regression, user validation (`10 - software testing.pdf`) | JUnit unit/integration tests; added boundary checks/regression coverage. | **Good automated baseline, partial validation.** Browser playthrough, SMTP provider, and production MariaDB were not run in this review. |
| Incremental/agile development and feedback (`5 - Agile development.pdf`; `4 - SwProc.pdf`) | The source evolves by features and has a test suite. | **Process evidence not inferable.** No trustworthy dated backlog history, burndowns, velocity, or stakeholder acceptance records were verified. Do not invent them. |
| Architecture, reuse, components (`9 - sw architecture.pdf`; `10 - reusecomponents.pdf`) | Spring Boot, Spring Data JPA, Vaadin, TOTP, mail, WebSocket, and framework-provided repositories are reused behind boundaries. | **Demonstrated.** Dependency versions, generated frontend artifact churn, and framework/build warnings deserve regular maintenance. |
| Evolution and regression (`11 - sw evolmaintenance.pdf`) | Repeatable Maven tests and encapsulated interfaces support change. | **Partial.** Add CI on declared Java 21, migrations, and a written dependency/update policy. |

## OOP review

- **Encapsulation — present.** Entities keep fields private and expose methods. `OtpChallenge` encapsulates OTP salt/digest/expiry; response DTOs separate public player data from the stored TOTP secret. Entities remain mutable via public setters, which is normal for JPA but permits invalid transient states. Prefer validated command methods for important domain transitions.
- **Abstraction — present.** `AnomalySignal`, Spring Data repository interfaces, and the TOTP service boundary hide implementation details. Callers can use those contracts without knowing concrete scoring or persistence internals.
- **Polymorphism — present and improved.** `AnomalyFusionEngine` processes a `List<AnomalySignal>`, so each strategy supplies its own score/name/weight implementation. New signal beans can join through DI without changing the fusion constructor.
- **Inheritance — limited, accurately so.** Repositories extend Spring Data's `JpaRepository`, and configuration adapts a framework extension point; the application has no meaningful project-owned concrete class hierarchy. Inheritance is not mandatory for good OOP. Interface realization, composition, and delegation are better demonstrated in this code than class subclassing.
- **Composition/dependency injection — strong.** Engines/controllers receive focused collaborators via constructors and compose services, repositories, and strategies.
- **Immutability/value modeling — present in records.** Records make requests, assessments, and context data concise; arrays/maps inside records are still mutable unless defensively copied.
- **SOLID/design-quality caveats.** Some REST controllers perform authorization, validation, business work, serialization, and persistence coordination together. Large mixed `Map<String,Object>` contracts and static in-memory room/token state hinder cohesion, concurrency, and testability. Authentication controller is especially responsibility-heavy and should eventually delegate password, challenge/session, and account flows to dedicated services.

## Requirement/feature coverage visible in source

These statuses describe implementation, not whether a professor has formally accepted scope changes.

| Area | Status | Evidence and caveat |
|---|---|---|
| Java/Spring application and build | **Implemented** | Java 21 target, Spring Boot 3.3.4, Maven wrapper. Verification ran under Java 27; CI must confirm the declared Java 21 baseline. |
| Vaadin and interactive UI | **Partial/implemented** | Vaadin dependencies and `DashboardView` provide a Flow route; game/campaign remain static HTML/CSS/JS. This is a mixed UI, not an entirely Vaadin game. |
| Persistence | **Partial** | JPA repositories; H2 tests/local, MariaDB production configuration. No fresh MariaDB/Compose startup was done in this review. Production uses `ddl-auto=update`, which should be replaced by migrations for controlled evolution. |
| Authentication scenario policy CRUD | **Implemented prototype** | `Scenario` has `@Version` and a soft-delete flag; list/get/update/delete filter deleted records; request policies are range-checked and writes require a teacher role or configured admin key. The discrete-event/compiler requirements below are separate and remain absent. |
| Simulation and event history | **Partial** | Synchronous lockout/password-entropy/MFA state evaluation with persisted event logs; event JSON and bounded CSV export exist. It does not implement a priority-queue discrete-event scheduler, replayable random seed, or compiled policy graph. Character-frequency Shannon entropy is a classroom heuristic, not production password guessability. |
| Hybrid risk assessment | **Implemented prototype** | Hand-authored rule layer plus pure-Java logistic regression; bounded feature/training inputs; deterministic synthetic generator, explanations, coach tips. Synthetic classes are easy to separate; held-out quality metrics and calibration are not established. |
| Anomaly scoring | **Implemented prototype** | Five strategy implementations and normalized weighted-average fusion; invalid signal score/weight rejected; behavior unit-tested. No verified GeoIP enrichment or persisted user-history pipeline; engine is not shown wired into normal login path. It is a weighted average, not majority voting. |
| Phishing detector and exercises | **Partial** | Explainable heuristic URL analyzer, stored phishing-template JPA model/repository, seeded exercise content. No 500-item evaluation benchmark, reported precision/recall, robust canonical-brand-distance matching, full message/body analysis, or semantic urgency model. |
| Multiplayer/classrooms | **Partial** | REST/WebSocket/STOMP share a typed in-memory room session; teacher ownership and member status access are checked; identities are omitted from public room views. Topic events go to private per-user queues, with authenticated member-only subscription checks. State remains ephemeral/process-local; expiry, durable/distributed storage and real-browser handshake integration still need verification. |
| TOTP and WebAuthn | **TOTP partial; WebAuthn missing** | TOTP primitive exists and secrets are encrypted at rest via AES-GCM when a configured key is present; production startup requires the key. No WebAuthn/FIDO2 enrollment/assertion implementation found. Local default tests do not exercise encrypted persistence. |
| GeoIP | **Missing** | Haversine calculation consumes coordinates; no IP lookup provider/adapter is implemented. |
| TF-IDF / text analysis | **Missing** | No TF-IDF utility was found in maintained application source. |
| Scenario JSON compiler | **Missing** | Quiz-content JSON generation is not validation/compilation of an instructor's policy/event graph. |
| Agile/UML deliverables | **Not evidenced / partial** | Reports contain explanatory current-state diagrams and traceability, but no verified sprint history/metrics or complete professor-approved UML package. |

## Security and quality risks that remain

1. **Account authentication still needs a focused security review.** Password hashes now use salted PBKDF2 and OTP challenges expire, hash their codes, and allow limited attempts. Sessions are established for successful OTP/device-token flows and CSRF remains enabled. OTPs/device tokens are still process-local; resend/overall login rate limits and multi-instance session handling are absent. Recovery challenge issuance timing/behavior deserves abuse testing.
2. **Review every route against actual identities and ownership.** Several sensitive routes now require a role, including scenarios, model training, profile listings, and event exports. Room create/join/status/topic routes and STOMP subscriptions enforce identity/room membership. Player profiles now redact secrets, constrain self-creation, and restrict detail reads to owner/teacher/admin; scores require teachers and have bounded deltas. Broader APIs still need a full object-level security matrix. Admin API key must be supplied securely and configured in production; never commit it.
3. **Persisted TOTP encryption key management.** Production requires `SECUREGAME_TOTP_ENCRYPTION_KEY`; backup, rotation, and recovery procedures matter because loss of the key makes secrets unreadable. Existing legacy secrets are migrated at application startup. Do not deploy until an operator supplies and backs up a strong independent key.
4. **Secrets/history.** Compose uses environment-variable placeholders now, but this review does not inspect or rewrite repository history or rotate old credentials. If real secrets ever appeared in commits, rotate them and assess clones/logs.
5. **Concurrency and fairness.** In-memory token/challenge/room stores are not clustered or durable. Room membership uses concurrent sets and room codes avoid active collisions, but the four-digit code space remains guessable and rooms lack expiry, quotas, and durable/multi-instance storage.
6. **Validation/error contracts.** Input validation is inconsistent, some endpoints expose raw entity/list structures, and some not-found paths throw generic runtime exceptions. Add request validation annotations/typed DTOs and consistent error responses.
7. **Frontend/infrastructure.** Maven emits a Vaadin plugin warning (`skip` parameter unknown); project dependencies include generated Vaadin assets. Run clean builds and browser smoke tests in CI. MariaDB/SMTP/Compose and accessible user workflows are unverified by this review.
8. **Testing.** Expand beyond the passing 97-test suite with broader API security-matrix tests (anonymous, student, teacher, admin, wrong owner), TOTP cipher round-trip/wrong-key/tamper tests, MariaDB migration tests, real browser tests for the SockJS/STOMP handshake and private queue delivery, and model held-out evaluation. Keep assertions meaningful.

## OOP concepts quick reference

| Concept | Concrete project example |
|---|---|
| Encapsulation | Private fields in `Scenario`/`PlayerProfile`; private `OtpChallenge`; response-only `PlayerView`. |
| Abstraction | `AnomalySignal`, `JpaRepository` contracts, TOTP service. |
| Polymorphism | Fusion loops over injected signal implementations through `AnomalySignal`. |
| Inheritance | Repository interfaces extend framework interfaces; few/no app-owned concrete superclass hierarchies. |
| Composition | `RiskEngine` combines rule/model/feedback collaborators; controllers compose services/repos. |
| Cohesion/separation | Controllers → services/engines → repositories; signal strategies isolate anomaly dimensions. Some controllers remain overloaded. |
| Immutability | Java records for features, contexts, DTOs and reports; referenced collections/arrays are shallowly mutable. |

## Prioritized next work

**Before real deployment:** finish route/ownership security testing, durable session/OTP storage and global rate limiting, verify key operations, rotate any historical credentials, and run full MariaDB + browser workflows.

**To meet a detailed academic algorithm rubric:** first obtain the actual signed-off requirements; then implement the real seeded event scheduler/compiler, any agreed WebAuthn/GeoIP/TF-IDF requirements, phishing benchmark and measured model reports.

**For engineering quality:** establish Java 21 CI, schema migrations, typed service/request models, richer security and browser regression tests, and record Agile metrics prospectively rather than reconstructing them.
