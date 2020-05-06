/*******************************************************************************
 Copyright 2020 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import grails.util.Holders
import groovy.json.JsonOutput
import groovy.sql.Sql
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.bdm.exception.BdmsException
import org.apache.juli.logging.Log
import org.hibernate.SessionFactory
import org.json.JSONArray

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class BdmFinanceService {

    private static final BiMap<String, String> atB = HashBiMap.create();

    private static final BiMap<String, String> btA = atB.inverse()
    static {
        atB.put("REQ", "REQUISITION")
        atB.put("PO", "PURCHASE ORDER")
        atB.put("INV", "INVOICE")
        atB.put("JV", "JOURNAL VOUCHER")
        atB.put("ENC", "ENCUMBRANCES")
        atB.put("DCR", "DIRECT CASH RECEIPT")
        atB.put("CHK", "CHECK")
        atB.put("FAS", "FIXED ASSETS")
    }

    //  Constant for JSON and ArrayList
    private static final int DOCUMENT_ID_INDEX = 0;
    private static final int DOCUMENT_TYPE_INDEX = 1;
    private static final String VALID = "T"
    private static final String INVALID = "F"

    // Tuple for JSON
    class Documents {
        String documentID
        String documentType
        String valid

        Documents(ArrayList l) {
            documentID = l.get(DOCUMENT_ID_INDEX)
            documentType = l.get(DOCUMENT_TYPE_INDEX)
            valid = INVALID
        }

    }

    public JSONArray getFinanceDocument(JSONArray jsonInput) {

        def timeStart = new Date()

        log.info("Getting Finance Document ResultSet")

        SessionFactory sessionFactory = Holders.getGrailsApplication().getMainContext().sessionFactory

        Connection conn = sessionFactory.getCurrentSession().connection()

        def sql = new Sql(conn)

        // Convert the input to ArrayList for Processing
        def inputArray = JsonArraytoList(jsonInput)

        // Convert the inputArray into Modified array for Document Type Conversion
        def sqlinputArray = convertATB(inputArray)

        StringBuilder sb = new StringBuilder()

        String prefix = ""

        sqlinputArray.each {
            item ->
                sb.append("${prefix}")
                prefix = ","
                sb.append("(")
                sb.append("?")
                sb.append(" , ")
                sb.append("")
                sb.append("?")
                sb.append(")")
        }
        sb.append(")")

        def sqlStartTime = new Date()
        def sqlEndTime = new Date()

        try {
            def stmt = "select FIELD1,FIELD3 from ae_dt506 where (field1,field3) in ( " + sb.toString()
            def stt = stmt.toString()
            def outputArray = []

            PreparedStatement ps = conn.prepareStatement(stt)
            ps.setFetchSize(200)

            int psIndex = 1
            sqlinputArray.each {
                item ->
                    ps.setString(psIndex++, "${item.get(DOCUMENT_ID_INDEX)}")
                    ps.setString(psIndex++, "${item.get(DOCUMENT_TYPE_INDEX)}")
            }
            sqlStartTime = new Date()

            ResultSet rs = ps.executeQuery()

            def a1
            def a2
            while (rs.next()) {
                a1 = rs.getString(1)
                a2 = rs.getString(2)
                a2 = btA.get(a2, a2)
                outputArray << [a1, a2, INVALID]
            }

            sqlEndTime = new Date()

            log.debug("Count of Valid Documents for FSS service is : " + outputArray.size())

            outputArray.each {
                def index = inputArray.indexOf(it)
                if (index >= 0) {
                    inputArray.get(index).set(2, VALID)
                }
            }

            def outputToFSS = listToJsonArray(inputArray)

            return outputToFSS
        } catch (SQLException sqle) {
            log.error("SQL error while performing getFinanceDocument : " + sqle.getMessage())

            throw sqle
        } catch (Exception e) {
            log.error("Exception occurred while performing getFinanceDocument : ", e)
            println("e.message=" + e.message + " and  e=" + e)
            throw new ApplicationException(BdmsException, new BusinessLogicValidationException("Invalid.FSS.message", []))

        } finally {
            def timeStop = new Date()
            TimeDuration duration = TimeCategory.minus(timeStop, timeStart)
            TimeDuration sqlTime = TimeCategory.minus(sqlEndTime, sqlStartTime)
            log.info("getFinance Document : SQL execution time is " + sqlTime)
            log.info("Total time is " + duration)
        }

    }

    /*
     Convert Document Type abbreviations to DOCUMENT TYPE
     Example : INV to INVOICE
     */
    def ArrayList convertATB(ArrayList input) {

        ArrayList output = []

        input.each {
            item ->
                output << [item.get(DOCUMENT_ID_INDEX), atB.get(item.get(DOCUMENT_TYPE_INDEX)), INVALID]
        }
        return output

    }

    /*
   * Function to Convert JSONArray to Arraylist
   * */
    def JsonArraytoList(JSONArray ja) {

        def arr = []

        for (int i = 0; i < ja.length(); i++) {
            def jsonObject = ja.get(i)

            def list = [jsonObject.get("documentID"), jsonObject.get("documentType"), INVALID]

            arr.add(list)
        }

        return arr

    }

    /*
    * Function to Convert Arraylist (Nested) to JSONArray
    * */
    JSONArray listToJsonArray(ArrayList resultSample) {

        def DocumentsArray = []
        try {

            resultSample.each {
                it ->
                    def a1 = new Documents(it)
                    DocumentsArray.add(a1)

            }
            def jsonOuput = JsonOutput.toJson(DocumentsArray)
            JSONArray ja = new JSONArray(jsonOuput)

            return ja

        } catch (Exception e) {
            log.error("Error occurred while converting ArrayList to JSON, e.message=" + e.message + " and  e=" + e)
            throw e
        }
    }
}