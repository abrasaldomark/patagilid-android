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
public val GlobeLocationPin: ImageVector
  get() {
    if (_globe_location_pin != null) {
      return _globe_location_pin!!
    }
    _globe_location_pin =
      ImageVector.Builder(
          name = "GlobeLocationPin",
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
            moveTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            quadToRelative(1.9f, 0f, 3.6f, 0.66f)
            reflectiveQuadTo(18.63f, 4.5f)
            reflectiveQuadToRelative(2.2f, 2.77f)
            quadToRelative(0.88f, 1.6f, 1.1f, 3.48f)
            quadTo(21.43f, 10.48f, 20.88f, 10.3f)
            quadTo(20.33f, 10.13f, 19.75f, 10.05f)
            quadTo(19.28f, 8.17f, 18.04f, 6.75f)
            quadTo(16.8f, 5.32f, 15f, 4.6f)
            verticalLineTo(5f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(13f, 7f)
            horizontalLineTo(11f)
            verticalLineTo(9f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(10f, 10f)
            horizontalLineTo(8f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(6.5f)
            quadToRelative(-0.72f, 0.8f, -1.11f, 1.8f)
            reflectiveQuadTo(13f, 15.9f)
            quadToRelative(0f, 1.95f, 0.78f, 3.03f)
            reflectiveQuadToRelative(2f, 2.35f)
            quadToRelative(-0.9f, 0.35f, -1.85f, 0.54f)
            reflectiveQuadTo(12f, 22f)
            close()
            moveTo(11f, 19.95f)
            verticalLineTo(18f)
            quadTo(10.18f, 18f, 9.59f, 17.41f)
            reflectiveQuadTo(9f, 16f)
            verticalLineTo(15f)
            lineTo(4.2f, 10.2f)
            quadTo(4.13f, 10.65f, 4.06f, 11.1f)
            reflectiveQuadTo(4f, 12f)
            quadToRelative(0f, 3.03f, 1.99f, 5.3f)
            reflectiveQuadTo(11f, 19.95f)
            close()
            moveToRelative(9.06f, -2.89f)
            quadTo(20.5f, 16.63f, 20.5f, 16f)
            reflectiveQuadTo(20.08f, 14.94f)
            reflectiveQuadTo(19.03f, 14.5f)
            quadToRelative(-0.65f, 0f, -1.09f, 0.44f)
            reflectiveQuadTo(17.5f, 16f)
            reflectiveQuadToRelative(0.44f, 1.06f)
            reflectiveQuadTo(19f, 17.5f)
            reflectiveQuadToRelative(1.06f, -0.44f)
            close()
            moveTo(19f, 22f)
            quadToRelative(-0.07f, 0f, -0.4f, -0.27f)
            lineTo(18.5f, 21.55f)
            quadTo(17.95f, 20.6f, 17.11f, 19.86f)
            reflectiveQuadTo(15.68f, 18.2f)
            quadTo(15.33f, 17.7f, 15.16f, 17.11f)
            quadTo(15f, 16.52f, 15f, 15.9f)
            quadToRelative(0f, -1.65f, 1.18f, -2.78f)
            reflectiveQuadTo(19f, 12f)
            reflectiveQuadToRelative(2.83f, 1.13f)
            reflectiveQuadTo(23f, 15.9f)
            quadToRelative(0f, 0.62f, -0.16f, 1.21f)
            reflectiveQuadTo(22.33f, 18.2f)
            quadToRelative(-0.6f, 0.93f, -1.44f, 1.66f)
            reflectiveQuadTo(19.5f, 21.55f)
            lineToRelative(-0.1f, 0.18f)
            quadToRelative(-0.05f, 0.13f, -0.16f, 0.2f)
            reflectiveQuadTo(19f, 22f)
            close()
          }
        }
        .build()
    return _globe_location_pin!!
  }

private var _globe_location_pin: ImageVector? = null
