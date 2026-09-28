---
name: tdd
description: "Use when implementing a feature or bugfix in this repo: drives test-first work, one small vertical behavior slice at a time, via Red -> Green -> Refactor."
---

#### TDD Skill

Use this skill whenever you are implementing a new feature or fixing a bug in this repository. It drives a strict Red -> Green -> Refactor cycle, one small vertical slice at a time.

Do NOT:
- write all tests upfront
- fully pre-implement a large test plan
- change multiple behaviors at once
- couple tests to internal implementation details

Instead, repeat this cycle for each behavior slice:

1. Identify the next small piece of desired behavior.
2. Write exactly one test for it.
3. Run it and confirm it fails for the expected reason (Red).
4. Write the smallest implementation that makes it pass (Green).
5. Re-run the tests.
6. Refactor if useful, keeping the suite green.
7. Pick the next behavior slice and repeat.

##### Understand the repository first

Before touching tests or production code, explore this repository's actual structure and conventions. Prior art in this repo always takes precedence over generic examples below.

- **Server (`customer-management-server/`)**: Java/Maven, JUnit 5. Look at `src/test/java/.../CustomerControllerTest.java` for the style of controller/API tests, `infrastructure/test/DatabaseTest.java` and `DatabaseExtension.java` for tests needing a real database (Testcontainers), and `infrastructure/test/WireMock.java`/`WireMockExtension.java` for stubbing external HTTP dependencies. `architecture/*ArchitectureTest.java` (ArchUnit) enforce structural rules — respect them, don't work around them.
- **Client (`customer-management-client/`)**: Playwright for end-to-end tests (`npm run test`/`test:dev`), Vitest + Testing Library for unit/component tests for parts that are **not** coverable by end-to-end tests (`tests/components/*.test.tsx`, run via `npm run test:unit`).
- Check for ADRs, a `CONTEXT.md`, or other architecture/domain documentation in the touched area and follow them.
- Read existing tests near the code you're changing before writing a new one; match their naming, setup, and assertion style.

##### Test philosophy

Test behavior, not implementation details. Prefer tests against stable public interfaces: public methods, HTTP endpoints/controllers, services, use cases, components. A good test should keep passing if the internals are refactored without a behavior change.

Good: `customer can be created with valid data`
Weaker: `CustomerService.create calls repository.save exactly once` (only justified if that exact interaction is itself part of the contract)

##### Vertical slices

Never write all tests for a feature before implementing anything. Instead, go through one full `test -> implementation -> green` cycle before starting the next one, and let each cycle inform the next.

##### Red phase

Before any production code change, a test must exist that describes the desired new behavior. Run it and confirm:
- it actually fails
- it fails for the expected reason
- it is not failing due to a syntax error, broken setup, or broken fixture

A test that passes immediately proves no new behavior. If that happens, check whether the behavior already exists, the test is wrong, or it doesn't reach the relevant code path.

##### Green phase

After a correctly red test, change only as much production code as needed to make it pass. No speculative design for hypothetical future requirements: avoid new APIs without current need, "just in case" abstraction, or generalization without a concrete test driving it. Afterwards, run the new test plus the relevant existing tests to confirm no regression.

##### Refactor phase

Only refactor with a green suite. Remove duplication, improve names, sharpen responsibilities, simplify interfaces, clean up test code — without changing behavior. Re-run tests after each relevant refactoring step.

##### Test level

Choose the lowest test level that reliably verifies the behavior through a stable public interface — don't dogmatically default to unit tests. Depending on context, a JUnit unit test, a Testcontainers-backed integration/database test, a WireMock-stubbed controller test, a Vitest component test, or a Playwright end-to-end test may be the right choice. If the behavior spans multiple real components, prefer one integration test over many heavily-mocked unit tests.

##### Mocking

Use mocks sparingly, primarily to replace real system boundaries: external APIs, the filesystem, time, randomness, network calls, third-party services (mirrors what `WireMockExtension` already does for HTTP in this repo). Don't reflexively mock internal classes or modules. If a test needs many internal mocks, check whether it's too coupled to implementation details, a better public interface exists, or an integration test would be simpler and more meaningful. Only assert on mock interactions when that interaction itself is part of the system's contract.

##### Bugfixes

Same cycle applies:
1. Reproduce the bug.
2. Write a reproducing test; run it and confirm it's red.
3. Implement the smallest possible fix.
4. Get the test green; run relevant regression tests.

Keep the reproducing test in the suite afterwards where possible.

##### Legacy / hard-to-test areas

Don't jump straight to a large architectural rewrite. Instead: understand the existing behavior, find the nearest stable seam, write a characterization test if needed, make the smallest safe refactoring step, then start the normal Red-Green cycle. Only do larger architectural changes if the current slice actually requires them.

##### Test quality

Avoid tests that: test private methods directly, check internal data structures without a business reason, exist only to raise coverage, test trivial getters/setters, assert exact internal call order without cause, reimplement production logic in the test, produce huge low-value snapshots, or need excessive mocking.

Aim for tests that are clearly named, describe exactly one relevant behavior, have minimal irrelevant setup, are deterministic, run independently of each other, and fail with an understandable message.

##### Running tests

Run the smallest relevant test scope first (a single test, class, or module), then the broader relevant scope before finishing, and the full suite when practical. Derive the test command from this repo's existing tooling instead of introducing new infrastructure:
- Server: Maven (e.g. `./mvnw test`, or scoped with `-Dtest=...`).
- Client: `npm run test:unit` (Vitest) for unit/component tests, `npm run test`/`test:dev` (Playwright) for end-to-end tests.

##### Working from a spec

If a local spec exists for the task (e.g. `docs/specs/<feature>.md`, see `.junie/commands/to-spec.md`), use it as the source of expected behavior. Decide independently which small vertical slices implement that spec. Do not modify the spec itself unless a discrepancy discovered during implementation makes an update clearly necessary — in that case, call out the discrepancy explicitly.

No ticket or issue tracker is required. This works equally from a plain user instruction, a local spec, a reported bug, or a concrete change request; any additional tickets are optional context, never a prerequisite.

##### Wrapping up

Summarize briefly: which behavior was implemented, which tests were added or changed, which Red-Green cycles mattered, which production files changed, which tests were run successfully, and any known limitations or open points. No detailed process chronicle — focus on the result and demonstrated test coverage.
