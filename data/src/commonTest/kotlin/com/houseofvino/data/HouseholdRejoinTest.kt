package com.houseofvino.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HouseholdRejoinTest {
    @Test
    fun rescanningTheSameInviteAfterStopSharingMakesItsKeyCurrentAgain() {
        val app = TestApp()
        val cid = app.collectionId
        val invite = app.household.create(cid, "Our cellar").copy(d = "partner-device", dn = "Partner")
        val keyId = app.household.key(cid)!!.id
        app.household.stopSharing(cid)
        assertNull(app.household.key(cid))
        app.household.join(invite, cid)
        assertNotNull(app.household.key(cid))
        assertEquals(keyId, app.household.key(cid)!!.id)
    }
}
