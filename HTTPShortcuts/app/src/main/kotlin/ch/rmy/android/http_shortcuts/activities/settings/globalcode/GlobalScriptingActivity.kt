package ch.rmy.android.http_shortcuts.activities.settings.globalcode

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.bindViewModel
import ch.rmy.android.framework.extensions.color
import ch.rmy.android.framework.extensions.consume
import ch.rmy.android.framework.extensions.initialize
import ch.rmy.android.framework.extensions.insertAroundCursor
import ch.rmy.android.framework.extensions.observe
import ch.rmy.android.framework.extensions.observeTextChanges
import ch.rmy.android.framework.extensions.setTextSafely
import ch.rmy.android.framework.ui.BaseIntentBuilder
import ch.rmy.android.framework.viewmodel.ViewModelEvent
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.activities.editor.scripting.codesnippets.CodeSnippetPickerActivity
import ch.rmy.android.http_shortcuts.databinding.ActivityGlobalScriptingBinding
import ch.rmy.android.http_shortcuts.scripting.shortcuts.ShortcutPlaceholderProvider
import ch.rmy.android.http_shortcuts.scripting.shortcuts.ShortcutSpanManager
import ch.rmy.android.http_shortcuts.variables.VariablePlaceholderProvider
import ch.rmy.android.http_shortcuts.variables.Variables
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class GlobalScriptingActivity : BaseActivity() {

    private val pickCodeSnippet = registerForActivityResult(CodeSnippetPickerActivity.PickCodeSnippet) { result ->
        if (result != null) {
            viewModel.onCodeSnippetPicked(result.textBeforeCursor, result.textAfterCursor)
        }
    }

    private val viewModel: GlobalScriptingViewModel by bindViewModel()

    private val variablePlaceholderProvider = VariablePlaceholderProvider()
    private val shortcutPlaceholderProvider = ShortcutPlaceholderProvider()
    private val variablePlaceholderColor by lazy {
        color(context, R.color.variable)
    }
    private val shortcutPlaceholderColor by lazy {
        color(context, R.color.shortcut)
    }

    private lateinit var binding: ActivityGlobalScriptingBinding
    private var saveButton: MenuItem? = null

    override fun onCreated(savedState: Bundle?) {
        viewModel.initialize()
        initViews()
        initUserInputBindings()
        initViewModelBindings()
    }

    private fun initViews() {
        binding = applyBinding(ActivityGlobalScriptingBinding.inflate(layoutInflater))
        binding.buttonAddCodeSnippet.setOnClickListener {
            viewModel.onCodeSnippetButtonClicked()
        }
    }

    private fun initUserInputBindings() {
        bindTextChangeListener(binding.inputCode)
    }

    private fun initViewModelBindings() {
        viewModel.viewState.observe(this) { viewState ->
            viewState.variables?.let(variablePlaceholderProvider::applyVariables)
            viewState.shortcuts?.let(shortcutPlaceholderProvider::applyShortcuts)
            binding.inputCode.setTextSafely(processTextForView(viewState.globalCode))
            saveButton?.isVisible = viewState.saveButtonVisible
            setDialogState(viewState.dialogState, viewModel)
        }
        viewModel.events.observe(this, ::handleEvent)
    }

    override fun handleEvent(event: ViewModelEvent) {
        when (event) {
            is GlobalScriptingEvent.ShowCodeSnippetPicker -> {
                pickCodeSnippet.launch {
                    includeResponseOptions(false)
                        .includeNetworkErrorOption(false)
                        .includeFileOptions(true)
                }
            }
            is GlobalScriptingEvent.InsertCodeSnippet -> {
                insertCodeSnippet(event.textBeforeCursor, event.textAfterCursor)
            }
            else -> super.handleEvent(event)
        }
    }

    private fun processTextForView(input: String): CharSequence {
        val text = SpannableStringBuilder(input)
        Variables.applyVariableFormattingToJS(
            text,
            variablePlaceholderProvider,
            variablePlaceholderColor,
        )
        ShortcutSpanManager.applyShortcutFormattingToJS(
            text,
            shortcutPlaceholderProvider,
            shortcutPlaceholderColor,
        )
        return text
    }

    private fun bindTextChangeListener(textView: EditText) {
        textView.observeTextChanges()
            .throttleFirst(300, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                viewModel.onGlobalCodeChanged(it.toString())
            }
            .attachTo(destroyer)
    }

    private fun insertCodeSnippet(textBeforeCursor: String, textAfterCursor: String) {
        binding.inputCode.insertAroundCursor(textBeforeCursor, textAfterCursor)
        binding.inputCode.text?.let {
            Variables.applyVariableFormattingToJS(it, variablePlaceholderProvider, variablePlaceholderColor)
            ShortcutSpanManager.applyShortcutFormattingToJS(it, shortcutPlaceholderProvider, shortcutPlaceholderColor)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.global_scripting_activity_menu, menu)
        saveButton = menu.findItem(R.id.action_save_changes)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_show_help -> consume {
            viewModel.onHelpButtonClicked()
        }
        R.id.action_save_changes -> consume {
            viewModel.onSaveButtonClicked()
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    override val navigateUpIcon = R.drawable.ic_clear

    class IntentBuilder : BaseIntentBuilder(GlobalScriptingActivity::class.java)
}
