#!/usr/bin/env groovy

/**
 * Marks a GitHub pull request as approved
 *
 * Only approves if the bot previously requested changes
 * Requires GitHub Branch Source plugin
 *
 * @param botUsername String username of the bot (default: "ironArt3mis")
 *
 * @example
 * post {
 *     success {
 *         githubMarkApproved()
 *     }
 * }
 */
def call(String botUsername = "ironArt3mis") {
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

