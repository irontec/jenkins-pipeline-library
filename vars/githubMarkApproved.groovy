#!/usr/bin/env groovy

import org.pipeline.Helpers

/**
 * Marks a GitHub pull request as approved
 *
 * Only approves if the bot previously requested changes
 * Requires GitHub Branch Source plugin
 *
 * @param botUsername String username of the bot (default:
 *                    env.GITHUB_BOT_USERNAME or "ironArt3mis")
 *
 * @example
 * post {
 *     success {
 *         githubMarkApproved()
 *     }
 * }
 */
def call(String botUsername = null) {
    if (!botUsername) {
        botUsername = Helpers.getBotUsername(env)
    }

    if (env.CHANGE_ID) {
        // Check last status of bot review
        def lastFuncReviewStatus
        for (review in pullRequest.reviews) {
            if (review.user == botUsername) {
                lastFuncReviewStatus = review.state
            }
        }

        // If PR was previously rejected approve it
        if (lastFuncReviewStatus == "CHANGES_REQUESTED") {
            pullRequest.review('APPROVE')
        }
    }
}

