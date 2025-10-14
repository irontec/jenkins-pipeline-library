#!/usr/bin/env groovy

/**
 * Gets the base branch for the current build
 *
 * Returns CHANGE_TARGET for PRs or GIT_BRANCH otherwise
 *
 * @return String base branch name
 *
 * @example
 * def base = getBaseBranch()
 * echo "Comparing against: ${base}"
 */
def call() {
    return env.CHANGE_TARGET ?: env.GIT_BRANCH
}

