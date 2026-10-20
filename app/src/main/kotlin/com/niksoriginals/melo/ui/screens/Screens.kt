

package com.niksoriginals.melo.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.niksoriginals.melo.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconIdInactive: Int,
    @DrawableRes val iconIdActive: Int,
    val route: String,
) {
    object Home : Screens(
        titleId = R.string.home,
        iconIdInactive = R.drawable.home_outlined,
        iconIdActive = R.drawable.home_filled,
        route = "home"
    )

    object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = R.drawable.search,
        iconIdActive = R.drawable.search,
        route = "search_input"
    )

    object ListenTogether : Screens(
        titleId = R.string.together,
        iconIdInactive = R.drawable.group_outlined,
        iconIdActive = R.drawable.group_filled,
        route = "listen_together"
    )

    object Library : Screens(
        titleId = R.string.profile,
        iconIdInactive = R.drawable.person,
        iconIdActive = R.drawable.person,
        route = "library"
    )

    companion object {
        val MainScreens = listOf(Home, Search, ListenTogether, Library)
    }
}
