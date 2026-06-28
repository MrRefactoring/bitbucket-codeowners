# Bitbucket Code Owners

A self-hosted, open-source **Bitbucket Data Center** app that enforces **path-based code
ownership with subgroup approval thresholds** — the piece that native Code Owners and native
merge checks can't do on their own.

- **Native Code Owners** (DC 8.14+) only *suggests/adds* reviewers — it does not enforce.
- **Native merge checks** ("Minimum approvals", "All reviewers approve") are global per PR,
  not path-based, and can't require "N approvals from a specific subgroup".

This app adds a **merge check** that blocks a merge until the configured number of approvals
from the required owner groups is reached for the paths a pull request changes — and optionally
auto-adds those owners as reviewers when the PR is opened.

## Why

In a **monorepo**, you often want: frontend files reviewed by frontend owners, backend files by
backend owners, and — critically — **at least one *senior*** sign-off for certain paths, where a
junior's approval must **not** satisfy the gate. That is expressed directly here:

```yaml
# codeowners.yml — at the repository root
groups:
  frontend-seniors: { users: [alice, bob] }
  backend-seniors:  { users: [carol, dave] }
rules:
  - paths: ["frontend/**", "*.tsx", "*.ts"]
    owners: ["@frontend-seniors"]
    minApprovals: 1
  - paths: ["backend/**", "*.java"]
    owners: ["@backend-seniors"]
    minApprovals: 1
options:
  autoAddReviewers: true
  unmatchedPolicy: allow
```

A junior approving a `*.tsx` change does not unblock the merge — only a member of
`frontend-seniors` does.

## How it works

| Concern              | Mechanism                                                                 |
|----------------------|---------------------------------------------------------------------------|
| Configuration        | `codeowners.yml` / `codeowners.yaml` at the **repo root**, read from the PR **target branch** (config-as-code) |
| Enforcement          | `RepositoryMergeCheck` SPI — vetoes the merge with an explanatory message  |
| Reviewer suggestion  | Listener on `PullRequestOpenedEvent` adds matched owners as reviewers      |
| Scope / isolation    | Enabled **per repository/project** under *Merge checks*                    |
| Native CO coexistence| Uses a distinct file, so no conflict and no server change needed           |

## Install

Download the `.jar` from [Releases](https://github.com/MrRefactoring/bitbucket-codeowners/releases)
and upload it via **Administration → Manage apps → Upload app**. Full steps, requirements, and
the native-Code-Owners notes are in [docs/deployment.md](./docs/deployment.md).

Requirements: **Bitbucket DC 10.x** (built against 10.3.1), **Java 21**. A **Bitbucket 9.x LTS**
build (Java 17) is also produced — see [docs/deployment.md](./docs/deployment.md).

## Configuration

Put a `codeowners.yml` (or `codeowners.yaml`) at the **root of the repository**, on the branch you
merge into. It is read from each PR's **target branch**, so the rules that gate a merge can't be
weakened by the same PR.

See [docs/configuration.md](./docs/configuration.md) for the full schema, glob semantics, and
owner-reference forms.

## Build from source

No Atlassian SDK / `atlas-*` CLI required — a plain Maven build produces the installable bundle:

```bash
mvn -B clean package
# -> target/bitbucket-codeowners-<version>.jar
```

Requirements to build: **JDK 21** and **Maven 3.9+**. The build resolves dependencies from the
Atlassian public Maven repository, which is declared in `pom.xml`.

## Releasing

CI builds every push/PR. To publish an artifact for devops:

```bash
# bump <version> in pom.xml to match, then:
git tag v0.1.0
git push origin v0.1.0
```

The `Release` workflow builds the plugin and attaches `target/*.jar` to the GitHub Release.

## Status

Early v0.x. The merge-check enforcement and config model are the stable core; resolving
**native Bitbucket groups** via `@@group` is gated behind per-deployment verification — model
the critical senior subgroup with explicit user lists (`groups:`), which is always reliable.

## License

[MIT](./LICENSE) © 2026 Vladislav Tupikin
