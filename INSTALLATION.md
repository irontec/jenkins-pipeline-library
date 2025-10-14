# Installation and Setup Guide

This guide explains how to configure and use the Irontec Jenkins Shared 
Library in your Jenkins environment.

## Prerequisites

### Required Jenkins Plugins

Install the following plugins from **Manage Jenkins** →
**Manage Plugins**:

1. **Pipeline: GitHub Groovy Libraries**
2. **GitHub Branch Source Plugin**
3. **GitHub Plugin**
4. **JIRA Pipeline Steps**
5. **Mattermost Notification Plugin**

### Required Credentials

Configure these credentials in **Manage Jenkins** →
**Manage Credentials**:

1. **GitHub Token**
   - Kind: Secret text or Username with password
   - ID: `github-credentials-id` (or your preference)
   - Scope: Global
   - Required permissions: repo, write:discussion

2. **JIRA API Token**
   - Kind: Username with password
   - Username: Your JIRA email
   - Password: JIRA API token
   - ID: (will be auto-configured by JIRA plugin)

3. **Mattermost Webhook**
   - Configure in Mattermost plugin settings
   - Get webhook URL from Mattermost

## Installation Methods

### Method 1: Global Shared Library (Recommended)

This makes the library available to all pipelines automatically.

1. Navigate to **Manage Jenkins** → **Configure System**

2. Scroll down to **Global Pipeline Libraries** section

3. Click **Add** button

4. Configure the library:
   ```
   Name: irontec-jenkins-library
   Default version: main

   [ ] Load implicitly
   [x] Allow default version to be overridden
   [x] Include @Library changes in job recent changes

   Retrieval method: Modern SCM

   Source Code Management: Git
     Project Repository:
       https://github.com/irontec/jenkins-shared-library.git
     (or your internal Git server URL)

     Credentials: (select your Git credentials if private repo)
   ```

5. Click **Save**

6. In your Jenkinsfile, add at the top:
   ```groovy
   @Library('irontec-jenkins-library') _

   pipeline {
       // ... your pipeline
   }
   ```

### Method 2: Per-Repository Configuration

Configure the library for specific pipelines only.

Add this at the top of your Jenkinsfile:

```groovy
@Library('irontec-jenkins-library@main') _

pipeline {
    // ... your pipeline
}
```

You can specify different versions:
- `@main` - Use main branch
- `@v1.0.0` - Use specific tag
- `@feature-branch` - Use specific branch

### Method 3: Dynamic Loading

Load the library dynamically within a pipeline:

```groovy
pipeline {
    agent any
    
    stages {
        stage('Load Library') {
            steps {
                script {
                    library identifier: 
                        'irontec-jenkins-library@main',
                        retriever: modernSCM([
                            $class: 'GitSCMSource',
                            remote: 
                                'https://github.com/irontec/jenkins-shared-library.git'
                        ])
                }
            }
        }
        
        stage('Use Library') {
            steps {
                script {
                    def ticket = getJiraTicket()
                    echo "Ticket: ${ticket}"
                }
            }
        }
    }
}
```

## Plugin Configuration

### GitHub Plugin

1. Go to **Manage Jenkins** → **Configure System**

2. Find **GitHub** section

3. Add GitHub Server:
   ```
   Name: github.com (or your GitHub Enterprise URL)
   API URL: https://api.github.com (or your GHE API URL)
   Credentials: (select your GitHub credentials)
   [x] Manage hooks
   ```

4. Click **Test connection** to verify

### JIRA Plugin

1. Go to **Manage Jenkins** → **Configure System**

2. Find **JIRA Steps** section

3. Add JIRA Site:
   ```
   Site name: yourcompany.atlassian.net
   URL: https://yourcompany.atlassian.net
   Login Type: Username and Password
   User Name: your-email@yourcompany.com
   Password: (your JIRA API token)
   ```

4. Click **Test Connection** to verify

### Mattermost Plugin

1. Go to **Manage Jenkins** → **Configure System**

2. Find **Mattermost Notifications** section

3. Configure:
   ```
   Endpoint: https://your-mattermost.com/hooks/xxxxx
   Channel: #your-project-notifications (or your channel)
   ```

4. Click **Test Connection** to verify

## Environment Configuration

Create a global properties file or configure environment variables
in your pipeline:

### Option 1: Global Environment Variables

1. Go to **Manage Jenkins** → **Configure System**

2. Find **Global properties** section

3. Check **Environment variables**

4. Add:
   ```
   GITHUB_CREDENTIALS = your-github-credentials-id
   GITHUB_CONTEXT_PREFIX = your-project-ci
   MATTERMOST_CHANNEL = #your-project-notifications
   MAX_HASHES = 100
   ```

