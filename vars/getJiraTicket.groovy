#!/usr/bin/env groovy

/**
 * Extracts JIRA ticket identifier from branch name
 * 
 * Expected branch format: TICKET-1234-description
 * 
 * @return String JIRA ticket identifier (e.g., "PROJ-123") or empty string
 * 
 * @example
 * def ticket = getJiraTicket()
 * if (ticket) {
 *     echo "Working on ticket: ${ticket}"
 * }
 */
def call() {
    def matcher = "${env.CHANGE_BRANCH}" =~ /^(?<jira>\w+-\d+)-.*$/
    if (matcher.matches()) {
        return matcher.group("jira")
    } else {
        return ""
    }
}

