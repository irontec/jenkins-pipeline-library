package jenkins.model

/**
 * Stand-in so the scripts from Harbor.versionChoicesScript() compile
 * outside Jenkins.
 */
class Jenkins {
    static Jenkins get() {
        return new Jenkins()
    }
}
