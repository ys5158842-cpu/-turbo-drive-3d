package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import com.example.data.CarBodyType
import com.example.data.CarModel
import kotlin.math.*

enum class ObstacleType {
    TRAFFIC_CAR,
    ROAD_BARRIER,
    OIL_SLICK,
    POLICE_CAR,
    DESERT_BOULDER
}

enum class PickupType {
    COIN,
    NITRO,
    REPAIR_WRENCH,
    TIME_BONUS
}

data class TrackObstacle(
    val id: Int,
    val type: ObstacleType,
    var segmentIndex: Int,
    var offsetPercent: Float, // -0.8f to +0.8f across road
    var speed: Float = 0f,    // Moving traffic speed
    var laneTarget: Float = offsetPercent,
    var widthScale: Float = 1.0f,
    var hit: Boolean = false
)

data class TrackPickup(
    val id: Int,
    val type: PickupType,
    val segmentIndex: Int,
    val offsetPercent: Float, // -0.7f to +0.7f
    var collected: Boolean = false,
    var spinAngle: Float = 0f
)

data class RoadsideProp(
    val type: PropType,
    val offsetPercent: Float // -2.5 to -1.2 (left) or 1.2 to 2.5 (right)
)

enum class PropType {
    PALM_TREE,
    DESERT_CACTUS,
    DESERT_PYRAMID,
    CYBER_TOWER,
    NEON_BILLBOARD,
    STREET_LAMP,
    GRANDSTAND
}

data class SegmentPoint(
    val world: FloatArray = FloatArray(3), // x, y, z
    val screen: FloatArray = FloatArray(4)  // x, y, w, scale
)

data class RoadSegment(
    val index: Int,
    val p1: SegmentPoint = SegmentPoint(),
    val p2: SegmentPoint = SegmentPoint(),
    var curve: Float = 0f,
    var clip: Float = 0f,
    val props: MutableList<RoadsideProp> = mutableListOf(),
    val obstacles: MutableList<TrackObstacle> = mutableListOf(),
    val pickups: MutableList<TrackPickup> = mutableListOf(),
    var isFinishLine: Boolean = false,
    var isStartLine: Boolean = false
)

