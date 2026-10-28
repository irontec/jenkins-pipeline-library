# Irontec Jenkins Shared Library

Reusable Jenkins pipeline functions for common CI/CD operations including
Git/branch management, GitHub integration, JIRA integration, Mattermost
notifications, and hash-based caching.

## Table of Contents

- [Installation](#installation)
- [Configuration](#configuration)
- [Available Functions](#available-functions)
  - [Git and Branch Management](#git-and-branch-management)
  - [GitHub Integration](#github-integration)
  - [JIRA Integration](#jira-integration)
  - [Mattermost Notifications](#mattermost-notifications)
  - [Hash and Caching](#hash-and-caching)
  - [Harbor Registry](#harbor-registry)
  - [GitOps](#gitops)
- [Usage Examples](#usage-examples)

## Installation

### Option 1: Global Shared Library

1. Go to **Jenkins** → **Manage Jenkins** → **Configure System**
2. Scroll to **Global Pipeline Libraries**
3. Click **Add**
4. Configure:
   - **Name**: `irontec-jenkins-library`
   - **Default version**: `main` (or your preferred branch)
   - **Retrieval method**: Modern SCM
   - **Source Code Management**: Git
   - **Project Repository**:
     `https://github.com/irontec/jenkins-shared-library.git`

### Option 2: Per-Pipeline Configuration

Add this at the top of your Jenkinsfile:

```groovy
@Library('irontec-jenkins-library@main') _

pipeline {
    // ... your pipeline
}
```

## Configuration

### Required Jenkins Plugins

- **GitHub Branch Source Plugin**: For GitHub integration
- **GitHub Plugin**: For GitHub notifications
- **JIRA Plugin**: For JIRA integration
- **Mattermost Plugin**: For Mattermost notifications
- **HTTP Request Plugin** and **Pipeline Utility Steps Plugin**: For the
  Harbor functions
- **Active Choices Plugin**: For `harborVersionParam()` and
  `gitopsTenantParam()`

### Environment Variables

Configure these in your pipeline or globally:

```groovy
environment {
    // GitHub Integration
    GITHUB_CREDENTIALS = 'your-github-credentials-id'
    GITHUB_CONTEXT_PREFIX = 'my-project-ci'  // Optional, default: "ci"

    // JIRA Integration
    JIRA_TICKET = getJiraTicket()  // Auto-extracted from branch

    // Mattermost Integration
    MATTERMOST_CHANNEL = '#my-project-ci'  // Optional

    // Hash Caching
    HASH_FILE = "${JENKINS_HOME}/caches/${JOB_NAME}/tested_hashes.txt"
    MAX_HASHES = 100  // Optional, default: 100

    // Harbor Registry
    HARBOR_PROJECT = 'my-project'
    HARBOR_REGISTRY = 'harbor.example.com'        // Optional, default: harbor.comms.irontec.com
    HARBOR_CREDENTIALS_ID = 'my-harbor-creds'     // Optional, default: harbor-comms-credentials
}
```

## Available Functions

### Git and Branch Management

#### `getJiraTicket()`

Extracts JIRA ticket identifier from branch name.

**Expected format**: `TICKET-123-description`

**Returns**: `String` - JIRA ticket (e.g., "PROJ-123") or empty string

**Example**:
```groovy
def ticket = getJiraTicket()
if (ticket) {
    echo "Working on ticket: ${ticket}"
}
```

---

#### `getBranchName()`

Gets the current branch name.

**Returns**: `String` - Current branch name

**Example**:
```groovy
def branch = getBranchName()
echo "Building branch: ${branch}"
```

---

#### `getBaseBranch()`

Gets the base branch for the current build.

**Returns**: `String` - Base branch name

**Example**:
```groovy
def base = getBaseBranch()
echo "Comparing against: ${base}"
```

---

#### `getDockerTag()`

Gets the Docker tag for the current build.

**Returns**: `String` - Docker tag to use

**Example**:
```groovy
def tag = getDockerTag()
docker.build("myapp:${tag}", ".")
```

---

#### `hasCommitTag(String module)`

Checks if any commit in the current PR contains a specific tag.

**Parameters**:
- `module` (String): Tag to search for in commit messages

**Returns**: `boolean` - True if tag is found

**Example**:
```groovy
if (hasCommitTag("backend:")) {
    echo "Backend changes detected"
}
```

---

#### `hasLabel(String label)`

Checks if a pull request has a specific label.

**Parameters**:
- `label` (String): Label name to check

**Returns**: `boolean` - True if PR has the label

**Example**:
```groovy
if (hasLabel("skip-ci")) {
    echo "CI skipped by label"
    return
}
```

---

### GitHub Integration

#### `notifySuccessGithub(String contextPrefix, String description, String credentialsId)`

Notifies GitHub of a successful stage completion.

**Parameters**:
- `contextPrefix` (String, optional): Prefix for context
  (default: env.GITHUB_CONTEXT_PREFIX or "ci")
- `description` (String, optional): Description (default: "Finished")
- `credentialsId` (String, optional): Credentials ID
  (default: env.GITHUB_CREDENTIALS)

**Example**:
```groovy
stage('tests') {
    steps {
        sh 'make test'
    }
    post {
        success { notifySuccessGithub() }
    }
}
```

---

#### `notifyFailureGithub(String contextPrefix, String description, String credentialsId)`

Notifies GitHub of a failed stage.

**Parameters**: Same as `notifySuccessGithub`

**Example**:
```groovy
post {
    failure { notifyFailureGithub() }
}
```

---

#### `notifyUnstableGithub(String contextPrefix, String description, String credentialsId)`

Notifies GitHub of an unstable/cancelled stage.

**Parameters**: Same as `notifySuccessGithub`

**Example**:
```groovy
post {
    unstable { notifyUnstableGithub() }
}
```

---

#### `githubMarkApproved(String botUsername)`

Marks a GitHub pull request as approved.

**Parameters**:
- `botUsername` (String, optional): Bot username (default: "ironArt3mis")

**Example**:
```groovy
post {
    success { githubMarkApproved() }
}
```

---

#### `githubMarkChangesRequested(String botUsername, String message)`

Marks a GitHub pull request as requiring changes.

**Parameters**:
- `botUsername` (String, optional): Bot username (default: "ironArt3mis")
- `message` (String, optional): Review message

**Example**:
```groovy
post {
    unstable {
        githubMarkChangesRequested("jenkins-bot",
            "Tests failed. Please fix before merging.")
    }
}
```

---

#### `githubUpdatePullRequestTitle(String jiraTicket)`

Updates GitHub pull request title to include JIRA ticket.

**Parameters**:
- `jiraTicket` (String, optional): JIRA ticket identifier

**Example**:
```groovy
def ticket = getJiraTicket()
if (ticket) {
    env.JIRA_TICKET = ticket
    githubUpdatePullRequestTitle()
}
```

---

### JIRA Integration

#### `jiraUpdateCustomFields(String site, String jiraTicket)`

Updates JIRA issue custom fields with PR and branch information.

**Parameters**:
- `site` (String, optional): JIRA site URL
  (default: 'irontec.atlassian.net')
- `jiraTicket` (String, optional): JIRA ticket identifier

**Custom fields updated**:
- `customfield_10165`: Pull Request
- `customfield_10166`: Branch name

**Example**:
```groovy
def ticket = getJiraTicket()
if (ticket) {
    env.JIRA_TICKET = ticket
    jiraUpdateCustomFields('mycompany.atlassian.net')
}
```

---

### Mattermost Notifications

#### `notifyFailureMattermost(String channel, String branch, String color, String icon)`

Sends a failure notification to Mattermost.

**Parameters**:
- `channel` (String, optional): Mattermost channel
  (default: env.MATTERMOST_CHANNEL)
- `branch` (String, optional): Branch to monitor (default: "main").
  Compared against `BRANCH_NAME` (or `GIT_BRANCH` when unset), with the
  `refs/heads/` and `origin/` prefixes stripped from both sides, so
  `origin/main` matches `main`
- `color` (String, optional): Message color (default: "#FF0000")
- `icon` (String, optional): Emoji icon (default: ":red_circle:")

**Example**:
```groovy
environment {
    MATTERMOST_CHANNEL = '#my-project-ci'
}

post {
    failure { notifyFailureMattermost() }
}
```

---

#### `notifyFixedMattermost(String channel, String branch, String color, String icon)`

Sends a fixed/recovered notification to Mattermost.

**Parameters**: Same as `notifyFailureMattermost`

**Example**:
```groovy
post {
    fixed { notifyFixedMattermost() }
}
```

---

### Hash and Caching

#### `getCurrentHash(String dir)`

Calculates a hash of all files in a directory.

**Parameters**:
- `dir` (String): Directory path to hash

**Returns**: `String` - SHA256 hash of directory contents

**Example**:
```groovy
def backendHash = getCurrentHash("backend")
def frontendHash = getCurrentHash("frontend")
```

---

#### `isHashTested(String hash, String hashFile)`

Checks if a hash has already been tested.

**Parameters**:
- `hash` (String): Hash to check
- `hashFile` (String, optional): Path to cache file

**Returns**: `boolean` - True if hash has been tested before

**Example**:
```groovy
def hash = getCurrentHash("app")
if (isHashTested(hash)) {
    echo "Already tested, skipping"
    return
}
```

---

#### `saveTestedHash(String hash, String hashFile, Integer maxHashes)`

Saves a tested hash to the cache file. The update runs inside a
`lock()` on the cache file path, so concurrent stages or builds sharing
the file do not lose each other's hashes (requires the Lockable
Resources plugin).

**Parameters**:
- `hash` (String): Hash to save
- `hashFile` (String, optional): Path to cache file
- `maxHashes` (Integer, optional): Maximum number of hashes to store

**Example**:
```groovy
def hash = getCurrentHash("app")
// Run tests...
saveTestedHash(hash)
```

---

### Harbor Registry

Housekeeping of images pushed to Harbor. All of these take a single map
argument. `project` defaults to `env.HARBOR_PROJECT`, `registry` to
`env.HARBOR_REGISTRY` and `credentialsId` to `env.HARBOR_CREDENTIALS_ID`
(library constants when neither is set); the credentials need delete
rights on the project.

Harbor keys artifacts by digest: two byte-identical images (two commits
with the same tree, a preview equal to `main`, a release equal to a
preview) are one artifact carrying several tags. These functions therefore
delete **tags**, never artifacts. Harbor's garbage collection must have
**Delete untagged artifacts** enabled or the digests left without tags
accumulate.

#### `harborListArtifacts(Map args)`

Lists every artifact of a repository, with its tags, paging through the API.

**Parameters**:
- `repo` (String): Repository inside the project
- `project`, `registry`, `credentialsId` (String, optional): See above

**Returns**: `List` - Artifacts as Harbor returns them (`digest`,
`push_time`, `tags[]`...), or `null` when the repository does not exist.
A 401/403 fails the step.

#### `harborDeleteTags(Map args)`

Deletes tags from one repository. Building block of the two functions
below.

**Parameters**:
- `repo` (String): Repository inside the project
- `targets` (List): `[digest: String, tag: String]` entries
- `project`, `registry`, `credentialsId` (String, optional): See above

**Returns**: `int` - Targets deleted. A 404 is skipped (already gone);
anything else fails the step.

#### `harborDeleteTagsByPrefix(Map args)`

Deletes every tag starting with a prefix. Tearing down `pr-<n>-*` preview
images is the typical use. Idempotent: a missing repository is skipped, a
prefix with no tags is a no-op.

**Parameters**:
- `repos` (List): Repositories inside the project
- `prefix` (String): Tag prefix, e.g. `"pr-12-"`
- `project`, `registry`, `credentialsId` (String, optional): See above

**Returns**: `int` - Tags deleted

**Example**:
```groovy
harborDeleteTagsByPrefix(
    project: 'aps',
    repos: ['aps-app', 'aps-web'],
    prefix: "pr-${env.PR_NUMBER}-"
)
```

#### `harborPruneTags(Map args)`

Keeps only the newest `keep` `<prefix><commit>` tags per repository, for
jobs that push one tag per commit plus a moving tag. Every `<prefix>*` tag
counts, including one sharing its digest with a preview, a release or
`latest`: those other tags are never touched, nor are the `<prefix>*` tags
on the digest the moving tag points at. Retention is counted over tags
and ordered by the push time of the tag itself, so a commit whose image
is byte-identical to an older one is not read as old. Untagged artifacts
(left behind when a commit is rebuilt) are left to Harbor's garbage
collection.

**Parameters**:
- `repos` (List): Repositories inside the project
- `keep` (int): Newest `<prefix>*` tags to keep per repository
- `prefix` (String, optional): Per-commit tag prefix, default `"main-"`
- `moving` (String, optional): Moving tag, default `"main"`
- `project`, `registry`, `credentialsId` (String, optional): See above

**Returns**: `int` - Tags deleted

**Example**:
```groovy
harborPruneTags(project: 'aps', repos: ['aps-app', 'aps-web'], keep: 3)
```

#### `harborVersionParam(Map args)`

Build parameter offering the version tags of a repository, newest first,
for the build form of jobs that deploy a chosen version. An Active Choices
single-select: the list is read when the form opens. Versions compare
numerically (`0.10.0` above `0.9.0`); only the newest 100 artifacts are
read. Errors (credentials missing, Harbor down) show up as an
`Error: ...` choice. Read access to the project is enough.

Active Choices runs that script on its own, without this library, so the
step generates the whole script and pastes it into the parameter. It runs
in the Groovy sandbox and needs, once, the In-process Script Approval
signatures it uses (credentials lookup, `plainText`, `URL.openConnection`).

**Parameters**:
- `name` (String): Parameter name
- `repo` (String): Repository inside the project
- `semverOnly` (boolean, optional): Only `X.Y.Z` tags, default `true`
- `description` (String, optional): Parameter description
- `project`, `registry`, `credentialsId` (String, optional): See above
- `current` (Map, optional): Where the version the selected tenant runs
  lives, to label it "(actual)" and preselect it (see below)

**Returns**: `Map` - Parameter definition for `properties([parameters([...])])`

**Example**:
```groovy
properties([parameters([
    harborVersionParam(name: 'CHART_VERSION', project: 'shop', repo: 'shop-chart',
                       semverOnly: false),
    harborVersionParam(name: 'API_VERSION', project: 'shop', repo: 'api'),
])])
```

**Current version**: with `current`, the parameter reacts to an
environment and a tenant parameter, as `gitopsTenantParam()` declares
them. It reads one YAML file of the selected tenant from GitHub and the
scalar at a `.a.b.c` path in it; that version is labelled "(actual)" and
preselected, and inserted in order if Harbor did not list it. When the
file cannot be read the list comes unmarked. Keys of `current`:
`environmentParam`, `tenantParam` (parameter names), `repos` (environment
-> GitHub `owner/name`), `dir` (directory of tenant directories), `file`
(inside the tenant directory), `path`, and optionally `ref` (default
`"main"`) and `credentialsId` (default the library's GitHub credentials).
The YAML reader only understands block mappings, which is what values
files holding versions look like.

```groovy
def repos = [eu: 'acme/gitops-eu', us: 'acme/gitops-us']
def current = [environmentParam: 'ENVIRONMENT', tenantParam: 'TENANT', repos: repos,
               dir: 'deploy/values']
properties([parameters([
    choice(name: 'ENVIRONMENT', choices: ['eu', 'us']),
    gitopsTenantParam(name: 'TENANT', environmentParam: 'ENVIRONMENT', repos: repos,
                      dir: 'deploy/values'),
    harborVersionParam(name: 'API_VERSION', project: 'shop', repo: 'api',
                       current: current + [file: 'versions.yaml', path: '.api.image.tag']),
])])
```

Parameters declared from a Jenkinsfile reach the build form after the
first run of the job.

The selection logic lives in `src/org/pipeline/Harbor.groovy` and runs
outside Jenkins: `groovy -cp src:test/stubs test/HarborTest.groovy`.


### GitOps

#### `gitopsTenantParam(Map args)`

Build parameter offering the tenants of a GitOps repository, chosen by
another parameter. An Active Choices single-select reactive on
`environmentParam`: when the environment changes in the build form, it
reads that environment's repository tree from GitHub (one API call, at
`ref`) and lists the directories under `dir` holding every one of
`requiredFiles`. Adding a tenant directory to the repository adds it to
the form, with no change to the job. Errors show up as an `Error: ...`
choice.

Like `harborVersionParam()`, the step generates the whole script and
pastes it into the parameter; it runs in the Groovy sandbox.

**Parameters**:
- `name` (String): Parameter name
- `environmentParam` (String): Parameter it reacts to; its values are the
  keys of `repos`
- `repos` (Map): Environment -> GitHub `owner/name`
- `dir` (String): Directory with one directory per tenant
- `requiredFiles` (List, optional): Files a tenant directory must hold,
  default none
- `exclude` (List, optional): Tenants never offered, such as one deployed
  by other means
- `ref` (String, optional): Branch read, default `"main"`
- `description` (String, optional): Parameter description
- `credentialsId` (String, optional): Username/token credentials with read
  access, default the library's GitHub credentials

**Returns**: `Map` - Parameter definition for `properties([parameters([...])])`

**Example**:
```groovy
def repos = [eu: 'acme/gitops-eu', us: 'acme/gitops-us']
properties([parameters([
    choice(name: 'ENVIRONMENT', choices: ['eu', 'us']),
    gitopsTenantParam(name: 'TENANT', environmentParam: 'ENVIRONMENT', repos: repos,
                      dir: 'deploy/values', requiredFiles: ['values.yaml'],
                      exclude: ['sandbox']),
])])
```

The selection logic lives in `src/org/pipeline/Gitops.groovy`:
`groovy -cp src:test/stubs test/GitopsTest.groovy`.

---

## Usage Examples

### Complete Pipeline Example

```groovy
@Library('irontec-jenkins-library@main') _

pipeline {
    agent any

    environment {
        // Configure for your project
        GITHUB_CREDENTIALS = 'github-credentials-id'
        GITHUB_CONTEXT_PREFIX = 'my-project'
        MATTERMOST_CHANNEL = '#my-project-ci'

        // Auto-generated values
        DOCKER_TAG = getDockerTag()
        BRANCH_NAME = getBranchName()
        BASE_BRANCH = getBaseBranch()
        JIRA_TICKET = getJiraTicket()
        HASH_BACK = getCurrentHash("backend")
        HASH_FILE = "${JENKINS_HOME}/caches/${JOB_NAME}/hashes.txt"
        MAX_HASHES = 100
    }

    stages {
        stage('Update PR Info') {
            when {
                expression { env.JIRA_TICKET }
            }
            steps {
                script {
                    jiraUpdateCustomFields()
                    githubUpdatePullRequestTitle()
                }
            }
        }

        stage('Check Cache') {
            steps {
                script {
                    env.CACHED = isHashTested(env.HASH_BACK)
                    echo "Hash tested before? ${env.CACHED}"
                }
            }
        }

        stage('Tests') {
            when {
                expression { env.CACHED != "true" }
            }
            steps {
                sh 'make test'
            }
            post {
                success { notifySuccessGithub() }
                failure { notifyFailureGithub() }
            }
        }
    }

    post {
        failure {
            notifyFailureMattermost()
        }
        success {
            githubMarkApproved()
            saveTestedHash(env.HASH_BACK)
        }
        fixed {
            notifyFixedMattermost()
        }
    }
}
```

### Conditional Testing Based on Changes

```groovy
stage('Backend') {
    when {
        anyOf {
            expression { hasLabel("force-backend-tests") }
            expression { hasCommitTag("backend:") }
            branch "main"
        }
    }
    steps {
        sh 'make test-backend'
    }
}
```

### Hash-based Caching

```groovy
environment {
    HASH_APP = getCurrentHash("app")
    HASH_WEB = getCurrentHash("web")
}

stages {
    stage('Check Caches') {
        steps {
            script {
                env.APP_CACHED = isHashTested(env.HASH_APP)
                env.WEB_CACHED = isHashTested(env.HASH_WEB)
            }
        }
    }

    stage('Test App') {
        when {
            expression { env.APP_CACHED != "true" }
        }
        steps {
            sh 'cd app && make test'
        }
    }
}

post {
    success {
        saveTestedHash(env.HASH_APP)
        saveTestedHash(env.HASH_WEB)
    }
}
```

## Contributing

When adding new functions to this library:

1. Create a new `.groovy` file in the `vars/` directory
2. Use the `call()` method for the main function
3. Add comprehensive documentation with examples
4. Make functions configurable via parameters and environment variables
5. Avoid hardcoding project-specific values
6. Update this README with the new function

## License

Internal use only - Irontec
