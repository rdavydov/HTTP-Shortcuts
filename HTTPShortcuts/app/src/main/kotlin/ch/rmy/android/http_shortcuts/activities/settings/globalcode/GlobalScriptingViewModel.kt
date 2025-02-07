package ch.rmy.android.http_shortcuts.activities.settings.globalcode

import android.app.Application
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.takeUnlessEmpty
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.framework.viewmodel.WithDialog
import ch.rmy.android.framework.viewmodel.viewstate.DialogState
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.data.domains.app.AppRepository
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutRepository
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.utils.ExternalURLs
import io.reactivex.android.schedulers.AndroidSchedulers

class GlobalScriptingViewModel(application: Application) : BaseViewModel<Unit, GlobalScriptingViewState>(application), WithDialog {

    private val appRepository = AppRepository()
    private val shortcutRepository = ShortcutRepository()
    private val variableRepository = VariableRepository()

    private var shortcutsInitialized = false
    private var variablesInitialized = false
    private var globalCodeInitialized = false

    override var dialogState: DialogState?
        get() = currentViewState?.dialogState
        set(value) {
            updateViewState {
                copy(dialogState = value)
            }
        }

    override fun initViewState() = GlobalScriptingViewState()

    override fun onInitialized() {
        shortcutRepository.getObservableShortcuts()
            .subscribe { shortcuts ->
                shortcutsInitialized = true
                updateViewState {
                    copy(shortcuts = shortcuts)
                }
            }
            .attachTo(destroyer)

        variableRepository.getObservableVariables()
            .subscribe { variables ->
                variablesInitialized = true
                initializeGlobalCodeIfPossible()
                updateViewState {
                    copy(variables = variables)
                }
            }
            .attachTo(destroyer)
    }

    private fun initializeGlobalCodeIfPossible() {
        if (globalCodeInitialized || !shortcutsInitialized || !variablesInitialized) {
            return
        }
        globalCodeInitialized = true
        appRepository.getGlobalCode()
            .subscribe { globalCode ->
                updateViewState {
                    copy(globalCode = globalCode)
                }
            }
            .attachTo(destroyer)
    }

    fun onHelpButtonClicked() {
        openURL(ExternalURLs.SCRIPTING_DOCUMENTATION)
    }

    fun onBackPressed() {
        doWithViewState { viewState ->
            if (viewState.saveButtonVisible) {
                dialogState = DialogState.create {
                    message(R.string.confirm_discard_changes_message)
                        .positive(R.string.dialog_discard) { onDiscardDialogConfirmed() }
                        .negative(R.string.dialog_cancel)
                        .build()
                }
            } else {
                finish()
            }
        }
    }

    private fun onDiscardDialogConfirmed() {
        finish()
    }

    fun onSaveButtonClicked() {
        doWithViewState { viewState ->
            appRepository.setGlobalCode(viewState.globalCode.trim().takeUnlessEmpty())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    finish()
                }
                .attachTo(destroyer)
        }
    }

    fun onGlobalCodeChanged(globalCode: String) {
        if (!globalCodeInitialized) {
            return
        }
        updateViewState {
            copy(
                globalCode = globalCode,
                saveButtonVisible = true,
            )
        }
    }

    fun onCodeSnippetButtonClicked() {
        emitEvent(GlobalScriptingEvent.ShowCodeSnippetPicker)
    }

    fun onCodeSnippetPicked(textBeforeCursor: String, textAfterCursor: String) {
        emitEvent(
            GlobalScriptingEvent.InsertCodeSnippet(textBeforeCursor, textAfterCursor)
        )
    }
}
