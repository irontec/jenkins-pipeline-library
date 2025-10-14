#!/usr/bin/env groovy

/**
 * Checks if a hash has already been tested
 * 
 * Looks up hash in cache file
 * 
 * @param hash String hash to check
 * @param hashFile String path to cache file (uses env.HASH_FILE if not
 *                 provided)
 * @return boolean true if hash has been tested before
 * 
 * @example
 * def hash = getCurrentHash("app")
 * if (isHashTested(hash)) {
 *     echo "Already tested, skipping"
 *     return
 * }
 */
def call(String hash, String hashFile = null) {
    if (!hashFile) {
        hashFile = env.HASH_FILE
    }
    
    if (!hashFile) {
        echo "No hash file specified"
        return false
    }
    
    if (!fileExists(hashFile)) {
        return false
    }
    
    def hashes = readFile(hashFile).split("\n")
    return hashes.contains(hash)
}

