# Changelog

All notable changes to the Irontec Jenkins Shared Library will be 
documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com),
and this project adheres to [Semantic Versioning](https://semver.org).

## [Unreleased]

### Added
- Initial release of the shared library
- Git and Branch management functions:
  - `getJiraTicket()`: Extract JIRA ticket from branch name
  - `getBranchName()`: Get current branch name
  - `getBaseBranch()`: Get base branch for comparison
  - `getDockerTag()`: Get Docker tag for current build
  - `hasCommitTag()`: Check for specific tags in commit messages
  - `hasLabel()`: Check if PR has a specific label

- GitHub integration functions:
  - `notifySuccessGithub()`: Notify GitHub of successful stage
  - `notifyFailureGithub()`: Notify GitHub of failed stage
  - `notifyUnstableGithub()`: Notify GitHub of unstable stage
  - `githubMarkApproved()`: Approve a pull request
  - `githubMarkChangesRequested()`: Request changes on a PR
  - `githubUpdatePullRequestTitle()`: Update PR title with JIRA ticket

- JIRA integration functions:
  - `jiraUpdateCustomFields()`: Update JIRA custom fields

- Mattermost notification functions:
  - `notifyFailureMattermost()`: Send failure notification to Mattermost
  - `notifyFixedMattermost()`: Send fixed notification to Mattermost

- Hash and caching functions:
  - `getCurrentHash()`: Calculate directory content hash
  - `isHashTested()`: Check if hash has been tested
  - `saveTestedHash()`: Save tested hash to cache

### Documentation
- Comprehensive README.md with function documentation
- Detailed INSTALLATION.md with setup instructions
- QUICKSTART.md for rapid onboarding
- Example Jenkinsfile showing library usage
- Helper script to list all available functions

### Features
- All functions are project-agnostic and configurable
- Support for environment variable defaults
- Optional parameters for maximum flexibility
- No hardcoded project-specific values

## Release Notes

### Version 1.0.0 (Unreleased)

Initial extraction of common pipeline functions into a reusable shared 
library. This library is designed to work with multiple projects.

**Key Features:**
- Configurable contexts for GitHub notifications
- Configurable channels for Mattermost notifications
- Environment variable-based defaults
- Project-agnostic design

**Configuration:**
All project-specific values can be configured via environment variables:
- `GITHUB_CONTEXT_PREFIX`: Prefix for GitHub status contexts
- `MATTERMOST_CHANNEL`: Target channel for notifications
- `GITHUB_CREDENTIALS`: GitHub credentials ID
- `HASH_FILE`: Path to hash cache file
- `MAX_HASHES`: Maximum hashes to store in cache

**Migration Notes:**
- All functions maintain backward compatibility
- Function signatures include optional parameters for flexibility
- Environment variables are used as defaults when parameters are 
  not provided
- GitHub context prefix defaults to "ci" if not configured
- Mattermost notifications are skipped if channel not configured

**Breaking Changes:**
- None (initial release)

**Known Issues:**
- None

---

## How to Update This Changelog

When making changes to the library:

1. Add your changes under the `[Unreleased]` section
2. Use these categories:
   - `Added` for new functions
   - `Changed` for changes in existing functionality
   - `Deprecated` for soon-to-be removed functions
   - `Removed` for removed functions
   - `Fixed` for bug fixes
   - `Security` for security-related changes

3. When creating a release:
   - Change `[Unreleased]` to `[X.Y.Z] - YYYY-MM-DD`
   - Create a new `[Unreleased]` section at the top
   - Update version in README.md

Example:
```markdown
## [Unreleased]

### Added
- New function `doSomething()` for doing something

## [1.0.0] - 2025-10-14

### Added
- Initial release
```
