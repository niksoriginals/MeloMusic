

package com.niksoriginals.melo.models

import com.music.innertube.models.YTItem
import com.niksoriginals.melo.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
