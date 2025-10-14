#!/usr/bin/env groovy

/**
 * Gets the Docker tag for the current build
 *
 * Returns CHANGE_ID for PRs or GIT_BRANCH otherwise
 *
 * @return String Docker tag to use
 *
 * @example
 * def tag = getDockerTag()
 * docker.build("myapp:${tag}", ".")
 */
def call() {
    return env.CHANGE_ID ?: env.GIT_BRANCH
}

