#!/usr/bin/env groovy

import org.pipeline.Helpers

/**
 * Lists the pull requests of the repository being built
 *
 * One call to the GitHub REST API, filtered server side. Requires the
 * HTTP Request and Pipeline Utility Steps plugins and a username/password
 * (or GitHub App) credential readable with withCredentials.
 *
 * @param args.state         String "open", "closed" or "all" (default: "open")
 * @param args.base          String only pull requests targeting this branch
 * @param args.head          String only pull requests whose head is this
 *                           branch of the same repository
 * @param args.repository    String "owner/name" (default: read from
 *                           CHANGE_URL or GIT_URL)
 * @param args.credentialsId String Jenkins credentials (default:
 *                           env.GITHUB_CREDENTIALS or the library constant)
 * @return List pull requests as GitHub returns them (`number`, `base.ref`,
 *         `head.ref`, `title`...), at most 100
 *
 * @example
 * def children = githubListPullRequests(base: env.BRANCH_NAME)
 * echo "stacked on this branch: ${children*.number}"
 */
def call(Map args = [:]) {
    def repository = args.repository ?: Helpers.getGithubRepository(env)
    if (!repository) {
        error 'githubListPullRequests: no GitHub repository in CHANGE_URL or GIT_URL'
    }
    def credentialsId = args.credentialsId ?: Helpers.getGithubCredentials(env)
    def owner = repository.tokenize('/')[0]

    def query = ["state=${args.state ?: 'open'}", 'per_page=100']
    if (args.base) {
        query << "base=${args.base}"
    }
    if (args.head) {
        query << "head=${owner}:${args.head}"
    }

    def content
    withCredentials([usernamePassword(
        credentialsId: credentialsId,
        usernameVariable: 'GITHUB_API_USER',
        passwordVariable: 'GITHUB_API_TOKEN'
    )]) {
        def resp = httpRequest(
            url: "https://api.github.com/repos/${repository}/pulls?${query.join('&')}",
            customHeaders: [
                [name: 'Authorization', value: "Bearer ${env.GITHUB_API_TOKEN}", maskValue: true],
                [name: 'Accept', value: 'application/vnd.github+json'],
                [name: 'X-GitHub-Api-Version', value: '2022-11-28'],
            ],
            validResponseCodes: '200:299'
        )
        content = resp.content
    }
    return readJSON(text: content)
}