class Road3DTrack(
    val mode: GameMode,
    val segmentLength: Float = 200f,
    val drawDistanceSegments: Int = 180,
    val roadWidth: Float = 2000f,
    val cameraHeight: Float = 1000f,
    val fieldOfView: Float = 100f
) {
    val totalSegments: Int = max(60, (mode.trackLengthMeters / (segmentLength / 20f)).toInt())
    val segments = ArrayList<RoadSegment>(totalSegments)
    val finishLineSegmentIndex: Int = totalSegments - 15

    init {
        buildTrack()
    }

    private fun buildTrack() {
        segments.clear()
        var currentY = 0f
        var currentX = 0f

        val random = java.util.Random(mode.id.hashCode().toLong())

        for (i in 0 until totalSegments) {
            val seg = RoadSegment(index = i)
            seg.p1.world[0] = currentX
            seg.p1.world[1] = currentY
            seg.p1.world[2] = i * segmentLength

            // Elevation and curves based on mode
            val progress = i.toFloat() / totalSegments.toFloat()
            val curveMagnitude = when (mode.theme) {
                TrackTheme.CYBERPUNK -> sin(progress * 14f) * 3.8f
                TrackTheme.DESERT -> sin(progress * 10f) * 2.5f + cos(progress * 18f) * 1.5f
                TrackTheme.MOUNTAIN_NIGHT -> sin(progress * 20f) * 4.5f
                else -> sin(progress * 8f) * 2.0f
            }

            val hillMagnitude = when (mode.theme) {
                TrackTheme.DESERT -> sin(progress * 12f) * 750f
                TrackTheme.MOUNTAIN_NIGHT -> cos(progress * 15f) * 900f
                TrackTheme.CYBERPUNK -> sin(progress * 6f) * 400f
                else -> sin(progress * 8f) * 500f
            }

            seg.curve = curveMagnitude
            currentX += curveMagnitude * 10f
            currentY = hillMagnitude

            seg.p2.world[0] = currentX
            seg.p2.world[1] = currentY
            seg.p2.world[2] = (i + 1) * segmentLength

            // Start & Finish line markers
            if (i == 3) seg.isStartLine = true
            if (i == finishLineSegmentIndex) seg.isFinishLine = true

            // Roadside Scenery props
            if (i % 3 == 0 && i < finishLineSegmentIndex) {
                val side = if (random.nextBoolean()) 1 else -1
                val dist = side * (1.3f + random.nextFloat() * 1.5f)
                val propType = when (mode.theme) {
                    TrackTheme.DESERT -> if (random.nextFloat() > 0.85f) PropType.DESERT_PYRAMID else if (random.nextFloat() > 0.4f) PropType.DESERT_CACTUS else PropType.GRANDSTAND
                    TrackTheme.CYBERPUNK -> if (random.nextFloat() > 0.5f) PropType.CYBER_TOWER else PropType.NEON_BILLBOARD
                    TrackTheme.HIGHWAY -> if (random.nextFloat() > 0.4f) PropType.PALM_TREE else PropType.STREET_LAMP
                    TrackTheme.MOUNTAIN_NIGHT -> if (random.nextFloat() > 0.4f) PropType.STREET_LAMP else PropType.PALM_TREE
                    TrackTheme.POLICE_ESCAPE -> if (random.nextFloat() > 0.5f) PropType.STREET_LAMP else PropType.NEON_BILLBOARD
                }
                seg.props.add(RoadsideProp(propType, dist))
            }

            // Obstacles generation
            if (i in 15 until (finishLineSegmentIndex - 10)) {
                // Traffic or obstacles
                if (random.nextFloat() < mode.obstacleDensity * 0.22f) {
                    val lane = when (random.nextInt(3)) {
                        0 -> -0.6f
                        1 -> 0.0f
                        else -> 0.6f
                    }
                    val obsType = if (mode.hasPolice && random.nextFloat() < 0.4f) {
                        ObstacleType.POLICE_CAR
                    } else if (mode.theme == TrackTheme.DESERT && random.nextFloat() < 0.35f) {
                        ObstacleType.DESERT_BOULDER
                    } else if (random.nextFloat() < 0.35f) {
                        ObstacleType.ROAD_BARRIER
                    } else if (random.nextFloat() < 0.3f) {
                        ObstacleType.OIL_SLICK
                    } else {
                        ObstacleType.TRAFFIC_CAR
                    }

                    val speed = if (obsType == ObstacleType.TRAFFIC_CAR) 70f + random.nextFloat() * 40f
                    else if (obsType == ObstacleType.POLICE_CAR) 90f + random.nextFloat() * 50f
                    else 0f

                    seg.obstacles.add(
                        TrackObstacle(
                            id = i * 10,
                            type = obsType,
                            segmentIndex = i,
                            offsetPercent = lane,
                            speed = speed
                        )
                    )
                }

                // Pickups generation (Coins, Nitro, Repair Wrench, Time Bonus)
                if (random.nextFloat() < 0.25f) {
                    val pLane = (random.nextFloat() * 1.4f) - 0.7f
                    val pType = if (mode.hasTimeLimit && random.nextFloat() < 0.3f) {
                        PickupType.TIME_BONUS
                    } else if (random.nextFloat() < 0.2f) {
                        PickupType.NITRO
                    } else if (random.nextFloat() < 0.15f) {
                        PickupType.REPAIR_WRENCH
                    } else {
                        PickupType.COIN
                    }
                    seg.pickups.add(
                        TrackPickup(
                            id = i * 20,
                            type = pType,
                            segmentIndex = i,
                            offsetPercent = pLane
                        )
                    )
                }
            }

            segments.add(seg)
        }
    }

    fun findSegment(positionZ: Float): RoadSegment {
        val index = (positionZ / segmentLength).toInt() % totalSegments
        return segments[max(0, min(totalSegments - 1, index))]
    }
}
