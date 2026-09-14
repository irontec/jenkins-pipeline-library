#!/usr/bin/env groovy

package org.pipeline

/**
 * Global constants for the Jenkins pipeline library
 */
class Constants {
    /**
     * GitHub credentials ID
     */
    static final String GITHUB_CREDENTIALS =
        'fc43e367-0256-4e91-883e-3c31886d4c32'

    /**
     * GitHub username of the review bot
     */
    static final String GITHUB_BOT_USERNAME = 'ironArt3mis'

    /**
     * Harbor registry the CI images are pushed to
     */
    static final String HARBOR_REGISTRY = 'harbor.comms.irontec.com'

    /**
     * Jenkins credentials ID with push and delete rights on Harbor
     */
    static final String HARBOR_CREDENTIALS_ID = 'harbor-comms-credentials'
}

