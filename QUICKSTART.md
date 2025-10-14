# Quick Start Guide

Get up and running with the Irontec Jenkins Shared Library in 5 minutes!

## Step 1: Configure Jenkins (One-time setup)

### Add the Shared Library

1. Go to **Jenkins** → **Manage Jenkins** → **Configure System**
2. Scroll to **Global Pipeline Libraries**
3. Click **Add**
4. Fill in:
   - Name: `irontec-jenkins-library`
   - Default version: `main`
   - Under **Retrieval method**, select **Modern SCM**
   - Select **Git**
   - Project Repository: `https://github.com/irontec/jenkins-shared-library.git`
5. Click **Save**

### Set Environment Variables (Optional)

1. In the same page, find **Global properties**
2. Check **Environment variables**
3. Add commonly used variables:
   - Name: `GITHUB_CREDENTIALS`
   - Value: `your-github-credentials-id`

## Step 2: Update Your Jenkinsfile

### Before (with local functions):

```groovy
pipeline {
    environment {
        DOCKER_TAG = env.CHANGE_ID ?: env.GIT_BRANCH
        JIRA_TICKET = getJiraTicket()
    }
    
    stages {
        stage('test') {
            steps {
                sh 'make test'
            }
            post {
                success { 
                    githubNotify([
                        context: "testing-${STAGE_NAME}",
                        status: "SUCCESS"
                    ])
                }
            }
        }
    }
}

// Local function definitions
def getJiraTicket() {
    def matcher = "${env.CHANGE_BRANCH}" =~ /^(?<jira>\w+-\d+)-.*$/
    if (matcher.matches()) {
        return matcher.group("jira")
    }
    return ""
}
```

### After (using shared library):

```groovy
@Library('irontec-jenkins-library') _

pipeline {
    environment {
        GITHUB_CREDENTIALS = 'github-creds'
        GITHUB_CONTEXT_PREFIX = 'my-project'  // Optional
        DOCKER_TAG = getDockerTag()
        JIRA_TICKET = getJiraTicket()
    }
    
    stages {
        stage('test') {
            steps {
                sh 'make test'
            }
            post {
                success { notifySuccessGithub() }
            }
        }
    }
}

// No local functions needed! 🎉
```

## Step 3: Configure for Your Project

Set project-specific values in your pipeline:

```groovy
@Library('irontec-jenkins-library') _

pipeline {
    agent any
    
    environment {
        // GitHub Integration
        GITHUB_CREDENTIALS = 'your-github-credentials-id'
        GITHUB_CONTEXT_PREFIX = 'myproject-ci'
        
        // Mattermost (optional)
        MATTERMOST_CHANNEL = '#myproject-notifications'
        
        // Hash caching (optional)
        HASH_FILE = "${JENKINS_HOME}/caches/${JOB_NAME}/hashes.txt"
    }
    
    stages {
        stage('Build') {
            steps {
                sh 'make build'
            }
        }
    }
}
```

## Step 4: Common Usage Patterns

### Pattern 1: Basic PR Workflow

```groovy
@Library('irontec-jenkins-library') _

pipeline {
    agent any
    
    environment {
        GITHUB_CREDENTIALS = 'github-creds'
        GITHUB_CONTEXT_PREFIX = 'myproject'
        JIRA_TICKET = getJiraTicket()
    }
    
    stages {
        stage('Update PR') {
            when { changeRequest() }
            steps {
                script {
                    githubUpdatePullRequestTitle()
                    jiraUpdateCustomFields('mycompany.atlassian.net')
                }
            }
        }
        
        stage('Test') {
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
        success { githubMarkApproved() }
        unstable { githubMarkChangesRequested() }
    }
}
```

### Pattern 2: Conditional Testing with Caching

```groovy
@Library('irontec-jenkins-library') _

pipeline {
    agent any
    
    environment {
        HASH_APP = getCurrentHash("app")
        HASH_FILE = "${JENKINS_HOME}/caches/${JOB_NAME}/hashes.txt"
    }
    
    stages {
        stage('Check Cache') {
            steps {
                script {
                    env.IS_CACHED = isHashTested(env.HASH_APP)
                }
            }
        }
        
        stage('Test') {
            when {
                expression { env.IS_CACHED != "true" }
            }
            steps {
                sh 'make test'
            }
        }
    }
    
    post {
        success {
            saveTestedHash(env.HASH_APP)
        }
    }
}
```

