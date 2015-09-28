/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import net.hedtech.banner.decorators.BdmMessageDecorator
import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.restfulapi.RestfulApiValidationUtility
import net.hedtech.bdm.vo.ViewDocVO
import net.hedtech.restfulapi.PagedResultArrayList
import org.codehaus.groovy.grails.commons.ConfigurationHolder

class BdmCompositeAttachmentService {

    def bdmAttachmentService

    static String tempPath =  ConfigurationHolder.config.bdmserver.file.location

    /** Fetch the documents from Ax server and  wrap the
     * document details(indexes) into Decorator and return
     * the result as PagedResultArrayList  */
    def list(Map params){
        log.debug("Getting list of BDM documents :" + params)

        String vpdiCode = params?.vpdiCode
        Map bdmServerConfigurations =BdmUtility.getBdmServerConfigurations()
        bdmServerConfigurations.put("AppName",params.get("formName"))
        Map criteria = [:]

        boolean isPostOperation =  RestfulApiValidationUtility.isQApiRequest(params)
        if(isPostOperation){
            addCriteria(criteria,params)
            log.debug("Post operations criteria  :" + criteria)
        }

        def decorators = getBdmAttachementDecorators(bdmAttachmentService.viewDocument(bdmServerConfigurations, criteria, vpdiCode))
        return new PagedResultArrayList(decorators ,decorators.size())
    }

    /**
     * Wrap the document details into decorator and
     * return the list of decorators
     * */
    private def getBdmAttachementDecorators(List viewDocVos){
        def decorators =[]
        viewDocVos.each {ViewDocVO viewDocVO ->
            def map = getMapFromUrlString(viewDocVO.viewURLNoCredential)
            def documentDecorator = new BdmAttachmentDecorator()
            documentDecorator.type = map.get("AppName")
            documentDecorator.docId =map.get("DocId")
            documentDecorator.ref = map.get("AppName") +"/" + map.get("DataSource") +"/" + map.get("DocId")
            documentDecorator.indexes = viewDocVO.docAttributes
            decorators << documentDecorator
        }
        log.debug("Decorator details :" + decorators)
        decorators
    }


    /**
     * Used when querying using QAPI . Add query criteria to the document.
     * */
    private  def addCriteria(Map criteria , Map params){
        Map indexes = params.get("indexes")
        if(indexes && indexes instanceof Map){
            indexes.each {key,value ->
                criteria.put(key,value)
            }
        }
        criteria
    }

    /**
     * Parse the urlString and return the docId present in
     * the url
     * */
    private def getDocIdFromUrlString(String urlString){
        return getMapFromUrlString(urlString).get("DocId")?:""
    }

    private Map getMapFromUrlString(String urlString){
        int firstIndex = urlString.indexOf("?");
        urlString = urlString.substring(firstIndex+1);
        String[] splitedData = urlString.split("&");
        def finalMap = [:]
        splitedData.each{token ->
            def tokens = token.split("=")
            finalMap.put(tokens[0],tokens[1])
        }
        return finalMap
    }

    /**
     * Returns number of records return by the list method.
     * Called automatically by RestfulServiceAdapter
     * */
    def count(Map params){
        list(params)?.size()
    }

    //TODO: Need to update
    def delete(Map params)throws ApplicationException{
        String vpdiCode = params?.vpdiCode
        Map bdmServerConfigurations =BdmUtility.getBdmServerConfigurations()
        bdmServerConfigurations.put("AppName",params.get("formName"))
        if(!(params.id)){
            def criteria =(isQueryWithAttributes(params))?addCriteria([:] ,params) :getDocIds(params)

            if(!criteria){
                throw new ApplicationException("BDM-Documents" ,"Delete operation should include either indexes or docIds field in JSON")
            }
            bdmAttachmentService.deleteDocument(bdmServerConfigurations,criteria ,vpdiCode)

        }else if(params.id){
            bdmAttachmentService.deleteDocument(bdmServerConfigurations,[params.id] ,vpdiCode)
        }
    }

    private def getDocIds(Map params){
        def docIds = []
        def temp =   (params.containsKey("docIds") && params.get("docIds") instanceof List)?params.get("docIds") :[]
        temp.each{docId-> docIds << docId } //Casting becuase params.get returns org.codehaus.groovy.grails.web.json.JSONArray
        return docIds
    }

    private boolean isQueryWithAttributes(Map params){
        params.containsKey("indexes")
    }

    private  boolean isQApiWithDelete(Map params) {
        boolean qapiReq = false
        if (params && params.action) {
            if (params.action["POST"] == "delete") {
                qapiReq = true
            }
        }
        return qapiReq
    }

    def create(Map params){
        BdmMessageDecorator message = new BdmMessageDecorator()
        Map infoMap = [:]

        if(params.containsKey("file")){
            def file = params.get("file")
            def map = bdmAttachmentService.createBDMLocation(file)

            String userDir =   map.get('userDir').toString().replace(tempPath,"").replace(map.get('fileName'),"")

            infoMap.put("status","Placed Successfully!")
            infoMap.put("fileName",map.get('fileName'))
            infoMap.put("directory",userDir)
            message.setMessage(infoMap)

            return message
        }else{
            String vpdiCode = params?.vpdiCode

            Map bdmServerConfigurations =BdmUtility.getBdmServerConfigurations()
            bdmServerConfigurations.put("AppName",params.get("formName"))
            String fileDirectory = tempPath + params.get("directory")
            File fileDest = new File(fileDirectory, params.get("fileName"))
            def filePath = fileDest.getAbsolutePath()

            def viewDocVos= bdmAttachmentService.createDocument(bdmServerConfigurations, filePath.toString() , params.indexes, vpdiCode)
            infoMap.put("status","Upload Done!")

            message.setMessage(infoMap)
            return message
        }
    }
}
