#!/usr/bin/env groovy

/**
 * Notifies GitHub of an unstable/cancelled stage
 * 
 * Requires GitHub integration plugin and GITHUB_CREDENTIALS env variable
 * Uses STAGE_NAME to identify the context
 * 
 * @param contextPrefix String prefix for GitHub context 
 *                      (default: env.GITHUB_CONTEXT_PREFIX or "ci")
 * @param description String description (default: "Cancelled")
 * @param credentialsId String credentials ID 
 *                      (default: env.GITHUB_CREDENTIALS)
 * 
 * @example
 * stage('tests') {
 *     steps {
 *         sh 'make test'
 *     }
 *     post {
 *         unstable { notifyUnstableGithub() }
 *     }
 * }
 */
def call(
    String contextPrefix = null,
    String description = "Cancelled",
    String credentialsId = null
) {
    if (!contextPrefix) {
        contextPrefix = env.GITHUB_CONTEXT_PREFIX ?: "ci"
    }
    
    if (!credentialsId) {
        credentialsId = env.GITHUB_CREDENTIALS
    }
    
    githubNotify([
        context: "${contextPrefix}-${STAGE_NAME}",
        description: description,
        status: "ERROR",
        credentialsId: credentialsId
    ])
}