### Pattern 3: Selective Testing Based on Labels

```groovy
@Library('irontec-jenkins-library') _

pipeline {
    agent any
    
    stages {
        stage('Backend Tests') {
            when {
                anyOf {
                    expression { hasLabel("test-backend") }
                    expression { hasCommitTag("backend:") }
                    branch "main"
                }
            }
            steps {
                sh 'make test-backend'
            }
        }
        
        stage('Frontend Tests') {
            when {
                anyOf {
                    expression { hasLabel("test-frontend") }
                    expression { hasCommitTag("frontend:") }
                    branch "main"
                }
            }
            steps {
                sh 'make test-frontend'
            }
        }
    }
}
```

### Pattern 4: Multi-channel Notifications

```groovy
@Library('irontec-jenkins-library') _

pipeline {
    agent any
    
    environment {
        GITHUB_CREDENTIALS = 'github-creds'
        GITHUB_CONTEXT_PREFIX = 'myproject'
        MATTERMOST_CHANNEL = '#myproject-ci'
    }
    
    stages {
        stage('Deploy') {
            steps {
                sh 'make deploy'
            }
        }
    }
    
    post {
        failure {
            notifyFailureGithub()
            notifyFailureMattermost()
        }
        fixed {
            notifyFixedMattermost()
        }
    }
}
```

## Most Used Functions

### Get Information
- `getJiraTicket()` - Extract ticket from branch name
- `getBranchName()` - Get current branch
- `getDockerTag()` - Get Docker tag for build

### Check Conditions
- `hasLabel("label")` - Check PR label
- `hasCommitTag("tag:")` - Check commit messages
- `isHashTested(hash)` - Check if already tested

### Notifications
- `notifySuccessGithub()` - GitHub success
- `notifyFailureGithub()` - GitHub failure
- `notifyFailureMattermost()` - Mattermost failure

### PR Management
- `githubMarkApproved()` - Approve PR
- `githubUpdatePullRequestTitle()` - Update PR title
- `jiraUpdateCustomFields()` - Update JIRA

## Testing Your Changes

Create a test branch and pipeline:

```groovy
@Library('irontec-jenkins-library@your-feature-branch') _

pipeline {
    agent any
    
    environment {
        GITHUB_CONTEXT_PREFIX = 'test'
    }
    
    stages {
        stage('Test') {
            steps {
                script {
                    echo "Branch: ${getBranchName()}"
                    echo "Ticket: ${getJiraTicket()}"
                    echo "Docker tag: ${getDockerTag()}"
                }
            }
        }
    }
}
```

## Project-Specific Configuration

### For APS Project

```groovy
environment {
    GITHUB_CONTEXT_PREFIX = 'aps-testing'
    MATTERMOST_CHANNEL = '#comms-aps'
}
```

### For Other Projects

```groovy
environment {
    GITHUB_CONTEXT_PREFIX = 'your-project-ci'
    MATTERMOST_CHANNEL = '#your-project-notifications'
}
```

## Troubleshooting

### "Library not found"
- Check the library name in `@Library('...')` matches Jenkins config
- Verify you have network access to the Git repository

### "Function not defined"
- Ensure `@Library('...')` is at the very top of Jenkinsfile
- Don't forget the underscore: `@Library('...') _`

### "GitHub notifications not working"
- Set `GITHUB_CREDENTIALS` environment variable
- Verify credentials in Jenkins
- Check `GITHUB_CONTEXT_PREFIX` is set (or accepts default "ci")

### "Mattermost notifications not sent"
- Set `MATTERMOST_CHANNEL` environment variable
- If not set, notifications are silently skipped (by design)

## Next Steps

1. ✅ Start with a simple test pipeline
2. 📖 Read the [full documentation](README.md)
3. 🔧 Check [INSTALLATION.md](INSTALLATION.md) for detailed setup
4. 📝 See [examples/Jenkinsfile.example](examples/Jenkinsfile.example)

## Getting Help

Run this to see all available functions:
```bash
./list-functions.sh
```

Or check the documentation:
- [README.md](README.md) - Complete function reference
- [INSTALLATION.md](INSTALLATION.md) - Detailed setup guide
- [examples/](examples/) - Example pipelines
