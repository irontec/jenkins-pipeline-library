#!/usr/bin/env groovy

/**
 * Updates JIRA issue custom fields with PR and branch information
 *
 * Updates:
 *   - customfield_10165: Pull Request
 *   - customfield_10166: Branch name
 *
 * Requires JIRA plugin and environment variables:
 *   - JIRA_TICKET: JIRA issue key
 *   - JOB_BASE_NAME: Jenkins job base name
 *   - CHANGE_BRANCH: Branch name
 *
 * @param site String JIRA site URL (default: 'irontec.atlassian.net')
 * @param jiraTicket String JIRA ticket identifier (uses env.JIRA_TICKET
 *                   if not provided)
 *
 * @example
 * def ticket = getJiraTicket()
 * if (ticket) {
 *     env.JIRA_TICKET = ticket
 *     jiraUpdateCustomFields()
 * }
 */
def call(
    String site = 'irontec.atlassian.net',
    String jiraTicket = null
) {
    if (!jiraTicket) {
        jiraTicket = env.JIRA_TICKET
    }

    if (!jiraTicket) {
        echo "No JIRA ticket provided"
        return
    }

    // customfield_10165 - Pull Request
    // customfield_10166 - Branch
    def fields = [
        fields: [
            customfield_10165: env.JOB_BASE_NAME,
            customfield_10166: env.CHANGE_BRANCH,
        ]
    ]

    jiraEditIssue(
        site: site,
        idOrKey: jiraTicket,
        issue: fields
    )
}

