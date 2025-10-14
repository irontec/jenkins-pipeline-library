#!/usr/bin/env groovy

/**
 * Saves a tested hash to the cache file
 *
 * Maintains a rolling cache of tested hashes
 * Removes oldest entries when maximum is reached
 *
 * @param hash String hash to save
 * @param hashFile String path to cache file (uses env.HASH_FILE if not
 *                 provided)
 * @param maxHashes Integer maximum number of hashes to store
 *                  (uses env.MAX_HASHES if not provided, default: 100)
 *
 * @example
 * def hash = getCurrentHash("app")
 * // Run tests...
 * saveTestedHash(hash)
 */
def call(
    String hash,
    String hashFile = null,
    Integer maxHashes = null
) {
    if (!hashFile) {
        hashFile = env.HASH_FILE
    }

    if (!maxHashes) {
        maxHashes = env.MAX_HASHES?.toInteger() ?: 100
    }

    if (!hashFile) {
        echo "No hash file specified"
        return
    }

    if (isHashTested(hash, hashFile)) {
        echo "Hash ${hash} already saved in cache file."
        return
    }

    def hashes = fileExists(hashFile) ?
        readFile(hashFile).split("\n") as List : []

    if (hashes.size() >= maxHashes) {
        hashes.remove(0)
    }

    hashes.add(hash)

    writeFile(file: hashFile, text: hashes.join("\n"))
    echo """
        Saved new tested hash: ${hash}. \
        Total hashes stored: ${hashes.size()}
    """.stripIndent().trim()
}

