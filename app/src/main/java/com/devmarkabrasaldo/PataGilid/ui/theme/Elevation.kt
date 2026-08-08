package com.devmarkabrasaldo.PataGilid.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val Elevation: ImageVector
  get() {
    if (_elevation != null) {
      return _elevation!!
    }
    _elevation =
      ImageVector.Builder(
          name = "Elevation",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(2.05f, 21f)
            lineTo(8.5f, 12f)
            horizontalLineToRelative(5.05f)
            lineTo(21f, 3.3f)
            verticalLineTo(21f)
            horizontalLineTo(2.05f)
            close()
            moveTo(3.8f, 15.18f)
            lineTo(2.2f, 14.02f)
            lineTo(6.5f, 8f)
            horizontalLineToRelative(5.05f)
            lineToRelative(4.7f, -5.48f)
            lineToRelative(1.5f, 1.3f)
            lineTo(12.45f, 10f)
            horizontalLineTo(7.5f)
            lineTo(3.8f, 15.18f)
            close()
            moveTo(5.95f, 19f)
            horizontalLineTo(19f)
            verticalLineTo(8.7f)
            lineTo(14.45f, 14f)
            horizontalLineTo(9.5f)
            lineTo(5.95f, 19f)
            close()
            moveTo(19f, 19f)
            close()
          }
        }
        .build()
    return _elevation!!
  }

private var _elevation: ImageVector? = null
