package com.cloudbees.plugins.credentials

/**
 * Stand-in so the scripts from Harbor.versionChoicesScript() compile
 * outside Jenkins. Finds nothing.
 */
class CredentialsProvider {
    static List lookupCredentials(Class type, Object context, Object auth, Object requirements) {
        return []
    }
}
