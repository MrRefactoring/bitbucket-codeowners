# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.1] - 2026-06-28

### Added
- **Advisory rules** (`minApprovals: 0`): a rule's owners are auto-added as reviewers but the rule
  never blocks the merge (not even when its owners cannot be resolved). Lets a mandatory rule and a
  non-mandatory rule coexist on the same paths without redundant veto messages.

## [0.1.0] - 2026-06-28

Initial release.

### Added
- Path-based code-owners **merge check** for Bitbucket Data Center: blocks a merge until the
  required number of approvals from each matched owner group is reached for the paths a pull
  request changes.
- Subgroup approval thresholds (`minApprovals`) so a "≥1 senior" gate can be expressed where a
  junior's approval does not count.
- Config-as-code in `codeowners.yml` / `codeowners.yaml` at the **repository root**, read from the
  pull request's target branch (first matching name wins, `.yml` preferred).
- Optional auto-add of matched owners as reviewers when a pull request is opened.
- Fail-closed validation: a present-but-invalid config (bad YAML, no rules, or unresolvable
  owners) blocks the merge with an explanatory message instead of being silently ignored.
- Cross-version build from a single source: the default artifact targets **Bitbucket 10.x /
  Java 21**, and the `bb9` Maven profile produces a **Bitbucket 9.x LTS / Java 17** artifact
  (verified on 9.3.2).
- GitHub Actions: CI build/test on every push & PR; a `v*` tag builds the plugin and attaches the
  `.jar` to the GitHub Release.

[Unreleased]: https://github.com/MrRefactoring/bitbucket-codeowners/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/MrRefactoring/bitbucket-codeowners/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/MrRefactoring/bitbucket-codeowners/releases/tag/v0.1.0
