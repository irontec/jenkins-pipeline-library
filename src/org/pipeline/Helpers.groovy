package org.pipeline

class Helpers implements Serializable {

    static String getGithubCredentials(def env) {
        return env.GITHUB_CREDENTIALS ?: Constants.GITHUB_CREDENTIALS
    }
}