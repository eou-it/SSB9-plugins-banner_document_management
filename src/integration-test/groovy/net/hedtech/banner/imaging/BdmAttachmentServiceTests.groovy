/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import net.hedtech.banner.testing.BaseIntegrationTestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback


@Integration
@Rollback
class BdmAttachmentServiceTests extends BaseIntegrationTestCase{

    @Before
    public void setUp() {
        formContext = ['GUAGMNU']
        super.setUp()
    }

    @After
    public void tearDown() {
        super.tearDown()
    }

    @Test
    void testDeleteDocument() {

        def bdmAttachmentService = new BdmAttachmentService()

        // Get BDM configuration
        Map bdmServerConfigurations = BdmUtility.getBdmServerConfigurations()
        // Enter the Application Name for which Data should be deleted
        bdmServerConfigurations.put("AppName", "B-A-ID");
        //
        // ArrayList docIds, String vpdiCode
        ArrayList<Integer> arrayList = new ArrayList<Integer>()
        // Insert the DOC ID to be deleted
        arrayList.add(Integer.parseInt('121'))

        String vpdiCode= null
        String result = null

        result = bdmAttachmentService.deleteDocument(bdmServerConfigurations, arrayList, vpdiCode);

       assertNull(result)

    }

}
