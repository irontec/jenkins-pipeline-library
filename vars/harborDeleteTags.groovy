#!/usr/bin/env groovy

import org.pipeline.Helpers

/**
 * Deletes tags from a Harbor repository
 *
 * Building block of harborDeleteTagsByPrefix and harborPruneTags. Each
 * target is a `[digest:, tag:]` map. Only tags are deleted, never
 * artifacts: an artifact left with no tag is reclaimed by Harbor's
 * scheduled garbage collection. A 404 (already gone, another run got there
 * first) is skipped; anything else, 401/403 included, fails the step: a
 * delete that did not happen must not report success.
 *
 * @param args.project       String Harbor project (default: env.HARBOR_PROJECT)
 * @param args.repo          String repository inside the project
 * @param args.targets       List of [digest: String, tag: String]
 * @param args.registry      String registry host (default: env.HARBOR_REGISTRY
 *                           or the library constant)
 * @param args.credentialsId String Jenkins credentials (default:
 *                           env.HARBOR_CREDENTIALS_ID or the library constant)
 * @return int number of targets actually deleted (2xx responses)
 *
 * @example
 * harborDeleteTags(project: 'aps', repo: 'aps-app',
 *     targets: [[digest: 'sha256:...', tag: 'pr-12-abc1234']])
 */
def call(Map args) {
    def project = args.project ?: env.HARBOR_PROJECT
    if (!project || !args.repo) {
        error 'harborDeleteTags: project and repo are required'
    }
    def registry = args.registry ?: Helpers.getHarborRegistry(env)
    def credentialsId = args.credentialsId ?: Helpers.getHarborCredentialsId(env)
    def base = "https://${registry}/api/v2.0/projects/${project}/repositories/${args.repo}/artifacts"

    int deleted = 0
    for (target in (args.targets ?: [])) {
        echo "deleting ${args.repo}:${target.tag}"
        def resp = httpRequest(
            url: "${base}/${target.digest}/tags/${target.tag}",
            httpMode: 'DELETE',
            authentication: credentialsId,
            validResponseCodes: '200:299,404'
        )
        if (resp.status < 300) {
            deleted++
        }
    }
    return deleted
}
