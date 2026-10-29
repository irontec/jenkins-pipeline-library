// Exercises org.pipeline.Harbor outside Jenkins. Run from the repository root:
//
//     groovy -cp src:test/stubs test/HarborTest.groovy
//
// or, without a local Groovy:
//
//     docker run --rm -v "$PWD:/w" -w /w groovy:4-jdk17 groovy -cp src:test/stubs test/HarborTest.groovy

import org.pipeline.Harbor

def art(digest, tagSpecs, artTime) {
    [digest: digest, push_time: artTime,
     tags: tagSpecs.collect { n, t -> [name: n, push_time: t] }]
}

int fails = 0
def check = { String name, List actual, List expected ->
    def got = actual.collect { "${it.digest}:${it.tag}".toString() }.sort()
    def want = expected.sort()
    if (got != want) {
        println "FAIL ${name}\n  got  ${got}\n  want ${want}"
        fails++
    } else {
        println "ok   ${name}"
    }
}

// --- staleTags -------------------------------------------------------------

// 1. plain history, keep 3, moving `main` on the newest
def a1 = [
    art('d5', [['main', 't5'], ['main-e', 't5']], 't5'),
    art('d4', [['main-d', 't4']], 't4'),
    art('d3', [['main-c', 't3']], 't3'),
    art('d2', [['main-b', 't2']], 't2'),
    art('d1', [['main-a', 't1']], 't1'),
]
check('keeps newest 3 + moving', Harbor.staleTags(a1, 'main-', 'main', 3), ['d2:main-b', 'd1:main-a'])

// 2. two commits with an identical tree share one artifact: the artifact's
//    push_time is old, the second tag is the newest thing in the repo
def a2 = [
    art('dShared', [['main-a', 't1'], ['main-z', 't9']], 't1'),
    art('d4', [['main-d', 't4']], 't4'),
    art('d3', [['main-c', 't3']], 't3'),
    art('d2', [['main-b', 't2']], 't2'),
]
check('tag push_time wins over artifact', Harbor.staleTags(a2, 'main-', 'main', 3), ['dShared:main-a', 'd2:main-b'])

// 3. untagged artifact is left to Harbor's garbage collection
def a3 = [art('dOrphan', [], 't1'), art('dNoTags', null, 't1'), art('d2', [['main-b', 't2']], 't2')]
check('untagged left to GC', Harbor.staleTags(a3, 'main-', 'main', 0), ['d2:main-b'])

// 4. on a digest shared with a preview or a release, the `main-*` tag is
//    pruned like any other and the foreign tags are never returned
def a4 = [
    art('dShared', [['main-a', 't1'], ['pr-7-abc', 't1']], 't1'),
    art('dRel', [['main-b', 't2'], ['v1.2.3', 't2'], ['latest', 't2']], 't2'),
]
check('only the prefixed tag of a shared digest', Harbor.staleTags(a4, 'main-', 'main', 0),
    ['dShared:main-a', 'dRel:main-b'])

// 4b. tags on shared digests count towards `keep`: a release and a preview
//     byte-identical to main builds must not stretch the history past it
def a4b = [
    art('dNew', [['pr-9-abc', 't7'], ['main', 't8'], ['main-f', 't8']], 't7'),
    art('d5', [['main-e', 't6']], 't6'),
    art('dRel', [['main-c', 't3'], ['v1.2.3', 't4'], ['latest', 't4'], ['main-d', 't5']], 't3'),
    art('d2', [['main-b', 't2']], 't2'),
    art('dOldRel', [['main-a', 't1'], ['v1.2.2', 't1']], 't1'),
]
check('shared digests count towards keep', Harbor.staleTags(a4b, 'main-', 'main', 3),
    ['dRel:main-c', 'd2:main-b', 'dOldRel:main-a'])

// 5. moving `main` parked on an old digest keeps that build
def a5 = [
    art('d3', [['main-c', 't3']], 't3'),
    art('d2', [['main-b', 't2']], 't2'),
    art('d1', [['main', 't1'], ['main-a', 't1']], 't1'),
]
check('moving main protects its digest', Harbor.staleTags(a5, 'main-', 'main', 1), ['d2:main-b'])

// 5b. byte-identical builds pile every tag onto the digest moving `main`
//     points at: only the newest of them is protected
def a5b = [
    art('dSame', [['main-a', 't1'], ['main-b', 't2'], ['main', 't3'], ['main-c', 't3']], 't1'),
]
check('identical builds still pruned', Harbor.staleTags(a5b, 'main-', 'main', 1),
    ['dSame:main-a', 'dSame:main-b'])

// 5c. moving `main` parked on an old digest shared by several tags keeps
//     only the newest of them
def a5c = [
    art('d3', [['main-c', 't3']], 't3'),
    art('d1', [['main', 't2'], ['main-a', 't1'], ['main-b', 't2']], 't1'),
]
check('parked moving keeps one tag', Harbor.staleTags(a5c, 'main-', 'main', 1), ['d1:main-a'])

// 6. keep larger than the history: nothing to do
check('nothing stale', Harbor.staleTags(a1, 'main-', 'main', 10), [])

// 7. missing tag push_time falls back to the artifact's
def a7 = [
    art('dNew', [['main-z', null]], 't9'),
    art('d2', [['main-b', 't2']], 't2'),
    art('d1', [['main-a', 't1']], 't1'),
]
check('null tag push_time falls back', Harbor.staleTags(a7, 'main-', 'main', 1), ['d2:main-b', 'd1:main-a'])

