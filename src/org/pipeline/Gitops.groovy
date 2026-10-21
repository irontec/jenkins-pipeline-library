package org.pipeline

import com.cloudbees.groovy.cps.NonCPS

/**
 * Script generation for gitopsTenantParam(). Like Harbor's, the tenant
 * selection is kept as source (TENANT_PICKER) because Active Choices runs
 * a parameter script on its own, without this library on the classpath;
 * test/GitopsTest.groovy evaluates it outside Jenkins.
 */
class Gitops implements Serializable {

    /**
     * Groovy source defining pickTenants(tree, dir, requiredFiles, exclude):
     * the names of the directories right under `dir` holding every one of
     * `requiredFiles`, sorted, leaving out the names in `exclude`. `tree` is
     * the `tree` list of the GitHub git/trees API with `recursive=1`. Uses
     * only calls the script-security generic whitelist allows.
     */
    static final String TENANT_PICKER = '''
def pickTenants(tree, String dir, List requiredFiles, List exclude) {
    def files = [:]
    String prefix = dir + '/'
    tree.each { entry ->
        String path = entry.path == null ? '' : entry.path.toString()
        if (entry.type == 'blob' && path.startsWith(prefix)) {
            def rest = path.substring(prefix.length()).tokenize('/')
            if (rest.size() == 2) {
                if (!files.containsKey(rest[0])) {
                    files[rest[0]] = []
                }
                files[rest[0]] << rest[1]
            }
        }
    }
    def tenants = []
    files.each { tenant, names ->
        if (names.containsAll(requiredFiles) && !exclude.contains(tenant)) {
            tenants << tenant
        }
    }
    return tenants.sort()
}
'''

    /**
     * Groovy source defining yamlValue(text, path): the scalar at a `.a.b.c`
     * path of a YAML document, unquoted, or null. Only block mappings are
     * understood (no flow style, anchors or lists), which is what values
     * files holding versions look like; it spares the parameter script a
     * YAML library and the Script Approval that would take.
     */
    static final String YAML_VALUE = '''
def yamlValue(String text, String path) {
    def keys = path.tokenize('.')
    def stack = []
    for (String line : text.readLines()) {
        def m = line =~ /^(\\s*)([A-Za-z0-9_-]+)\\s*:(?:\\s+(.*?))?\\s*$/
        if (!m.matches()) {
            continue
        }
        int indent = m.group(1).length()
        while (stack && stack[-1].indent >= indent) {
            stack.remove(stack.size() - 1)
        }
        stack << [indent: indent, key: m.group(2)]
        String value = m.group(3)
        if (value && stack.collect { it.key } == keys) {
            value = value.replaceAll(/\\s+#.*$/, '')
            return value.replaceAll(/^["']|["']$/, '')
        }
    }
    return null
}
'''

    /**
     * Groovy source defining currentValue(): the value at `path` of `file`
     * in the tenant directory the build form has selected, read from
     * GitHub, or null when it cannot be read. For harborVersionParam()'s
     * `current`; needs YAML_VALUE and the credentials imports in the script.
     *
     * The script reads the environment and the tenant from the parameters
     * it reacts to, `current.environmentParam` and `current.tenantParam`.
     */
    @NonCPS
    static String currentValueSource(Map current) {
        [current.environmentParam, current.tenantParam].each { name ->
            if (!(name ==~ /[A-Za-z_][A-Za-z0-9_]*/)) {
                throw new IllegalArgumentException("unsafe parameter name: '${name}'")
            }
        }
        ([current.dir, current.file, current.path, current.credentialsId, current.ref] +
                current.repos.keySet().toList()).each { value ->
            if (!(value ==~ /[A-Za-z0-9._\/-]+/)) {
                throw new IllegalArgumentException("unsafe GitOps parameter value: '${value}'")
            }
        }
        current.repos.values().each { value ->
            if (!(value ==~ /[A-Za-z0-9._-]+\/[A-Za-z0-9._-]+/)) {
                throw new IllegalArgumentException("repository is not owner/name: '${value}'")
            }
        }
        String reposLiteral = '[' + current.repos.collect { k, v -> "'${k}': '${v}'" }.join(', ') + ']'

        return """
def currentValue() {
    try {
        def repo = ${reposLiteral}[${current.environmentParam}]
        String tenant = ${current.tenantParam}
        String file = '${current.dir}/' + tenant + '/${current.file}'
""" + '''        if (!repo || !(tenant ==~ /[A-Za-z0-9._-]+/)) {
            return null
        }
        def credentials = CredentialsProvider.lookupCredentials(
            StandardUsernamePasswordCredentials.class, Jenkins.get(), null, null
        ).find { it.id == ''' + "'${current.credentialsId}'" + ''' }
        if (!credentials) {
            return null
        }
        def auth = "${credentials.username}:${credentials.password.plainText}".bytes.encodeBase64().toString()
        def connection = new URL("https://api.github.com/repos/${repo}/contents/${file}?ref=''' +
            current.ref + '''").openConnection() as HttpURLConnection
        connection.setRequestMethod('GET')
        connection.setRequestProperty('Authorization', "Basic ${auth}")
        connection.setRequestProperty('Accept', 'application/vnd.github.raw')
        connection.setConnectTimeout(5000)
        connection.setReadTimeout(5000)
        if (connection.responseCode != 200) {
            return null
        }
        return yamlValue(connection.inputStream.text, ''' + "'${current.path}'" + ''')
    } catch (Exception e) {
        // Also MissingPropertyException: the referenced parameters are not bound yet.
        return null
    }
}
'''
    }

