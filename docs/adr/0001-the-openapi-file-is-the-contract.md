---
status: accepted
date: 2026-11-13
decision-makers: Alex Negrila
---

# The OpenAPI file is the contract, and the build fails when the code drifts from it

## Context and Problem Statement

An AI coding agent implements this API. Agents write plausible code quickly, and plausible code drifts: an extra field nobody asked for, a different status code, an endpoint nobody agreed. Review catches some of it, some of the time.

How do we make "the code matches the agreed contract" something the build checks on every change, instead of something a reviewer hopes to notice?

## Decision Drivers

* Drift fails the build on every push and pull request, with a message that names the mismatch.
* No extra test code per endpoint: the tests we write anyway must be enough.
* One command, `./mvnw verify`, with no running server, broker or extra service.
* The contract is readable by people and by agents, and lives in the repository.

## Considered Options

* Contract-first: generate the Spring interface from `api/openapi.yaml`, and validate every MockMvc exchange in the tests against the same file.
* Code-first: generate the spec from the code (springdoc) and diff it against the committed file in CI (oasdiff).
* Specmatic: contract tests generated from the spec, run against the running application.
* Schemathesis: property-based fuzzing of the running application from the spec.
* Consumer-driven contracts with Pact.

## Decision Outcome

Chosen option: **contract-first**, because it meets every driver with two well-maintained libraries (openapi-generator and Atlassian's openapi-request-validator), runs inside `./mvnw verify`, and turns every existing test into a contract test.

How it works:

1. `openapi-generator` generates `TimesheetsApi` and its models from the spec at build time (`interfaceOnly`). The controller implements that interface, so a drift in paths, parameters or types does not compile.
2. One test configuration, `ContractValidation`, attaches the validator to every MockMvc request the Spring Boot tests make. Any response field, status code, header or content type the spec does not document fails the test that produced it. So does a request to a path or method the spec does not have.
3. `ContractGuardTest` proves the validator still rejects an undocumented field, so the check cannot be weakened by accident.

### Consequences

* Good, because signature drift fails the compile and behaviour drift fails a test, both with a message that names the mismatch.
* Good, because the spec stays the single source of truth; generated code is never edited or committed.
* Bad, because only exchanges that tests exercise are checked. An operation with no test is not checked. Mitigation for now: review checks that every operation in the spec has at least one test.
* Bad, because request-body and parameter validation are switched off in tests, so negative tests can send invalid input on purpose. Request shape is enforced in the application instead, by the generated models and Bean Validation.
* Neutral, because breaking changes to the spec itself are not detected yet. The planned second check is `oasdiff breaking` against `main`, with a breaking change allowed only alongside an ADR.

### Confirmation

* `.github/workflows/contract.yml` runs `./mvnw verify` on every push and pull request.
* `ContractGuardTest` fails if the validator stops rejecting an undocumented field.
* `docs/drift/` holds two patches that make the build fail on purpose: an undocumented response field, and an undocumented status code.

## Pros and Cons of the Options

### Contract-first: generated interface plus validation of every test exchange

* Good, because it needs no running server and no extra tool.
* Good, because the agent cannot change the contract by changing code.
* Bad, because the generator adds a build step and generated models to learn.

### Code-first: springdoc plus oasdiff

* Good, because there is no code generation.
* Bad, because the spec becomes an output of the code, so an agent can change the contract by changing code, and drift shows up as a diff to review instead of a failure.

### Specmatic

* Good, because it also gives consumers stubs generated from the same spec.
* Bad, because it needs the application running and another tool in the loop.

### Schemathesis

* Good, because it finds edge cases nobody thought to test.
* Bad, because it needs a running server and a Python tool, and it is slower. It fits better as a nightly job than as the drift gate.

### Pact

* Good, because it suits APIs with many consumers that evolve separately.
* Bad, because it needs consumer tests and a broker, and this API has no consumers yet.

## More Information

* Birgitta Böckeler, [Understanding Spec-Driven-Development: Kiro, spec-kit, and Tessl](https://www.martinfowler.com/articles/exploring-gen-ai/sdd-3-tools.html): this repository is "spec-anchored", with the anchor enforced by CI.
* [openapi-generator](https://github.com/OpenAPITools/openapi-generator), [openapi-request-validator](https://bitbucket.org/atlassian/swagger-request-validator), [oasdiff](https://github.com/oasdiff/oasdiff).
* Template: [MADR 4](https://adr.github.io/madr/).
