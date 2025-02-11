package ch.rmy.android.http_shortcuts.activities.editor.body

import android.app.Application
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.swapped
import ch.rmy.android.framework.utils.localization.StringResLocalizable
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.framework.viewmodel.WithDialog
import ch.rmy.android.framework.viewmodel.viewstate.DialogState
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.TemporaryShortcutRepository
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.data.enums.RequestBodyType
import ch.rmy.android.http_shortcuts.data.models.ParameterModel
import ch.rmy.android.http_shortcuts.data.models.ShortcutModel

class RequestBodyViewModel(application: Application) : BaseViewModel<Unit, RequestBodyViewState>(application), WithDialog {

    private val temporaryShortcutRepository = TemporaryShortcutRepository()
    private val variableRepository = VariableRepository()

    private var parameters: List<ParameterModel> = emptyList()
        set(value) {
            field = value
            updateViewState {
                copy(
                    parameters = mapParameters(value),
                )
            }
        }

    override var dialogState: DialogState?
        get() = currentViewState?.dialogState
        set(value) {
            updateViewState {
                copy(dialogState = value)
            }
        }

    override fun onInitializationStarted(data: Unit) {
        finalizeInitialization(silent = true)
    }

    override fun initViewState() = RequestBodyViewState()

    override fun onInitialized() {
        temporaryShortcutRepository.getTemporaryShortcut()
            .subscribe(
                ::initViewStateFromShortcut,
                ::onInitializationError,
            )
            .attachTo(destroyer)

        variableRepository.getObservableVariables()
            .subscribe { variables ->
                updateViewState {
                    copy(variables = variables)
                }
            }
            .attachTo(destroyer)
    }

    private fun initViewStateFromShortcut(shortcut: ShortcutModel) {
        atomicallyUpdateViewState {
            this.parameters = shortcut.parameters
            updateViewState {
                copy(
                    requestBodyType = shortcut.bodyType,
                    bodyContent = shortcut.bodyContent,
                    contentType = shortcut.contentType,
                )
            }
        }
    }

    private fun onInitializationError(error: Throwable) {
        handleUnexpectedError(error)
        finish()
    }

    fun onRequestBodyTypeChanged(type: RequestBodyType) {
        atomicallyUpdateViewState {
            if (type == RequestBodyType.X_WWW_FORM_URLENCODE) {
                parameters = parameters.filter { it.isStringParameter }
            }
            updateViewState {
                copy(requestBodyType = type)
            }
        }
        performOperation(
            temporaryShortcutRepository.setRequestBodyType(type)
        )
    }

    fun onParameterMoved(parameterId1: String, parameterId2: String) {
        parameters = parameters.swapped(parameterId1, parameterId2) { id }
        performOperation(
            temporaryShortcutRepository.moveParameter(parameterId1, parameterId2)
        )
    }

    fun onAddStringParameterDialogConfirmed(key: String, value: String) {
        temporaryShortcutRepository.addStringParameter(key, value)
            .compose(progressMonitor.singleTransformer())
            .subscribe { newParameter ->
                parameters = parameters.plus(newParameter)
            }
            .attachTo(destroyer)
    }

    fun onAddFileParameterDialogConfirmed(key: String, fileName: String, multiple: Boolean) {
        temporaryShortcutRepository.addFileParameter(key, fileName, multiple)
            .compose(progressMonitor.singleTransformer())
            .subscribe { newParameter ->
                parameters = parameters.plus(newParameter)
            }
            .attachTo(destroyer)
    }

    fun onEditParameterDialogConfirmed(parameterId: String, key: String, value: String = "", fileName: String = "") {
        parameters = parameters
            .map { parameter ->
                if (parameter.id == parameterId) {
                    ParameterModel(
                        id = parameterId,
                        key = key,
                        value = value,
                        type = parameter.type,
                        fileName = parameter.fileName,
                    )
                } else {
                    parameter
                }
            }
        performOperation(
            temporaryShortcutRepository.updateParameter(parameterId, key, value, fileName)
        )
    }

    fun onRemoveParameterButtonClicked(parameterId: String) {
        parameters = parameters
            .filter { parameter ->
                parameter.id != parameterId
            }
        performOperation(
            temporaryShortcutRepository.removeParameter(parameterId)
        )
    }

    fun onAddParameterButtonClicked() {
        doWithViewState { viewState ->
            if (viewState.requestBodyType == RequestBodyType.FORM_DATA) {
                showParameterTypeDialog()
            } else {
                emitEvent(RequestBodyEvent.ShowAddParameterForStringDialog)
            }
        }
    }

    private fun showParameterTypeDialog() {
        dialogState = DialogState.create {
            title(R.string.dialog_title_parameter_type)
                .item(R.string.option_parameter_type_string) {
                    emitEvent(RequestBodyEvent.ShowAddParameterForStringDialog)
                }
                .item(R.string.option_parameter_type_file) {
                    emitEvent(RequestBodyEvent.ShowAddParameterForFileDialog(multiple = false))
                }
                .item(R.string.option_parameter_type_files) {
                    emitEvent(RequestBodyEvent.ShowAddParameterForFileDialog(multiple = true))
                }
                .build()
        }
    }

    fun onParameterClicked(id: String) {
        parameters.firstOrNull { parameter ->
            parameter.id == id
        }
            ?.let { parameter ->
                emitEvent(
                    if (parameter.isFileParameter || parameter.isFilesParameter) {
                        RequestBodyEvent.ShowEditParameterForFileDialog(
                            id,
                            parameter.key,
                            showFileNameOption = parameter.isFileParameter,
                            fileName = parameter.fileName,
                        )
                    } else {
                        RequestBodyEvent.ShowEditParameterForStringDialog(
                            id,
                            parameter.key,
                            parameter.value,
                        )
                    }
                )
            }
    }

    fun onContentTypeChanged(contentType: String) {
        updateViewState {
            copy(contentType = contentType)
        }
        performOperation(
            temporaryShortcutRepository.setContentType(contentType)
        )
    }

    fun onBodyContentChanged(bodyContent: String) {
        doWithViewState { viewState ->
            if (viewState.contentType.isEmpty() && bodyContent.isJsonObjectStart()) {
                onContentTypeChanged("application/json")
            }
            updateViewState {
                copy(bodyContent = bodyContent)
            }
            performOperation(
                temporaryShortcutRepository.setBodyContent(bodyContent)
            )
        }
    }

    fun onBackPressed() {
        waitForOperationsToFinish {
            finish()
        }
    }

    companion object {
        private fun mapParameters(parameters: List<ParameterModel>): List<ParameterListItem> =
            parameters.map { parameter ->
                ParameterListItem.Parameter(
                    id = parameter.id,
                    key = parameter.key,
                    value = parameter.value.takeIf { parameter.isStringParameter },
                    label = when {
                        parameter.isFileParameter -> {
                            StringResLocalizable(R.string.subtitle_parameter_value_file)
                        }
                        parameter.isFilesParameter -> {
                            StringResLocalizable(R.string.subtitle_parameter_value_files)
                        }
                        else -> null
                    },
                )
            }
                .ifEmpty {
                    listOf(ParameterListItem.EmptyState)
                }

        private val JSON_OBJECT_START = "^\\s*\\{\\s*\".*".toRegex()

        private fun String.isJsonObjectStart() =
            matches(JSON_OBJECT_START)
    }
}
