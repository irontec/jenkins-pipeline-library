#!/usr/bin/env groovy

/**
 * Checks if a pull request has a specific label
 *
 * Requires the GitHub Branch Source plugin and pullRequest global variable
 *
 * @param label String label name to check
 * @return boolean true if PR has the label
 *
 * @example
 * if (hasLabel("ci-force-tests")) {
 *     echo "Forced testing enabled"
 * }
 */
def call(String label) {
    return env.CHANGE_ID && pullRequest.labels.contains(label)
}

