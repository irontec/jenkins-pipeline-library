#!/usr/bin/env groovy

/**
 * Numbers of the open pull requests stacked on the one being built
 *
 * A pull request is stacked on this one when its base is this build's
 * branch. Empty when none is, and on a build that is not a pull request.
 *
 * @return List<Integer> pull request numbers, ascending
 *
 * @example
 * def children = stackedPullRequests()
 * if (children) {
 *     echo "Tests deferred to #${children.join(', #')}"
 * }
 */
def call() {
    if (!env.CHANGE_ID) {
        return []
    }
    return githubListPullRequests(base: env.BRANCH_NAME)
        .collect { it.number as Integer }
        .sort()
}
