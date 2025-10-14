#!/usr/bin/env groovy

/**
 * Calculates a hash of all files in a directory
 * 
 * Creates a deterministic hash by:
 * 1. Calculating SHA256 of each file
 * 2. Sorting the results
 * 3. Calculating SHA256 of the combined result
 * 
 * Excludes .git directory
 * 
 * @param dir String directory path to hash
 * @return String SHA256 hash of directory contents
 * 
 * @example
 * def backHash = getCurrentHash("app")
 * def frontHash = getCurrentHash("web")
 * echo "Backend hash: ${backHash}"
 */
def call(String dir) {
    return sh(
        script: """
            find ${dir} -type f -not -path './.git/*' \
                -exec sha256sum {} + | \
                sort | \
                sha256sum | \
                awk '{print \$1}'
        """,
        returnStdout: true
    ).trim()
}