### Option 2: Per-Pipeline Configuration

In your Jenkinsfile:

```groovy
pipeline {
    environment {
        GITHUB_CREDENTIALS = 'github-credentials-id'
        GITHUB_CONTEXT_PREFIX = 'my-project'
        MATTERMOST_CHANNEL = '#my-project-ci'
        JIRA_TICKET = getJiraTicket()
        HASH_FILE = 
            "${JENKINS_HOME}/caches/${JOB_NAME}/hashes.txt"
        MAX_HASHES = 100
    }
}
```

## Testing the Installation

Create a test pipeline to verify the library is working:

```groovy
@Library('irontec-jenkins-library') _

pipeline {
    agent any
    
    environment {
        GITHUB_CREDENTIALS = 'your-credentials-id'
        GITHUB_CONTEXT_PREFIX = 'test-project'
    }
    
    stages {
        stage('Test Library Functions') {
            steps {
                script {
                    echo "Testing shared library functions..."
                    
                    // Test branch functions
                    def branch = getBranchName()
                    echo "Current branch: ${branch}"
                    
                    def baseBranch = getBaseBranch()
                    echo "Base branch: ${baseBranch}"
                    
                    def dockerTag = getDockerTag()
                    echo "Docker tag: ${dockerTag}"
                    
                    // Test JIRA function
                    def ticket = getJiraTicket()
                    if (ticket) {
                        echo "JIRA ticket: ${ticket}"
                    } else {
                        echo "No JIRA ticket found in branch name"
                    }
                    
                    echo "Library functions working correctly!"
                }
            }
        }
    }
}
```

## Migrating Existing Pipeline

To migrate your existing pipeline to use the shared library:

1. **Backup your current Jenkinsfile**

2. **Add library import** at the top:
   ```groovy
   @Library('irontec-jenkins-library') _
   ```

3. **Replace function calls** with library functions:

   Before:
   ```groovy
   def getJiraTicket() {
       def matcher = "${env.CHANGE_BRANCH}" =~ /^(?<jira>\w+-\d+)-.*$/
       if (matcher.matches()) {
           return matcher.group("jira")
       }
       return ""
   }

   def ticket = getJiraTicket()
   ```

   After:
   ```groovy
   def ticket = getJiraTicket()
   ```

4. **Remove local function definitions** that are now in the library

5. **Update environment variables** to use library functions:

   Before:
   ```groovy
   environment {
       DOCKER_TAG = env.CHANGE_ID ?: env.GIT_BRANCH
   }
   ```

   After:
   ```groovy
   environment {
       DOCKER_TAG = getDockerTag()
   }
   ```

6. **Test the pipeline** with a test branch

## Troubleshooting

### Library Not Found

**Error**: `Unable to resolve irontec-jenkins-library@main`

**Solution**: 
- Check library name matches configuration
- Verify Git repository URL is accessible
- Check credentials if using private repository

### Function Not Found

**Error**: `No such DSL method 'functionName'`

**Solution**:
- Ensure library is loaded with `@Library('irontec-jenkins-library') _`
- Check function name spelling
- Verify you're using the correct library version

### GitHub Notifications Not Working

**Solution**:
- Verify `GITHUB_CREDENTIALS` environment variable is set
- Check GitHub token has correct permissions
- Ensure GitHub plugin is configured correctly

### JIRA Integration Issues

**Solution**:
- Verify JIRA site configuration
- Check JIRA API token is valid
- Ensure `env.JIRA_TICKET` is set before calling JIRA functions

### Mattermost Notifications Not Sent

**Solution**:
- Verify webhook URL is correct
- Check channel name format (should start with #)
- Ensure Mattermost plugin is installed

## Updating the Library

When changes are made to the shared library:

1. **Using main branch** (development):
   - Changes are immediately available
   - No pipeline changes needed

2. **Using tags** (production):
   ```groovy
   @Library('irontec-jenkins-library@v1.0.0') _
   ```
   - Update version tag in Jenkinsfile
   - Commit and push changes

3. **Testing new versions**:
   ```groovy
   @Library('irontec-jenkins-library@feature-branch') _
   ```
   - Point to feature branch temporarily
   - Switch back to main/tag after testing

## Support

For issues or questions:

1. Check this documentation
2. Review function documentation in README.md
3. Check Jenkins console output for detailed errors
4. Contact DevOps team

## Next Steps

- Review [README.md](README.md) for function documentation
- Check [examples/Jenkinsfile.example](examples/Jenkinsfile.example)
  for usage examples
- Start migrating your pipelines to use the shared library

