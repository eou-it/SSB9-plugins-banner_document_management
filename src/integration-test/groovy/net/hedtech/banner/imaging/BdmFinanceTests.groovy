/*******************************************************************************
 Copyright 2020 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.util.Holders
import groovy.sql.Sql
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import net.hedtech.banner.testing.BaseIntegrationTestCase
import org.hibernate.SessionFactory
import org.json.JSONArray
import org.junit.Test

import java.sql.SQLException

@Integration
@Rollback
class BdmFinanceTests extends BaseIntegrationTestCase {

    //  Constant for JSON and ArrayList
    private static final int DOCUMENT_ID_INDEX = 0;
    private static final int DOCUMENT_TYPE_INDEX = 1;
    private static final String VALID = "T"
    private static final String INVALID = "F"
    BdmFinanceService bfs = new BdmFinanceService()

    @Test
    void testGetFinanceDocument() {

        BdmFinanceService bfs = new BdmFinanceService()

        JSONArray jaInput = getInput()
        def inputLength = jaInput.length()
        JSONArray jaOuput = bfs.getFinanceDocument(jaInput)
        def outputLength = jaOuput.length()

        assertTrue(inputLength == outputLength)

    }

    // Get Sample input for the end-point - ONLY FOR TESTING PURPOSE
    public JSONArray getInput() {

        SessionFactory sessionFactory = Holders.getGrailsApplication().getMainContext().sessionFactory

        def sql = new Sql(sessionFactory.getCurrentSession().connection())

        // Get the predicate for the SQL statement
        StringBuilder sb = new StringBuilder()

        sb.append("select FIELD1,FIELD3 from ae_dt506 where rownum<=10") // where rownum<=350
        // end

        def inputList = []
        try {

            sql.eachRow(sb.toString()) {
                p ->
                    def documentId = p[DOCUMENT_ID_INDEX]
                    def documentType = p[DOCUMENT_TYPE_INDEX]
                    documentType = bfs.btA.get(documentType, documentType)
                    inputList << [documentId, documentType, INVALID]
                    //inputList << [documentId, documentType]

            }

            // Converting List TO JSON
            def abc = bfs.listToJsonArray(inputList)

            return abc

        } catch (SQLException sqle) {
            println("SQL ERROR occurred in TEST CLASS -> getInput" + sqle.getMessage())
            throw sqle
        } catch (Exception e) {
            println("Error Ocuured while getting the input in TEST CLASS, e.message=" + e.message + " and  e=" + e)

        } finally {

        }
    }

}
