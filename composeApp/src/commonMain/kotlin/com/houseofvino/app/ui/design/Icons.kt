package com.houseofvino.app.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Thin-line icons specific to wine; generic glyphs come from material-icons-core. */
object HvIcons {
    private fun line(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round, pathBuilder = block)
        }.build()

    val Bottle: ImageVector by lazy {
        line("Bottle") {
            moveTo(10f, 2.5f); lineTo(14f, 2.5f)
            moveTo(10.5f, 2.5f); lineTo(10.5f, 7f)
            curveTo(10.5f, 8.2f, 8f, 9f, 8f, 11.5f); lineTo(8f, 20.5f)
            curveTo(8f, 21.1f, 8.4f, 21.5f, 9f, 21.5f); lineTo(15f, 21.5f)
            curveTo(15.6f, 21.5f, 16f, 21.1f, 16f, 20.5f); lineTo(16f, 11.5f)
            curveTo(16f, 9f, 13.5f, 8.2f, 13.5f, 7f); lineTo(13.5f, 2.5f)
            moveTo(8f, 14f); lineTo(16f, 14f)
            moveTo(8f, 18f); lineTo(16f, 18f)
        }
    }

    val Glass: ImageVector by lazy {
        line("Glass") {
            moveTo(7f, 3f); lineTo(17f, 3f)
            curveTo(17.5f, 7.5f, 16f, 11.5f, 12f, 11.8f)
            curveTo(8f, 11.5f, 6.5f, 7.5f, 7f, 3f)
            moveTo(7.3f, 6.5f); lineTo(16.7f, 6.5f)
            moveTo(12f, 11.8f); lineTo(12f, 20.5f)
            moveTo(8.5f, 20.5f); lineTo(15.5f, 20.5f)
        }
    }

    val Cellar: ImageVector by lazy {
        line("Cellar") {
            moveTo(3f, 10f); lineTo(12f, 3.5f); lineTo(21f, 10f)
            moveTo(5f, 9f); lineTo(5f, 20.5f); lineTo(19f, 20.5f); lineTo(19f, 9f)
            moveTo(8f, 13f); arcToRelative(1.3f, 1.3f, 0f, true, true, 0.01f, 0f)
            moveTo(12f, 13f); arcToRelative(1.3f, 1.3f, 0f, true, true, 0.01f, 0f)
            moveTo(16f, 13f); arcToRelative(1.3f, 1.3f, 0f, true, true, 0.01f, 0f)
            moveTo(8f, 17.2f); arcToRelative(1.3f, 1.3f, 0f, true, true, 0.01f, 0f)
            moveTo(12f, 17.2f); arcToRelative(1.3f, 1.3f, 0f, true, true, 0.01f, 0f)
            moveTo(16f, 17.2f); arcToRelative(1.3f, 1.3f, 0f, true, true, 0.01f, 0f)
        }
    }

    val Journal: ImageVector by lazy {
        line("Journal") {
            moveTo(6f, 3.5f); lineTo(17f, 3.5f); curveTo(17.8f, 3.5f, 18.5f, 4.2f, 18.5f, 5f); lineTo(18.5f, 20.5f)
            lineTo(7f, 20.5f); curveTo(6.2f, 20.5f, 5.5f, 19.8f, 5.5f, 19f); lineTo(5.5f, 4f)
            moveTo(9f, 8f); lineTo(15f, 8f)
            moveTo(9f, 11.5f); lineTo(15f, 11.5f)
            moveTo(9f, 15f); lineTo(12.5f, 15f)
        }
    }

    val Wishlist: ImageVector by lazy {
        line("Wishlist") {
            moveTo(7f, 3.5f); lineTo(17f, 3.5f); lineTo(17f, 20.5f); lineTo(12f, 16.5f); lineTo(7f, 20.5f); close()
        }
    }

    val Barcode: ImageVector by lazy {
        line("Barcode") {
            moveTo(4f, 6f); lineTo(4f, 18f)
            moveTo(6.5f, 6f); lineTo(6.5f, 18f)
            moveTo(9f, 6f); lineTo(9f, 15f)
            moveTo(11f, 6f); lineTo(11f, 18f)
            moveTo(13.5f, 6f); lineTo(13.5f, 15f)
            moveTo(16f, 6f); lineTo(16f, 18f)
            moveTo(18f, 6f); lineTo(18f, 15f)
            moveTo(20f, 6f); lineTo(20f, 18f)
        }
    }

    val Label: ImageVector by lazy {
        line("Label") {
            moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 19f); lineTo(4f, 19f); close()
            moveTo(7f, 9f); lineTo(17f, 9f)
            moveTo(9f, 12f); lineTo(15f, 12f)
            moveTo(10.5f, 15f); lineTo(13.5f, 15f)
        }
    }

    val Insights: ImageVector by lazy {
        line("Insights") {
            moveTo(4f, 20f); lineTo(20f, 20f)
            moveTo(7f, 20f); lineTo(7f, 13f)
            moveTo(12f, 20f); lineTo(12f, 7f)
            moveTo(17f, 20f); lineTo(17f, 10f)
        }
    }

    val Transfer: ImageVector by lazy {
        line("Transfer") {
            moveTo(4f, 8f); lineTo(18f, 8f); moveTo(15f, 5f); lineTo(18f, 8f); lineTo(15f, 11f)
            moveTo(20f, 16f); lineTo(6f, 16f); moveTo(9f, 13f); lineTo(6f, 16f); lineTo(9f, 19f)
        }
    }

    val Sync: ImageVector by lazy {
        line("Sync") {
            moveTo(19f, 8f); curveTo(17.6f, 5.6f, 15f, 4f, 12f, 4f); curveTo(7.6f, 4f, 4f, 7.6f, 4f, 12f)
            moveTo(19f, 4f); lineTo(19f, 8f); lineTo(15f, 8f)
            moveTo(5f, 16f); curveTo(6.4f, 18.4f, 9f, 20f, 12f, 20f); curveTo(16.4f, 20f, 20f, 16.4f, 20f, 12f)
            moveTo(5f, 20f); lineTo(5f, 16f); lineTo(9f, 16f)
        }
    }

    val Filter: ImageVector by lazy {
        line("Filter") {
            moveTo(4f, 6f); lineTo(20f, 6f)
            moveTo(7f, 12f); lineTo(17f, 12f)
            moveTo(10f, 18f); lineTo(14f, 18f)
        }
    }

    val Grid: ImageVector by lazy {
        line("Grid") {
            moveTo(4f, 4f); lineTo(10f, 4f); lineTo(10f, 10f); lineTo(4f, 10f); close()
            moveTo(14f, 4f); lineTo(20f, 4f); lineTo(20f, 10f); lineTo(14f, 10f); close()
            moveTo(4f, 14f); lineTo(10f, 14f); lineTo(10f, 20f); lineTo(4f, 20f); close()
            moveTo(14f, 14f); lineTo(20f, 14f); lineTo(20f, 20f); lineTo(14f, 20f); close()
        }
    }

    val Rows: ImageVector by lazy {
        line("Rows") {
            moveTo(4f, 6f); lineTo(20f, 6f)
            moveTo(4f, 12f); lineTo(20f, 12f)
            moveTo(4f, 18f); lineTo(20f, 18f)
        }
    }
}