    /**
     * Active Choices script, reactive on `environmentParam`, listing the
     * tenants of the GitOps repository that environment maps to;
     * gitopsTenantParam() wraps it in a build parameter.
     *
     * Reads the repository tree at `ref` in one GitHub API call. Any
     * failure comes back as a single "Error: ..." choice.
     */
    @NonCPS
    static String tenantChoicesScript(Map repos, String environmentParam, String dir,
                                      List requiredFiles, List exclude, String credentialsId,
                                      String ref) {
        // The values are pasted into the script source: refuse anything that
        // could close a string literal or is not what its name says.
        if (!(environmentParam ==~ /[A-Za-z_][A-Za-z0-9_]*/)) {
            throw new IllegalArgumentException("unsafe parameter name: '${environmentParam}'")
        }
        ([dir, credentialsId, ref] + requiredFiles + exclude + repos.keySet().toList()).each { value ->
            if (!(value ==~ /[A-Za-z0-9._\/-]+/)) {
                throw new IllegalArgumentException("unsafe GitOps parameter value: '${value}'")
            }
        }
        repos.values().each { value ->
            if (!(value ==~ /[A-Za-z0-9._-]+\/[A-Za-z0-9._-]+/)) {
                throw new IllegalArgumentException("repository is not owner/name: '${value}'")
            }
        }
        String reposLiteral = '[' + repos.collect { k, v -> "'${k}': '${v}'" }.join(', ') + ']'
        String filesLiteral = '[' + requiredFiles.collect { "'${it}'" }.join(', ') + ']'
        String excludeLiteral = '[' + exclude.collect { "'${it}'" }.join(', ') + ']'

        return '''import groovy.json.JsonSlurper
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import jenkins.model.Jenkins
''' + TENANT_PICKER + """
def repos = ${reposLiteral}
def credsId = '${credentialsId}'
def dir = '${dir}'
def requiredFiles = ${filesLiteral}
def exclude = ${excludeLiteral}
def ref = '${ref}'
def environment
try {
    environment = ${environmentParam}
} catch (MissingPropertyException e) {
    return ['Error: ${environmentParam} not set']
}
""" + '''
def repo = repos[environment]
if (!repo) {
    return ["Error: no repository for '${environment}'"]
}
try {
    def credentials = CredentialsProvider.lookupCredentials(
        StandardUsernamePasswordCredentials.class, Jenkins.get(), null, null
    ).find { it.id == credsId }
    if (!credentials) {
        return ["Error: credentials '${credsId}' not found in Jenkins"]
    }
    def auth = "${credentials.username}:${credentials.password.plainText}".bytes.encodeBase64().toString()

    def apiUrl = "https://api.github.com/repos/${repo}/git/trees/${ref}?recursive=1"
    def connection = new URL(apiUrl).openConnection() as HttpURLConnection
    connection.setRequestMethod('GET')
    connection.setRequestProperty('Authorization', "Basic ${auth}")
    connection.setRequestProperty('Accept', 'application/vnd.github+json')
    connection.setConnectTimeout(5000)
    connection.setReadTimeout(5000)
    if (connection.responseCode != 200) {
        return ["Error: GitHub HTTP ${connection.responseCode}: ${connection.responseMessage}"]
    }

    def tenants = pickTenants(new JsonSlurper().parseText(connection.inputStream.text).tree, dir, requiredFiles, exclude)
    return tenants ?: ["Error: no tenants in ${repo}/${dir}"]
} catch (Exception e) {
    return ["Error: ${e.message}"]
}
'''
    }
}
