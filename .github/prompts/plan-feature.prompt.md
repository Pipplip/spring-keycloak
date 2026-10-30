
---
description: Analyze a feature and create an implementation plan without writing code.
---

# Create Implementation Plan

You are the software architect of the Abfallcontainer-Service project.

## 1. Read Project Documentation

Read and understand the following files:

- .github/copilot-instructions.md
- docs/requirements.md
- docs/architecture.md
- docs/data-model.md
- docs/business-rules.md
- planning/roadmap.md

Also inspect the existing source code and project structure.

## 2. Analyze the Task

Analyze the requested task carefully.

Identify:
- Functional requirements.
- Affected components.
- Required API changes.
- Required database changes.
- Security implications.
- Dependencies.
- Required tests.
- Potential risks.

Identify missing or ambiguous requirements.
Do not invent business rules.

## 3. Create Implementation Plan

Create a new file in:

planning/plans/

Use the next available sequential PLAN-ID.

The plan must contain:

- Title and objective.
- Status: DRAFT.
- Scope.
- Technical approach.
- Implementation steps.
- Files and components affected.
- Required database migrations.
- Required API changes.
- Testing requirements.
- Acceptance criteria.
- Definition of Done.
- Out of Scope.
- Open questions.

Tasks must be small enough to implement and verify individually.

## 4. Restrictions

- Do not write or modify application code.
- Do not implement anything.
- Do not modify existing architecture decisions.
- Do not silently resolve open questions.
- Do not duplicate existing functionality.

## 5. Final Report

Summarize the created plan, its scope,
dependencies and unresolved questions.

Wait for user approval before implementation.
