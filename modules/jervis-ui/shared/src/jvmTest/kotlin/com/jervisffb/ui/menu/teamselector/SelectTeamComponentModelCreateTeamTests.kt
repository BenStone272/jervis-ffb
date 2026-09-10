package com.jervisffb.ui.menu.teamselector

import com.jervisffb.engine.bb2025.StandardBB2025Rules
import com.jervisffb.engine.model.Coach
import com.jervisffb.engine.model.CoachId
import com.jervisffb.resources.bb2025.StandaloneRosters2025
import com.jervisffb.ui.game.viewmodel.MenuViewModel
import com.jervisffb.ui.menu.components.teamselector.SelectTeamComponentModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectTeamComponentModelCreateTeamTests {
    @Test
    fun createTeamFromRoster_savesTheTeamAndAddsItToSelector() = runTest {
        val menuViewModel = MenuViewModel()
        val model = SelectTeamComponentModel(
            menuViewModel = menuViewModel,
            getCoach = { Coach(CoachId("test-coach"), "Test Coach") },
            onTeamSelected = {},
        )
        val rules = StandardBB2025Rules()
        model.initialize(rules)

        val roster = StandaloneRosters2025.defaultRosters.values.first().roster
        val result = model.createTeamFromRoster(
            roster = roster,
            teamName = "My Custom Team",
            selectedPlayersByPosition = mapOf(roster.positions.first().id to 1),
            rerolls = 2,
        )

        assertEquals("My Custom Team", result.teamName)
        assertTrue(model.availableTeams.value.any { it.teamName == "My Custom Team" })
        assertEquals(2, result.teamData?.model?.rerolls?.size)
    }
}
