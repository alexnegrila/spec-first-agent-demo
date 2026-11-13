# AGENTS.md

Rules for any coding agent, and any person, changing this repository.

## The contract comes first

1. `api/openapi.yaml` is the contract. To change what the API accepts or returns, change the spec first, in the same pull request as the code.
2. Never edit the spec only to make a failing test pass. A red contract check means the code drifted from the contract: fix the code, or propose the spec change on its own and say why.
3. A change that can break existing clients needs an ADR in `docs/adr/`, in the MADR format of ADR 0001. That means removing or renaming a field, path or status code, or tightening a constraint.
4. Loosening the check is a contract change too. Do not add `additionalProperties: true`, drop `required` fields, or lower the validator's levels to get a green build.
5. Do not edit or commit generated code (`target/generated-sources/openapi`). The controller implements the generated `TimesheetsApi` interface; change the spec and rebuild.

## Before handing back

* Run `./mvnw verify`. It must pass.
* Put the command and its result line in the pull request description.

## Claims need runs

* A blocking review comment names the run that proves it: a failing test, a red build, a log line. No run, and it is a question.

## Layout

* `api/openapi.yaml`: the contract.
* `docs/adr/`: the decisions.
* `src/main/java/uk/co/lexcoders/demo/timesheets/`: the implementation of `TimesheetsApi`.
* `src/test/java/uk/co/lexcoders/demo/timesheets/ContractValidation.java`: checks every MockMvc exchange against the contract.
* `docs/drift/`: patches that make the build fail on purpose, to show the check working.

## Out of bounds

* No secrets, personal data or client data in code, tests or fixtures. Use synthetic data.
