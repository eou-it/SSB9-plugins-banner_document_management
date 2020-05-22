/*******************************************************************************
 Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.banner.restfulapi.RestfulApiRequestParams
import net.hedtech.banner.restfulapi.RestfulApiValidationUtility
import net.hedtech.restfulapi.PagedResultArrayList
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils
import grails.util.Holders

import org.json.JSONObject

/**
 * Service class which interacts with bdmAttachmentService
 * to make CRUD operation in AX .
 * */
class BdmCompositeAttachmentService {

    def bdmAttachmentService
    final String SUPPORTED_OPERATOR = "#OR"

    //Delete functionality by passing docref and ID for result check log file - DM
    def delete(Map params) {
        ArrayList<Integer> arrayList = new ArrayList<Integer>()
        String vpdiCode = getVpdiCode()
        JSONObject result = null;
        Map bdmServerConfigurations = BdmUtility.getBdmServerConfigurations(params?.dmType)

        if (!(params.id)) {
            def criteria = (params.containsKey("indexes")) ? addCriteria([:], params) : getDocIds(params)

            if (!criteria) {
                throw new ApplicationException("BDM-Documents", new BusinessLogicValidationException("Invalid.Delete.Request", []))
            }

            result = bdmAttachmentService.deleteDocument(bdmServerConfigurations, criteria, vpdiCode)
            log.info("Return Message from AX for batch deletion " + result)

        } else {
            String docref = decodeDocRef(params.id)
            String[] token = docref.split("/");
            bdmServerConfigurations.put("AppName", token[1]);
            arrayList.add(Integer.parseInt(token[2]));
            result = bdmAttachmentService.deleteDocument(bdmServerConfigurations, arrayList, vpdiCode);

            log.info("Return Message from AX While deleting by DocRef " + result)

        }

        log.debug("Delete return object=" + result)
//        Map infoMap = [:]
//        infoMap.put("status",result.get("Message"))
//        infoMap.put("DocID",result.get("DocId"))
//        log.debug(infoMap)
        return result
    }// delete end

    /** Fetch a particular document from Ax server
     *  and wrap the document details(indexes) into
     *  Decorator */
    def get(def encodedDocRef) {
        log.debug("BdmCompositeAttachmentService Show :: Encoded document reference ::" + encodedDocRef)

        def docRefMap = getConfigDetailsAndDocId(encodedDocRef)
        def bdmConfigurations = BdmUtility.getBdmServerConfigurations(docRefMap[1], docRefMap[0])
        def documentDecorator = getDocumentDecorator(bdmAttachmentService.searchDocument(bdmConfigurations, new String(encodedDocRef.decodeBase64()), getVpdiCode()))
        log.debug("BdmCompositeAttachmentService Show :: Response Details ::" + documentDecorator)
        documentDecorator
    }


    private def getConfigDetailsAndDocId(def encodedData) {
        validateEncodedData(encodedData)
        byte[] decoded = encodedData.decodeBase64()
        def decodedDocRef = new String(decoded)

        log.info("Decoded document reference is ::" + decodedDocRef)

        def decodedData = decodedDocRef.split("/")
        if (decodedData.length != 3) {
            log.error("Invalid document reference in the decoded doc ref , Decoded doc ref :: " + new String(decoded))
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.DocRef.Data", [encodedData]))
        }

        decodedData
    }

    /** Fetch the documents from Ax server and  wrap the
     * document details(indexes) into Decorator and return
     * the result as PagedResultArrayList  */
    def list(Map params) {
        log.debug("Getting list of BDM documents :" + params)
        Map bdmServerConfigurations = validateAndGetBdmServerConfigurations(params)
        boolean isPostOperation = RestfulApiValidationUtility.isQApiRequest(params)
        def criteriaList = []

        if (isPostOperation) {
            getListOfCriteria(criteriaList, params)
            log.debug("Post operations criteria  :" + criteriaList)
        }
        def decorators = getBdmAttachementDecorators(bdmAttachmentService.searchDocument(bdmServerConfigurations, criteriaList, getVpdiCode()))
        log.trace("List of BDM documents details ::" + decorators)
        new PagedResultArrayList(decorators, decorators.size())
    }

