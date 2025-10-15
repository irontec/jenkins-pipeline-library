#!/usr/bin/env groovy

import org.pipeline.Constants

/**
 * Notifies GitHub of an unstable/cancelled stage
 *
 * Requires GitHub integration plugin and GITHUB_CREDENTIALS env variable
 * Uses STAGE_NAME to identify the context
 *
 * @param contextPrefix String prefix for GitHub context
 *                      (default: env.GITHUB_CONTEXT_PREFIX or "ci")
 * @param description String description (default: "Cancelled")
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
    String description = "Cancelled"
) {
    if (!contextPrefix) {
        contextPrefix = env.GITHUB_CONTEXT_PREFIX ?: "ci"
    }

    githubNotify([
        context: "${contextPrefix}-${STAGE_NAME}",
        description: description,
        status: "ERROR",
        credentialsId: Constants.GITHUB_CREDENTIALS
    ])
}

