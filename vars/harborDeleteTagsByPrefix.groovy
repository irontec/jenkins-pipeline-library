#!/usr/bin/env groovy

import org.pipeline.Harbor

/**
 * Deletes every tag starting with a prefix from Harbor repositories
 *
 * Tags, not artifacts: Harbor keys artifacts by digest, so an image that
 * came out byte-identical to another (another PR's preview, main, a
 * release) shares one artifact with it, and deleting the artifact would
 * take the other tags too. An artifact left with no tag is reclaimed by
 * Harbor's garbage collection ("Delete untagged artifacts" must be on).
 *
 * Idempotent: a missing repository is skipped, a prefix with no tags is
 * a no-op. Typical use is tearing down `pr-<n>-*` preview images.
 *
 * @param args.project       String Harbor project (default: env.HARBOR_PROJECT)
 * @param args.repos         List of repositories inside the project
 * @param args.prefix        String tag prefix, e.g. "pr-12-"
 * @param args.registry      String registry host (default: env.HARBOR_REGISTRY
 *                           or the library constant)
 * @param args.credentialsId String Jenkins credentials (default:
 *                           env.HARBOR_CREDENTIALS_ID or the library constant)
 * @return int number of tags deleted
 *
 * @example
 * def deleted = harborDeleteTagsByPrefix(
 *     project: 'aps',
 *     repos: ['aps-app', 'aps-web'],
 *     prefix: "pr-${env.PR_NUMBER}-"
 * )
 */
def call(Map args) {
    if (!args.prefix || !args.repos) {
        error 'harborDeleteTagsByPrefix: repos and prefix are required'
    }

    int deleted = 0
    for (repo in args.repos) {
        echo "=== ${args.project ?: env.HARBOR_PROJECT}/${repo} ==="
        def artifacts = harborListArtifacts(args + [repo: repo])
        if (artifacts == null) {
            echo 'repo not found, skipping'
            continue
        }
        deleted += harborDeleteTags(args + [repo: repo, targets: Harbor.prefixedTags(artifacts, args.prefix)])
    }
    echo "Deleted ${deleted} tag(s) matching ${args.prefix}*"
    return deleted
}
