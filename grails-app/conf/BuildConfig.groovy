grails.project.dependency.resolver = "ivy"
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.plugin.location.'banner-core' = "../banner_core.git"
grails.plugin.location.'banner-seeddata-catalog' = "../banner_seeddata_catalog.git"
grails.plugin.location.'banner-codenarc' = "../banner_codenarc.git"
grails.plugin.location.'i18n-core'="../i18n_core.git"
grails.plugin.location.'banner-general-validation-common' = "../banner_general_validation_common.git"
grails.plugin.location.'banner-general-person' = "../banner_general_person.git"


grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'

    repositories {
        if (System.properties['PROXY_SERVER_NAME']) {
            mavenRepo "${System.properties['PROXY_SERVER_NAME']}"
        } else {
            grailsPlugins()
            grailsHome()
            grailsCentral()
            mavenCentral()
            mavenRepo "http://repository.jboss.org/maven2/"
            mavenRepo "http://repository.codehaus.org"
        }
    }

    plugins {
        runtime ":hibernate:3.6.10.10"
        build ":tomcat:7.0.52.1"
        test ':code-coverage:1.2.5'
        compile ":functional-test:2.0.0"
        runtime ":webxml:1.4.1"
    }

    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        // Note: elvyx-1.0.24_beta.jar remains in the lib/ directory of the project as it is not available in a public repo due to licensing issues.
        build 'org.antlr:antlr:3.2',
              'com.thoughtworks.xstream:xstream:1.2.1',
              'javassist:javassist:3.8.0.GA'
        runtime "javax.servlet:jstl:1.1.2"

        runtime 'org.springframework:spring-test:3.1.0.RELEASE'

        runtime 'com.googlecode.json-simple:json-simple:1.1'

        runtime 'org.json:json:20090211'

        runtime 'jdom:jdom:1.0'

        runtime 'commons-codec:commons-codec:1.3'

    }
}
