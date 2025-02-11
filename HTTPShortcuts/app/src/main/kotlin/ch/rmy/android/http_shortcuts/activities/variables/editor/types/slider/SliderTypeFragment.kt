package ch.rmy.android.http_shortcuts.activities.variables.editor.types.slider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.bindViewModel
import ch.rmy.android.framework.extensions.initialize
import ch.rmy.android.framework.extensions.observe
import ch.rmy.android.framework.extensions.observeChecked
import ch.rmy.android.framework.extensions.observeTextChanges
import ch.rmy.android.framework.extensions.setTextSafely
import ch.rmy.android.framework.ui.BaseFragment
import ch.rmy.android.http_shortcuts.databinding.VariableEditorSliderBinding

class SliderTypeFragment : BaseFragment<VariableEditorSliderBinding>() {

    private val viewModel: SliderTypeViewModel by bindViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initialize()
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
        VariableEditorSliderBinding.inflate(inflater, container, false)

    override fun setupViews() {
        initUserInputBindings()
        initViewModelBindings()
    }

    private fun initUserInputBindings() {
        binding.inputRememberValue
            .observeChecked()
            .subscribe(viewModel::onRememberValueChanged)
            .attachTo(destroyer)

        binding.inputSliderMin
            .observeTextChanges()
            .subscribe {
                viewModel.onMinValueChanged(it.toString())
            }
            .attachTo(destroyer)

        binding.inputSliderMax
            .observeTextChanges()
            .subscribe {
                viewModel.onMaxValueChanged(it.toString())
            }
            .attachTo(destroyer)

        binding.inputSliderStep
            .observeTextChanges()
            .subscribe {
                viewModel.onStepSizeChanged(it.toString())
            }
            .attachTo(destroyer)
    }

    private fun initViewModelBindings() {
        viewModel.viewState.observe(this) { viewState ->
            binding.inputSliderMin.setTextSafely(viewState.minValueText)
            binding.inputSliderMax.setTextSafely(viewState.maxValueText)
            binding.inputSliderStep.setTextSafely(viewState.stepSizeText)
            binding.inputRememberValue.isChecked = viewState.rememberValue
        }
        viewModel.events.observe(this, ::handleEvent)
    }
}
