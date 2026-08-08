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
public val UTurnLeft: ImageVector
  get() {
    if (_u_turn_left != null) {
      return _u_turn_left!!
    }
    _u_turn_left =
      ImageVector.Builder(
          name = "UTurnLeft",
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
            moveTo(16f, 21f)
            verticalLineTo(9f)
            quadTo(16f, 7.35f, 14.83f, 6.18f)
            reflectiveQuadTo(12f, 5f)
            reflectiveQuadTo(9.18f, 6.18f)
            reflectiveQuadTo(8f, 9f)
            verticalLineToRelative(4.2f)
            lineTo(9.6f, 11.6f)
            lineTo(11f, 13f)
            lineTo(7f, 17f)
            lineTo(3f, 13f)
            lineTo(4.4f, 11.6f)
            lineTo(6f, 13.2f)
            verticalLineTo(9f)
            quadTo(6f, 6.5f, 7.75f, 4.75f)
            reflectiveQuadTo(12f, 3f)
            reflectiveQuadToRelative(4.25f, 1.75f)
            reflectiveQuadTo(18f, 9f)
            verticalLineTo(21f)
            horizontalLineTo(16f)
            close()
          }
        }
        .build()
    return _u_turn_left!!
  }

private var _u_turn_left: ImageVector? = null
