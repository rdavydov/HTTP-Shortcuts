package ch.rmy.android.framework.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity

object FilePickerUtil {

    object PickFile : ActivityResultContract<String?, Uri?>() {
        override fun createIntent(context: Context, input: String?): Intent =
            createIntent(type = input)

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
            if (resultCode == AppCompatActivity.RESULT_OK) {
                intent?.data
            } else null
    }

    object PickFiles : ActivityResultContract<Boolean, List<Uri>?>() {
        override fun createIntent(context: Context, input: Boolean): Intent =
            createIntent(multiple = true)

        override fun parseResult(resultCode: Int, intent: Intent?): List<Uri>? =
            if (resultCode == AppCompatActivity.RESULT_OK) {
                intent?.let(::extractUris)
            } else null

        private fun extractUris(intent: Intent): List<Uri>? =
            intent.clipData
                ?.let { data ->
                    buildList {
                        for (i in 0 until data.itemCount) {
                            val uri = data.getItemAt(i).uri
                            add(uri)
                        }
                    }
                }
                ?: intent.data?.let { listOf(it) }
    }

    private fun createIntent(multiple: Boolean = false, type: String? = null): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT)
            .apply {
                this.type = type ?: "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)
                addCategory(Intent.CATEGORY_OPENABLE)
            }
}
