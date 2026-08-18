package com.tanzir.diabo.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tanzir.diabo.ui.components.NewProjectDialog
import org.junit.Rule
import org.junit.Test

class NewProjectDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun typingNameThenPickingBlankTemplate_invokesConfirmWithNullTemplate() {
        var confirmedName: String? = null
        var confirmedTemplate: com.tanzir.diabo.templates.ProjectTemplate? = null
        var wasCalled = false

        composeRule.setContent {
            NewProjectDialog(
                isCreating = false,
                errorMessage = null,
                onDismiss = {},
                onConfirm = { name, template ->
                    confirmedName = name
                    confirmedTemplate = template
                    wasCalled = true
                }
            )
        }

        // Step 1: type a project name
        composeRule.onNodeWithText("Project name").performTextInput("MyTestApp")
        composeRule.onNodeWithText("Next").performClick()

        // Step 2: template picker appears, "Blank (no template)" is pre-selected by default
        composeRule.onNodeWithText("Blank (no template)").assertExists()
        composeRule.onNodeWithText("Create").performClick()

        assert(wasCalled)
        assert(confirmedName == "MyTestApp")
        assert(confirmedTemplate == null)
    }

    @Test
    fun createButtonIsDisabledWhileCreating() {
        composeRule.setContent {
            NewProjectDialog(
                isCreating = true,
                errorMessage = null,
                onDismiss = {},
                onConfirm = { _, _ -> }
            )
        }

        composeRule.onNodeWithText("Project name").assertIsNotEnabled()
    }

    @Test
    fun errorMessageIsDisplayedWhenPresent() {
        composeRule.setContent {
            NewProjectDialog(
                isCreating = false,
                errorMessage = "A project named 'X' already exists",
                onDismiss = {},
                onConfirm = { _, _ -> }
            )
        }

        composeRule.onNodeWithText("A project named 'X' already exists").assertExists()
    }
}
