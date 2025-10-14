#!/usr/bin/env groovy

/**
 * Validates that a pull request is ready to be merged
 * 
 * Performs various checks including:
 * - Validates target branch
 * - Checks if subtasks are completed
 * - Verifies feature branch is up to date
 * - Validates fix versions
 * 
 * @param site String JIRA site URL 
 *             (default: env.JIRA_SITE or 'irontec.atlassian.net')
 * @param jiraTicket String JIRA ticket identifier 
 *                   (default: env.JIRA_TICKET)
 * @param mainBranch String name of main branch (default: 'main')
 * @param validatedStatus String status name for validated issues 
 *                        (default: 'VALIDATED')
 * @param completedStatus String status name for completed issues 
 *                        (default: 'VALIDATED')
 * @param skipBotBranches String prefix for bot branches to skip 
 *                        (default: 'dependabot')
 * @return boolean true if merge validation passed, 
 *                 false if validation failed
 * 
 * @example
 * stage('mergeability') {
 *     steps {
 *         script {
 *             validateMergeability()
 *         }
 *     }
 * }
 * 
 * @example
 * // With custom configuration
 * validateMergeability(
 *     site: 'mycompany.atlassian.net',
 *     mainBranch: 'master',
 *     validatedStatus: 'Done'
 * )
 */
def call(
    String site = null,
    String jiraTicket = null,
    String mainBranch = 'main',
    String validatedStatus = 'VALIDATED',
    String completedStatus = 'VALIDATED',
    String skipBotBranches = 'dependabot'
) {
    if (!jiraTicket) {
        jiraTicket = env.JIRA_TICKET
    }
    
    if (!site) {
        site = env.JIRA_SITE ?: 'irontec.atlassian.net'
    }
    
    if (!env.CHANGE_TARGET) {
        echo """
            Not a merge request branch. \
            Merge checks not required.
        """
        return true
    }

    if (env.BRANCH_NAME.startsWith(skipBotBranches)) {
        echo """
            Security alarm branch. \
            Merge checks not required.
        """
        return true
    }

    if (!jiraTicket) {
        failure """
            No ticket associated. \
            Can not validate mergeability.
        """
        return false
    }

    def issue = jiraGetIssue(
        site: site,
        idOrKey: jiraTicket
    )

    def isSubtask = issue.data.fields.issuetype.subtask
    
    if (isSubtask) {
        return validateSubtaskMergeability(
            issue,
            jiraTicket,
            mainBranch,
            validatedStatus
        )
    } else {
        return validateTaskMergeability(
            issue,
            jiraTicket,
            mainBranch,
            completedStatus
        )
    }
}

/**
 * Validates mergeability for a subtask
 * 
 * @param issue JIRA issue object
 * @param jiraTicket String JIRA ticket identifier
 * @param mainBranch String name of main branch
 * @param validatedStatus String status name for validated issues
 * @return boolean true if validation passed
 */
private boolean validateSubtaskMergeability(
    def issue,
    String jiraTicket,
    String mainBranch,
    String validatedStatus
) {
    def task = issue.data.fields.parent
    echo "${jiraTicket} is a subtask part of a feature task."

    // Check target branch matches feature branch
    if (!env.CHANGE_TARGET.startsWith(task.key)) {
        unstable """
            Target branch ${env.CHANGE_TARGET} is not a feature \
            branch. Merge will be blocked until all previous tasks \
            are merged
        """
        return false
    }

    // Check feature task is validated
    def status = task.fields.status
    if (status.name != validatedStatus) {
        unstable "Feature not yet validated. Merge is blocked."
        return false
    }

    // Check feature branch is up to date with main
    try {
        sh """
            git merge-base --is-ancestor \
                origin/${mainBranch} origin/${env.CHANGE_TARGET}
        """
    } catch (Exception e) {
        unstable """
            Feature branch ${env.CHANGE_TARGET} is not properly \
            rebased. Merge is blocked.
        """
        return false
    }
    
    return true
}

/**
 * Validates mergeability for a task
 * 
 * @param issue JIRA issue object
 * @param jiraTicket String JIRA ticket identifier
 * @param mainBranch String name of main branch
 * @param completedStatus String status name for completed issues
 * @return boolean true if validation passed
 */
private boolean validateTaskMergeability(
    def issue,
    String jiraTicket,
    String mainBranch,
    String completedStatus
) {
    // Check fix version
    def currentVersion = issue.data.fields.fixVersions[0]
    if (currentVersion && !currentVersion.name.contains('current')) {
        unstable """
            Pull request fixedVersions (${currentVersion.name}) is \
            not current version. Merge is blocked.
        """
        return false
    } else if (currentVersion) {
        echo "Fixed Version: ${currentVersion.name}"
    }

    echo "${jiraTicket} is a task. Checking subtasks..."

    // Check target branch is main
    if (env.CHANGE_TARGET != mainBranch) {
        unstable """
            Target branch ${env.CHANGE_TARGET} is not the main \
            branch.
        """
        return false
    }

    // Check all subtasks are completed
    def subtasks = issue.data.fields.subtasks
    def allCompleted = true
    
    subtasks.each { subtask ->
        def status = subtask.fields.status
        if (status.name != completedStatus) {
            unstable """
                Subtask ${subtask.key} is not completed \
                (Status: ${status.name}).
            """
            allCompleted = false
        }
    }
    
    return allCompleted
}

