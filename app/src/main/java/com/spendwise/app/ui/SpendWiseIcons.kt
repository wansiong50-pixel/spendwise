package com.spendwise.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// SpendWise glyph set — line icons traced 1:1 from the v2 design handoff
// (`design-package/spendwise/project/tokens.jsx` → Glyph component).
// All icons share a 24×24 viewport and are stroked only (fill = none) so
// `Icon(tint = ...)` colours the stroke consistently with the JSX source.

private fun outlinedIcon(
    name: String,
    strokeWidth: Float = 1.8f,
    build: PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = "SpendWise.$name",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 1f,
        pathFillType = PathFillType.NonZero,
        pathBuilder = build,
    )
}.build()

object SpendWiseIcons {
    // <path d="M3 11l9-7 9 7v9a2 2 0 0 1-2 2h-4v-6h-6v6H5a2 2 0 0 1-2-2v-9z"/>
    val Home: ImageVector by lazy {
        outlinedIcon("Home") {
            moveTo(3f, 11f)
            lineToRelative(9f, -7f)
            lineToRelative(9f, 7f)
            verticalLineToRelative(9f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, 2f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(-6f)
            horizontalLineToRelative(-6f)
            verticalLineToRelative(6f)
            horizontalLineTo(5f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, -2f)
            verticalLineToRelative(-9f)
            close()
        }
    }

    // <path d="M4 6h16M4 12h16M4 18h10"/>
    val List: ImageVector by lazy {
        outlinedIcon("List") {
            moveTo(4f, 6f); horizontalLineToRelative(16f)
            moveTo(4f, 12f); horizontalLineToRelative(16f)
            moveTo(4f, 18f); horizontalLineToRelative(10f)
        }
    }

    // <path d="M4 20V10M10 20V4M16 20v-8M22 20H2"/>
    val Chart: ImageVector by lazy {
        outlinedIcon("Chart") {
            moveTo(4f, 20f); verticalLineTo(10f)
            moveTo(10f, 20f); verticalLineTo(4f)
            moveTo(16f, 20f); verticalLineToRelative(-8f)
            moveTo(22f, 20f); horizontalLineTo(2f)
        }
    }

    // <path d="M12 5v14M5 12h14"/> — stroke 2.4 in the source for the FAB plus
    val Plus: ImageVector by lazy {
        outlinedIcon("Plus", strokeWidth = 2.4f) {
            moveTo(12f, 5f); verticalLineToRelative(14f)
            moveTo(5f, 12f); horizontalLineToRelative(14f)
        }
    }
}
