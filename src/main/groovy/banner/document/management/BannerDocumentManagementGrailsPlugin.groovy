/** *******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 ********************************************************************************* */


package banner.document.management

import grails.plugins.*

/**
 * A Grails Plugin providing cross cutting concerns such as security and database access
 * for Banner web applications.
 * */
class BannerDocumentManagementGrailsPlugin extends Plugin {
	
	 String version = "1.0.1"
	 
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.3.2 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def author = "Ellucian"
    def authorEmail = "actionline@ellucian.com"
    def title = "BannerDocumentManagement Plugin"
    def description = '''This plugin is BannerDocumentManagement.'''//.stripMargin()  // TODO Enable this once we adopt Groovy 1.7.3

    def documentation = "http://sungardhe.com/development/horizon/plugins/banner-document-management"

    def profiles = ['web']


    Closure doWithSpring() { {->
           // no-op
        }
    }

    void doWithDynamicMethods() {
        // no-op
    }

    void doWithApplicationContext() {
        // no-op
    }

    void onChange(Map<String, Object> event) {
       // no-op
    }

    void onConfigChange(Map<String, Object> event) {
        // no-op
    }

    void onShutdown(Map<String, Object> event) {
       // no-op
    }
}
