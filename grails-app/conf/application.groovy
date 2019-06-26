/*******************************************************************************
 Copyright 2015-2018 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
dataSource {

    dialect = "org.hibernate.dialect.Oracle10gDialect"
    loggingSql = false
}


hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory'
    //hbm2ddl.auto = null
    show_sql = false
    packagesToScan="net.hedtech.**.*"
    flush.mode = AUTO
    dialect = "org.hibernate.dialect.Oracle10gDialect"
    config.location = [
          //  "classpath:applicationContext.xml"
         //"classpath:hibernate-banner-core.cfg.xml",
         // "classpath:hibernate-banner-general-validation-common.cfg.xml",
       //  "classpath:hibernate-banner-general-person.cfg.xml",
    ]
}

/*
environments {
    test {
        grails.plugin.springsecurity.saml.active = false
        grails.plugin.springsecurity.cas.active = false
    }
}
*/

//Added for integration tests to run in plugin level
grails.config.locations = [
        BANNER_APP_CONFIG: "banner_configuration.groovy",
]
// environment specific settings
environments {
    development {
        dataSource {
        }
    }
    test {
        dataSource {
        }
    }
    production {
        dataSource {
        }
    }
}

/*
grails.config.locations = [
        BANNER_APP_CONFIG                    : "banner_configuration.groovy",
        BANNER_DOCUMENT_MANAGEMENT_API_CONFIG: "DocumentManagementApi_configuration.groovy"
]
*/