    /**
     * Wrap the document details into decorator and
     * return the list of decorators
     * */
    private def getBdmAttachementDecorators(List resourceDetails) {

        log.debug("GetBdmAttachementDecorators : " + resourceDetails)

        def decorators = []
        resourceDetails?.each { org.json.JSONObject resourceDetail ->
            decorators << getDocumentDecorator(resourceDetail)
        }

        log.debug("Decorator details :" + decorators)
        decorators
    }

    private def getDocumentDeleteDecorator(org.json.JSONObject resourceDetail1) {
        def documentDecorator1 = new BdmAttachmentDecorator()
        documentDecorator1.docRef = resourceDetail1.opt("Message")
        documentDecorator1.dmType = resourceDetail1.opt("Message")
        documentDecorator1.docId = resourceDetail1.opt("DocId")
        documentDecorator1.indexes = null
        return documentDecorator1
    }

    private def getDocumentDecorator(org.json.JSONObject resourceDetail) {
        def documentDecorator = new BdmAttachmentDecorator()
        documentDecorator.docRef = resourceDetail.opt('docRef')?.encodeAsBase64()
        documentDecorator.dmType = resourceDetail.opt("dmType")
        documentDecorator.docId = resourceDetail.opt("docId")
        documentDecorator.indexes = toMap(resourceDetail.opt("indexes"))
        return documentDecorator
    }

    /** Converts JSONObject to map used by view BDM doc method*/
    private def toMap(org.json.JSONObject indexes) {
        def indexValuesAsMap = [:]
        def keys = indexes.keys()
        while (keys.hasNext()) {
            def key = keys.next()
            indexValuesAsMap.put(key, indexes.opt(key))
        }
        indexValuesAsMap
    }

    /**
     * Used by list method for supporting Logical operator
     * in the INPUT json
     * */
    private def getListOfCriteria(List criterias, Map params) {
        Map indexes = params.get("indexes")
        println("indexes=" + indexes)
        try {
            if (indexes?.containsKey(SUPPORTED_OPERATOR)) {
                println("supported_operator=" + SUPPORTED_OPERATOR)
                org.json.JSONArray indexValues = indexes.get(SUPPORTED_OPERATOR)
                def indexLength = indexValues.length()
                (0..(indexLength - 1)).each { length ->
                    criterias << indexValues.getJSONObject(length)
                }
            } else {
                criterias << new org.json.JSONObject(indexes)
            }
        } catch (e) {
            log.error "Error in input data", e
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.Index.Value", []))
        }
        criterias
    }

    /**
     * Used when querying using QAPI . Add query criteria to the document.
     * */
    private def addCriteria(Map criteria, Map params) {
        Map indexes = params.get("indexes")
        if (indexes && indexes instanceof Map) {
            indexes.each { key, value ->
                criteria.put(key, value)
            }
        }
        criteria
    }

    /**
     * Returns number of records return by the list method.
     * Called automatically by RestfulServiceAdapter
     * */
    def count(Map params) {
        list(params)?.size()
    }

    /**
     * Take the uploaded document from temp path
     * and push the document with indexes to AX
     * */
    def create(Map params) {
        log.debug("Creating BDM documents :" + params)

        def bdmServerConfigurations = validateAndGetBdmServerConfigurations(params)
        def decorators = createDocumentInAX(params, bdmServerConfigurations, getVpdiCode())

        log.info("Uploaded file to AX successfully. Response details :" + decorators)
        return decorators
    }


    private def validateAndGetBdmServerConfigurations(def params) {

        if (!params?.dmType) {
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.AppName.Request", []))
        }

        Map bdmServerConfigurations = BdmUtility.getBdmServerConfigurations(params?.dmType, params?.BdmDataSource)

        return bdmServerConfigurations
    }

