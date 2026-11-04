package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lucide icons (stroke style) as Compose ImageVectors.
 * Plan §12: prefer minimal Lucide line icons.
 */
object LucideIcons {
    val ArrowLeft: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.ArrowLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("m12 19-7-7 7-7").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M19 12H5").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val ArrowRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.ArrowRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M5 12h14").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("m12 5 7 7-7 7").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val ArrowUpDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.ArrowUpDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("m21 16-4 4-4-4").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M17 20V4").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("m3 8 4-4 4 4").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M7 4v16").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val ArrowUpLeft: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.ArrowUpLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M7 17V7h10").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M17 17 7 7").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Award: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Award",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("m15.477 12.89 1.515 8.526a.5.5 0 0 1-.81.47l-3.58-2.687a1 1 0 0 0-1.197 0l-3.586 2.686a.5.5 0 0 1-.81-.469l1.514-8.526").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M 6 8 A 6 6 0 1 0 18 8 A 6 6 0 1 0 6 8").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val BadgeCheck: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.BadgeCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("m16 9-5.5 5.5L8 12").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val BadgePercent: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.BadgePercent",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("m15 9-6 6").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M9 9h.01").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M15 15h.01").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Banknote: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Banknote",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M6 12h.01M18 12h.01").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M 10 12 A 2 2 0 1 0 14 12 A 2 2 0 1 0 10 12").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M 4 6 H 20 A 2 2 0 0 1 22 8 V 16 A 2 2 0 0 1 20 18 H 4 A 2 2 0 0 1 2 16 V 8 A 2 2 0 0 1 4 6").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Bell: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Bell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M10.268 21a2 2 0 0 0 3.464 0").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Box: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Box",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("m3.3 7 8.7 5 8.7-5").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M12 22V12").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M20 6 9 17l-5-5").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val CheckCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.CheckCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M21.801 10A10 10 0 1 1 17 3.335").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("m9 11 3 3L22 4").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val ChevronDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.ChevronDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("m6 9 6 6 6-6").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val ChevronLeft: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.ChevronLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("m15 18-6-6 6-6").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.ChevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("m9 18 6-6-6-6").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val CirclePlus: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.CirclePlus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M8 12h8").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M12 8v8").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M 2 12 A 10 10 0 1 0 22 12 A 10 10 0 1 0 2 12").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val CircleUser: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.CircleUser",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M7 20.662V19a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v1.662").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M 2 12 A 10 10 0 1 0 22 12 A 10 10 0 1 0 2 12").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M 9 10 A 3 3 0 1 0 15 10 A 3 3 0 1 0 9 10").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val ClipboardCheck: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.ClipboardCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("m9 14 2 2 4-4").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M 9 2 H 15 A 1 1 0 0 1 16 3 V 5 A 1 1 0 0 1 15 6 H 9 A 1 1 0 0 1 8 5 V 3 A 1 1 0 0 1 9 2").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Clock: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Clock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M12 6v6l4 2").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M 2 12 A 10 10 0 1 0 22 12 A 10 10 0 1 0 2 12").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Copy: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Copy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M 10 8 H 20 A 2 2 0 0 1 22 10 V 20 A 2 2 0 0 1 20 22 H 10 A 2 2 0 0 1 8 20 V 10 A 2 2 0 0 1 10 8").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val FileText: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.FileText",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M14 2v5a1 1 0 0 0 1 1h5").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M10 9H8").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M16 13H8").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes4 = PathParser().parsePathString("M16 17H8").toNodes()
            addPath(
                pathData = nodes4,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Filter: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Filter",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M2 5h20").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M6 12h12").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M9 19h6").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Flame: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Flame",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M12 3q1 4 4 6.5t3 5.5a1 1 0 0 1-14 0 5 5 0 0 1 1-3 1 1 0 0 0 5 0c0-2-1.5-3-1.5-5q0-2 2.5-4").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val GalleryHorizontal: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.GalleryHorizontal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M2 3v18").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M22 3v18").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M 8 3 H 16 A 2 2 0 0 1 18 5 V 19 A 2 2 0 0 1 16 21 H 8 A 2 2 0 0 1 6 19 V 5 A 2 2 0 0 1 8 3").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Handshake: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Handshake",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("m11 17 2 2a1 1 0 1 0 3-3").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("m14 14 2.5 2.5a1 1 0 1 0 3-3l-3.88-3.88a3 3 0 0 0-4.24 0l-.88.88a1 1 0 1 1-3-3l2.81-2.81a5.79 5.79 0 0 1 7.06-.87l.47.28a2 2 0 0 0 1.42.25L21 4").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("m21 3 1 11h-2").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M3 3 2 14l6.5 6.5a1 1 0 1 0 3-3").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes4 = PathParser().parsePathString("M3 4h8").toNodes()
            addPath(
                pathData = nodes4,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Headphones: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Headphones",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M3 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a9 9 0 0 1 18 0v7a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Heart: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Heart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val HelpCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.HelpCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M12 17h.01").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M 2 12 A 10 10 0 1 0 22 12 A 10 10 0 1 0 2 12").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val History: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.History",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M3 3v5h5").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M12 7v5l4 2").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Hourglass: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Hourglass",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M5 22h14").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M5 2h14").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M17 22v-4.172a2 2 0 0 0-.586-1.414L12 12l-4.414 4.414A2 2 0 0 0 7 17.828V22").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M7 2v4.172a2 2 0 0 0 .586 1.414L12 12l4.414-4.414A2 2 0 0 0 17 6.172V2").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val House: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.House",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val ImageOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.ImageOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M10.41 10.41a2 2 0 1 1-2.83-2.83").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M3.59 3.59A1.99 1.99 0 0 0 3 5v14a2 2 0 0 0 2 2h14c.55 0 1.052-.22 1.41-.59").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M21 15V5a2 2 0 0 0-2-2H9").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M 2 2 L 22 22").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes4 = PathParser().parsePathString("M 13.5 13.5 L 6 21").toNodes()
            addPath(
                pathData = nodes4,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes5 = PathParser().parsePathString("M 18 12 L 21 15").toNodes()
            addPath(
                pathData = nodes5,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val ImagePlus: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.ImagePlus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M16 5h6").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M19 2v6").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M21 11.5V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h7.5").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes4 = PathParser().parsePathString("M 7 9 A 2 2 0 1 0 11 9 A 2 2 0 1 0 7 9").toNodes()
            addPath(
                pathData = nodes4,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Info",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M12 16v-4").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M12 8h.01").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M 2 12 A 10 10 0 1 0 22 12 A 10 10 0 1 0 2 12").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Landmark: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Landmark",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M10 18v-7").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M11.119 2.205a2 2 0 0 1 1.762 0l7.84 3.846A.5.5 0 0 1 20.5 7h-17a.5.5 0 0 1-.22-.949z").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M14 18v-7").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M18 18v-7").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes4 = PathParser().parsePathString("M3 22h18").toNodes()
            addPath(
                pathData = nodes4,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes5 = PathParser().parsePathString("M6 18v-7").toNodes()
            addPath(
                pathData = nodes5,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Laptop: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Laptop",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M18 5a2 2 0 0 1 2 2v8.526a2 2 0 0 0 .212.897l1.068 2.127a1 1 0 0 1-.9 1.45H3.62a1 1 0 0 1-.9-1.45l1.068-2.127A2 2 0 0 0 4 15.526V7a2 2 0 0 1 2-2z").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M20.054 15.987H3.946").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val LayoutGrid: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.LayoutGrid",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M 4 3 H 9 A 1 1 0 0 1 10 4 V 9 A 1 1 0 0 1 9 10 H 4 A 1 1 0 0 1 3 9 V 4 A 1 1 0 0 1 4 3").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M 15 3 H 20 A 1 1 0 0 1 21 4 V 9 A 1 1 0 0 1 20 10 H 15 A 1 1 0 0 1 14 9 V 4 A 1 1 0 0 1 15 3").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M 15 14 H 20 A 1 1 0 0 1 21 15 V 20 A 1 1 0 0 1 20 21 H 15 A 1 1 0 0 1 14 20 V 15 A 1 1 0 0 1 15 14").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M 4 14 H 9 A 1 1 0 0 1 10 15 V 20 A 1 1 0 0 1 9 21 H 4 A 1 1 0 0 1 3 20 V 15 A 1 1 0 0 1 4 14").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val MapPin: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.MapPin",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M 9 10 A 3 3 0 1 0 15 10 A 3 3 0 1 0 9 10").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val MapPinPlus: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.MapPinPlus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M19.914 11.105A7.298 7.298 0 0 0 20 10a8 8 0 0 0-16 0c0 4.993 5.539 10.193 7.399 11.799a1 1 0 0 0 1.202 0 32 32 0 0 0 .824-.738").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M16 18h6").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M19 15v6").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M 9 10 A 3 3 0 1 0 15 10 A 3 3 0 1 0 9 10").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val MessageCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.MessageCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M2.992 16.342a2 2 0 0 1 .094 1.167l-1.065 3.29a1 1 0 0 0 1.236 1.168l3.413-.998a2 2 0 0 1 1.099.092 10 10 0 1 0-4.777-4.719").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val MessageSquarePlus: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.MessageSquarePlus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M22 17a2 2 0 0 1-2 2H6.828a2 2 0 0 0-1.414.586l-2.202 2.202A.71.71 0 0 1 2 21.286V5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2z").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M12 8v6").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M9 11h6").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Minus: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Minus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M5 12h14").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Moon: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Moon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val MoreVertical: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.MoreVertical",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M 11 12 A 1 1 0 1 0 13 12 A 1 1 0 1 0 11 12").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M 11 5 A 1 1 0 1 0 13 5 A 1 1 0 1 0 11 5").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M 11 19 A 1 1 0 1 0 13 19 A 1 1 0 1 0 11 19").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Package: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Package",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M12 22V12").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("m7.5 4.27 9 5.15").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M 3.29 7 L 12 12 L 20.71 7").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val PackageOpen: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.PackageOpen",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M12 22v-9").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M15.17 2.21a1.67 1.67 0 0 1 1.63 0L21 4.57a1.93 1.93 0 0 1 0 3.36L8.82 14.79a1.655 1.655 0 0 1-1.64 0L3 12.43a1.93 1.93 0 0 1 0-3.36z").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M20 13v3.87a2.06 2.06 0 0 1-1.11 1.83l-6 3.08a1.93 1.93 0 0 1-1.78 0l-6-3.08A2.06 2.06 0 0 1 4 16.87V13").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M21 12.43a1.93 1.93 0 0 0 0-3.36L8.83 2.2a1.64 1.64 0 0 0-1.63 0L3 4.57a1.93 1.93 0 0 0 0 3.36l12.18 6.86a1.636 1.636 0 0 0 1.63 0z").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Percent: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Percent",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M 4 6.5 A 2.5 2.5 0 1 0 9 6.5 A 2.5 2.5 0 1 0 4 6.5").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M 15 17.5 A 2.5 2.5 0 1 0 20 17.5 A 2.5 2.5 0 1 0 15 17.5").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M 19 5 L 5 19").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Phone: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Phone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M13.832 16.568a1 1 0 0 0 1.213-.303l.355-.465A2 2 0 0 1 17 15h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2A18 18 0 0 1 2 4a2 2 0 0 1 2-2h3a2 2 0 0 1 2 2v3a2 2 0 0 1-.8 1.6l-.468.351a1 1 0 0 0-.292 1.233 14 14 0 0 0 6.392 6.384").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Plus: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Plus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M5 12h14").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M12 5v14").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Receipt: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Receipt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M12 17V7").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M16 8h-6a2 2 0 0 0 0 4h4a2 2 0 0 1 0 4H8").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M4 3a1 1 0 0 1 1-1 1.3 1.3 0 0 1 .7.2l.933.6a1.3 1.3 0 0 0 1.4 0l.934-.6a1.3 1.3 0 0 1 1.4 0l.933.6a1.3 1.3 0 0 0 1.4 0l.933-.6a1.3 1.3 0 0 1 1.4 0l.934.6a1.3 1.3 0 0 0 1.4 0l.933-.6A1.3 1.3 0 0 1 19 2a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1 1.3 1.3 0 0 1-.7-.2l-.933-.6a1.3 1.3 0 0 0-1.4 0l-.934.6a1.3 1.3 0 0 1-1.4 0l-.933-.6a1.3 1.3 0 0 0-1.4 0l-.933.6a1.3 1.3 0 0 1-1.4 0l-.934-.6a1.3 1.3 0 0 0-1.4 0l-.933.6a1.3 1.3 0 0 1-.7.2 1 1 0 0 1-1-1z").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Search",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("m21 21-4.34-4.34").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M 3 11 A 8 8 0 1 0 19 11 A 8 8 0 1 0 3 11").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val SearchX: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.SearchX",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("m13.5 8.5-5 5").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("m8.5 8.5 5 5").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("m21 21-4.3-4.3").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M 3 11 A 8 8 0 1 0 19 11 A 8 8 0 1 0 3 11").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Send: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Send",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M14.536 21.686a.5.5 0 0 0 .937-.024l6.5-19a.496.496 0 0 0-.635-.635l-19 6.5a.5.5 0 0 0-.024.937l7.93 3.18a2 2 0 0 1 1.112 1.11z").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("m21.854 2.147-10.94 10.939").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M 9 12 A 3 3 0 1 0 15 12 A 3 3 0 1 0 9 12").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Share2: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Share2",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M 15 5 A 3 3 0 1 0 21 5 A 3 3 0 1 0 15 5").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M 3 12 A 3 3 0 1 0 9 12 A 3 3 0 1 0 3 12").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M 15 19 A 3 3 0 1 0 21 19 A 3 3 0 1 0 15 19").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M 8.59 13.51 L 15.42 17.49").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes4 = PathParser().parsePathString("M 15.41 6.51 L 8.59 10.49").toNodes()
            addPath(
                pathData = nodes4,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Shield: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Shield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val ShieldCheck: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.ShieldCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("m9 12 2 2 4-4").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Shirt: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Shirt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M20.38 3.46 16 2a4 4 0 0 1-8 0L3.62 3.46a2 2 0 0 0-1.34 2.23l.58 3.47a1 1 0 0 0 .99.84H6v10c0 1.1.9 2 2 2h8a2 2 0 0 0 2-2V10h2.15a1 1 0 0 0 .99-.84l.58-3.47a2 2 0 0 0-1.34-2.23z").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val ShoppingBag: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.ShoppingBag",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M16 10a4 4 0 0 1-8 0").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M3.103 6.034h17.794").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M3.4 5.467a2 2 0 0 0-.4 1.2V20a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6.667a2 2 0 0 0-.4-1.2l-2-2.667A2 2 0 0 0 17 2H7a2 2 0 0 0-1.6.8z").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val ShoppingCart: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.ShoppingCart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("m2.05 2.05 1.099-.028a1 1 0 0 1 1.008.815l2.69 14.347A1 1 0 0 0 7.83 18H18").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M4.563 5h16.435a1 1 0 0 1 .981 1.204l-1.026 6.226A2 2 0 0 1 18.962 14H6.25").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M 16 20 A 2 2 0 1 0 20 20 A 2 2 0 1 0 16 20").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M 6 20 A 2 2 0 1 0 10 20 A 2 2 0 1 0 6 20").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val SlidersHorizontal: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.SlidersHorizontal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M10 5H3").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M12 19H3").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M14 3v4").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M16 17v4").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes4 = PathParser().parsePathString("M21 12h-9").toNodes()
            addPath(
                pathData = nodes4,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes5 = PathParser().parsePathString("M21 19h-5").toNodes()
            addPath(
                pathData = nodes5,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes6 = PathParser().parsePathString("M21 5h-7").toNodes()
            addPath(
                pathData = nodes6,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes7 = PathParser().parsePathString("M8 10v4").toNodes()
            addPath(
                pathData = nodes7,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes8 = PathParser().parsePathString("M8 12H3").toNodes()
            addPath(
                pathData = nodes8,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Smartphone: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Smartphone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M12 18h.01").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M 7 2 H 17 A 2 2 0 0 1 19 4 V 20 A 2 2 0 0 1 17 22 H 7 A 2 2 0 0 1 5 20 V 4 A 2 2 0 0 1 7 2").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Smile: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Smile",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M15 10V9").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M16.472 15a6 6 0 01-8.943 0").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M9 10V9").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M 2 12 A 10 10 0 1 0 22 12 A 10 10 0 1 0 2 12").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Sofa: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Sofa",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M20 9V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v3").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M2 16a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-5a2 2 0 0 0-4 0v1.5a.5.5 0 0 1-.5.5h-11a.5.5 0 0 1-.5-.5V11a2 2 0 0 0-4 0z").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M4 18v2").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M20 18v2").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes4 = PathParser().parsePathString("M12 4v9").toNodes()
            addPath(
                pathData = nodes4,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Sparkles: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Sparkles",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M20 2v4").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M22 4h-4").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M 2 20 A 2 2 0 1 0 6 20 A 2 2 0 1 0 2 20").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Star: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Star",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Storefront: ImageVector get() = Store

    val Store: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Store",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M15 21v-5a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v5").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M17.774 10.31a1.12 1.12 0 0 0-1.549 0 2.5 2.5 0 0 1-3.451 0 1.12 1.12 0 0 0-1.548 0 2.5 2.5 0 0 1-3.452 0 1.12 1.12 0 0 0-1.549 0 2.5 2.5 0 0 1-3.77-3.248l2.889-4.184A2 2 0 0 1 7 2h10a2 2 0 0 1 1.653.873l2.895 4.192a2.5 2.5 0 0 1-3.774 3.244").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M4 10.95V19a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8.05").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Sun: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Sun",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M12 2v2").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M12 20v2").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("m4.93 4.93 1.41 1.41").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("m17.66 17.66 1.41 1.41").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes4 = PathParser().parsePathString("M2 12h2").toNodes()
            addPath(
                pathData = nodes4,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes5 = PathParser().parsePathString("M20 12h2").toNodes()
            addPath(
                pathData = nodes5,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes6 = PathParser().parsePathString("m6.34 17.66-1.41 1.41").toNodes()
            addPath(
                pathData = nodes6,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes7 = PathParser().parsePathString("m19.07 4.93-1.41 1.41").toNodes()
            addPath(
                pathData = nodes7,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes8 = PathParser().parsePathString("M 8 12 A 4 4 0 1 0 16 12 A 4 4 0 1 0 8 12").toNodes()
            addPath(
                pathData = nodes8,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val SunMoon: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.SunMoon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M12 2v2").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M14.837 16.385a6 6 0 1 1-7.223-7.222c.624-.147.97.66.715 1.248a4 4 0 0 0 5.26 5.259c.589-.255 1.396.09 1.248.715").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M16 12a4 4 0 0 0-4-4").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("m19 5-1.256 1.256").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes4 = PathParser().parsePathString("M20 12h2").toNodes()
            addPath(
                pathData = nodes4,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val TicketPercent: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.TicketPercent",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M2 9a3 3 0 1 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 1 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M9 9h.01").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("m15 9-6 6").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M15 15h.01").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Trash2: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Trash2",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M10 11v6").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M14 11v6").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M3 6h18").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes4 = PathParser().parsePathString("M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2").toNodes()
            addPath(
                pathData = nodes4,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val TrendingUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.TrendingUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M16 7h6v6").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("m22 7-8.5 8.5-5-5L2 17").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val TriangleAlert: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.TriangleAlert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M12 9v4").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M12 17h.01").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Truck: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Truck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M15 18H9").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes2 = PathParser().parsePathString("M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.624l-3.48-4.35A1 1 0 0 0 17.52 8H14").toNodes()
            addPath(
                pathData = nodes2,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes3 = PathParser().parsePathString("M 15 18 A 2 2 0 1 0 19 18 A 2 2 0 1 0 15 18").toNodes()
            addPath(
                pathData = nodes3,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes4 = PathParser().parsePathString("M 5 18 A 2 2 0 1 0 9 18 A 2 2 0 1 0 5 18").toNodes()
            addPath(
                pathData = nodes4,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val User: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.User",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M 8 7 A 4 4 0 1 0 16 7 A 4 4 0 1 0 8 7").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Wallet: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Wallet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val X: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.X",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M18 6 6 18").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
            val nodes1 = PathParser().parsePathString("m6 6 12 12").toNodes()
            addPath(
                pathData = nodes1,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    val Zap: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.Zap",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes0 = PathParser().parsePathString("M15.914 4a1.5 1.5 0 00-2.474-1.561l-9 9A1.5 1.5 0 005.5 14h4.002a.5.5 0 01.471.666L8.086 20a1.5 1.5 0 002.475 1.56l9-9A1.5 1.5 0 0018.5 10h-3.997a.5.5 0 01-.472-.667z").toNodes()
            addPath(
                pathData = nodes0,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            )
        }.build()
    }

    /** Filled star for ratings */
    val StarFilled: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.StarFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes = PathParser().parsePathString(
                "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z"
            ).toNodes()
            addPath(
                pathData = nodes,
                fill = SolidColor(Color.Black),
                strokeLineWidth = 0f
            )
        }.build()
    }

    /** Filled heart for wishlist */
    val HeartFilled: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lucide.HeartFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val nodes = PathParser().parsePathString(
                "M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"
            ).toNodes()
            addPath(
                pathData = nodes,
                fill = SolidColor(Color.Black),
                strokeLineWidth = 0f
            )
        }.build()
    }
}
