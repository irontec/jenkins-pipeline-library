#!/usr/bin/env groovy

import org.pipeline.Harbor

/**
 * Keeps only the newest N `<prefix><commit>` tags of Harbor repositories
 *
 * Housekeeping for jobs that push one tag per commit plus a moving tag
 * (`main-<commit>` and `main`). Every `<prefix>*` tag counts, including
 * one sharing its digest with a preview, a release or `latest`; those
 * other tags are never touched, and neither is the newest `<prefix>*` tag
 * on the digest the moving tag points at. Deletes tags, not artifacts, so two
 * commits sharing a byte-identical image do not take each other down;
 * retention is counted over tags and ordered by the push time of the tag
 * itself.
 *
 * Requires "Delete untagged artifacts" in Harbor's garbage collection, or
 * the digests left without tags accumulate.
 *
 * @param args.project       String Harbor project (default: env.HARBOR_PROJECT)
 * @param args.repos         List of repositories inside the project
 * @param args.keep          int newest `<prefix>*` tags to keep per repository
 * @param args.prefix        String per-commit tag prefix (default: "main-")
 * @param args.moving        String moving tag name (default: "main")
 * @param args.registry      String registry host (default: env.HARBOR_REGISTRY
 *                           or the library constant)
 * @param args.credentialsId String Jenkins credentials (default:
 *                           env.HARBOR_CREDENTIALS_ID or the library constant)
 * @return int number of tags deleted
 *
 * @example
 * harborPruneTags(project: 'aps', repos: ['aps-app', 'aps-web'], keep: 3)
 */
def call(Map args) {
    if (args.keep == null || !args.repos) {
        error 'harborPruneTags: repos and keep are required'
    }
    int keep = args.keep as int
    String prefix = args.prefix ?: 'main-'
    String moving = args.moving ?: 'main'

    int deleted = 0
    for (repo in args.repos) {
        echo "=== ${args.project ?: env.HARBOR_PROJECT}/${repo} (keeping newest ${keep} ${prefix}* tags) ==="
        def artifacts = harborListArtifacts(args + [repo: repo])
        if (artifacts == null) {
            echo 'repo not found, skipping'
            continue
        }
        deleted += harborDeleteTags(args + [repo: repo, targets: Harbor.staleTags(artifacts, prefix, moving, keep)])
    }
    echo "Deleted ${deleted} stale ${prefix}* tag(s)"
    return deleted
}
