#!/usr/bin/env groovy

/**
 * Gets the current branch name
 *
 * Returns CHANGE_BRANCH for PRs or GIT_BRANCH otherwise
 *
 * @return String current branch name
 *
 * @example
 * def branch = getBranchName()
 * echo "Building branch: ${branch}"
 */
def call() {
    return env.CHANGE_BRANCH ?: env.GIT_BRANCH
}

