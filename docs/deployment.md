# Deployment (for devops)

This plugin is a standard Atlassian **P2 / OSGi** app for **Bitbucket Data Center**. It is
distributed as a single self-contained `.jar` attached to each GitHub Release — no Atlassian
Marketplace listing, no external services, no outbound network calls.

## Requirements

| Item            | Value                                                                  |
|-----------------|------------------------------------------------------------------------|
| Bitbucket DC    | **10.x** for the default release artifact (built & verified against 10.3.1) |
| Java runtime    | **21** (the JVM Bitbucket 10.x runs on)                                |
| Permission      | A Bitbucket **system administrator** to upload the app                |
| Bitbucket type  | **Data Center** (the merge-check SPI is not available on Cloud)        |

### Bitbucket 9.x LTS

The default artifact targets the Bitbucket 10.x API / Java 21. A separate build profile targets the
**9.x LTS line (Java 17)** and has been verified on **9.3.2**:

```bash
mvn -B -Pbb9 clean package      # -> target/bitbucket-codeowners-<version>.jar for Bitbucket 9.x
```

CI also produces this artifact (the *Build (Bitbucket 9.x LTS)* workflow). Install the **9.x build
on 9.x** and the **default build on 10.x** — the two are not interchangeable because they compile
against different platform APIs.

## Install

1. Download `bitbucket-codeowners-<version>.jar` from the
   [GitHub Releases](https://github.com/MrRefactoring/bitbucket-codeowners/releases) page
   (use the 9.x-labelled artifact for a 9.x server).
2. In Bitbucket: **Administration → Manage apps → Upload app**.
3. Select the `.jar` and confirm. The app appears under **User-installed apps**, enabled.

No restart is required. (If your instance enforces secure administrator sessions, you may be
prompted to re-authenticate before uploading — this is normal.)

## Enable per repository (isolation)

The merge check is **off by default** and is enabled per repository, so other teams are unaffected
until you opt in:

1. Go to the repository → **Repository settings → Merge checks**.
2. Enable **Code Owners Merge Check**.
3. Commit a `codeowners.yml` to the **root** of the repository's default/target branch
   (see [configuration.md](./configuration.md)).

To apply it across many repositories at once, enable it at **Project settings → Merge checks**
instead — it then applies to every repository in the project.

## Native Code Owners coexistence

Bitbucket DC ships a **native Code Owners** feature (since 8.14) that reads a `CODEOWNERS` file
(default `.bitbucket/CODEOWNERS`) and *adds reviewers* — it does **not** enforce approvals.

This plugin reads a **different file** — `codeowners.yml` (or `codeowners.yaml`) at the repository
**root** — so there is **no conflict** with native Code Owners and **no server change is required**.
The two can run side by side: native CO suggests reviewers; this plugin enforces the thresholds.

If you ever want to turn native Code Owners off entirely, note that its switch is **instance-wide**
(there is no per-repository toggle):

```properties
# <bitbucket-home>/shared/bitbucket.properties
feature.code.owners=false
```

A restart is required for property changes. Leaving native Code Owners enabled is fine — it does not
touch `codeowners.yml`.

## What the check does at merge time

1. Reads `codeowners.yml` / `codeowners.yaml` from the repository **root** on the PR's **target
   branch** (at its latest commit).
2. Computes the files changed by the PR.
3. For every rule whose globs match a changed file, requires `minApprovals` approvals **from that
   rule's owners** (approvals are matched case-insensitively by username).
4. **Blocks** the merge with an explanatory, per-rule message until every requirement is met.

Failure modes are **fail-closed** and surfaced to the user:

- File **absent** → check does nothing (merge allowed).
- File **present but invalid** (bad YAML, no rules, unresolvable owners) → merge **blocked** with a
  message naming the problem, so the misconfiguration is visible and fixable.

See [configuration.md → Validation & failure modes](./configuration.md#validation--failure-modes)
for the full table.

## Verify the install

After enabling the check on a test repository with a `codeowners.yml`:

1. Open a PR that changes an **owned** path → the merge is blocked and the PR shows the
   "Code owner approval required" check with the unmet requirement(s).
2. Have a member of the required group approve → the check clears and the merge is allowed.
3. Open a PR that changes only **unowned** paths → the check does not block (with the default
   `unmatchedPolicy: allow`).

## Troubleshooting

| Symptom                                              | Likely cause / fix                                                            |
|------------------------------------------------------|-------------------------------------------------------------------------------|
| Upload rejected / app won't enable                   | Wrong artifact for the server version — use the 9.x build on 9.x, default on 10.x. |
| Check never blocks                                   | Check not enabled on the repo/project, or no `codeowners.yml` on the **target** branch, or no rule matches the changed paths. |
| "Invalid code owners configuration" on every merge   | YAML error or missing `rules`. Open the PR's check message for the exact reason; validate the file on the target branch. |
| "Owners could not be resolved"                       | A rule references an unknown `@group` or relies on `@@bitbucket-group`. Define the group's users explicitly under `groups:`. |
| Rule change didn't take effect                       | Config is read from the **target branch** — merge the rule change into that branch first. |

## Uninstall / rollback

- **Stop enforcing without uninstalling:** disable **Code Owners Merge Check** in the repository or
  project **Merge checks** settings.
- **Remove entirely:** **Manage apps → User-installed apps → Uninstall**. No data is left behind;
  the `codeowners.yml` files in repositories are plain content and can be removed at your discretion.
