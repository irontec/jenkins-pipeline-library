#!/usr/bin/env groovy

import org.pipeline.Constants
import org.pipeline.Gitops
import org.pipeline.Harbor
import org.pipeline.Helpers

/**
 * Build parameter offering the version tags of a Harbor repository
 *
 * An Active Choices single-select listing the repository's tags, newest
 * version first, for `properties([parameters([...])])`. The list is read
 * when the build form opens, by a script Active Choices runs on its own
 * (see Harbor.versionChoicesScript()), so it does not depend on this
 * library being loaded at that point. Requires the Active Choices plugin.
 *
 * With `current`, the parameter reacts to an environment and a tenant
 * parameter (see gitopsTenantParam()): it reads the version the selected
 * tenant has in its GitOps repository, labels it "(actual)" and
 * preselects it. If that file cannot be read, the list comes unmarked.
 *
 * @param args.name          String parameter name
 * @param args.repo          String repository inside the project
 * @param args.project       String Harbor project (default: env.HARBOR_PROJECT)
 * @param args.semverOnly    boolean offer only `X.Y.Z` tags (default: true)
 * @param args.description   String parameter description (default: none)
 * @param args.registry      String registry host (default: env.HARBOR_REGISTRY
 *                           or the library constant)
 * @param args.credentialsId String Jenkins credentials, read access is
 *                           enough (default: env.HARBOR_CREDENTIALS_ID or
 *                           the library constant)
 * @param args.current       Map, optional, where the current version lives:
 *                           environmentParam, tenantParam (parameter names),
 *                           repos (environment -> GitHub `owner/name`), dir
 *                           (directory of tenant directories), file (inside
 *                           the tenant directory), path (`.a.b.c` in that
 *                           YAML file), and optionally ref (default: "main")
 *                           and credentialsId (default: the library's GitHub
 *                           credentials)
 * @return Map parameter definition
 *
 * @example
 * def repos = [eu: 'acme/gitops-eu', us: 'acme/gitops-us']
 * def current = [environmentParam: 'ENVIRONMENT', tenantParam: 'TENANT', repos: repos,
 *                dir: 'deploy/values']
 * properties([parameters([
 *     choice(name: 'ENVIRONMENT', choices: ['eu', 'us']),
 *     gitopsTenantParam(name: 'TENANT', environmentParam: 'ENVIRONMENT', repos: repos,
 *                       dir: 'deploy/values'),
 *     harborVersionParam(name: 'API_VERSION', project: 'shop', repo: 'api',
 *                        current: current + [file: 'versions.yaml', path: '.api.tag']),
 *     harborVersionParam(name: 'CHART_VERSION', project: 'shop', repo: 'shop-chart',
 *                        semverOnly: false),
 * ])])
 */
def call(Map args) {
    def project = args.project ?: env.HARBOR_PROJECT
    if (!args.name || !project || !args.repo) {
        error 'harborVersionParam: name, project and repo are required'
    }
    boolean semverOnly = args.containsKey('semverOnly') ? args.semverOnly as boolean : true

    def current = args.current
    String currentSource = null
    if (current) {
        def missing = ['environmentParam', 'tenantParam', 'repos', 'dir', 'file', 'path'].findAll { !current[it] }
        if (missing) {
            error "harborVersionParam: current needs ${missing.join(', ')}"
        }
        currentSource = Gitops.currentValueSource(current + [
            ref: current.ref ?: 'main',
            credentialsId: current.credentialsId ?: Constants.GITHUB_CREDENTIALS,
        ])
    }

    def script = Harbor.versionChoicesScript(
        args.registry ?: Helpers.getHarborRegistry(env),
        project,
        args.repo,
        args.credentialsId ?: Helpers.getHarborCredentialsId(env),
        semverOnly,
        currentSource
    )

    def definition = [
        $class: current ? 'CascadeChoiceParameter' : 'ChoiceParameter',
        name: args.name,
        description: args.description ?: '',
        // Active Choices keys the parameter's form element on it: stable
        // per name, so it does not change on every build.
        randomName: "choice-parameter-${Math.abs(args.name.hashCode())}",
        choiceType: 'PT_SINGLE_SELECT',
        filterable: false,
        filterLength: 1,
        script: [
            $class: 'GroovyScript',
            script: [script: script, sandbox: true, classpath: []],
            fallbackScript: [
                script: "return ['Error: could not list ${project}/${args.repo}']",
                sandbox: true,
                classpath: []
            ]
        ]
    ]
    if (current) {
        definition.referencedParameters = "${current.environmentParam},${current.tenantParam}".toString()
    }
    return definition
}
