#!/usr/bin/env groovy

/**
 * Checks if any commit in the current PR contains a specific tag
 *
 * Searches commit messages between target branch and current commit
 *
 * @param module String tag to search for in commit messages
 * @return boolean true if tag is found in any commit
 *
 * @example
 * if (hasCommitTag("core:")) {
 *     echo "Core changes detected"
 * }
 */
def call(String module) {
    return env.CHANGE_TARGET && sh(
        returnStatus: true,
        script: """
            git log --oneline \
                origin/${env.CHANGE_TARGET}...${env.GIT_COMMIT} | \
                grep ${module}
        """
    ) == 0
}

