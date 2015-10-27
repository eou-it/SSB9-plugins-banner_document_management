/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.banner.restfulapi.RestfulApiValidationUtility
import net.hedtech.bdm.vo.ViewDocVO
import net.hedtech.restfulapi.PagedResultArrayList
import org.codehaus.groovy.grails.commons.ConfigurationHolder

class BdmCompositeAttachmentService {

    def bdmAttachmentService
    def messageSource


    /** Fetch the documents from Ax server and  wrap the
     * document details(indexes) into Decorator and return
     * the result as PagedResultArrayList  */
    def list(Map params){
        log.debug("Getting list of BDM documents :" + params)

        String vpdiCode = (params?.vpdiCode == null || params?.vpdiCode == "" || params?.vpdiCode == "null")?null:params?.vpdiCode
        Map bdmServerConfigurations =BdmUtility.getBdmServerConfigurations()
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
            def docRef =(map.get("DataSource") +"/" + map.get("AppName") +"/" + map.get("DocId")).bytes.encodeBase64()?.toString()
            def documentDecorator = new BdmAttachmentDecorator()
            documentDecorator.dmType = map.get("AppName")
            documentDecorator.docId = map.get("DocId")
            documentDecorator.docRef = docRef
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


    def create(Map params) {
        log.debug("Creating BDM documents :" + params)

        String vpdiCode = (params?.vpdiCode == null || params?.vpdiCode == "" || params?.vpdiCode == "null") ? null : params?.vpdiCode

        Map bdmServerConfigurations = BdmUtility.getBdmServerConfigurations()
        bdmServerConfigurations.put("AppName", params.dmType)

        if (!bdmServerConfigurations.get("AppName")) {
            throw new ApplicationException("BDM-Documents", new BusinessLogicValidationException("invalid.appName.request", []))
        }

        def decorators = uploadDocToAX(params, bdmServerConfigurations, vpdiCode)

        log.info("Uploaded file to AX successfully. Response details :" + decorators)
        return decorators
    }

    //TODO : DO JSON validation
    private def uploadDocToAX(params ,bdmServerConfigurations ,vpdiCode){
        def decorators = []
        def tempPath = ConfigurationHolder.config.bdmserver.file.location
        params.get('fileRefs')?.each { String fileRefPath ->
            File fileDest = new File(tempPath, fileRefPath)
            if (!fileDest.exists() ) {
                throw new ApplicationException("BDM-Documents", new BusinessLogicValidationException("invalid.fileRef.request", [fileRefPath]))
            }

            bdmAttachmentService.createDocument(bdmServerConfigurations, fileDest.absolutePath, params.indexes, vpdiCode)
            def decorator = getBdmAttachementDecorators(bdmAttachmentService.viewDocument(bdmServerConfigurations, params.indexes, vpdiCode))
            decorators << decorator[0]
        }
        return decorators[0]
    }

    //TODO: Need to update
    def delete(Map params)throws ApplicationException{
        String vpdiCode = params?.vpdiCode
        Map bdmServerConfigurations =BdmUtility.getBdmServerConfigurations()
        if(!(params.id)){
            def criteria =(isQueryWithAttributes(params))?addCriteria([:] ,params) :getDocIds(params)

            if(!criteria){
                throw new ApplicationException("BDM-Documents" , new BusinessLogicValidationException("invalid.delete.request", [] ))
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

    def update(Map params)throws ApplicationException{

        String docRef = params.docRef
        String vpdiCode = (params?.vpdiCode == null || params?.vpdiCode == "" || params?.vpdiCode == "null") ? null : params?.vpdiCode

        Map bdmServerConfigurations = BdmUtility.getBdmServerConfigurations()
        bdmServerConfigurations.put("AppName", params.dmType)

        bdmAttachmentService.updateDocument(bdmServerConfigurations, docRef, params.indexes, vpdiCode)
        print params
    }
}
