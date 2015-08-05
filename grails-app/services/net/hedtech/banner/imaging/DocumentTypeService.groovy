/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 ****************************************************************************** */
package net.hedtech.banner.imaging

import groovy.sql.Sql
import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.service.ServiceBase
import org.apache.log4j.Logger

import java.sql.SQLException

/**
 * This service helps to get document type for BDM attachments
 */
class DocumentTypeService extends ServiceBase {

    def log = Logger.getLogger(DocumentTypeService.name)

    /**
     *
     * @param text
     * @param max
     * @param offset
     * @return
     */
     def getCommonMatchingDocs(text, max, offset) {
        def filterText = BdmUtility.getLikeFormattedFilter(text)
        def paginationParams = BdmUtility.getPagingParams( max, offset )
        def documents = []
        def sql
        try {
            sql = new Sql(BdmUtility.getConnection())
            def commonMatchSql = "SELECT e.ETVDTYP_CODE AS code, e.ETVDTYP_DESC AS DESCRIPTION \
                FROM ETVDTYP e, otgmgr.ul496_3 \
                WHERE e.ETVDTYP_CODE = otgmgr.ul496_3.item \
                AND (UPPER(e.ETVDTYP_CODE) LIKE :docTypCode OR  UPPER(e.ETVDTYP_DESC) LIKE :docTypDesc)"

            sql.eachRow(commonMatchSql, [docTypCode: filterText, docTypDesc:filterText],
                    paginationParams?.offset, paginationParams?.max) { commonMatchDoc ->
                documents << ['code' : commonMatchDoc.code, 'description' : commonMatchDoc.description]
            }
        }catch (SQLException ae) {
            throw new ApplicationException(DocumentTypeService,ae)
        }
        finally {
            sql?.close()
        }
        return documents
    }
}
