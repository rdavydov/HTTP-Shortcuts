package ch.rmy.android.http_shortcuts.activities.main.usecases

import androidx.annotation.CheckResult
import ch.rmy.android.framework.extensions.runFor
import ch.rmy.android.framework.viewmodel.viewstate.DialogState
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.main.ShortcutListViewModel
import ch.rmy.android.http_shortcuts.data.domains.categories.CategoryId
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutId

class GetMoveToCategoryDialogUseCase {

    @CheckResult
    operator fun invoke(shortcutId: ShortcutId, categoryOptions: List<CategoryOption>, viewModel: ShortcutListViewModel) =
        DialogState.create {
            title(R.string.title_move_to_category)
                .runFor(categoryOptions) { categoryOption ->
                    item(name = categoryOption.name) {
                        viewModel.onMoveTargetCategorySelected(shortcutId, categoryOption.categoryId)
                    }
                }
                .build()
        }

    data class CategoryOption(val categoryId: CategoryId, val name: String)
}
