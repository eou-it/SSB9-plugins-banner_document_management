/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.banner.general.person.PersonIdentificationName
import net.hedtech.banner.positioncontrol.utils.PositionDescriptionStatus
import net.hedtech.bdm.exception.BdmsException
import org.codehaus.groovy.grails.commons.ConfigurationHolder as CH
import org.json.JSONObject

import java.text.DateFormat
import java.text.SimpleDateFormat

class BdmAttachmentCompositeService {

    def bdmAttachmentService
    def positionDescriptionService
    def grailsApplication

    final String BDM_DATE_FORMAT = "YYYY-MM-dd HH:mm:ss"

    /**
     * This Method is Used to upload document to BDM server using BDM Client API
     * @param file : Multipart file uploaded by user
     * @param posDescId : position desc id for which doucument is being uploaded
     * @param ownerPidm : UserId
     * @param vpdiCode : vpdi Code
     * @return : redirect to list view of docs
     */
    def uploadDocument(file, posDescId, docType, ownerPidm, vpdiCode){
        File fileDest
        String fileName

        try {
            String tempPath =  CH.config.bdm.file.location

            fileName = file.getOriginalFilename()
            String hashedName = java.util.UUID.randomUUID().toString()

            File userDir = new File(tempPath, hashedName)
            userDir.mkdir()

            fileDest = new File(userDir, fileName)
            file.transferTo(fileDest)

            def absoluteFileName = fileDest.getAbsolutePath()
            uploadDocToBdmServer(posDescId, docType, ownerPidm, fileName, absoluteFileName, vpdiCode)
            userDir.deleteDir()
        } catch(FileNotFoundException fnfe){
            throw new ApplicationException(BdmAttachmentCompositeService, new BusinessLogicValidationException(fnfe.getMessage(), []))
        } catch (BdmsException bdme) {
            throw new ApplicationException(BdmAttachmentCompositeService, new BusinessLogicValidationException(bdme.getMessage(), []))
        }

    }

    /**
     * This Method is Used private method helps to upload document to BDM server using BDM Client API
     * @param posDescId : position desc id for which doucument is being uploaded
     * @param docType : Supplied as per user
     * @param ownerPidm : User Id
     * @param fileName : Name of file uploaded
     * @param absoluteFileName : As After file is being uploaded we are storing at server before uploading to bdm
     * server in order to avoid concurrency
     * @param vpdiCode
     * @return
     * @throws BdmsException
     */
    def uploadDocToBdmServer(posDescId, docType, ownerPidm, fileName, absoluteFileName, vpdiCode) throws BdmsException{

        //Hardcoded Dateformat expected by external BDM application
        DateFormat srcDf = new SimpleDateFormat(BDM_DATE_FORMAT)

        def positionDescription = positionDescriptionService.fetchPositionDescById(posDescId)

        JSONObject docAttributes = new JSONObject();
        docAttributes.put("OWNER_PIDM", ownerPidm);
        docAttributes.put("ACTIVITY DATE", srcDf.format(new Date()));
        docAttributes.put("POSITION NUMBER", positionDescription.positionCode?.positionCode ?: "PDESC")
        docAttributes.put("POSITION TITLE", positionDescription.positionTitle ?: "PDESC")
        docAttributes.put("DOCUMENT TYPE", docType)
        docAttributes.put("POSITION CLASS", positionDescription.positionPclsCode)
        docAttributes.put("EMPLOYEE CLASS",  positionDescription.positionEclsCode)
        docAttributes.put("POSITION_DESCRIPTION_ID",  posDescId)
        docAttributes.put("DOCUMENT_NAME",  fileName)
        bdmAttachmentService.createDocument(getBdmParams(), absoluteFileName, docAttributes, vpdiCode)
    }

    /**
     * This method will return List of documents with position description
     * @param posDescId
     * @param vpdiCode
     */
    def listDocumentsByPositionDescription(def posDescId, vpdiCode, userPidm){
        JSONObject queryCriteria = new JSONObject();
        queryCriteria.put("POSITION_DESCRIPTION_ID", posDescId);
        def docList
        try {
            docList = bdmAttachmentService.viewDocument(getBdmParams(), queryCriteria, vpdiCode)
        }catch(BdmsException b){
            throw new ApplicationException(BdmAttachmentCompositeService, new BusinessLogicValidationException(b.getMessage(), []))
        }

        def map = [:]
        def positionDescription = positionDescriptionService.fetchPositionDescByCode(posDescId)

        map.positionDescription = ['id'            : positionDescription.id,
                                   'positionNumber': positionDescription.positionCode?.positionCode,
                                   'status'        : PositionDescriptionStatus.
                                           getPositionDescStatusConstant(positionDescription.positionDescStatus).getStatusValue(),
                                   'positionTitle' : positionDescription.positionTitle,
                                   'pclsCode'      : positionDescription.positionPclsCode,
                                   'pclsCodeDesc'  : positionDescription.positionPclsDesc,
                                   'eclsCode'      : positionDescription.positionEclsCode,
                                   'eclsCodeDesc'  : positionDescription.positionEclsLongDesc ?: positionDescription.positionEclsShortDesc,
                                   'effectiveDate' : positionDescription.positionDescEffectiveDate,
                                   'templateType'  : positionDescription.getTemplateIndicator()
        ]


        def docWithUsername = []
        SimpleDateFormat srcDf = new SimpleDateFormat(BDM_DATE_FORMAT)
        docList.each{doc->
            String ownerPidm = doc.docAttributes.get('OWNER_PIDM')
            String userName = PersonIdentificationName.findByPidm(ownerPidm)?.fullName
            Map docAttrs = doc.docAttributes
            docAttrs.put('USER_NAME', userName)
            docAttrs.put('ACTIVITY_DATE', srcDf.parse(docAttrs['ACTIVITY DATE']))
            doc.docAttributes = docAttrs
            docWithUsername.add(doc)
        }
        map.bdmList = docList
        return map
    }

    /**
     * this method will delete the documents uploaded to BDM server
     * @param posDescId
     * @param vpdiCode
     */
    def deleteDocumentsByPositionDescription(docId, vpdiCode){
        List docIds = new ArrayList<Integer>();
        docIds.add(docId)

        try {
            bdmAttachmentService.deleteDocument(getBdmParams(), docIds, vpdiCode)
        } catch(BdmsException b){
            throw new ApplicationException(BdmAttachmentCompositeService, new BusinessLogicValidationException(b.getMessage(), []))
        }
    }

    /**
     *
     * @return
     */
     private JSONObject getBdmParams(){
        JSONObject bdmParams = new JSONObject()
         CH.config.bdmserver.each{k,v->
            bdmParams.put(k,v)
        }
         log.info("BDMParams :: "+bdmParams)

        return bdmParams
    }
}
