package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.data.CarBodyType
import com.example.data.CarModel
import com.example.ui.theme.*
import kotlin.math.*

object Road3DRenderer {

    fun renderScene(
        scope: DrawScope,
        track: Road3DTrack,
        car: CarModel,
        gameState: GameState,
        particles: List<RaceParticle>,
        animationTime: Float
    ) {
        val width = scope.size.width
        val height = scope.size.height

        val cameraDepth = 1.0f / tan((track.fieldOfView / 2f) * (PI.toFloat() / 180f))
        val cameraX = gameState.playerX * track.roadWidth
        val cameraY = track.cameraHeight
        val cameraZ = gameState.playerZ

        // 1. Draw Sky & Backdrop with Clouds
        drawSky(scope, width, height, track.mode, gameState.playerX, animationTime)

        // 2. Project Segments
        val baseSegment = track.findSegment(cameraZ)
        var maxy = height

        val visibleSegments = mutableListOf<RoadSegment>()
        for (n in 0 until track.drawDistanceSegments) {
            val segIndex = (baseSegment.index + n)
            if (segIndex >= track.totalSegments) break
            val seg = track.segments[segIndex]

            projectPoint(seg.p1, cameraX, cameraY, cameraZ, cameraDepth, width, height, track.roadWidth)
            projectPoint(seg.p2, cameraX, cameraY, cameraZ, cameraDepth, width, height, track.roadWidth)

            if (seg.p1.screen[1] >= seg.p2.screen[1] || seg.p2.screen[1] >= maxy) {
                continue
            }

            visibleSegments.add(seg)
        }

        // Render road segments from back to front
        for (i in visibleSegments.indices.reversed()) {
            val seg = visibleSegments[i]
            drawSegment(scope, seg, track.mode)
        }

        // Render roadside props, obstacles, and pickups from back to front
        for (i in visibleSegments.indices.reversed()) {
            val seg = visibleSegments[i]
            // Draw props
            for (prop in seg.props) {
                drawProp(scope, prop, seg, width)
            }
            // Draw pickups
            for (pickup in seg.pickups) {
                if (!pickup.collected) {
                    drawPickup(scope, pickup, seg, animationTime)
                }
            }
            // Draw obstacles
            for (obs in seg.obstacles) {
                drawObstacle(scope, obs, seg, animationTime)
            }
            // Draw Finish Line Arch
            if (seg.isFinishLine) {
                drawFinishArch(scope, seg, width)
            }
        }

        // 3. Draw Player Car in 3D Perspective
        drawPlayerCar(
            scope = scope,
            car = car,
            width = width,
            height = height,
            gameState = gameState,
            animationTime = animationTime
        )

        // 4. Draw Nitro Speed Lines / Warp Effect
        if (gameState.isNitroActive && gameState.speedKmh > 100f) {
            drawSpeedLines(scope, width, height, animationTime)
        }

        // 5. Draw Particles (Smoke, Sparks, Confetti)
        drawParticles(scope, particles)
    }

    private fun projectPoint(
        p: SegmentPoint,
        camX: Float,
        camY: Float,
        camZ: Float,
        camDepth: Float,
        width: Float,
        height: Float,
        roadWidth: Float
    ) {
        val transX = p.world[0] - camX
        val transY = p.world[1] - camY
        val transZ = p.world[2] - camZ

        if (transZ <= 0) {
            p.screen[3] = 0f
            return
        }

        val scale = camDepth / transZ
        p.screen[0] = (width / 2f) + (scale * transX * (width / 2f))
        p.screen[1] = (height / 2f) - (scale * transY * (height / 2f))
        p.screen[2] = scale * roadWidth * (width / 2f)
        p.screen[3] = scale
    }

