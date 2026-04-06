# CONTRIBUTING.md

## Overview

Thank you for contributing!   
This project is maintained with a focus on consistency across multiple related repositories.

---

## Branch Strategy

* `main` and `develop` branches are managed directly by maintainers
* Contributors should NOT push directly to `main` or `develop`
* All changes must be submitted via Pull Request

### Release

* Releases are managed via Git tags
* No separate release branches are used

---

## Pull Request Guidelines

* All changes must go through Pull Request
* Keep PRs small and focused (one logical change per PR)
* Avoid mixing unrelated changes

### PR Description must include:

* What was changed
* Why it was changed
* How it was tested (or verified)

---

## Commit Message Convention

Use the following prefixes:

* `feat` - for new features or enhancements
* `fix` - for bug fixes
* `chore` - for maintenance tasks (e.g., updating dependencies, formatting)
* `test` - for adding or updating tests
* `build` - for build-related changes (e.g., CI configuration)
* `refactor` - for code restructuring without changing functionality

### Rules

* Messages must be written in **sentence form**
* `feat` messages must start with an **imperative verb**

### Examples

```
feat: Add terrain sampling option
fix: Resolve bounding box overflow issue
refactor: Simplify voxel processing logic
chore: Update dependency versions
```

---

## Common Module Policy (mago-common)

`mago-common` is a shared module used across multiple projects such as:

* mago-3d-tiler
* mago-3d-terrainer

### Rules

* Only implement **general-purpose and reusable logic**
* Avoid project-specific code inside `mago-common`
* Keep the structure simple and broadly applicable

### IMPORTANT

* All modifications must be **carefully reviewed before merging**
* Changes in common logic may affect multiple projects

---

## Subtree Sync Policy

This repository uses `git subtree` for synchronization.

### Rules

* Do NOT run `git subtree push`
* Use `git subtree pull` only when syncing
* Shared logic must be updated at the source before syncing

---

## Review Criteria

Pull Requests will be reviewed based on:

* Reusability (no project-specific logic in common modules)
* Code clarity and simplicity
* Avoidance of duplicated logic
* Minimal and focused changes

---

## Testing

* Add tests when applicable
* If tests are not included, clearly describe how the change was verified

---

## Notes

If your change affects shared logic or has wide impact,   
please discuss it before implementation.

When unsure, open an issue first.
