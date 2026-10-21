#!/usr/bin/env groovy

import org.pipeline.Constants
import org.pipeline.Gitops

/**
 * Build parameter offering the tenants of the GitOps repository picked in another parameter
 *
 * An Active Choices single-select, reactive on `environmentParam`: when
 * the environment changes in the build form, it lists the directories
 * under `dir` of that environment's repository holding every one of
 * `requiredFiles`, read from GitHub at that moment. The script runs on
 * its own (see Gitops.tenantChoicesScript()), so it does not depend on
 * this library being loaded at that point. Requires the Active Choices
 * plugin.
 *
 * @param args.name             String parameter name
 * @param args.environmentParam String name of the parameter it reacts to;
 *                              its values are the keys of `repos`
 * @param args.repos            Map environment -> GitHub `owner/name`
 * @param args.dir              String directory holding one directory per
 *                              tenant
 * @param args.requiredFiles    List files a tenant directory must hold
 *                              (default: none)
 * @param args.exclude          List tenants never offered, such as one
 *                              deployed by other means (default: none)
 * @param args.ref              String branch read (default: "main")
 * @param args.description      String parameter description (default: none)
 * @param args.credentialsId    String Jenkins username/token credentials
 *                              with read access (default: the library's
 *                              GitHub credentials)
 * @return Map parameter definition
 *
 * @example
 * def repos = [eu: 'acme/gitops-eu', us: 'acme/gitops-us']
 * properties([parameters([
 *     choice(name: 'ENVIRONMENT', choices: ['eu', 'us']),
 *     gitopsTenantParam(name: 'TENANT', environmentParam: 'ENVIRONMENT', repos: repos,
 *                       dir: 'deploy/values', requiredFiles: ['values.yaml']),
 * ])])
 */
def call(Map args) {
    if (!args.name || !args.environmentParam || !args.repos || !args.dir) {
        error 'gitopsTenantParam: name, environmentParam, repos and dir are required'
    }
    def script = Gitops.tenantChoicesScript(
        args.repos,
        args.environmentParam,
        args.dir,
        args.requiredFiles ?: [],
        args.exclude ?: [],
        args.credentialsId ?: Constants.GITHUB_CREDENTIALS,
        args.ref ?: 'main'
    )

    return [
        $class: 'CascadeChoiceParameter',
        name: args.name,
        description: args.description ?: '',
        // Active Choices keys the parameter's form element on it: stable
        // per name, so it does not change on every build.
        randomName: "choice-parameter-${Math.abs(args.name.hashCode())}",
        referencedParameters: args.environmentParam,
        choiceType: 'PT_SINGLE_SELECT',
        filterable: false,
        filterLength: 1,
        script: [
            $class: 'GroovyScript',
            script: [script: script, sandbox: true, classpath: []],
            fallbackScript: [
                script: "return ['Error: could not list the tenants']",
                sandbox: true,
                classpath: []
            ]
        ]
    ]
}
