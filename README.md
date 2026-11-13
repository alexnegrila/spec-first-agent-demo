# Give your AI agent a contract, not a wish

[![contract](https://github.com/alexnegrila/spec-first-agent-demo/actions/workflows/contract.yml/badge.svg)](https://github.com/alexnegrila/spec-first-agent-demo/actions/workflows/contract.yml)

A small, runnable version of how I work with AI coding agents: **write the contract first, let the agent build against it, and make the build fail when the code drifts from it.**

An agent writes plausible code fast. Plausible is not the same as agreed. One extra field here, a different status code there, and the API your consumers rely on has quietly changed. Review catches some of that, some of the time. A build that fails catches it every time.

## The contract, before any code

| File | What it is |
|---|---|
| [`api/openapi.yaml`](api/openapi.yaml) | The API: a two-operation Timesheet API, every schema closed with `additionalProperties: false`. |
| [`docs/adr/0001-the-openapi-file-is-the-contract.md`](docs/adr/0001-the-openapi-file-is-the-contract.md) | The decision: why this file is the contract, how drift is caught, and the options that were not chosen. |
| [`AGENTS.md`](AGENTS.md) | The rules the agent works under: spec first, never edit the spec to make a test pass, run `./mvnw verify` before handing back. |

## How drift fails the build

1. **The interface is generated from the contract.** [openapi-generator](https://github.com/OpenAPITools/openapi-generator) turns `api/openapi.yaml` into a Spring interface, `TimesheetsApi`, on every build. The controller implements it, so a path, parameter or type that drifts from the spec does not compile.
2. **Every test exchange is checked against the contract.** One test configuration, [`ContractValidation`](src/test/java/uk/co/lexcoders/demo/timesheets/ContractValidation.java), attaches Atlassian's [openapi-request-validator](https://bitbucket.org/atlassian/swagger-request-validator) to every MockMvc request. The behaviour tests need no extra code: a response field, status code, header or content type the spec does not document fails the test that produced it.
3. **A guard test proves the check still bites.** [`ContractGuardTest`](src/test/java/uk/co/lexcoders/demo/timesheets/ContractGuardTest.java) fails if anyone loosens the spec or the validator until an undocumented field or status code gets through.

[CI](.github/workflows/contract.yml) runs `./mvnw verify` on every push and pull request.

## Show me the run

Two patches in [`docs/drift/`](docs/drift/) make the kind of change an agent makes when nobody gave it a contract:

* [`01-undocumented-response-field.patch`](docs/drift/01-undocumented-response-field.patch): "show the total hours too". The agent cannot change the generated interface, so it subclasses the model and adds `totalHours`. It compiles.
* [`02-undocumented-status-code.patch`](docs/drift/02-undocumented-status-code.patch): `POST /timesheets` answers `200` instead of the documented `201`. It compiles.

Both builds fail, each with a message that names the mismatch:

```text
$ git apply docs/drift/01-undocumented-response-field.patch && ./mvnw verify
[ERROR]   TimesheetApiTest.readsATimesheetBack:51 » OpenApiValidation
    "key" : "validation.response.body.schema.additionalProperties",
    "message" : "property 'totalHours' is not defined in the schema and the schema does not allow additional properties",
[INFO] BUILD FAILURE

$ git apply docs/drift/02-undocumented-status-code.patch && ./mvnw verify
[ERROR]   TimesheetApiTest.createsADraftTimesheet:39 » OpenApiValidation
[ERROR]   TimesheetApiTest.readsATimesheetBack:48 » OpenApiValidation
    "key" : "validation.response.status.unknown",
    "message" : "Response status 200 not defined for path '/timesheets'.",
[INFO] BUILD FAILURE
```

`readsATimesheetBack` never asserts the status of the request that creates its timesheet. It fails anyway, because every exchange is checked.

Each patch is also an open pull request, with its runs on GitHub:

* [#1, the undocumented field](https://github.com/alexnegrila/spec-first-agent-demo/pull/1): the drift commit [fails](https://github.com/alexnegrila/spec-first-agent-demo/actions/runs/43466089775). The fix then goes through the contract first: [one commit changes only `api/openapi.yaml`](https://github.com/alexnegrila/spec-first-agent-demo/pull/1/commits/57ab9b4e2d6dce0cc80bcbd2ebda8d827d93bccb), [the next makes the code follow](https://github.com/alexnegrila/spec-first-agent-demo/pull/1/commits/a96a0faee8dfed278c462a1abe7e84e189d480d8), and that run [passes](https://github.com/alexnegrila/spec-first-agent-demo/actions/runs/43466365335).
* [#2, the undocumented status code](https://github.com/alexnegrila/spec-first-agent-demo/pull/2): [fails](https://github.com/alexnegrila/spec-first-agent-demo/actions/runs/43466098911). Here the contract is right and the code is wrong, so the fix belongs in the code.

GitHub deletes run logs after 90 days. The output above shows the same failures.

Try it yourself:

```bash
git apply docs/drift/01-undocumented-response-field.patch
./mvnw verify          # fails: the response has a field the contract does not document
git apply -R docs/drift/01-undocumented-response-field.patch
```

The fix is spec first, as in [#1](https://github.com/alexnegrila/spec-first-agent-demo/pull/1): add `totalHours` to the `Timesheet` schema in `api/openapi.yaml`, rebuild, then return it from the controller.

## Run it

Needs Java 21.

```bash
./mvnw verify
./mvnw spring-boot:run
```

```bash
curl -i -X POST localhost:8080/timesheets \
  -H 'Content-Type: application/json' \
  -d '{"weekStarting": "2026-11-09", "entries": [{"date": "2026-11-09", "hours": 7.5}]}'
```

## What this is not

It is not a framework, and it does not replace consumer-driven contracts (Pact), fuzzing (Schemathesis) or contract stubs (Specmatic) in a larger system. ADR 0001 says why this repository does not need them yet. The check covers what the tests exercise: an operation with no test is not checked.

## How this repository was made

The contract, the ADR and `AGENTS.md` came first. The implementation and the tests were then written by an AI coding agent (Claude Code) working from them, and reviewed by me. I have used the same pattern on client work: an OpenAPI-first template service with contract tests inside the integration tests (2023), and contract tests over every existing integration test on a brownfield platform (2025–26).

## Further reading

* Birgitta Böckeler, [Understanding Spec-Driven-Development: Kiro, spec-kit, and Tessl](https://www.martinfowler.com/articles/exploring-gen-ai/sdd-3-tools.html). This repository is "spec-anchored", and CI enforces the anchor.
* [GitHub Spec Kit](https://github.com/github/spec-kit) and [OpenSpec](https://github.com/Fission-AI/OpenSpec): spec-driven workflows for coding agents.
* [antonarhipov/sdd-demo](https://github.com/antonarhipov/sdd-demo): a Java spec-driven development walk-through, from request to tested code.
* Pooya Golchian, [How to write specs for AI agents](https://pooyagolchian.com/blog/how-to-write-specs-for-ai-agents-2026/), makes the same point: "Users can log in" is a wish.
* [AGENTS.md](https://agents.md), the open format for agent instructions; [MADR](https://adr.github.io/madr/), the ADR template.

## Licence

[MIT](LICENSE). By Alexandrian (Alex) Negrila, [Lex Coders Ltd](https://lexcoders.co.uk) · [LinkedIn](https://www.linkedin.com/in/alexandrian-negrila-71a49213/).
