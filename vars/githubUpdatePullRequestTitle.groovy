#!/usr/bin/env groovy

/**
 * Updates GitHub pull request title to include JIRA ticket
 *
 * Prepends [JIRA-TICKET] to title if not already present
 * Requires GitHub Branch Source plugin
 *
 * @param jiraTicket String JIRA ticket identifier (if not provided,
 *                   uses env.JIRA_TICKET)
 *
 * @example
 * def ticket = getJiraTicket()
 * if (ticket) {
 *     githubUpdatePullRequestTitle(ticket)
 * }
 */
def call(String jiraTicket = null) {
    if (!jiraTicket) {
        jiraTicket = env.JIRA_TICKET
    }

    if (!jiraTicket) {
        echo "No JIRA ticket provided"
        return
    }

    def title = pullRequest.title
    if (!title.startsWith("[${jiraTicket}]")) {
        pullRequest.title = "[${jiraTicket}] " + title
    }
}

