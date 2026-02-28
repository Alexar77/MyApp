package com.example.habittracker.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Hand-crafted subset of Material icons so we can drop
 * `material-icons-extended` (~50 000 classes) from the APK.
 */
object AppIcons {

    val ExpandMore: ImageVector by lazy {
        ImageVector.Builder("ExpandMore", 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black)) {
                moveTo(16.59f, 8.59f)
                lineTo(12f, 13.17f)
                lineTo(7.41f, 8.59f)
                lineTo(6f, 10f)
                lineToRelative(6f, 6f)
                lineToRelative(6f, -6f)
                close()
            }.build()
    }

    val ExpandLess: ImageVector by lazy {
        ImageVector.Builder("ExpandLess", 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 8f)
                lineToRelative(-6f, 6f)
                lineToRelative(1.41f, 1.41f)
                lineTo(12f, 10.83f)
                lineToRelative(4.59f, 4.58f)
                lineTo(18f, 14f)
                close()
            }.build()
    }

    val ContentCopy: ImageVector by lazy {
        ImageVector.Builder("ContentCopy", 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black)) {
                moveTo(16f, 1f)
                lineTo(4f, 1f)
                curveTo(2.9f, 1f, 2f, 1.9f, 2f, 3f)
                verticalLineToRelative(14f)
                horizontalLineToRelative(2f)
                lineTo(4f, 3f)
                horizontalLineToRelative(12f)
                lineTo(16f, 1f)
                close()
                moveTo(19f, 5f)
                lineTo(8f, 5f)
                curveTo(6.9f, 5f, 6f, 5.9f, 6f, 7f)
                verticalLineToRelative(14f)
                curveTo(6f, 22.1f, 6.9f, 23f, 8f, 23f)
                horizontalLineToRelative(11f)
                curveTo(20.1f, 23f, 21f, 22.1f, 21f, 21f)
                lineTo(21f, 7f)
                curveTo(21f, 5.9f, 20.1f, 5f, 19f, 5f)
                close()
                moveTo(19f, 21f)
                lineTo(8f, 21f)
                lineTo(8f, 7f)
                horizontalLineToRelative(11f)
                verticalLineToRelative(14f)
                close()
            }.build()
    }

    val SwapHoriz: ImageVector by lazy {
        ImageVector.Builder("SwapHoriz", 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black)) {
                moveTo(6.99f, 11f)
                lineTo(3f, 15f)
                lineToRelative(3.99f, 4f)
                verticalLineToRelative(-3f)
                lineTo(14f, 16f)
                verticalLineToRelative(-2f)
                lineTo(6.99f, 14f)
                verticalLineToRelative(-3f)
                close()
                moveTo(21f, 9f)
                lineToRelative(-3.99f, -4f)
                verticalLineToRelative(3f)
                lineTo(10f, 8f)
                verticalLineToRelative(2f)
                lineToRelative(7.01f, 0f)
                verticalLineToRelative(3f)
                lineTo(21f, 9f)
                close()
            }.build()
    }

    val Cake: ImageVector by lazy {
        ImageVector.Builder("Cake", 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 6f)
                curveTo(13.11f, 6f, 14f, 5.1f, 14f, 4f)
                curveTo(14f, 3.62f, 13.85f, 3.27f, 13.6f, 2.99f)
                lineTo(12f, 1f)
                lineToRelative(-1.6f, 1.99f)
                curveTo(10.15f, 3.27f, 10f, 3.62f, 10f, 4f)
                curveTo(10f, 5.1f, 10.9f, 6f, 12f, 6f)
                close()
                moveTo(16.5f, 8f)
                curveTo(15.12f, 8f, 13.82f, 8.43f, 12.75f, 9.15f)
                curveTo(12.29f, 9.46f, 11.71f, 9.46f, 11.25f, 9.15f)
                curveTo(10.18f, 8.43f, 8.88f, 8f, 7.5f, 8f)
                curveTo(4.42f, 8f, 2f, 10.42f, 2f, 13.5f)
                verticalLineTo(18f)
                curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
                verticalLineTo(22f)
                horizontalLineToRelative(16f)
                verticalLineTo(20f)
                curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
                verticalLineTo(13.5f)
                curveTo(22f, 10.42f, 19.58f, 8f, 16.5f, 8f)
                close()
            }.build()
    }

    val NotificationsActive: ImageVector by lazy {
        ImageVector.Builder("NotificationsActive", 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black)) {
                moveTo(7.58f, 4.08f)
                lineTo(6.15f, 2.65f)
                curveTo(3.75f, 4.48f, 2.17f, 7.3f, 2.03f, 10.5f)
                horizontalLineToRelative(2f)
                curveTo(4.18f, 7.85f, 5.56f, 5.53f, 7.58f, 4.08f)
                close()
                moveTo(19.97f, 10.5f)
                horizontalLineToRelative(2f)
                curveTo(21.82f, 7.3f, 20.24f, 4.48f, 17.85f, 2.65f)
                lineToRelative(-1.44f, 1.43f)
                curveTo(18.43f, 5.53f, 19.82f, 7.85f, 19.97f, 10.5f)
                close()
                moveTo(18f, 11f)
                curveTo(18f, 7.93f, 16.36f, 5.36f, 13.5f, 4.68f)
                verticalLineTo(4f)
                curveTo(13.5f, 3.17f, 12.83f, 2.5f, 12f, 2.5f)
                curveTo(11.17f, 2.5f, 10.5f, 3.17f, 10.5f, 4f)
                verticalLineToRelative(0.68f)
                curveTo(7.63f, 5.36f, 6f, 7.92f, 6f, 11f)
                verticalLineToRelative(5f)
                lineToRelative(-2f, 2f)
                verticalLineToRelative(1f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(-1f)
                lineToRelative(-2f, -2f)
                verticalLineToRelative(-5f)
                close()
                moveTo(12f, 22f)
                curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f)
                horizontalLineToRelative(-4f)
                curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f)
                close()
            }.build()
    }

    val CalendarToday: ImageVector by lazy {
        ImageVector.Builder("CalendarToday", 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 3f)
                horizontalLineToRelative(-1f)
                lineTo(19f, 1f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(2f)
                lineTo(7f, 3f)
                lineTo(7f, 1f)
                lineTo(5f, 1f)
                verticalLineToRelative(2f)
                lineTo(4f, 3f)
                curveTo(2.9f, 3f, 2f, 3.9f, 2f, 5f)
                verticalLineToRelative(16f)
                curveTo(2f, 22.1f, 2.9f, 23f, 4f, 23f)
                horizontalLineToRelative(16f)
                curveTo(21.1f, 23f, 22f, 22.1f, 22f, 21f)
                lineTo(22f, 5f)
                curveTo(22f, 3.9f, 21.1f, 3f, 20f, 3f)
                close()
                moveTo(20f, 21f)
                lineTo(4f, 21f)
                lineTo(4f, 8f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(13f)
                close()
            }.build()
    }

    val AccessTime: ImageVector by lazy {
        ImageVector.Builder("AccessTime", 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black)) {
                moveTo(11.99f, 2f)
                curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.47f, 10f, 9.99f, 10f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                reflectiveCurveTo(17.52f, 2f, 11.99f, 2f)
                close()
                moveTo(12f, 20f)
                curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
                reflectiveCurveTo(7.58f, 4f, 12f, 4f)
                reflectiveCurveToRelative(8f, 3.58f, 8f, 8f)
                reflectiveCurveToRelative(-3.58f, 8f, -8f, 8f)
                close()
                moveTo(12.5f, 7f)
                lineTo(11f, 7f)
                verticalLineToRelative(6f)
                lineToRelative(5.25f, 3.15f)
                lineToRelative(0.75f, -1.23f)
                lineToRelative(-4.5f, -2.67f)
                close()
            }.build()
    }

    val RadioButtonUnchecked: ImageVector by lazy {
        ImageVector.Builder("RadioButtonUnchecked", 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveTo(12f, 20f)
                curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
                reflectiveCurveTo(7.58f, 4f, 12f, 4f)
                reflectiveCurveToRelative(8f, 3.58f, 8f, 8f)
                reflectiveCurveToRelative(-3.58f, 8f, -8f, 8f)
                close()
            }.build()
    }

    val Flag: ImageVector by lazy {
        ImageVector.Builder("Flag", 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black)) {
                moveTo(14.4f, 6f)
                lineTo(14f, 4f)
                lineTo(5f, 4f)
                verticalLineToRelative(17f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-7f)
                horizontalLineToRelative(5.6f)
                lineToRelative(0.4f, 2f)
                horizontalLineToRelative(7f)
                lineTo(20f, 6f)
                close()
            }.build()
    }
}
