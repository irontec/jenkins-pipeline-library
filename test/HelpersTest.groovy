// Exercises org.pipeline.Helpers outside Jenkins. Run from the repository root:
//
//     groovy -cp src:test/stubs test/HelpersTest.groovy
//
// or, without a local Groovy:
//
//     docker run --rm -v "$PWD:/w" -w /w groovy:4-jdk17 groovy -cp src:test/stubs test/HelpersTest.groovy

import org.pipeline.Helpers

int fails = 0
def check = { String name, actual, expected ->
    if (actual != expected) {
        println "FAIL ${name}\n  got  ${actual}\n  want ${expected}"
        fails++
    } else {
        println "ok   ${name}"
    }
}

// --- getGithubRepository ---------------------------------------------------

check('pull request url',
    Helpers.getGithubRepository([CHANGE_URL: 'https://github.com/irontec-comms/konect-nixon/pull/594']),
    'irontec-comms/konect-nixon')
check('https clone url',
    Helpers.getGithubRepository([GIT_URL: 'https://github.com/irontec-comms/konect-nixon.git']),
    'irontec-comms/konect-nixon')
check('ssh clone url',
    Helpers.getGithubRepository([GIT_URL: 'git@github.com:irontec-comms/konect-nixon.git']),
    'irontec-comms/konect-nixon')
check('clone url without .git',
    Helpers.getGithubRepository([GIT_URL: 'https://github.com/irontec-comms/konect-nixon']),
    'irontec-comms/konect-nixon')
check('CHANGE_URL wins over GIT_URL',
    Helpers.getGithubRepository([CHANGE_URL: 'https://github.com/a/b/pull/1', GIT_URL: 'git@github.com:c/d.git']),
    'a/b')
check('not github',
    Helpers.getGithubRepository([GIT_URL: 'https://gitlab.example.com/a/b.git']),
    null)
check('nothing set',
    Helpers.getGithubRepository([:]),
    null)

// --- normalizeBranch -------------------------------------------------------

check('strips refs/heads', Helpers.normalizeBranch('refs/heads/main'), 'main')
check('strips origin/', Helpers.normalizeBranch('origin/main'), 'main')
check('keeps inner slashes', Helpers.normalizeBranch('feature/login'), 'feature/login')

if (fails) {
    println "${fails} failure(s)"
    System.exit(1)
}
println 'all ok'
