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
public val FlagCheck: ImageVector
  get() {
    if (_flag_check != null) {
      return _flag_check!!
    }
    _flag_check =
      ImageVector.Builder(
          name = "FlagCheck",
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
            moveTo(12.5f, 10f)
            close()
            moveTo(5f, 21f)
            verticalLineTo(4f)
            horizontalLineToRelative(6.25f)
            quadToRelative(-0.13f, 0.5f, -0.2f, 1f)
            reflectiveQuadTo(11f, 6f)
            horizontalLineTo(7f)
            verticalLineToRelative(6f)
            horizontalLineToRelative(7.25f)
            lineToRelative(0.4f, 2f)
            horizontalLineTo(18f)
            verticalLineTo(12.85f)
            quadToRelative(0.5f, 0f, 1f, -0.08f)
            reflectiveQuadToRelative(1f, -0.22f)
            verticalLineTo(16f)
            horizontalLineTo(13f)
            lineTo(12.6f, 14f)
            horizontalLineTo(7f)
            verticalLineToRelative(7f)
            horizontalLineTo(5f)
            close()
            moveTo(17.28f, 8.1f)
            lineTo(20.75f, 4.65f)
            lineTo(19.7f, 3.6f)
            lineTo(17.28f, 5.97f)
            lineTo(16.3f, 5f)
            lineTo(15.25f, 6.07f)
            lineTo(17.28f, 8.1f)
            close()
            moveTo(21.54f, 2.31f)
            quadTo(23f, 3.77f, 23f, 5.85f)
            quadToRelative(0f, 2.08f, -1.46f, 3.54f)
            reflectiveQuadTo(18f, 10.85f)
            quadToRelative(-2.07f, 0f, -3.54f, -1.46f)
            quadTo(13f, 7.93f, 13f, 5.85f)
            reflectiveQuadTo(14.46f, 2.31f)
            reflectiveQuadTo(18f, 0.85f)
            reflectiveQuadToRelative(3.54f, 1.46f)
            close()
          }
        }
        .build()
    return _flag_check!!
  }

private var _flag_check: ImageVector? = null
