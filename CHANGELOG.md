# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Path-based code-owners **merge check** for Bitbucket Data Center: blocks a merge until the
  required number of approvals from each matched owner group is reached.
- Subgroup approval thresholds (`minApprovals`) so a "≥1 senior" gate can be expressed where a
  junior's approval does not count.
- Config-as-code in `.bitbucket/codeowners.yml`, read from the pull request's target branch.
- Optional auto-add of matched owners as reviewers when a pull request is opened.
- GitHub Actions: CI build/test on every push & PR; tagged releases (`v*`) attach the built
  `.jar` to the GitHub Release.
