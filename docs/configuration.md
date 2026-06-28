# Configuration

The plugin is configured **as code**, per repository, in a single YAML file at the
**repository root**.

## File location and name

Place the file at the **root of the repository** (not in a subfolder). Either YAML extension is
accepted; the plugin looks for these names, in order, and uses the **first one that exists**:

| Order | Path (repository root) |
|-------|------------------------|
| 1     | `codeowners.yml`       |
| 2     | `codeowners.yaml`      |

> `codeowners.yml` is the recommended name. If both files exist, `codeowners.yml` wins and
> `codeowners.yaml` is ignored — keep only one.

The file is read from the **target branch** of each pull request, at the branch's latest commit.
Because of this, a change to the rules only takes effect **after it is merged into the target
branch** — the same property that makes the policy itself reviewable and tamper-resistant (a PR
cannot weaken the rules that gate its own merge).

If no config file exists on the target branch, the check does nothing and the merge is allowed.

## Quick start

```yaml
# codeowners.yml — at the repository root
version: 1

groups:
  frontend-seniors: { users: [alice, bob] }
  backend-seniors:  { users: [carol, dave] }

rules:
  - paths: ["frontend/**", "**/*.tsx", "**/*.ts"]
    owners: ["@frontend-seniors"]
    minApprovals: 1
  - paths: ["backend/**", "**/*.java"]
    owners: ["@backend-seniors"]
    minApprovals: 1

options:
  autoAddReviewers: true
  unmatchedPolicy: allow
```

A junior approving a `*.tsx` change does **not** unblock the merge — only a member of
`frontend-seniors` does, because the junior is not in that group.

## Schema reference

### Top level

