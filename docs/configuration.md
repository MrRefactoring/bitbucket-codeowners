# Configuration

The plugin is configured **as code**, per repository, in a YAML file read from the
**destination branch** of each pull request:

```
.bitbucket/codeowners.yml
```

Because the file is read from the PR's target branch, a change to the rules only takes
effect once it is merged into that branch.

## Schema

```yaml
version: 1

# Named groups of owners. Members are Bitbucket usernames.
# A "seniors" subgroup is just a group that contains only the senior users.
groups:
  frontend-seniors:
    users: [alice, bob]
  backend-seniors:
    users: [carol, dave]

# Ownership rules, evaluated against the paths changed in the pull request.
rules:
  - paths: ["frontend/**", "*.tsx", "*.ts"]
    owners: ["@frontend-seniors"]   # must approve
    minApprovals: 1                  # how many approvals are required from this rule's owners
  - paths: ["backend/**", "*.java"]
    owners: ["@backend-seniors"]
    minApprovals: 1

options:
  autoAddReviewers: true             # add matched owners as reviewers when a PR is opened
  unmatchedPolicy: allow             # allow | require-default
```

### `groups`

A map of `group-name -> { users: [ ... ] }`. Members are Bitbucket usernames
(the `name`/slug shown on a user's profile). Groups exist so the *senior* subgroup can be
modelled explicitly and reused across rules.

### `rules`

Each rule has:

| Field          | Meaning                                                                                  |
|----------------|------------------------------------------------------------------------------------------|
| `paths`        | List of glob patterns (`.gitignore`-style). A rule matches a PR if **any** changed path matches **any** of its globs. |
| `owners`       | List of owners. Each entry is one of: `@group-name` (resolves to that group's users), a bare `username`, or `@@bitbucket-group` (a real Bitbucket group — see note). |
| `minApprovals` | Number of approvals required **from this rule's owners** before the merge is allowed. Defaults to `1`. |

**Why subgroups matter.** To require "at least one senior", make the owner group contain
**only seniors** and set `minApprovals: 1`. A junior's approval does not count, because the
junior is not a member of the senior group.

### `options`

| Field             | Default | Meaning                                                                                   |
|-------------------|---------|-------------------------------------------------------------------------------------------|
| `autoAddReviewers`| `true`  | When a PR is opened, add the matched owners as reviewers so they are notified.             |
| `unmatchedPolicy` | `allow` | `allow`: PRs that touch no owned path can merge freely. `require-default`: reserved for a future "fallback owners" rule. |

## Glob semantics

- `frontend/**` — anything under `frontend/`.
- `*.tsx` — any file with that extension, in any directory.
- `docs/` — the `docs` directory subtree.
- A leading `/` anchors to the repository root.

Rules are evaluated top-to-bottom. Every rule whose globs match a changed path contributes
its `(owners, minApprovals)` requirement; **all** matched requirements must be satisfied for
the merge to proceed.

## Owner reference forms

| Form              | Resolves to                                                            |
|-------------------|------------------------------------------------------------------------|
| `@group-name`     | The `users` of that group from the `groups:` map. **Recommended.**     |
| `username`        | A single Bitbucket user.                                               |
| `@@bitbucket-group` | The members of a real Bitbucket group. See the note below.          |

> **Note on `@@bitbucket-group`.** Resolving the members of a native Bitbucket group depends
> on a host API that is verified per deployment. Prefer modelling the critical senior subgroup
> with explicit `groups: { ...: { users: [...] } }`, which is always reliable.

## Example: monorepo with senior gates

```yaml
version: 1
groups:
  frontend-seniors: { users: [alice, bob] }
  backend-seniors:  { users: [carol, dave] }
rules:
  - paths: ["apps/web/**", "**/*.tsx", "**/*.ts", "**/*.css"]
    owners: ["@frontend-seniors"]
    minApprovals: 1
  - paths: ["services/**", "**/*.java", "**/*.kt"]
    owners: ["@backend-seniors"]
    minApprovals: 1
options:
  autoAddReviewers: true
  unmatchedPolicy: allow
```

A PR that changes both a `.tsx` and a `.java` file must collect **one approval from a frontend
senior and one from a backend senior** before it can be merged.
