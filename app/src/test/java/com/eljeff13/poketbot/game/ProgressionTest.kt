package com.eljeff13.poketbot.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {

    @Test
    fun `a fresh save starts with the free starter bot only`() {
        val save = SaveState()

        assertEquals(setOf(BotCatalog.starterId), save.unlocked)
        assertEquals(BotCatalog.starterId, save.activeBotId)
        assertEquals(0, save.scrap)
        assertEquals(1, save.nextStage)
    }

    @Test
    fun `upgrades cost scrap and raise the matching stat`() {
        val save = SaveState(scrap = 1000)
        val before = Progression.statsFor(save, BotCatalog.starterId)

        val after = Progression.applyUpgrade(save, BotCatalog.starterId, UpgradeTrack.ATTACK)
        val stats = Progression.statsFor(after, BotCatalog.starterId)

        assertEquals(1000 - Progression.upgradeCost(0), after.scrap)
        assertEquals(before.attack + Progression.gainPerLevel(UpgradeTrack.ATTACK), stats.attack)
        assertEquals(before.hp, stats.hp)
    }

    @Test
    fun `an unaffordable upgrade changes nothing`() {
        val save = SaveState(scrap = 0)
        assertFalse(Progression.canUpgrade(save, BotCatalog.starterId, UpgradeTrack.HP))
        assertEquals(save, Progression.applyUpgrade(save, BotCatalog.starterId, UpgradeTrack.HP))
    }

    @Test
    fun `upgrades stop at the level cap`() {
        var save = SaveState(scrap = 100_000)
        repeat(Progression.MAX_LEVEL + 5) {
            save = Progression.applyUpgrade(save, BotCatalog.starterId, UpgradeTrack.SPEED)
        }

        assertEquals(Progression.MAX_LEVEL, save.upgradesFor(BotCatalog.starterId).speed)
        assertFalse(Progression.canUpgrade(save, BotCatalog.starterId, UpgradeTrack.SPEED))
    }

    @Test
    fun `unlocking a bot spends its price and makes it active`() {
        val save = SaveState(scrap = BotCatalog.voltik.unlockCost)
        val after = Progression.applyUnlock(save, BotCatalog.voltik.id)

        assertTrue(after.isUnlocked(BotCatalog.voltik.id))
        assertEquals(BotCatalog.voltik.id, after.activeBotId)
        assertEquals(0, after.scrap)
    }

    @Test
    fun `a bot cannot be unlocked twice or bought without scrap`() {
        val poor = SaveState(scrap = 10)
        assertEquals(poor, Progression.applyUnlock(poor, BotCatalog.oxide.id))

        val owner = SaveState(scrap = 5000, unlocked = setOf(BotCatalog.starterId, BotCatalog.medix.id))
        assertEquals(owner, Progression.applyUnlock(owner, BotCatalog.medix.id))
    }

    @Test
    fun `winning a new stage advances the campaign and pays scrap`() {
        val save = SaveState()
        val after = Progression.applyBattleResult(save, stage = 1, won = true, flawless = false)

        assertEquals(1, after.stagesCleared)
        assertEquals(1, after.battlesWon)
        assertTrue(after.scrap > 0)
        assertEquals(2, after.nextStage)
    }

    @Test
    fun `losing changes nothing`() {
        val save = SaveState(scrap = 300, stagesCleared = 4)
        assertEquals(save, Progression.applyBattleResult(save, stage = 5, won = false, flawless = false))
    }

    @Test
    fun `replaying a cleared stage pays less and does not rewind progress`() {
        val save = SaveState(stagesCleared = 5)
        val replay = Progression.applyBattleResult(save, stage = 2, won = true, flawless = false)
        val fresh = Progression.applyBattleResult(SaveState(stagesCleared = 1), stage = 2, won = true, flawless = false)

        assertEquals(5, replay.stagesCleared)
        assertTrue("replays should pay less", replay.scrap < fresh.scrap)
        assertTrue(replay.scrap > 0)
    }

    @Test
    fun `only the next stage is playable`() {
        val save = SaveState(stagesCleared = 3)

        assertTrue(save.isStagePlayable(1))
        assertTrue(save.isStagePlayable(4))
        assertFalse(save.isStagePlayable(5))
        assertFalse(save.isStagePlayable(0))
        assertFalse(save.isStagePlayable(Campaign.STAGES + 1))
    }

    @Test
    fun `the campaign ends after the final stage`() {
        val save = SaveState(stagesCleared = Campaign.STAGES - 1)
        val after = Progression.applyBattleResult(save, Campaign.STAGES, won = true, flawless = true)

        assertTrue(after.campaignComplete)
        assertEquals(Campaign.STAGES, after.nextStage)
    }

    @Test
    fun `flawless wins pay a bonus`() {
        val plain = Campaign.scrapReward(3, flawless = false, replay = false)
        val clean = Campaign.scrapReward(3, flawless = true, replay = false)

        assertNotEquals(plain, clean)
        assertTrue(clean > plain)
    }

    @Test
    fun `enemies scale up across the campaign`() {
        val enemies = (1..Campaign.STAGES).map { Campaign.enemyFor(it) }

        assertTrue(enemies.last().stats.attack > enemies.first().stats.attack * 2)
        assertTrue(Campaign.difficulty(20) > Campaign.difficulty(19))
        assertTrue(enemies.all { it.name.isNotBlank() && it.hp == it.maxHp })
    }

    @Test
    fun `stage generation is stable across calls`() {
        val first = Campaign.enemyFor(7)
        val second = Campaign.enemyFor(7)

        assertEquals(first.name, second.name)
        assertEquals(first.stats, second.stats)
    }
}
