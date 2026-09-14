package org.pipeline

import com.cloudbees.groovy.cps.NonCPS

/**
 * Pure selection logic for the harbor* steps. No pipeline steps in here:
 * everything is @NonCPS so it runs as plain Groovy, needs nothing from
 * In-process Script Approval, and can be exercised outside Jenkins
 * (see test/HarborTest.groovy).
 *
 * Harbor keys artifacts by digest. Two byte-identical images (two commits
 * with an identical tree, a preview equal to main, a release equal to a
 * preview) are ONE artifact carrying several tags. The steps therefore
 * delete tags, never artifacts, unless an artifact carries no tag at all.
 */
class Harbor implements Serializable {

    /**
     * Tags whose name starts with prefix, as [digest:, tag:] entries.
     */
    @NonCPS
    static List prefixedTags(artifacts, String prefix) {
        def found = []
        for (int i = 0; i < artifacts.size(); i++) {
            def artifact = artifacts.get(i)
            def tags = artifact.tags
            if (!(tags instanceof List)) {
                continue
            }
            for (int t = 0; t < tags.size(); t++) {
                def name = tags.get(t).name
                String tag = name == null ? '' : name.toString()
                if (tag.startsWith(prefix)) {
                    found.add([digest: artifact.digest.toString(), tag: tag])
                }
            }
        }
        return found
    }

    /**
     * Stale `<prefix><commit>` tags, as [digest:, tag:] entries. A tag of
     * null means the artifact carries no tag at all and is to be deleted
     * whole.
     *
     * Candidates are artifacts whose every tag is `<moving>` or
     * `<prefix>*`; anything carrying another tag (a preview, a release,
     * `latest`) is left alone. Of the candidate tags the newest `keep`
     * survive, and so does every tag on the digest `<moving>` points at.
     *
     * Retention counts tags and orders them by the push time of the tag
     * itself, not of the artifact: the artifact keeps the push time of the
     * first commit that produced that digest, so a later commit hanging its
     * tag off the same artifact would otherwise read as old and be pruned
     * right after being published.
     *
     * Untagged artifacts appear when a rebuild of the same commit re-pushes
     * its tag onto a new digest and leaves the previous one behind. Having no
     * tags, they are shared with nothing, so deleting them whole is safe.
     */
    @NonCPS
    static List staleTags(artifacts, String prefix, String moving, int keep) {
        def stale = []
        // [digest, tag, time] of every `<prefix>*` tag, newest first.
        def candidates = []
        String movingDigest = null

        for (int i = 0; i < artifacts.size(); i++) {
            def artifact = artifacts.get(i)
            String digest = artifact.digest.toString()
            def tags = artifact.tags
            if (!(tags instanceof List) || tags.isEmpty()) {
                stale.add([digest: digest, tag: null])
                continue
            }
            boolean onlyOurs = true
            for (int t = 0; t < tags.size(); t++) {
                def name = tags.get(t).name
                String tag = name == null ? '' : name.toString()
                if (tag != moving && !tag.startsWith(prefix)) {
                    onlyOurs = false
                    break
                }
            }
            if (!onlyOurs) {
                continue
            }
            // The artifact's push time is the fallback ordering key: Harbor
            // does fill in the tag's own, but an absent one must not read as
            // the oldest tag in the repository and be pruned on the spot.
            String fallback = artifact.push_time == null ? '' : artifact.push_time.toString()
            for (int t = 0; t < tags.size(); t++) {
                def entry = tags.get(t)
                String tag = entry.name == null ? '' : entry.name.toString()
                if (tag == moving) {
                    movingDigest = digest
                    continue
                }
                String time = entry.push_time == null ? fallback : entry.push_time.toString()
                // Insertion sort, newest first. The times are ISO-8601 UTC,
                // so comparing them as strings compares them chronologically.
                int pos = 0
                while (pos < candidates.size() && candidates.get(pos).time >= time) {
                    pos++
                }
                candidates.add(pos, [digest: digest, tag: tag, time: time])
            }
        }

        int retained = 0
        for (int i = 0; i < candidates.size(); i++) {
            def candidate = candidates.get(i)
            if (candidate.digest == movingDigest || retained < keep) {
                retained++
                continue
            }
            stale.add([digest: candidate.digest, tag: candidate.tag])
        }
        return stale
    }
}