    /**
     * Take the uploaded document from temp path
     * and push the document with indexes to AX
     * */
    private def createDocumentInAX(params, bdmServerConfigurations, vpdiCode) {
        def dir = ""
        // Get Instance of BDMFileEncrypt
        def bdmFileEncrypt = new BDMFileEncrypt()

        String tempPath = Holders?.config.bdmserver.file.location

        def documentDecorator
        params.get('fileRefs')?.each { String fileRefPath -> // If two or more files are pushed to doc then all are uploaded but that is not allowed as of now
            fileRefPath = bdmFileEncrypt.decrypt(fileRefPath)

            File fileDest = new File(tempPath, fileRefPath)
            if (!fileDest.exists()) {
                throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.FileRef.Request", [fileRefPath]))
            }

            def docRef = bdmAttachmentService.createDocument(bdmServerConfigurations, fileDest.absolutePath, params.indexes, vpdiCode)
            documentDecorator = getDocumentDecorator(bdmAttachmentService.searchDocument(bdmServerConfigurations, docRef, vpdiCode))

            // clean up temporary file
            deleteDirectory(tempPath, fileRefPath)
        }
        documentDecorator
    }

    /**
     * Delete the file once the file is uploaded to AX server
     * @param tempPath
     * @param fileRefPath
     * */
    private def deleteDirectory(tempPath, fileRefPath) {
        try {
            def dir = fileRefPath.split("/");
            FileUtils.deleteDirectory(new File(tempPath, dir[0]));
        } catch (Exception e) {
            //Since file uploading is success but deleting file is throwing exception , just log the exception and pass throw
            log.error("Error deleting the file but upload is success !!", e)
        }
    }


    private def getDocIds(Map params) {
        def docIds = []
        def temp = (params.containsKey("docIds") && params.get("docIds") instanceof List) ? params.get("docIds") : []
        temp.each { docId -> docIds << docId }
        //Casting becuase params.get returns org.codehaus.groovy.grails.web.json.JSONArray
        docIds
    }

    /**
     * Update the particular record and
     * return the index details of the record
     * */
    def update(Map params) throws ApplicationException {

        log.debug("Updating BDM document with details ::" + params)
        def docRef = decodeDocRef(params.docRef)
        log.info("decoded docRef= $docRef")

        def vpdiCode = getVpdiCode()

        def bdmServerConfigurations = validateAndGetBdmServerConfigurations(params)
        bdmAttachmentService.updateDocument(bdmServerConfigurations, docRef, params.indexes, vpdiCode)
        def decorator = getDocumentDecorator(bdmAttachmentService.searchDocument(bdmServerConfigurations, docRef, vpdiCode))

        log.debug("BDM document after updated :: " + decorator)

        decorator
    }

    private def decodeDocRef(String encodedData) {

        def decodedDocRef
        //println("encodeddata="+encodedData)
        //fix for update functionality for non-encode docref
        def isEncodedData = Base64.isArrayByteBase64(encodedData.getBytes() ?: "");
        try {
            decodedDocRef = new String(encodedData.decodeBase64())
        } catch (RuntimeException e) {
            log.debug("e.getmessage=" + e.getMessage())

            if (e.message.equals("bad character in base64 value")) {
                log.warn("it is not recommended to pass decoded docref while update")
                decodedDocRef = encodedData
            }
            // println("e.getmessage="+e.getMessage())
        }
// OLD code
// if (isEncodedData){
//            decodedDocRef = new String(encodedData.decodeBase64())}
//        else{
//            decodedDocRef = encodedData; // Accept docRef isn't encoded
//			}

        log.info("Decoded document reference is ::" + decodedDocRef)

        def decodedData = decodedDocRef.split("/")
        if (decodedData.length != 3) {
            log.error("Invalid document reference in the decoded doc ref , Decoded doc ref :: " + new String(decodedDocRef))
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.DocRef.Data", [encodedData]))
        }

        decodedDocRef
    }

    private def validateEncodedData(String encodedData) {

        def isEncodedData = Base64.isArrayByteBase64(encodedData.getBytes() ?: "");
        if (!isEncodedData) {
            log.error "Invalid encoded document reference ", e

            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.DocRef.Data", [encodedData]))
        }
    }

    private def getVpdiCode() {
        def params = RestfulApiRequestParams.get();
        def mepCode = params.get("mepCode")
        return mepCode;
    }
}
