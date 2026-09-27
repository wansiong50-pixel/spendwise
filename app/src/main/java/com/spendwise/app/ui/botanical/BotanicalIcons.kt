package com.spendwise.app.ui.botanical

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The prototype's single icon language: 24×24 strokes, 1.6 wide, round caps
 * and joins (BotanicalUI.tsx `paths`). Path data is copied verbatim; circles
 * and rounded rects are expanded to arcs.
 */
object BotIcons {
    val ChevronLeft by lazy { icon("ChevronLeft", "m15 6-6 6 6 6") }
    val ChevronRight by lazy { icon("ChevronRight", "m9 6 6 6-6 6") }
    val ChevronDown by lazy { icon("ChevronDown", "m6 9 6 6 6-6") }
    val Home by lazy { icon("Home", "m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z") }
    val Activity by lazy {
        icon(
            "Activity",
            "M8 5h13M8 12h13M8 19h13",
            circle(3f, 5f, .5f), circle(3f, 12f, .5f), circle(3f, 19f, .5f)
        )
    }
    val Insights by lazy { icon("Insights", "M4 20V10m8 10V4m8 16v-7") }
    val Plus by lazy { icon("Plus", "M12 5v14M5 12h14") }
    val Close by lazy { icon("Close", "m6 6 12 12M6 18 18 6") }
    val Back by lazy { icon("Back", "m14 5-7 7 7 7") }
    val Next by lazy { icon("Next", "m9 5 7 7-7 7") }
    val Menu by lazy { icon("Menu", "M4 6h16M4 12h16M4 18h16") }
    val Down by lazy { icon("Down", "M12 4v16m-6-6 6 6 6-6") }
    val Up by lazy { icon("Up", "M12 20V4m-6 6 6-6 6 6") }
    val Transfer by lazy { icon("Transfer", "M3 7h17l-4-4M21 17H4l4 4") }
    val Wallet by lazy {
        icon("Wallet", roundRect(3f, 5f, 18f, 15f, 4f), "M17 5V3H6a3 3 0 0 0-3 3m18 5h-6v5h6")
    }
    val Search by lazy { icon("Search", circle(10f, 10f, 6f), "m15 15 6 6") }
    val Filter by lazy {
        icon("Filter", "M4 7h9M17 7h3M4 17h3M11 17h9", circle(15f, 7f, 2f), circle(9f, 17f, 2f))
    }
    val Repeat by lazy {
        icon("Repeat", "M4 9a8 8 0 0 1 14-4l3 3m0-5v5h-5M20 15a8 8 0 0 1-14 4l-3-3m0 5v-5h5")
    }
    val Check by lazy { icon("Check", "m4 12 5 5L20 6") }
    val Edit by lazy { icon("Edit", "m15 4 5 5M4 20l5-1L21 7l-4-4L5 15z") }
    val Download by lazy { icon("Download", "M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5") }
    val Upload by lazy { icon("Upload", "M12 16V4m-5 5 5-5 5 5M4 16v5h16v-5") }
    val Grid by lazy {
        icon(
            "Grid",
            roundRect(3f, 3f, 7f, 7f, 2f), roundRect(14f, 3f, 7f, 7f, 2f),
            roundRect(3f, 14f, 7f, 7f, 2f), roundRect(14f, 14f, 7f, 7f, 2f)
        )
    }
    val Calendar by lazy { icon("Calendar", roundRect(3f, 5f, 18f, 16f, 3f), "M3 10h18M8 3v4m8-4v4") }
    val Share by lazy { icon("Share", "M12 15V3m-5 5 5-5 5 5M5 12v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-7") }

    /** The Activity "About net cash flow" glyph (drawn inline in the prototype). */
    val Info by lazy { icon("Info", circle(12f, 12f, 9f), "M12 11v6", circle(12f, 7f, .8f)) }

    private fun icon(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            name = "Botanical.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        paths.forEach { data ->
            builder.addPath(
                pathData = PathParser().parsePathString(data).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        return builder.build()
    }

    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

    private fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float): String =
        "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}" +
            "h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"
}