// 8. the moving tag alone on an artifact is never a candidate
def a8 = [art('dOnlyMoving', [['main', 't1']], 't1')]
check('lone moving tag untouched', Harbor.staleTags(a8, 'main-', 'main', 0), [])

// --- prefixedTags ----------------------------------------------------------

// 9. every pr-7-* tag, and only those, across shared artifacts
def a9 = [
    art('dShared', [['pr-7-aaa', 't1'], ['main-a', 't1'], ['pr-8-aaa', 't1']], 't1'),
    art('d2', [['pr-7-bbb', 't2']], 't2'),
    art('dOrphan', [], 't3'),
    art('dNoTags', null, 't4'),
]
check('prefixed tags only', Harbor.prefixedTags(a9, 'pr-7-'), ['dShared:pr-7-aaa', 'd2:pr-7-bbb'])

// 10. no match, nothing
check('prefix without matches', Harbor.prefixedTags(a9, 'pr-9-'), [])

// --- VERSION_PICKER --------------------------------------------------------

def pick = { artifacts, boolean semverOnly ->
    new GroovyShell(new Binding(artifacts: artifacts))
        .evaluate(Harbor.VERSION_PICKER + "\nreturn pickVersions(artifacts, ${semverOnly})")
}
def checkList = { String name, actual, List expected ->
    if (actual != expected) {
        println "FAIL ${name}\n  got  ${actual}\n  want ${expected}"
        fails++
    } else {
        println "ok   ${name}"
    }
}

// 11. numeric order, not string order: 0.10.0 above 0.9.0
def a11 = [
    art('d1', [['0.9.0', 't1'], ['main-a', 't1']], 't1'),
    art('d2', [['0.10.0', 't2'], ['latest', 't2']], 't2'),
    art('d3', [['0.10.1', 't3'], ['0.10.1-rc1', 't3']], 't3'),
    art('d4', [['1.0', 't4']], 't4'),
]
checkList('semver only, newest first', pick(a11, true), ['0.10.1', '0.10.0', '0.9.0'])

// 12. without the filter every tag comes back, versions still ordered
checkList('every tag', pick(a11, false),
    ['main-a', 'latest', '1.0', '0.10.1-rc1', '0.10.1', '0.10.0', '0.9.0'])

// 13. one tag on two digests once; untagged artifacts ignored
def a13 = [art('d1', [['1.2.3', 't1']], 't1'), art('d2', [['1.2.3', 't2']], 't2'),
           art('dOrphan', [], 't3'), art('dNoTags', null, 't4')]
checkList('duplicates and untagged', pick(a13, true), ['1.2.3'])

// --- markCurrent -----------------------------------------------------------

def mark = { List versions, String current ->
    new GroovyShell(new Binding(versions: versions, current: current))
        .evaluate(Harbor.VERSION_PICKER + '\nreturn markCurrent(versions, current)')
}
def keysAndLabels = { Map m -> m.collect { k, v -> "${k}=${v}".toString() } }

// 15b. the current version is labelled and preselected, order kept
checkList('current in the list', keysAndLabels(mark(['0.10.0', '0.9.1', '0.9.0'], '0.9.1')),
    ['0.10.0=0.10.0', '0.9.1=0.9.1 (actual):selected', '0.9.0=0.9.0'])

// 15c. a current version missing from the listing is inserted in order
checkList('current not listed', keysAndLabels(mark(['0.10.0', '0.9.0'], '0.9.5')),
    ['0.10.0=0.10.0', '0.9.5=0.9.5 (actual):selected', '0.9.0=0.9.0'])

// 15d. no current version: the plain list
checkList('no current', mark(['0.10.0', '0.9.0'], null), ['0.10.0', '0.9.0'])

// --- versionChoicesScript --------------------------------------------------

// 14. the generated script compiles, and fails soft without credentials
def generated = Harbor.versionChoicesScript('harbor.example.com', 'shop', 'api', 'harbor-creds', true)
checkList('script without credentials', new GroovyShell().evaluate(generated),
    ["Error: credentials 'harbor-creds' not found in Jenkins"])

// 14b. with a current source too
def withCurrent = Harbor.versionChoicesScript('harbor.example.com', 'shop', 'api', 'harbor-creds', true,
    org.pipeline.Gitops.currentValueSource([environmentParam: 'ENVIRONMENT', tenantParam: 'TENANT',
        repos: [eu: 'acme/gitops-eu'], dir: 'deploy/values', file: 'versions.yaml',
        path: '.api.image.tag', ref: 'main', credentialsId: 'github-creds']))
checkList('script with current, without credentials',
    new GroovyShell(new Binding(ENVIRONMENT: 'eu', TENANT: 'alpha')).evaluate(withCurrent),
    ["Error: credentials 'harbor-creds' not found in Jenkins"])

// 15. a value that would break out of the string literal is refused
try {
    Harbor.versionChoicesScript('harbor.example.com', "shop'; evil()", 'api', 'harbor-creds', true)
    println 'FAIL unsafe value refused'
    fails++
} catch (IllegalArgumentException e) {
    println 'ok   unsafe value refused'
}

println fails == 0 ? 'ALL OK' : "${fails} FAILING"
System.exit(fails == 0 ? 0 : 1)
