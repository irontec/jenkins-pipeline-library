package org.pipeline

class Helpers implements Serializable {

    static String getGithubCredentials(def env) {
        return env.GITHUB_CREDENTIALS ?: Constants.GITHUB_CREDENTIALS
    }

    static String getBotUsername(def env) {
        return env.GITHUB_BOT_USERNAME ?: Constants.GITHUB_BOT_USERNAME
    }

    /**
     * Returns the branch being built, without the remote or ref prefixes
     * the git plugin may add.
     *
     * BRANCH_NAME is set by multibranch jobs and is preferred; GIT_BRANCH
     * is the git plugin's own variable and, depending on the checkout
     * refspec, carries values such as "origin/main" or
     * "refs/heads/main". Comparing those raw against a plain branch name
     * silently never matches.
     */
    static String getCurrentBranch(def env) {
        return normalizeBranch(env.BRANCH_NAME ?: env.GIT_BRANCH)
    }

    /**
     * Strips the "refs/heads/" and "origin/" prefixes from a branch name.
     *
     * Only those two prefixes are removed, so branch names that legitimately
     * contain slashes (e.g. "feature/login") are left untouched.
     */
    static String normalizeBranch(String branch) {
        if (!branch) {
            return branch
        }

        String name = branch

        if (name.startsWith('refs/heads/')) {
            name = name.substring('refs/heads/'.length())
        }

        if (name.startsWith('origin/')) {
            name = name.substring('origin/'.length())
        }

        return name
    }

    /**
     * Returns the GitHub repository being built as "owner/name".
     *
     * Read from CHANGE_URL on a pull request build
     * (https://github.com/owner/name/pull/N) and from GIT_URL otherwise,
     * in either its https or its ssh spelling, with or without ".git".
     * Null when neither names a GitHub repository.
     */
    static String getGithubRepository(def env) {
        for (String url : [env.CHANGE_URL, env.GIT_URL]) {
            if (!url) {
                continue
            }
            def m = url =~ /github\.com[\/:]([^\/]+)\/([^\/]+?)(?:\.git)?(?:\/.*)?$/
            if (m.find()) {
                return "${m.group(1)}/${m.group(2)}".toString()
            }
        }
        return null
    }

    static String getHarborRegistry(def env) {
        return env.HARBOR_REGISTRY ?: Constants.HARBOR_REGISTRY
    }

    static String getHarborCredentialsId(def env) {
        return env.HARBOR_CREDENTIALS_ID ?: Constants.HARBOR_CREDENTIALS_ID
    }
}
