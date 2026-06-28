# Deployment (for devops)

This plugin is a standard Atlassian **P2 / OSGi** app for **Bitbucket Data Center**. It is
distributed as a single `.jar` attached to each GitHub Release.

## Requirements

| Item              | Value                                                            |
|-------------------|-----------------------------------------------------------------|
| Bitbucket DC      | 10.x (built and verified against **10.3.1**)                    |
| Java runtime      | 21 (the JVM Bitbucket 10.x runs on)                             |
| Permission        | A Bitbucket **system administrator** to upload the app          |

> The release artifact targets the Bitbucket 10.x API and Java 21. To support an LTS line
> (e.g. 9.x / Java 17) the project would need to be rebuilt with a lower `bitbucket.version`
> and `maven.compiler.release` — see `pom.xml`.

## Install

1. Download `bitbucket-codeowners-<version>.jar` from the
   [GitHub Releases](https://github.com/MrRefactoring/bitbucket-codeowners/releases) page.
2. In Bitbucket: **Administration → Manage apps → Upload app**.
3. Select the `.jar` and confirm. The app appears under **User-installed apps**.

No Atlassian Marketplace listing is required — this is a private/self-hosted upload.

## Enable per repository (isolation)

The merge check is **off by default** and is enabled per repository, so other teams are
unaffected:

1. Go to the repository → **Repository settings → Merge checks**.
2. Enable **Code Owners Merge Check**.
3. Add a `.bitbucket/codeowners.yml` to the repository's default/target branch
   (see [configuration.md](./configuration.md)).

You can also enable it at **Project settings → Merge checks** to apply it to all repositories
in a project.

## Native Code Owners coexistence

Bitbucket DC ships a **native Code Owners** feature (since 8.14) that reads
`.bitbucket/CODEOWNERS` and *adds reviewers* (it does not enforce approvals).

This plugin reads a **different file** (`.bitbucket/codeowners.yml`), so there is **no conflict**
with native Code Owners and **no server change is required** — the two coexist.

If you ever want to turn native Code Owners off entirely, note that its switch is
**instance-wide** (there is no per-repository toggle):

```properties
# <bitbucket-home>/shared/bitbucket.properties
feature.code.owners=false
```

(Alternatively, point native at an unused path with `code.owners.file.path=...`.) A restart is
required for property changes. Leaving native enabled is fine — it simply won't touch
`.bitbucket/codeowners.yml`.

## What the check does at merge time

1. Reads `.bitbucket/codeowners.yml` from the PR's **target branch**.
2. Computes the files changed by the PR.
3. For every rule whose globs match a changed file, requires `minApprovals` approvals from that
   rule's owners.
4. Blocks the merge with an explanatory message until every requirement is met.

If the file is absent, the check does nothing (merge allowed). If the file is present but
invalid, the merge is blocked with a parse-error message so the misconfiguration is visible.

## Uninstall / rollback

Remove the app under **Manage apps → User-installed apps → Uninstall**, or simply disable the
merge check in repository/project settings to stop enforcement without uninstalling.
