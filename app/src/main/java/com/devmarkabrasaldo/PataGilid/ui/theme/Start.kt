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
public val Start: ImageVector
  get() {
    if (_start != null) {
      return _start!!
    }
    _start =
      ImageVector.Builder(
          name = "Start",
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
            moveTo(2f, 18f)
            verticalLineTo(6f)
            horizontalLineTo(4f)
            verticalLineTo(18f)
            horizontalLineTo(2f)
            close()
            moveToRelative(14f, 0f)
            lineTo(14.58f, 16.6f)
            lineTo(18.18f, 13f)
            horizontalLineTo(6f)
            verticalLineTo(11f)
            horizontalLineTo(18.18f)
            lineTo(14.6f, 7.4f)
            lineTo(16f, 6f)
            lineToRelative(6f, 6f)
            lineToRelative(-6f, 6f)
            close()
          }
        }
        .build()
    return _start!!
  }

private var _start: ImageVector? = null
