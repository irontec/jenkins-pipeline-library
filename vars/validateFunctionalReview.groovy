#!/usr/bin/env groovy

/**
 * Validates that a JIRA issue has passed functional review
 *
 * Checks if an issue has a functional reviewer assigned and if so,
 * verifies that the issue has been validated.
 *
 * @param site String JIRA site URL
 *             (default: env.JIRA_SITE or 'irontec.atlassian.net')
 * @param jiraTicket String JIRA ticket identifier
 *                   (default: env.JIRA_TICKET)
 * @param functionalReviewerField String custom field ID for functional
 *                                reviewer (default: 'customfield_10168')
 * @param validatedStatus String status name for validated issues
 *                        (default: 'VALIDATED')
 * @return boolean true if validation passed or not required,
 *                 false if validation failed
 *
 * @example
 * stage('functional') {
 *     steps {
 *         script {
 *             validateFunctionalReview()
 *         }
 *     }
 * }
 *
 * @example
 * // With custom configuration
 * validateFunctionalReview(
 *     site: 'mycompany.atlassian.net',
 *     validatedStatus: 'Done'
 * )
 */
def call(
    String site = null,
    String jiraTicket = null,
    String functionalReviewerField = 'customfield_10168',
    String validatedStatus = 'VALIDATED'
) {
    if (!jiraTicket) {
        jiraTicket = env.JIRA_TICKET
    }

    if (!site) {
        site = env.JIRA_SITE ?: 'irontec.atlassian.net'
    }

    if (!jiraTicket) {
        echo "No ticket associated."
        return true
    }

    if (!env.CHANGE_ID) {
        echo "Not a Pull request."
        return true
    }

    def issue = jiraGetIssue(
        site: site,
        idOrKey: jiraTicket
    )

    // Check for functional reviewer
    def functionalReviewer =
        issue.data.fields."${functionalReviewerField}"

    if (functionalReviewer) {
        println "Functional Reviewer: ${functionalReviewer.displayName}"
    } else {
        println "No functional reviewer assigned."
        return true
    }

    // Check validation status
    def status = issue.data.fields.status
    println "Issue Status: ${status.name} (${status.id})"

    // For Issues with Functional reviewer
    if (functionalReviewer) {
        // Not validated
        if (status.name != validatedStatus) {
            unstable """
                Changes not yet validated. \
                Functional review required.
            """
            return false
        }
    }

    return true
}

