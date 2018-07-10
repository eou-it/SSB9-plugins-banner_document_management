/*
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 */

// Clean up files from stagingDir - DET doesn't have to be in War
eventCreateWarStart = { warName, stagingDir ->
    println "Move BDM jar files to web-inf/lib"

    def pluginBasedir=bannerDocumentManagementPluginDir.toString().replace('\\','/')
    def srcDir="$pluginBasedir/lib"
    def destDir="${stagingDir}/WEB-INF/lib/"

    ant.copy(todir: "${destDir}")  {
        fileset(dir:"${srcDir}")
    }
}
