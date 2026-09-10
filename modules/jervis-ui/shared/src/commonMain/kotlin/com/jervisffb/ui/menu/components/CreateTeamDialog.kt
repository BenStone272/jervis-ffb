package com.jervisffb.ui.menu.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jervisffb.engine.model.PositionId
import com.jervisffb.engine.rules.common.roster.Roster
import com.jervisffb.ui.game.dialogs.DialogSize
import com.jervisffb.ui.game.view.JervisTheme
import com.jervisffb.ui.game.view.JervisTheme.buttonTextColor
import com.jervisffb.ui.game.view.utils.JervisButton
import com.jervisffb.ui.menu.components.teamselector.SelectTeamComponentModel
import com.jervisffb.ui.menu.p2p.host.DropdownEntry
import kotlinx.coroutines.launch

private data class RosterDropdownEntry(
    override val name: String,
    override val available: Boolean = true,
    val roster: Roster,
) : DropdownEntry

@Composable
fun CreateTeamDialog(
    viewModel: SelectTeamComponentModel,
    onDismissRequest: () -> Unit,
) {
    val availableRosters = remember(viewModel) { viewModel.getAvailableRosters() }
    var teamName by remember { mutableStateOf("") }
    var selectedRoster by remember(availableRosters) {
        mutableStateOf(availableRosters.firstOrNull())
    }
    var rerolls by remember(selectedRoster) { mutableStateOf(0) }
    var selectedCounts by remember(selectedRoster) {
        mutableStateOf<Map<PositionId, Int>>(selectedRoster?.positions?.associate { it.id to 0 } ?: emptyMap())
    }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(availableRosters) {
        if (selectedRoster == null && availableRosters.isNotEmpty()) {
            selectedRoster = availableRosters.first()
        }
    }

    LaunchedEffect(selectedRoster) {
        if (selectedRoster != null) {
            rerolls = 0
            selectedCounts = selectedRoster!!.positions.associate { it.id to 0 }.toMap()
        }
    }

    JervisDialog(
        title = "Create Team",
        width = DialogSize.MEDIUM,
        backgroundScrim = true,
        content = { _, _ ->
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text("Choose a roster and give the team a name:")
                Spacer(modifier = Modifier.height(12.dp))

                val dropdownEntries = availableRosters.map { roster ->
                    RosterDropdownEntry(name = roster.name, roster = roster, available = true)
                }
                val selectedEntry = dropdownEntries.firstOrNull { it.roster == selectedRoster }

                if (dropdownEntries.isNotEmpty()) {
                    JervisDropDownMenu(
                        title = "Roster",
                        entries = dropdownEntries,
                        selectedEntry = selectedEntry,
                        onSelected = { selectedRoster = it.roster },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                JervisOutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = "Team Name",
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Players on team:")
                selectedRoster?.positions.orEmpty().forEach { position ->
                    val currentCount = selectedCounts[position.id] ?: 0
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(
                            text = "${position.title} (${position.quantity})",
                            modifier = Modifier.weight(1f),
                        )
                        JervisButton(
                            text = "-",
                            onClick = {
                                selectedCounts = selectedCounts.toMutableMap().apply {
                                    put(position.id, (currentCount - 1).coerceAtLeast(0))
                                }
                            },
                            enabled = currentCount > 0,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = currentCount.toString())
                        Spacer(modifier = Modifier.width(8.dp))
                        JervisButton(
                            text = "+",
                            onClick = {
                                selectedCounts = selectedCounts.toMutableMap().apply {
                                    put(position.id, (currentCount + 1).coerceAtMost(position.quantity))
                                }
                            },
                            enabled = currentCount < position.quantity,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Team rerolls:", modifier = Modifier.weight(1f))
                    JervisButton(
                        text = "-",
                        onClick = { rerolls = (rerolls - 1).coerceAtLeast(0) },
                        enabled = rerolls > 0,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = rerolls.toString())
                    Spacer(modifier = Modifier.width(8.dp))
                    JervisButton(
                        text = "+",
                        onClick = { rerolls = (rerolls + 1).coerceAtMost(selectedRoster?.numberOfRerolls ?: 0) },
                        enabled = rerolls < (selectedRoster?.numberOfRerolls ?: 0),
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = JervisTheme.rulebookRed,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        buttons = {
            JervisButton(
                text = "Cancel",
                onClick = onDismissRequest,
            )
            Spacer(modifier = Modifier.weight(1f))
            JervisButton(
                text = if (isCreating) "Creating..." else "Create Team",
                onClick = {
                    val roster = selectedRoster ?: run {
                        errorMessage = "Please choose a roster first"
                        return@JervisButton
                    }
                    val normalizedName = teamName.trim()
                    if (normalizedName.isBlank()) {
                        errorMessage = "Team name is required"
                        return@JervisButton
                    }

                    isCreating = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val createdTeam = viewModel.createTeamFromRoster(
                                roster = roster,
                                teamName = normalizedName,
                                selectedPlayersByPosition = selectedCounts,
                                rerolls = rerolls,
                            )
                            viewModel.setSelectedTeam(createdTeam)
                            onDismissRequest()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Could not create that team"
                        } finally {
                            isCreating = false
                        }
                    }
                },
                enabled = !isCreating && selectedRoster != null && teamName.isNotBlank(),
                buttonColor = JervisTheme.rulebookBlue,
                textColor = buttonTextColor,
            )
        },
        onDismissRequest = onDismissRequest,
    )
}
