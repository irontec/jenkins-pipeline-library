package org.pipeline

class Helpers implements Serializable {

    static String getGithubCredentials(def env) {
        return env.GITHUB_CREDENTIALS ?: Constants.GITHUB_CREDENTIALS
    }

    static String getBotUsername(def env) {
        return env.GITHUB_BOT_USERNAME ?: Constants.GITHUB_BOT_USERNAME
    }
}