#!/usr/bin/env groovy

import org.pipeline.Helpers

/**
 * Lists every artifact of a Harbor repository, with its tags
 *
 * Pages through the Harbor v2 API. Only a 404 (no such repository) is
 * tolerated, as null; a 401/403 fails the step instead of surfacing as an
 * unparseable body a few lines later. Requires the HTTP Request and
 * Pipeline Utility Steps plugins.
 *
 * @param args.project       String Harbor project (default: env.HARBOR_PROJECT)
 * @param args.repo          String repository inside the project
 * @param args.registry      String registry host (default: env.HARBOR_REGISTRY
 *                           or the library constant)
 * @param args.credentialsId String Jenkins credentials (default:
 *                           env.HARBOR_CREDENTIALS_ID or the library constant)
 * @return List artifacts as returned by Harbor (`digest`, `push_time`,
 *         `tags[]`...), or null when the repository does not exist
 *
 * @example
 * def artifacts = harborListArtifacts(project: 'aps', repo: 'aps-app')
 * if (artifacts == null) {
 *     echo 'repo not found'
 * }
 */
def call(Map args) {
    def project = args.project ?: env.HARBOR_PROJECT
    if (!project || !args.repo) {
        error 'harborListArtifacts: project and repo are required'
    }
    def registry = args.registry ?: Helpers.getHarborRegistry(env)
    def credentialsId = args.credentialsId ?: Helpers.getHarborCredentialsId(env)
    def base = "https://${registry}/api/v2.0/projects/${project}/repositories/${args.repo}/artifacts"

    def artifacts = []
    def page = 1
    while (true) {
        def resp = httpRequest(
            url: "${base}?page=${page}&page_size=100&with_tag=true",
            authentication: credentialsId,
            acceptType: 'APPLICATION_JSON',
            validResponseCodes: '200:299,404'
        )
        if (resp.status == 404) {
            return null
        }
        def batch = readJSON(text: resp.content)
        if (batch.isEmpty()) {
            break
        }
        artifacts.addAll(batch)
        if (batch.size() < 100) {
            break
        }
        page++
    }
    return artifacts
}
