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
 * delete tags, never artifacts: an artifact left with no tag is Harbor's
 * garbage collection to reclaim, not ours.
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
     * Stale `<prefix><commit>` tags, as [digest:, tag:] entries.
     *
     * Every `<prefix>*` tag in the repository is a candidate, whatever else
     * its artifact carries: a build byte-identical to a preview or a
     * release shares their digest, and skipping those artifacts would leave
     * its tag out of the count and keep it for as long as the release
     * lives. Other tags (a preview, a release, `latest`) are never
     * returned, so they and their artifact stay. Of the candidates the
     * newest `keep` survive, and so does every one on the digest `<moving>`
     * points at.
     *
     * Retention counts tags and orders them by the push time of the tag
     * itself, not of the artifact: the artifact keeps the push time of the
     * first commit that produced that digest, so a later commit hanging its
     * tag off the same artifact would otherwise read as old and be pruned
     * right after being published.
     *
     * Untagged artifacts (a rebuild of the same commit re-pushes its tag onto
     * a new digest and leaves the previous one behind) are skipped: Harbor's
     * scheduled garbage collection reclaims them.
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
                if (!tag.startsWith(prefix)) {
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

    /**
     * Groovy source defining pickVersions(artifacts, semverOnly): the tag
     * names of a Harbor artifact listing, newest version first. With
     * semverOnly, only plain `X.Y.Z` tags. Versions compare numerically
     * field by field (0.10.0 above 0.9.0), not as strings.
     *
     * It is source, not a method, because Active Choices runs a parameter
     * script on its own, outside any pipeline and without this library on
     * the classpath: versionChoicesScript() embeds it in the script it
     * generates, and test/HarborTest.groovy evaluates it on its own. It
     * only uses calls the script-security generic whitelist allows.
     */
    static final String VERSION_PICKER = '''
def pickVersions(artifacts, boolean semverOnly) {
    def versions = []
    artifacts.each { artifact ->
        artifact.tags?.each { tag ->
            String name = tag.name == null ? '' : tag.name.toString()
            if (name && (!semverOnly || name ==~ /^\\d+\\.\\d+\\.\\d+$/)) {
                versions << name
            }
        }
    }
    return versions.unique().sort { a, b -> compareVersions(b, a) }
}

def compareVersions(String a, String b) {
    def x = a.tokenize('.')
    def y = b.tokenize('.')
    int n = x.size() < y.size() ? x.size() : y.size()
    for (int i = 0; i < n; i++) {
        int c = (x[i].isInteger() && y[i].isInteger()) ?
            x[i].toInteger() <=> y[i].toInteger() : x[i] <=> y[i]
        if (c != 0) {
            return c
        }
    }
    return x.size() <=> y.size()
}
'''

    /**
     * Active Choices script listing the version tags of a Harbor repository,
     * newest first; harborVersionParam() wraps it in a build parameter.
     *
     * Reads the newest 100 artifacts. Any failure comes back as a single
     * "Error: ..." choice, so a broken registry shows in the build form
     * instead of an empty list.
     */
    @NonCPS
    static String versionChoicesScript(String registry, String project, String repo,
                                       String credentialsId, boolean semverOnly) {
        // The values are pasted into the script source: refuse anything that
        // could close the string literal.
        [registry, project, repo, credentialsId].each { value ->
            if (!(value ==~ /[A-Za-z0-9._\/-]+/)) {
                throw new IllegalArgumentException("unsafe Harbor parameter value: '${value}'")
            }
        }
        String url = "https://${registry}/api/v2.0/projects/${project}/repositories/" +
            "${repo.replace('/', '%252F')}/artifacts?page_size=100&with_tag=true&sort=-push_time"
        return '''import groovy.json.JsonSlurper
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import jenkins.model.Jenkins
''' + VERSION_PICKER + """
def credsId = '${credentialsId}'
def apiUrl = '${url}'
""" + '''
try {
    def credentials = CredentialsProvider.lookupCredentials(
        StandardUsernamePasswordCredentials.class, Jenkins.get(), null, null
    ).find { it.id == credsId }
    if (!credentials) {
        return ["Error: credentials '${credsId}' not found in Jenkins"]
    }
    def auth = "${credentials.username}:${credentials.password.plainText}".bytes.encodeBase64().toString()

    def connection = new URL(apiUrl).openConnection() as HttpURLConnection
    connection.setRequestMethod('GET')
    connection.setRequestProperty('Authorization', "Basic ${auth}")
    connection.setRequestProperty('Accept', 'application/json')
    connection.setConnectTimeout(5000)
    connection.setReadTimeout(5000)
    if (connection.responseCode != 200) {
        return ["Error: Harbor HTTP ${connection.responseCode}: ${connection.responseMessage}"]
    }

    def versions = pickVersions(new JsonSlurper().parseText(connection.inputStream.text), ''' + semverOnly + ''')
    return versions ?: ["Error: no ''' + (semverOnly ? 'X.Y.Z ' : '') + '''tags in ''' + "${project}/${repo}" + '''"]
} catch (Exception e) {
    return ["Error: ${e.message}"]
}
'''
    }
}
