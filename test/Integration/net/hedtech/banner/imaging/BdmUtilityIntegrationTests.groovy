/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import net.hedtech.banner.testing.BaseIntegrationTestCase
import org.junit.After
import org.junit.Before
import org.junit.Test

class BdmUtilityIntegrationTests extends BaseIntegrationTestCase{


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
    void testIfBDMExists() {
        def isBDMInstalled = BdmUtility.isBDMInstalled()
        assertTrue ( isBDMInstalled )
    }

}
