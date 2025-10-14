#!/usr/bin/env groovy

/**
 * Marks a GitHub pull request as requiring changes
 *
 * Only marks once to avoid duplicate reviews
 * Requires GitHub Branch Source plugin
 *
 * @param botUsername String username of the bot (default: "ironArt3mis")
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
    String botUsername = "ironArt3mis",
    String message = "This PR is not ready to merge."
) {
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

