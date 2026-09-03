#!/usr/bin/env groovy

import org.pipeline.Helpers

/**
 * Sends a failure notification to Mattermost
 * 
 * Only sends notification for specified branches (default: main)
 * Requires Mattermost plugin
 * 
 * @param channel String Mattermost channel 
 *                (default: env.MATTERMOST_CHANNEL)
 * @param branch String branch to monitor (default: "main")
 * @param color String message color (default: "#FF0000")
 * @param icon String emoji icon (default: ":red_circle:")
 * 
 * @example
 * // Using environment variable
 * environment {
 *     MATTERMOST_CHANNEL = "#my-project-ci"
 * }
 * post {
 *     failure {
 *         notifyFailureMattermost()
 *     }
 * }
 * 
 * @example
 * // Using explicit parameter
 * post {
 *     failure {
 *         notifyFailureMattermost("#my-channel")
 *     }
 * }
 */
def call(
    String channel = null,
    String branch = "main",
    String color = "#FF0000",
    String icon = ":red_circle:"
) {
    if (!channel) {
        channel = env.MATTERMOST_CHANNEL
    }
    
    if (!channel) {
        echo "No Mattermost channel configured. Skipping notification."
        return
    }
    
    String currentBranch = Helpers.getCurrentBranch(env)

    if (currentBranch != Helpers.normalizeBranch(branch)) {
        echo "Branch ${currentBranch} is not ${branch}. Skipping notification."
        return
    }

    mattermostSend([
        channel: channel,
        color: color,
        message: """
            ${icon} Branch ${currentBranch} tests failed \
            ${icon} - (<${env.BUILD_URL}|Open>)
        """.stripIndent().trim()
    ])
}

