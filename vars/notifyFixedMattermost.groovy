#!/usr/bin/env groovy

/**
 * Sends a fixed/recovered notification to Mattermost
 * 
 * Only sends notification for specified branches (default: main)
 * Requires Mattermost plugin
 * 
 * @param channel String Mattermost channel 
 *                (default: env.MATTERMOST_CHANNEL)
 * @param branch String branch to monitor (default: "main")
 * @param color String message color (default: "#008000")
 * @param icon String emoji icon (default: ":thumbsup_all:")
 * 
 * @example
 * // Using environment variable
 * environment {
 *     MATTERMOST_CHANNEL = "#my-project-ci"
 * }
 * post {
 *     fixed {
 *         notifyFixedMattermost()
 *     }
 * }
 * 
 * @example
 * // Using explicit parameter
 * post {
 *     fixed {
 *         notifyFixedMattermost("#my-channel")
 *     }
 * }
 */
def call(
    String channel = null,
    String branch = "main",
    String color = "#008000",
    String icon = ":thumbsup_all:"
) {
    if (!channel) {
        channel = env.MATTERMOST_CHANNEL
    }
    
    if (!channel) {
        echo "No Mattermost channel configured. Skipping notification."
        return
    }
    
    if (env.GIT_BRANCH == branch) {
        mattermostSend([
            channel: channel,
            color: color,
            message: """
                ${icon} Branch ${env.GIT_BRANCH} tests fixed \
                ${icon} - (<${env.BUILD_URL}|Open>)
            """.stripIndent().trim()
        ])
    }
}

