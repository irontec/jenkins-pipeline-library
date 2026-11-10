#!/usr/bin/env groovy

/**
 * Number of the open pull request this one is stacked on
 *
 * This pull request is stacked when its target branch is the head of
 * another open pull request. Null when the target is a plain branch,
 * and on a build that is not a pull request.
 *
 * @return Integer pull request number, or null
 *
 * @example
 * def parent = stackParentPullRequest()
 * if (parent) {
 *     echo "stacked on #${parent}"
 * }
 */
def call() {
    if (!env.CHANGE_TARGET) {
        return null
    }
    def parents = githubListPullRequests(head: env.CHANGE_TARGET)
    return parents ? parents[0].number as Integer : null
}
