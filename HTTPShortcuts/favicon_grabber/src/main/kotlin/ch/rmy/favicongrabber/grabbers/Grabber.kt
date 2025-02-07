package ch.rmy.favicongrabber.grabbers

import ch.rmy.favicongrabber.models.IconResult
import okhttp3.HttpUrl

interface Grabber {
    fun grabIconsFrom(pageUrl: HttpUrl, preferredSize: Int): List<IconResult>
}
