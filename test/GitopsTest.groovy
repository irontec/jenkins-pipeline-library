// Exercises org.pipeline.Gitops outside Jenkins. Run from the repository root:
//
//     groovy -cp src:test/stubs test/GitopsTest.groovy
//
// or, without a local Groovy:
//
//     docker run --rm -v "$PWD:/w" -w /w groovy:4-jdk17 groovy -cp src:test/stubs test/GitopsTest.groovy

import org.pipeline.Gitops

int fails = 0
def check = { String name, actual, expected ->
    if (actual != expected) {
        println "FAIL ${name}\n  got  ${actual}\n  want ${expected}"
        fails++
    } else {
        println "ok   ${name}"
    }
}
def blob = { path -> [path: path, type: 'blob'] }
def tree = { path -> [path: path, type: 'tree'] }
def pick = { entries, List required, List exclude = [] ->
    new GroovyShell(new Binding(entries: entries, required: required, exclude: exclude))
        .evaluate(Gitops.TENANT_PICKER + "\nreturn pickTenants(entries, 'deploy/values', required, exclude)")
}

// --- TENANT_PICKER ---------------------------------------------------------

def t1 = [
    tree('deploy/values'),
    blob('deploy/values/base.yaml'),
    tree('deploy/values/alpha'),
    blob('deploy/values/alpha/values.yaml'),
    blob('deploy/values/alpha/versions.yaml'),
    blob('deploy/values/alpha/custom.yaml'),
    blob('deploy/values/beta/custom.yaml'),
    blob('deploy/values/beta/extra.yaml'),
    blob('deploy/values/sandbox/values.yaml'),
    blob('deploy/values/sandbox/versions.yaml'),
    blob('deploy/values/sandbox/nested/values.yaml'),
    blob('deploy/values-other/x/values.yaml'),
    blob('other/values/y/values.yaml'),
]

// 1. only tenants holding every required file, sorted
check('tenants with both files', pick(t1, ['values.yaml', 'versions.yaml']), ['alpha', 'sandbox'])

// 2. no requirement: every directory with files right under dir
check('every tenant directory', pick(t1, []), ['alpha', 'beta', 'sandbox'])

// 2b. excluded tenants are left out
check('excluded tenant', pick(t1, ['values.yaml', 'versions.yaml'], ['sandbox']), ['alpha'])

// 3. nothing under dir
check('empty tree', pick([blob('README.md')], ['values.yaml']), [])

// --- tenantChoicesScript ---------------------------------------------------

def repos = [eu: 'acme/gitops-eu', us: 'acme/gitops-us']
def generated = Gitops.tenantChoicesScript(repos, 'ENVIRONMENT', 'deploy/values',
    ['values.yaml', 'versions.yaml'], ['sandbox'], 'github-creds', 'main')

// 4. the generated script compiles, and fails soft without credentials
check('script without credentials',
    new GroovyShell(new Binding(ENVIRONMENT: 'us')).evaluate(generated),
    ["Error: credentials 'github-creds' not found in Jenkins"])

// 5. environment without a repository
check('unknown environment',
    new GroovyShell(new Binding(ENVIRONMENT: 'nope')).evaluate(generated),
    ["Error: no repository for 'nope'"])

// 6. referenced parameter not bound yet
check('environment not set', new GroovyShell().evaluate(generated), ['Error: ENVIRONMENT not set'])

// 7. values that would break out of the script are refused
[
    [[eu: "x/y'; evil()"], 'ENVIRONMENT'],
    [["li'ne": 'x/y'], 'ENVIRONMENT'],
    [repos, 'ENV; evil()'],
    [[eu: 'not-owner-slash-name'], 'ENVIRONMENT'],
].each { r, param ->
    try {
        Gitops.tenantChoicesScript(r, param, 'deploy/values', ['values.yaml'], [], 'c', 'main')
        println "FAIL unsafe value refused: ${r} ${param}"
        fails++
    } catch (IllegalArgumentException e) {
        println "ok   unsafe value refused: ${e.message}"
    }
}

println fails == 0 ? 'ALL OK' : "${fails} FAILING"
System.exit(fails == 0 ? 0 : 1)
