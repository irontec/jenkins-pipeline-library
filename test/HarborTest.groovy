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

println fails == 0 ? 'ALL OK' : "${fails} FAILING"
System.exit(fails == 0 ? 0 : 1)
