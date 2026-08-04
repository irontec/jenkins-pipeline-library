#!/usr/bin/env groovy

import org.pipeline.Helpers

/**
 * Marks a GitHub pull request as requiring changes
 *
 * Only marks once to avoid duplicate reviews
 * Requires GitHub Branch Source plugin
 *
 * @param botUsername String username of the bot (default:
 *                    env.GITHUB_BOT_USERNAME or "ironArt3mis")
 * @param message String review message (default: "This PR is not ready
 *                to merge.")
 *
 * @example
 * post {
 *     unstable {
 *         githubMarkChangesRequested()
 *     }
 * }
 */
def call(
    String botUsername = null,
    String message = "This PR is not ready to merge."
) {
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

        // PR already marked as review requested
        if (lastFuncReviewStatus == "CHANGES_REQUESTED") {
            echo "This PR is already marked as not ready to merge"
            return
        }

        pullRequest.review('REQUEST_CHANGES', message)
    }
}