    private fun drawSky(
        scope: DrawScope,
        width: Float,
        height: Float,
        mode: GameMode,
        playerX: Float,
        animationTime: Float
    ) {
        val horizonY = height * 0.45f

        // Sky gradient
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(mode.skyColorTop, mode.skyColorBottom),
                startY = 0f,
                endY = horizonY
            ),
            topLeft = Offset.Zero,
            size = Size(width, horizonY)
        )

        // Draw animated fluffy clouds across the sky
        drawClouds(scope, width, horizonY, mode, playerX, animationTime)

        // Distant silhouettes
        val panOffset = -playerX * 80f
        when (mode.theme) {
            TrackTheme.CYBERPUNK -> {
                // Neon futuristic skyscrapers skyline
                val buildingCount = 18
                val bWidth = width / (buildingCount - 4)
                for (b in 0 until buildingCount) {
                    val bx = (b * bWidth + panOffset) % (width + bWidth) - bWidth
                    val bHeight = 40f + ((b * 47) % 110)
                    val by = horizonY - bHeight
                    scope.drawRect(
                        color = Color(0xFF0F0B26),
                        topLeft = Offset(bx, by),
                        size = Size(bWidth - 4f, bHeight)
                    )
                    // Neon window dots
                    if (b % 2 == 0) {
                        scope.drawRect(
                            color = if (b % 4 == 0) TurboCyan else TurboOrange,
                            topLeft = Offset(bx + 4f, by + 10f),
                            size = Size(4f, 15f)
                        )
                    }
                }
            }
            TrackTheme.DESERT -> {
                // Dunes & distant pyramids
                val p1 = Path().apply {
                    moveTo(0f, horizonY)
                    val px = width * 0.35f + panOffset * 0.5f
                    lineTo(px, horizonY - 90f)
                    lineTo(px + 140f, horizonY)
                    close()
                }
                scope.drawPath(p1, color = Color(0xFFB8860B).copy(alpha = 0.5f))

                val p2 = Path().apply {
                    moveTo(0f, horizonY)
                    val px2 = width * 0.7f + panOffset * 0.5f
                    lineTo(px2, horizonY - 60f)
                    lineTo(px2 + 90f, horizonY)
                    close()
                }
                scope.drawPath(p2, color = Color(0xFF996515).copy(alpha = 0.4f))
            }
            else -> {
                // Rolling Mountain Silhouettes
                val mPath = Path().apply {
                    moveTo(0f, horizonY)
                    for (x in 0..width.toInt() step 60) {
                        val mh = sin((x + panOffset) * 0.015f) * 35f + 40f
                        lineTo(x.toFloat(), horizonY - mh)
                    }
                    lineTo(width, horizonY)
                    close()
                }
                scope.drawPath(mPath, color = mode.terrainColorDark.copy(alpha = 0.7f))
            }
        }
    }

    private data class CloudConfig(val relX: Float, val relY: Float, val scale: Float, val speed: Float)

    private fun drawClouds(
        scope: DrawScope,
        width: Float,
        horizonY: Float,
        mode: GameMode,
        playerX: Float,
        animationTime: Float
    ) {
        val (cloudMain, cloudShadow, cloudRim) = when (mode.theme) {
            TrackTheme.MOUNTAIN_NIGHT, TrackTheme.POLICE_ESCAPE -> Triple(
                Color(0xFF475569).copy(alpha = 0.88f),
                Color(0xFF1E293B).copy(alpha = 0.92f),
                Color(0xFF94A3B8).copy(alpha = 0.5f)
            )
            TrackTheme.DESERT -> Triple(
                Color(0xFFFFF7ED).copy(alpha = 0.92f),
                Color(0xFFFED7AA).copy(alpha = 0.75f),
                Color(0xFFFFFFFF).copy(alpha = 0.85f)
            )
            TrackTheme.CYBERPUNK -> Triple(
                Color(0xFF381B5E).copy(alpha = 0.72f),
                Color(0xFF1A0B2E).copy(alpha = 0.85f),
                TurboCyan.copy(alpha = 0.45f)
            )
            else -> Triple(
                Color.White.copy(alpha = 0.94f),
                Color(0xFFCBD5E1).copy(alpha = 0.75f),
                Color.White
            )
        }

        val cloudList = listOf(
            CloudConfig(0.10f, 0.20f, 1.15f, 0.45f),
            CloudConfig(0.40f, 0.12f, 1.45f, 0.30f),
            CloudConfig(0.72f, 0.26f, 0.95f, 0.55f),
            CloudConfig(0.92f, 0.15f, 1.30f, 0.35f),
            CloudConfig(0.26f, 0.34f, 0.85f, 0.65f),
            CloudConfig(0.58f, 0.36f, 0.90f, 0.50f)
        )

        val panOffset = -playerX * 70f
        val wrapWidth = width + 300f

        for (c in cloudList) {
            val drift = (animationTime * 12f * c.speed)
            var cx = ((c.relX * width + drift + panOffset) % wrapWidth) - 150f
            if (cx < -150f) cx += wrapWidth

            val cy = c.relY * horizonY
            val baseW = 115f * c.scale
            val baseH = 44f * c.scale

            drawSingleCloud(scope, cx, cy, baseW, baseH, cloudMain, cloudShadow, cloudRim)
        }
    }

    private fun drawSingleCloud(
        scope: DrawScope,
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        bodyColor: Color,
        shadowColor: Color,
        rimColor: Color
    ) {
        // Soft shaded bottom / base of cloud
        scope.drawOval(
            color = shadowColor,
            topLeft = Offset(cx - w * 0.48f, cy + h * 0.05f),
            size = Size(w * 0.96f, h * 0.62f)
        )

        // Multiple overlapping puffy circular billows
        scope.drawCircle(
            color = bodyColor,
            radius = h * 0.44f,
            center = Offset(cx - w * 0.28f, cy + h * 0.08f)
        )
        scope.drawCircle(
            color = bodyColor,
            radius = h * 0.40f,
            center = Offset(cx + w * 0.26f, cy + h * 0.10f)
        )
        scope.drawCircle(
            color = bodyColor,
            radius = h * 0.54f,
            center = Offset(cx - w * 0.08f, cy - h * 0.12f)
        )
        scope.drawCircle(
            color = bodyColor,
            radius = h * 0.60f,
            center = Offset(cx + w * 0.10f, cy - h * 0.16f)
        )
        scope.drawCircle(
            color = bodyColor,
            radius = h * 0.32f,
            center = Offset(cx - w * 0.42f, cy + h * 0.20f)
        )
        scope.drawCircle(
            color = bodyColor,
            radius = h * 0.30f,
            center = Offset(cx + w * 0.40f, cy + h * 0.22f)
        )

        // Flat bottom cloud shelf
        scope.drawRoundRect(
            color = bodyColor,
            topLeft = Offset(cx - w * 0.44f, cy - h * 0.04f),
            size = Size(w * 0.88f, h * 0.54f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.25f, h * 0.25f)
        )

        // Top highlight sunny rim
        scope.drawArc(
            color = rimColor.copy(alpha = 0.55f),
            startAngle = 190f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - w * 0.25f, cy - h * 0.72f),
            size = Size(w * 0.45f, h * 0.95f),
            style = Stroke(width = 2.2f)
        )
    }

    private fun drawSegment(scope: DrawScope, seg: RoadSegment, mode: GameMode) {
        val x1 = seg.p1.screen[0]
        val y1 = seg.p1.screen[1]
        val w1 = seg.p1.screen[2]

        val x2 = seg.p2.screen[0]
        val y2 = seg.p2.screen[1]
        val w2 = seg.p2.screen[2]

        if (y2 >= y1) return

        val isOdd = (seg.index / 3) % 2 == 1

        // 1. Background Terrain (Grass / Sand / City Outskirts)
        val grassColor = if (isOdd) mode.terrainColorLight else mode.terrainColorDark
        scope.drawRect(
            color = grassColor,
            topLeft = Offset(0f, y2),
            size = Size(scope.size.width, y1 - y2)
        )

        // Curb width & Sidewalk width
        val curbW1 = w1 * 0.16f
        val curbW2 = w2 * 0.16f
        val sidewalkW1 = w1 * 0.32f
        val sidewalkW2 = w2 * 0.32f

        // 2. Paved Sidewalk (رصيف المشاة المبلط) on both sides of the road
        val sidewalkColor = when (mode.theme) {
            TrackTheme.DESERT -> if (isOdd) Color(0xFFFDE68A) else Color(0xFFF59E0B)
            TrackTheme.CYBERPUNK -> if (isOdd) Color(0xFF1E1B4B) else Color(0xFF0F0B26)
            else -> if (isOdd) Color(0xFFE2E8F0) else Color(0xFFCBD5E1)
        }

        // Left Sidewalk
        drawPolygon(
            scope,
            x1 - w1 - curbW1 - sidewalkW1, y1,
            x1 - w1 - curbW1, y1,
            x2 - w2 - curbW2, y2,
            x2 - w2 - curbW2 - sidewalkW2, y2,
            sidewalkColor
        )

        // Right Sidewalk
        drawPolygon(
            scope,
            x1 + w1 + curbW1, y1,
            x1 + w1 + curbW1 + sidewalkW1, y1,
            x2 + w2 + curbW2 + sidewalkW2, y2,
            x2 + w2 + curbW2, y2,
            sidewalkColor
        )

        // Sidewalk paving tile joints (خطوط فواصل بلاط الرصيف)
        val tileJointColor = if (mode.theme == TrackTheme.CYBERPUNK) TurboCyan.copy(alpha = 0.3f) else Color(0x33000000)
        scope.drawLine(
            color = tileJointColor,
            start = Offset(x1 - w1 - curbW1 - sidewalkW1, y1),
            end = Offset(x1 - w1 - curbW1, y1),
            strokeWidth = 1.5f
        )
        scope.drawLine(
            color = tileJointColor,
            start = Offset(x1 + w1 + curbW1, y1),
            end = Offset(x1 + w1 + curbW1 + sidewalkW1, y1),
            strokeWidth = 1.5f
        )

        // Outer Sidewalk Guardrail / Barrier (حاجز أمان جانبي للرصيف)
        val railW1 = w1 * 0.04f
        val railW2 = w2 * 0.04f
        val railColor = if (isOdd) Color(0xFF94A3B8) else Color(0xFF64748B)

        // Left outer guardrail
        drawPolygon(
            scope,
            x1 - w1 - curbW1 - sidewalkW1 - railW1, y1,
            x1 - w1 - curbW1 - sidewalkW1, y1,
            x2 - w2 - curbW2 - sidewalkW2, y2,
            x2 - w2 - curbW2 - sidewalkW2 - railW2, y2,
            railColor
        )
        // Right outer guardrail
        drawPolygon(
            scope,
            x1 + w1 + curbW1 + sidewalkW1, y1,
            x1 + w1 + curbW1 + sidewalkW1 + railW1, y1,
            x2 + w2 + curbW2 + sidewalkW2 + railW2, y2,
            x2 + w2 + curbW2 + sidewalkW2, y2,
            railColor
        )

        // 3. Sidewalk Curb Stones (برودورة الرصيف - حافة الرصيف البارزة ثلاثية الأبعاد)
        val curbColor = if (isOdd) mode.curbColorA else mode.curbColorB

        // Left curb
        drawPolygon(
            scope,
            x1 - w1 - curbW1, y1,
            x1 - w1, y1,
            x2 - w2, y2,
            x2 - w2 - curbW2, y2,
            curbColor
        )

        // Right curb
        drawPolygon(
            scope,
            x1 + w1, y1,
            x1 + w1 + curbW1, y1,
            x2 + w2 + curbW2, y2,
            x2 + w2, y2,
            curbColor
        )

        // 3D Curb Drop / Edge Shadow (العمق ثلاثي الأبعاد لحافة الرصيف فوق الأسفلت)
        val curbDropW1 = curbW1 * 0.22f
        val curbDropW2 = curbW2 * 0.22f
        drawPolygon(
            scope,
            x1 - w1 - curbDropW1, y1,
            x1 - w1, y1,
            x2 - w2, y2,
            x2 - w2 - curbDropW2, y2,
            Color(0x55000000)
        )
        drawPolygon(
            scope,
            x1 + w1, y1,
            x1 + w1 + curbDropW1, y1,
            x2 + w2 + curbDropW2, y2,
            x2 + w2, y2,
            Color(0x55000000)
        )

        // 4. Main Asphalt Road Surface (الطريق الأسفلتي الرئيسي)
        val roadColor = if (seg.isFinishLine || seg.isStartLine) {
            Color(0xFFEEEEEE)
        } else if (isOdd) {
            mode.roadColorLight
        } else {
            mode.roadColorDark
        }

        drawPolygon(
            scope,
            x1 - w1, y1,
            x1 + w1, y1,
            x2 + w2, y2,
            x2 - w2, y2,
            roadColor
        )

        // Solid White Road Boundary Edge Lines (خطوط حافة الطريق البيضاء المتصلة بجانب الرصيف)
        val roadEdgeW1 = w1 * 0.035f
        val roadEdgeW2 = w2 * 0.035f
        val edgeLineColor = if (mode.theme == TrackTheme.CYBERPUNK) TurboCyan else Color(0xFFFFFFFF)

        // Left white road edge
        drawPolygon(
            scope,
            x1 - w1, y1,
            x1 - w1 + roadEdgeW1, y1,
            x2 - w2 + roadEdgeW2, y2,
            x2 - w2, y2,
            edgeLineColor
        )
        // Right white road edge
        drawPolygon(
            scope,
            x1 + w1 - roadEdgeW1, y1,
            x1 + w1, y1,
            x2 + w2, y2,
            x2 + w2 - roadEdgeW2, y2,
            edgeLineColor
        )

        // 5. Road Lane Markings / Checkered Finish Line
        if (seg.isFinishLine) {
            // Draw Checkered road asphalt
            val checkCount = 8
            val laneW1 = (w1 * 2f) / checkCount
            val laneW2 = (w2 * 2f) / checkCount
            for (c in 0 until checkCount) {
                if (c % 2 == 0) {
                    val cx1 = (x1 - w1) + c * laneW1
                    val cx2 = (x2 - w2) + c * laneW2
                    drawPolygon(
                        scope,
                        cx1, y1,
                        cx1 + laneW1, y1,
                        cx2 + laneW2, y2,
                        cx2, y2,
                        Color(0xFF111111)
                    )
                }
            }
        } else if (isOdd) {
            // Center dashed lane lines
            val laneW1 = w1 * 0.04f
            val laneW2 = w2 * 0.04f
            val laneColor = if (mode.theme == TrackTheme.CYBERPUNK) TurboCyan else Color(0xFFFFF9C4)
            drawPolygon(
                scope,
                x1 - laneW1, y1,
                x1 + laneW1, y1,
                x2 + laneW2, y2,
                x2 - laneW2, y2,
                laneColor
            )

            // Side lane markings
            val sideOffset1 = w1 * 0.5f
            val sideOffset2 = w2 * 0.5f
            drawPolygon(
                scope,
                x1 - sideOffset1 - (laneW1 * 0.6f), y1,
                x1 - sideOffset1 + (laneW1 * 0.6f), y1,
                x2 - sideOffset2 + (laneW2 * 0.6f), y2,
                x2 - sideOffset2 - (laneW2 * 0.6f), y2,
                laneColor.copy(alpha = 0.6f)
            )
            drawPolygon(
                scope,
                x1 + sideOffset1 - (laneW1 * 0.6f), y1,
                x1 + sideOffset1 + (laneW1 * 0.6f), y1,
                x2 + sideOffset2 + (laneW2 * 0.6f), y2,
                x2 + sideOffset2 - (laneW2 * 0.6f), y2,
                laneColor.copy(alpha = 0.6f)
            )
        }
    }

    private fun drawPolygon(
        scope: DrawScope,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        x4: Float, y4: Float,
        color: Color
    ) {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
            lineTo(x3, y3)
            lineTo(x4, y4)
            close()
        }
        scope.drawPath(path, color = color)
    }

    private fun drawFinishArch(scope: DrawScope, seg: RoadSegment, screenWidth: Float) {
        val sx = seg.p1.screen[0]
        val sy = seg.p1.screen[1]
        val sw = seg.p1.screen[2]
        val scale = seg.p1.screen[3]

        if (scale <= 0.001f) return

        val archHeight = sw * 0.85f
        val archTop = sy - archHeight
        val poleW = max(4f, sw * 0.08f)

        // Left Pillar
        scope.drawRect(
            color = Color(0xFF212121),
            topLeft = Offset(sx - sw - poleW, archTop),
            size = Size(poleW, archHeight)
        )
        // Right Pillar
        scope.drawRect(
            color = Color(0xFF212121),
            topLeft = Offset(sx + sw, archTop),
            size = Size(poleW, archHeight)
        )

        // Overhead Crossbar Banner
        val bannerH = archHeight * 0.45f
        val bannerTop = archTop - (bannerH * 0.2f)
        val bannerW = (sw * 2f) + (poleW * 2f)
        val bannerLeft = sx - sw - poleW

        // Checkered Banner Board
        scope.drawRect(
            color = Color(0xFF101010),
            topLeft = Offset(bannerLeft, bannerTop),
            size = Size(bannerW, bannerH)
        )

        // Checkered pattern inside banner
        val checkCols = 16
        val checkRows = 3
        val cw = bannerW / checkCols
        val ch = bannerH / checkRows
        for (r in 0 until checkRows) {
            for (c in 0 until checkCols) {
                if ((r + c) % 2 == 0) {
                    scope.drawRect(
                        color = Color.White,
                        topLeft = Offset(bannerLeft + c * cw, bannerTop + r * ch),
                        size = Size(cw, ch)
                    )
                }
            }
        }

        // Glowing Neon Border on Banner
        scope.drawRect(
            color = TurboGold,
            topLeft = Offset(bannerLeft, bannerTop),
            size = Size(bannerW, bannerH),
            style = Stroke(width = max(2f, 4f * scale * 100f))
        )
    }

    private fun drawProp(scope: DrawScope, prop: RoadsideProp, seg: RoadSegment, screenWidth: Float) {
        val sx = seg.p1.screen[0]
        val sy = seg.p1.screen[1]
        val sw = seg.p1.screen[2]
        val scale = seg.p1.screen[3]

        if (scale <= 0.001f) return

        val propX = sx + (prop.offsetPercent * sw)
        val propSize = sw * 0.65f

        when (prop.type) {
            PropType.PALM_TREE -> {
                // Trunk
                val trunkW = max(3f, propSize * 0.12f)
                val trunkH = propSize * 1.5f
                val trunkTop = sy - trunkH
                scope.drawRect(
                    color = Color(0xFF5D4037),
                    topLeft = Offset(propX - trunkW / 2f, trunkTop),
                    size = Size(trunkW, trunkH)
                )
                // Palm foliage
                val palmR = propSize * 0.7f
                scope.drawCircle(
                    color = Color(0xFF2E7D32),
                    radius = palmR,
                    center = Offset(propX, trunkTop)
                )
            }
            PropType.DESERT_CACTUS -> {
                val cW = max(4f, propSize * 0.18f)
                val cH = propSize * 1.1f
                val cTop = sy - cH
                scope.drawRoundRect(
                    color = Color(0xFF388E3C),
                    topLeft = Offset(propX - cW / 2f, cTop),
                    size = Size(cW, cH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cW / 2f)
                )
                // Cactus arms
                val armW = cW * 0.8f
                val armH = cH * 0.4f
                scope.drawRoundRect(
                    color = Color(0xFF2E7D32),
                    topLeft = Offset(propX - cW * 1.4f, cTop + cH * 0.25f),
                    size = Size(armW, armH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(armW / 2f)
                )
                scope.drawRoundRect(
                    color = Color(0xFF2E7D32),
                    topLeft = Offset(propX + cW * 0.6f, cTop + cH * 0.35f),
                    size = Size(armW, armH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(armW / 2f)
                )
            }
            PropType.CYBER_TOWER -> {
                val tW = max(10f, propSize * 0.7f)
                val tH = propSize * 2.8f
                val tTop = sy - tH
                scope.drawRect(
                    color = Color(0xFF140D36),
                    topLeft = Offset(propX - tW / 2f, tTop),
                    size = Size(tW, tH)
                )
                // Glowing cyan beacon line
                scope.drawLine(
                    color = TurboCyan,
                    start = Offset(propX, tTop),
                    end = Offset(propX, sy),
                    strokeWidth = max(2f, tW * 0.1f)
                )
            }
            PropType.STREET_LAMP -> {
                val lW = max(2f, propSize * 0.08f)
                val lH = propSize * 1.3f
                val lTop = sy - lH
                scope.drawRect(
                    color = Color(0xFF455A64),
                    topLeft = Offset(propX - lW / 2f, lTop),
                    size = Size(lW, lH)
                )
                // Light glow
                scope.drawCircle(
                    color = Color(0xFFFFF59D),
                    radius = max(3f, propSize * 0.2f),
                    center = Offset(propX, lTop)
                )
            }
            else -> {
                // Rock or Billboard
                val rW = max(6f, propSize * 0.6f)
                val rH = max(4f, propSize * 0.4f)
                scope.drawRoundRect(
                    color = Color(0xFF8D6E63),
                    topLeft = Offset(propX - rW / 2f, sy - rH),
                    size = Size(rW, rH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(rW * 0.2f)
                )
            }
        }
    }

    private fun drawPickup(scope: DrawScope, pickup: TrackPickup, seg: RoadSegment, animationTime: Float) {
        val sx = seg.p1.screen[0]
        val sy = seg.p1.screen[1]
        val sw = seg.p1.screen[2]
        val scale = seg.p1.screen[3]

        if (scale <= 0.001f) return

        val pX = sx + (pickup.offsetPercent * sw)
        val floatOffset = sin(animationTime * 4f + pickup.id) * 8f * scale * 100f
        val pY = (sy - (sw * 0.35f)) - floatOffset
        val size = max(6f, sw * 0.26f)

        // Drop shadow on asphalt
        scope.drawOval(
            color = Color(0x66000000),
            topLeft = Offset(pX - size * 0.5f, sy - size * 0.15f),
            size = Size(size, size * 0.3f)
        )

        when (pickup.type) {
            PickupType.COIN -> {
                // Spinning 3D gold coin
                val spin = cos(animationTime * 5f + pickup.id)
                val coinW = size * abs(spin).coerceAtLeast(0.18f)
                scope.drawOval(
                    color = TurboGold,
                    topLeft = Offset(pX - coinW / 2f, pY - size / 2f),
                    size = Size(coinW, size)
                )
                // Inner rim
                scope.drawOval(
                    color = Color(0xFFFFB300),
                    topLeft = Offset(pX - (coinW * 0.7f) / 2f, pY - (size * 0.7f) / 2f),
                    size = Size(coinW * 0.7f, size * 0.7f)
                )
            }
            PickupType.NITRO -> {
                // Glowing cyan NOS Canister
                val cW = size * 0.65f
                val cH = size * 1.1f
                scope.drawRoundRect(
                    color = TurboCyan,
                    topLeft = Offset(pX - cW / 2f, pY - cH / 2f),
                    size = Size(cW, cH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cW * 0.3f)
                )
                scope.drawRect(
                    color = Color(0xFF00B0FF),
                    topLeft = Offset(pX - cW * 0.35f, pY - cH * 0.2f),
                    size = Size(cW * 0.7f, cH * 0.4f)
                )
            }
            PickupType.REPAIR_WRENCH -> {
                // Wrench tool
                scope.drawCircle(
                    color = TurboGreen,
                    radius = size * 0.45f,
                    center = Offset(pX, pY)
                )
                scope.drawCircle(
                    color = Color(0xFF003300),
                    radius = size * 0.2f,
                    center = Offset(pX, pY)
                )
            }
            PickupType.TIME_BONUS -> {
                // Clock/Timer
                scope.drawCircle(
                    color = TurboOrange,
                    radius = size * 0.5f,
                    center = Offset(pX, pY)
                )
                scope.drawCircle(
                    color = Color.White,
                    radius = size * 0.35f,
                    center = Offset(pX, pY)
                )
                scope.drawLine(
                    color = Color.Black,
                    start = Offset(pX, pY),
                    end = Offset(pX, pY - size * 0.25f),
                    strokeWidth = max(2f, size * 0.08f)
                )
            }
        }
    }

    private fun drawObstacle(scope: DrawScope, obs: TrackObstacle, seg: RoadSegment, animationTime: Float) {
        val sx = seg.p1.screen[0]
        val sy = seg.p1.screen[1]
        val sw = seg.p1.screen[2]
        val scale = seg.p1.screen[3]

        if (scale <= 0.001f) return

        val oX = sx + (obs.offsetPercent * sw)
        val oY = sy
        val carW = sw * 0.48f * obs.widthScale
        val carH = carW * 0.55f

        when (obs.type) {
            ObstacleType.TRAFFIC_CAR, ObstacleType.POLICE_CAR -> {
                val isPolice = obs.type == ObstacleType.POLICE_CAR
                val bodyColor = if (isPolice) Color(0xFF1E293B) else if (obs.id % 2 == 0) Color(0xFFD32F2F) else Color(0xFF1976D2)

                // Shadow
                scope.drawOval(
                    color = Color(0x88000000),
                    topLeft = Offset(oX - carW * 0.55f, oY - carH * 0.2f),
                    size = Size(carW * 1.1f, carH * 0.4f)
                )

                // Main Car Body
                val bodyTop = oY - carH
                val p = Path().apply {
                    moveTo(oX - carW * 0.5f, oY - carH * 0.2f)
                    lineTo(oX - carW * 0.42f, bodyTop + carH * 0.35f)
                    lineTo(oX - carW * 0.32f, bodyTop)
                    lineTo(oX + carW * 0.32f, bodyTop)
                    lineTo(oX + carW * 0.42f, bodyTop + carH * 0.35f)
                    lineTo(oX + carW * 0.5f, oY - carH * 0.2f)
                    close()
                }
                scope.drawPath(p, color = bodyColor)

                // Rear Windshield Glass
                val glassPath = Path().apply {
                    moveTo(oX - carW * 0.36f, bodyTop + carH * 0.35f)
                    lineTo(oX - carW * 0.28f, bodyTop + carH * 0.08f)
                    lineTo(oX + carW * 0.28f, bodyTop + carH * 0.08f)
                    lineTo(oX + carW * 0.36f, bodyTop + carH * 0.35f)
                    close()
                }
                scope.drawPath(glassPath, color = Color(0xFF0F172A))

                // Tail Lights
                val lightW = carW * 0.16f
                val lightH = carH * 0.16f
                val lightY = bodyTop + carH * 0.52f
                scope.drawRect(
                    color = Color(0xFFFF1744),
                    topLeft = Offset(oX - carW * 0.45f, lightY),
                    size = Size(lightW, lightH)
                )
                scope.drawRect(
                    color = Color(0xFFFF1744),
                    topLeft = Offset(oX + carW * 0.45f - lightW, lightY),
                    size = Size(lightW, lightH)
                )

                // Police Siren Flashing Lights
                if (isPolice) {
                    val sirenW = carW * 0.35f
                    val sirenH = carH * 0.18f
                    val sirenLeft = oX - sirenW / 2f
                    val sirenTop = bodyTop - sirenH * 0.8f

                    val flash = (animationTime * 12f).toInt() % 2 == 0
                    scope.drawRect(
                        color = if (flash) Color(0xFFFF1744) else Color(0xFF2979FF),
                        topLeft = Offset(sirenLeft, sirenTop),
                        size = Size(sirenW / 2f, sirenH)
                    )
                    scope.drawRect(
                        color = if (!flash) Color(0xFFFF1744) else Color(0xFF2979FF),
                        topLeft = Offset(sirenLeft + sirenW / 2f, sirenTop),
                        size = Size(sirenW / 2f, sirenH)
                    )
                }

                // Tires
                val tireW = carW * 0.16f
                val tireH = carH * 0.4f
                scope.drawRoundRect(
                    color = Color(0xFF111111),
                    topLeft = Offset(oX - carW * 0.52f, oY - tireH),
                    size = Size(tireW, tireH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(tireW * 0.2f)
                )
                scope.drawRoundRect(
                    color = Color(0xFF111111),
                    topLeft = Offset(oX + carW * 0.52f - tireW, oY - tireH),
                    size = Size(tireW, tireH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(tireW * 0.2f)
                )
            }
            ObstacleType.ROAD_BARRIER -> {
                // Hazard Barricade
                val bW = carW * 0.9f
                val bH = carH * 0.7f
                val bTop = oY - bH
                scope.drawRect(
                    color = Color(0xFFFF6D00),
                    topLeft = Offset(oX - bW / 2f, bTop),
                    size = Size(bW, bH)
                )
                // White hazard stripes
                scope.drawRect(
                    color = Color.White,
                    topLeft = Offset(oX - bW * 0.3f, bTop + bH * 0.2f),
                    size = Size(bW * 0.15f, bH * 0.6f)
                )
                scope.drawRect(
                    color = Color.White,
                    topLeft = Offset(oX + bW * 0.15f, bTop + bH * 0.2f),
                    size = Size(bW * 0.15f, bH * 0.6f)
                )
            }
            ObstacleType.OIL_SLICK -> {
                // Slick on asphalt
                val sW = carW * 0.85f
                val sH = carH * 0.35f
                scope.drawOval(
                    color = Color(0xCC111111),
                    topLeft = Offset(oX - sW / 2f, oY - sH),
                    size = Size(sW, sH)
                )
                scope.drawOval(
                    color = Color(0x667C4DFF),
                    topLeft = Offset(oX - sW * 0.35f, oY - sH * 0.8f),
                    size = Size(sW * 0.7f, sH * 0.6f)
                )
            }
            ObstacleType.DESERT_BOULDER -> {
                // Large Desert Rock
                val rW = carW * 0.75f
                val rH = carH * 0.85f
                val rockPath = Path().apply {
                    moveTo(oX - rW * 0.45f, oY)
                    lineTo(oX - rW * 0.5f, oY - rH * 0.6f)
                    lineTo(oX - rW * 0.2f, oY - rH)
                    lineTo(oX + rW * 0.35f, oY - rH * 0.8f)
                    lineTo(oX + rW * 0.5f, oY - rH * 0.3f)
                    lineTo(oX + rW * 0.4f, oY)
                    close()
                }
                scope.drawPath(rockPath, color = Color(0xFF6D4C41))
            }
        }
    }

    private fun drawPlayerCar(
        scope: DrawScope,
        car: CarModel,
        width: Float,
        height: Float,
        gameState: GameState,
        animationTime: Float
    ) {
        val carScale = width / 800f
        val carW = 210f * carScale
        val carH = 125f * carScale

        val baseCarX = (width / 2f) + (gameState.cameraShake * 6f)
        val carY = height * 0.88f + (sin(animationTime * 25f) * (gameState.speedKmh / 200f) * 2.5f)

        val rollAngle = gameState.steerRoll * 7.5f

        scope.rotate(degrees = rollAngle, pivot = Offset(baseCarX, carY)) {
            // 1. Car Shadow
            scope.drawOval(
                color = Color(0x99000000),
                topLeft = Offset(baseCarX - carW * 0.55f, carY - carH * 0.15f),
                size = Size(carW * 1.1f, carH * 0.42f)
            )

            // 2. Wide Racing Tires
            val tireW = carW * 0.17f
            val tireH = carH * 0.45f
            val tireY = carY - tireH * 0.9f
            // Left Tire
            scope.drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(baseCarX - carW * 0.52f, tireY),
                size = Size(tireW, tireH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(tireW * 0.25f)
            )
            // Left Rim
            scope.drawCircle(
                color = TurboCyan,
                radius = tireW * 0.25f,
                center = Offset(baseCarX - carW * 0.52f + tireW / 2f, tireY + tireH / 2f)
            )

            // Right Tire
            scope.drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(baseCarX + carW * 0.52f - tireW, tireY),
                size = Size(tireW, tireH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(tireW * 0.25f)
            )
            // Right Rim
            scope.drawCircle(
                color = TurboCyan,
                radius = tireW * 0.25f,
                center = Offset(baseCarX + carW * 0.52f - tireW / 2f, tireY + tireH / 2f)
            )

            // 3. Lower Diffuser & Carbon Skirt
            val diffuserW = carW * 0.82f
            val diffuserH = carH * 0.25f
            scope.drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(baseCarX - diffuserW / 2f, carY - diffuserH),
                size = Size(diffuserW, diffuserH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(diffuserH * 0.2f)
            )

            // 4. Main Aerodynamic Chassis Body
            val bodyTop = carY - carH
            val bodyPath = Path().apply {
                moveTo(baseCarX - carW * 0.48f, carY - carH * 0.25f)
                lineTo(baseCarX - carW * 0.44f, bodyTop + carH * 0.42f)
                lineTo(baseCarX - carW * 0.34f, bodyTop + carH * 0.12f)
                lineTo(baseCarX + carW * 0.34f, bodyTop + carH * 0.12f)
                lineTo(baseCarX + carW * 0.44f, bodyTop + carH * 0.42f)
                lineTo(baseCarX + carW * 0.48f, carY - carH * 0.25f)
                close()
            }
            scope.drawPath(bodyPath, color = car.primaryColor)

            // Highlight gradient on chassis
            val highlightPath = Path().apply {
                moveTo(baseCarX - carW * 0.35f, bodyTop + carH * 0.12f)
                lineTo(baseCarX + carW * 0.35f, bodyTop + carH * 0.12f)
                lineTo(baseCarX + carW * 0.28f, bodyTop + carH * 0.35f)
                lineTo(baseCarX - carW * 0.28f, bodyTop + carH * 0.35f)
                close()
            }
            scope.drawPath(highlightPath, color = car.secondaryColor.copy(alpha = 0.8f))

            // 5. Rear Window & Canopy
            val windowPath = Path().apply {
                moveTo(baseCarX - carW * 0.28f, bodyTop + carH * 0.14f)
                lineTo(baseCarX + carW * 0.28f, bodyTop + carH * 0.14f)
                lineTo(baseCarX + carW * 0.33f, bodyTop + carH * 0.40f)
                lineTo(baseCarX - carW * 0.33f, bodyTop + carH * 0.40f)
                close()
            }
            scope.drawPath(windowPath, color = Color(0xFF090D16))

            // Cabin interior reflection
            scope.drawLine(
                color = Color(0x55FFFFFF),
                start = Offset(baseCarX - carW * 0.15f, bodyTop + carH * 0.18f),
                end = Offset(baseCarX - carW * 0.05f, bodyTop + carH * 0.36f),
                strokeWidth = max(2f, 3f * carScale)
            )

            // 6. Tail Lights (Dynamic Braking Glow)
            val isBraking = false // can be enhanced with braking state
            val lightColor = if (isBraking) Color(0xFFFF0033) else Color(0xFFFF1744)
            val lightW = carW * 0.20f
            val lightH = carH * 0.12f
            val lightY = bodyTop + carH * 0.48f

            // Left tail light
            scope.drawRoundRect(
                color = lightColor,
                topLeft = Offset(baseCarX - carW * 0.42f, lightY),
                size = Size(lightW, lightH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(lightH * 0.3f)
            )
            // Right tail light
            scope.drawRoundRect(
                color = lightColor,
                topLeft = Offset(baseCarX + carW * 0.42f - lightW, lightY),
                size = Size(lightW, lightH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(lightH * 0.3f)
            )

            // 7. Spoiler Wing
            val spoilerW = carW * 0.88f
            val spoilerH = carH * 0.10f
            val spoilerY = bodyTop - carH * 0.05f
            scope.drawRoundRect(
                color = car.secondaryColor,
                topLeft = Offset(baseCarX - spoilerW / 2f, spoilerY),
                size = Size(spoilerW, spoilerH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(spoilerH * 0.3f)
            )
            // Spoiler stanchions/supports
            scope.drawRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(baseCarX - carW * 0.25f, spoilerY + spoilerH),
                size = Size(carW * 0.05f, carH * 0.15f)
            )
            scope.drawRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(baseCarX + carW * 0.20f, spoilerY + spoilerH),
                size = Size(carW * 0.05f, carH * 0.15f)
            )

            // 8. Dual Exhaust Pipes & Nitro Flames
            val pipeW = carW * 0.08f
            val pipeH = carH * 0.10f
            val pipeY = carY - carH * 0.20f

            val leftPipeX = baseCarX - carW * 0.22f
            val rightPipeX = baseCarX + carW * 0.14f

            scope.drawRoundRect(
                color = Color(0xFF334155),
                topLeft = Offset(leftPipeX, pipeY),
                size = Size(pipeW, pipeH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(pipeW * 0.4f)
            )
            scope.drawRoundRect(
                color = Color(0xFF334155),
                topLeft = Offset(rightPipeX, pipeY),
                size = Size(pipeW, pipeH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(pipeW * 0.4f)
            )

            // Nitro Boost Exhaust Flames!
            if (gameState.isNitroActive && gameState.speedKmh > 30f) {
                val flameFlicker = (sin(animationTime * 40f) * 0.3f + 1f)
                val flameLen = carH * 0.75f * flameFlicker
                val flameW = pipeW * 1.5f

                // Outer Flame (Electric Cyan or Blazing Orange)
                val flameColor = if (car.bodyType == CarBodyType.CYBER_RUNNER) TurboCyan else TurboOrange
                val innerFlameColor = Color(0xFFFFFFFF)

                // Left Flame
                drawFlame(scope, leftPipeX + pipeW / 2f, pipeY + pipeH, flameW, flameLen, flameColor, innerFlameColor)
                // Right Flame
                drawFlame(scope, rightPipeX + pipeW / 2f, pipeY + pipeH, flameW, flameLen, flameColor, innerFlameColor)
            }
        }
    }

    private fun drawFlame(
        scope: DrawScope,
        cx: Float,
        topY: Float,
        width: Float,
        length: Float,
        outerColor: Color,
        innerColor: Color
    ) {
        val outerPath = Path().apply {
            moveTo(cx - width / 2f, topY)
            lineTo(cx, topY + length)
            lineTo(cx + width / 2f, topY)
            close()
        }
        scope.drawPath(outerPath, color = outerColor)

        val innerPath = Path().apply {
            moveTo(cx - width * 0.25f, topY)
            lineTo(cx, topY + length * 0.6f)
            lineTo(cx + width * 0.25f, topY)
            close()
        }
        scope.drawPath(innerPath, color = innerColor)
    }

    private fun drawSpeedLines(scope: DrawScope, width: Float, height: Float, animationTime: Float) {
        val lineCount = 14
        val centerX = width / 2f
        val centerY = height * 0.48f

        for (i in 0 until lineCount) {
            val angle = (i * (2f * PI / lineCount)) + (animationTime * 3f)
            val innerR = width * 0.22f
            val outerR = width * 0.55f

            val sx = centerX + cos(angle).toFloat() * innerR
            val sy = centerY + sin(angle).toFloat() * innerR
            val ex = centerX + cos(angle).toFloat() * outerR
            val ey = centerY + sin(angle).toFloat() * outerR

            scope.drawLine(
                color = TurboCyan.copy(alpha = 0.45f),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = 3f
            )
        }
    }

    private fun drawParticles(scope: DrawScope, particles: List<RaceParticle>) {
        for (p in particles) {
            if (p.life > 0f) {
                scope.drawCircle(
                    color = Color(p.color).copy(alpha = (p.life / p.maxLife) * p.alpha),
                    radius = p.size,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}