| Key       | Required | Type    | Notes                                                                 |
|-----------|----------|---------|-----------------------------------------------------------------------|
| `version` | no       | integer | Forward-compatibility marker. Currently not validated; keep `1`.      |
| `groups`  | no       | mapping | Named owner groups. See [`groups`](#groups).                          |
| `rules`   | **yes**  | list    | At least one rule is required. See [`rules`](#rules).                 |
| `options` | no       | mapping | Behavioural options. See [`options`](#options).                      |

If `rules` is missing or empty, the file is treated as **invalid** and the merge is blocked with a
parse-error message (see [Validation & failure modes](#validation--failure-modes)).

### `groups`

A map of `group-name -> { users: [ ... ] }`. Members are **Bitbucket usernames** (the `name`/slug
shown on the user's profile, not the display name). Username matching at approval time is
**case-insensitive**.

```yaml
groups:
  frontend-seniors:
    users: [alice, bob]
  # shorthand form (a bare list) is also accepted:
  release-managers: [erin, frank]
```

Groups exist so a **subgroup** — e.g. only the *senior* engineers — can be modelled explicitly and
reused across rules. This is the mechanism behind "at least one senior must approve".

### `rules`

A list, evaluated against the paths changed by the pull request.

| Field          | Required | Default | Meaning                                                                                  |
|----------------|----------|---------|------------------------------------------------------------------------------------------|
| `paths`        | **yes**  | —       | Non-empty list of glob patterns. The rule matches a PR if **any** changed path matches **any** of its globs. See [Glob semantics](#glob-semantics). |
| `owners`       | **yes**  | —       | Non-empty list of owner references. See [Owner reference forms](#owner-reference-forms). |
| `minApprovals` | no       | `1`     | Approvals required **from this rule's owners**. Must be `>= 1`.                           |

Rules are independent and additive: **every** rule whose globs match at least one changed path
contributes a requirement, and **all** contributed requirements must be satisfied before the merge
is allowed.

**Why subgroups matter.** To require "at least one senior", make the rule's owner group contain
**only seniors** and set `minApprovals: 1`. A junior's approval does not count, because the junior
is not a member of the senior group.

### `options`

| Field             | Default | Meaning                                                                                   |
|-------------------|---------|-------------------------------------------------------------------------------------------|
| `autoAddReviewers`| `true`  | When a PR is opened, add the matched owners as reviewers so they are notified. Best-effort: never blocks PR creation. Accepts `true`/`yes`/`on`. |
| `unmatchedPolicy` | `allow` | `allow`: PRs that touch no owned path can merge freely. `require-default`: reserved for a future "fallback owners" rule — currently behaves like `allow`. |

## Owner reference forms

Each entry in a rule's `owners` list is one of:

| Form                | Resolves to                                                           | Reliability |
|---------------------|----------------------------------------------------------------------|-------------|
| `@group-name`       | The `users` of that group from the `groups:` map.                    | **Recommended** |
| `username`          | A single Bitbucket user (bare username, no `@`).                     | Reliable |
| `@@bitbucket-group` | The members of a **native Bitbucket group**.                         | See note below |

> **Note on `@@bitbucket-group`.** Enumerating the members of a native Bitbucket group depends on a
> host API that is verified per deployment. In the current release this resolves to **no members**,
> which makes any rule that relies solely on `@@group` **fail closed** (the merge is blocked with a
> clear "owners could not be resolved" message) rather than silently passing. **Model the critical
> senior subgroup with an explicit `groups: { ...: { users: [...] } }` list**, which is always
> reliable.

## Glob semantics

Patterns are `.gitignore`/CODEOWNERS-style and match against repository-root-relative paths
(forward slashes, no leading slash).

| Token         | Matches                                                                 |
|---------------|-------------------------------------------------------------------------|
| `*`           | Any run of characters **within a single path segment** (does not cross `/`). |
| `**`          | Any run of characters **across** path segments.                         |
| `?`           | Exactly one character, except `/`.                                      |
| leading `/`   | Anchors the pattern to the repository root.                            |
| trailing `/`  | A directory subtree, e.g. `docs/` is equivalent to `docs/**`.          |

Anchoring rule: a pattern that contains an internal `/` (or a leading `/`) is **anchored to the
root**; a pattern with **no** slash (e.g. `*.tsx`) matches at **any directory depth**.

| Pattern        | Matches                                   | Does not match                |
|----------------|-------------------------------------------|-------------------------------|
| `*.tsx`        | `a.tsx`, `src/ui/Button.tsx`              | `a.ts`                        |
| `frontend/**`  | `frontend/a.ts`, `frontend/ui/b.tsx`      | `services/frontend/a.ts`      |
| `/README.md`   | `README.md` (root only)                   | `docs/README.md`              |
| `docs/`        | `docs/intro.md`, `docs/api/v1.md`         | `website/docs/intro.md`       |
| `**/*.java`    | `Main.java`, `src/main/A.java`            | `Main.kt`                     |

## Validation & failure modes

The check is **fail-closed** on misconfiguration so problems are visible, not silently ignored:

| Situation                                   | Behaviour                                                            |
|---------------------------------------------|---------------------------------------------------------------------|
| No config file on the target branch         | Check passes — merge allowed (no enforcement).                      |
| File present, valid, no rule matches the PR | Check passes — merge allowed.                                       |
| File present, valid, a requirement unmet    | Merge **blocked** with a per-rule message listing what's missing.   |
| File empty / not a mapping / no `rules`     | Merge **blocked** — `Invalid code owners configuration` + reason.   |
| YAML syntax error                           | Merge **blocked** — `Invalid code owners configuration` + reason.   |
| Owners unresolvable (e.g. unknown `@group`) | Merge **blocked** — "owners could not be resolved" for that rule.   |

## Full example: monorepo with senior gates

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
    minApprovals: 2          # require two backend seniors for service changes

options:
  autoAddReviewers: true
  unmatchedPolicy: allow
```

A PR that changes both a `.tsx` and a `.java` file must collect **one approval from a frontend
senior and two from backend seniors** before it can be merged.
