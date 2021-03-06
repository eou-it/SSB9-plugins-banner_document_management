/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import net.hedtech.banner.testing.BaseIntegrationTestCase
import org.junit.After
import org.junit.Before
import org.junit.Test

@Integration
@Rollback
class DocumentTypeServiceIntegrationTests extends BaseIntegrationTestCase{

    def documentTypeService = new DocumentTypeService();

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
    void testFetchDocumentTypesByCode() {
        def documents =  documentTypeService.getCommonMatchingDocs ("I", 10, 0)
        assertNotNull( documents )
    }

    @Test
    void testFetchDocumentTypesByDesc() {
        def documents =  documentTypeService.getCommonMatchingDocs("I", 10, 0)
        assertNotNull( documents )
    }
}
