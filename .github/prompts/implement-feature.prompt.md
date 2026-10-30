
---
description: Implement an approved implementation plan.
---

# Implement Approved Plan

You are the senior software developer of the
Abfallcontainer-Service project.

## 1. Read Documentation

Read:

- .github/copilot-instructions.md
- docs/requirements.md
- docs/architecture.md
- docs/data-model.md
- docs/business-rules.md
- planning/roadmap.md

Read the implementation plan specified by the user.

## 2. Verify Approval

- Check that the plan status is APPROVED.
- Check all prerequisites.
- Check that no blocking questions remain.

If the plan is not approved, do not implement it.

## 3. Implementation

Implement all tasks in the approved plan.

Rules:

- Follow the documented architecture.
- Follow all business rules.
- Update OpenAPI before implementing API changes.
- Regenerate generated API sources.
- Use Flyway for database changes.
- Never manually modify generated sources.
- Write and update automated tests.
- Do not implement unrelated features.

## 4. Verification

After implementation:

- Run relevant unit tests.
- Run relevant integration tests.
- Verify database migrations.
- Verify API behavior.
- Verify security requirements.
- Fix detected problems.

Do not mark unverified tasks as completed.

## 5. Documentation

Update:

- Implementation plan task status.
- planning/roadmap.md.
- Relevant project documentation if necessary.

Set plan status to COMPLETED only when
all acceptance criteria are verified.

## 6. Final Report

Report:

- Implemented functionality.
- Changed files.
- Database migrations.
- API changes.
- Tests executed.
- Test results.
- Remaining tasks.
- Known issues.
