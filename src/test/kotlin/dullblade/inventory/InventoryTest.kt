package dullblade.inventory

import dullblade.*
import dullblade.queue.PacketForgeQueueDataNotify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TestGameSession : GameSession() {
    private val packets = mutableListOf<BasePacket>()

    override fun send(vararg packets: PacketForgeQueueDataNotify) {
        this.packets.addAll(packets)
    }
}

val account = Account(12345, "login", "70k3n")
val session = TestGameSession().also { it.account = account }

internal class InventoryManagerTest {
    @Test
    fun weaponTotalExpDataTest() {
        assertEquals(9730500, weaponTotalExpData[4][89])
    }

    @Test
    fun upgradeWeapon() {
        // add 3 dull blades and some ore to inventory
        val dullbladeId = 11101
        val weapon = Weapon(account.newGuid(), dullbladeId)
        val dullblade1 = Weapon(account.newGuid(), dullbladeId)
        val dullblade2 = Weapon(account.newGuid(), dullbladeId)
        account.inventory.weapons[weapon.guid] = weapon
        account.inventory.weapons[dullblade1.guid] = dullblade1
        account.inventory.weapons[dullblade2.guid] = dullblade2
        val fodderWeaponGuids = listOf(dullblade1.guid, dullblade2.guid)
        val ore1 = Material(account.newGuid(), InventoryService.WEAPON_ORE_1, 100)
        val ore2 = Material(account.newGuid(), InventoryService.WEAPON_ORE_2, 100)
        account.inventory.materials[ore1.id] = ore1
        account.inventory.materials[ore2.id] = ore2
        val ores = listOf(CostItem(ore1.id, 5), CostItem(ore2.id, 5))
        account.inventory.mora = 100000

        // upgrade dullblade with 2 other dullblades and some ore
        InventoryService.upgradeWeapon(session, weapon.guid, fodderWeaponGuids, ores)
        assertEquals(95, account.inventory.materials[ore1.id]?.count)
        assertEquals(95, account.inventory.materials[ore2.id]?.count)
        assertEquals(14, weapon.level)
        assertEquals(1225, weapon.exp)

        // calc leftover for lv20 upgrade
        val ore3 = Material(account.newGuid(), InventoryService.WEAPON_ORE_3, 100)
        account.inventory.materials[ore3.id] = ore3
        val ores0 = listOf(CostItem(ore3.id, 2))
        val leftovers = InventoryService.calcWeaponUpgradeLeftovers(session, weapon.guid, emptyList(), ores0)
        assertEquals(2, leftovers.size)
        assertEquals(2, leftovers[0].second)
        assertEquals(4, leftovers[1].second)

        // upgrade dullblade to lv20
        val packets0 = InventoryService.upgradeWeapon(session, weapon.guid, emptyList(), ores0)
        assertEquals(98, account.inventory.materials[ore3.id]?.count)
        assertEquals(97, account.inventory.materials[ore2.id]?.count)
        assertEquals(99, account.inventory.materials[ore1.id]?.count)
        assertEquals(20, weapon.level)
        assertEquals(0, weapon.exp)
        println(packets0)

        // ascend dullblade
        account.inventory.materials[1000] = Material(account.newGuid(), 1000, 100)
        account.inventory.materials[1001] = Material(account.newGuid(), 1001, 100)
        account.inventory.materials[1002] = Material(account.newGuid(), 1002, 100)
        val packets3 = InventoryService.promoteWeapon(session, weapon.guid)
        assertEquals(100, account.inventory.materials[1000]?.count)
        assertEquals(100, account.inventory.materials[1001]?.count)
        assertEquals(100, account.inventory.materials[1002]?.count)
        assertEquals(20, weapon.level)
        assertEquals(0, weapon.exp)
        assertEquals(1, weapon.promoteLevel)
        println(packets3)

        // lock weapon
        InventoryService.lockWeapon(session, weapon.guid, true)
        assertTrue(weapon.locked)

        // refine weapon
        val dullblade4 = Weapon(account.newGuid(), dullbladeId)
        account.inventory.weapons[dullblade4.guid] = dullblade4
        InventoryService.refineWeapon(session, weapon.guid, dullblade4.guid)
        assertEquals(1, weapon.refinement)
    }

    @Test
    fun upgradeRelic() {
        // add 3 relics (adventurer feather) and some ore to inventory
        val relics = listOf(
            Relic(account.newGuid(), 51120, 12001, mutableListOf()),
            Relic(account.newGuid(), 51120, 12001, mutableListOf()),
            Relic(account.newGuid(), 51120, 12001, mutableListOf())
        )
        account.inventory.relics.putAll(relics.associateBy(Relic::guid))
        account.inventory.materials.putAll(listOf(
            Material(account.newGuid(), 105002, 20)
        ).associateBy { it.id })
        account.inventory.mora = 100000

        // upgrade with 2 other relics and one material
        Rng.mode = Rng.Mode.UNLUCKIEST
        val relic = relics[0]
        InventoryService.upgradeRelic(
            session, relic.guid, listOf(relics[1].guid),
            listOf(CostItem(105002, 1))
        )
        assertEquals(19, account.inventory.materials[105002]?.count)
        assertEquals(3, relic.level)
        assertEquals(695, relic.exp)
        InventoryService.upgradeRelic(session, relic.guid, listOf(relics[2].guid))
        assertEquals(4, relic.level)
        assertEquals(0, relic.exp)
        assertEquals(1, relic.appendPropIdList.size)
        assertEquals(101021, relic.appendPropIdList[0]) // flat HP low roll
    }
}