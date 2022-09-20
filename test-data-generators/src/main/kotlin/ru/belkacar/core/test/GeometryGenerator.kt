package ru.belkacar.telematics.geofence

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.geojson.GeoJsonReader
import ru.belkacar.core.test.ObjectGenerator

class GeometryGenerator : ObjectGenerator<Geometry> {
//    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    private val defualtPolygon = """
        {
            "type": "Polygon",
            "coordinates": [
                [
                    [
                        37.559165954589844,
                        55.7309766355099
                    ],
                    [
                        37.69477844238281,
                        55.72846342126635
                    ],
                    [
                        37.70061492919922,
                        55.78217228729694
                    ],
                    [
                        37.56156921386719,
                        55.78352371436103
                    ],
                    [
                        37.559165954589844,
                        55.7309766355099
                    ]
                ]
                ]
            }
    """.trimIndent()

    private val point = """
        {
            "type": "Point",
            "coordinates": [
                37.262015108398444,
                55.86303565487453
            ]
        }
    """.trimIndent()

    private val linestring = """
        {
            "type": "LineString",
            "coordinates": [
                [
                    37.185110811523444,
                    55.85878782891449
                ],
                [
                    37.285361055664055,
                    55.91744376349636
                ]
            ]
        }
    """.trimIndent()

    private fun decodeGeoJson(geometryJson: String): Geometry {
        return with(GeoJsonReader()) {
            read(geometryJson)
        }
    }

    private var geometry: () -> Geometry = { decodeGeoJson(defualtPolygon) }

    fun linestring() = apply { geometry = { decodeGeoJson(linestring) } }
    fun point() = apply { geometry = { decodeGeoJson(point) } }

    override fun generate(): Geometry {
        return geometry()
    }
}
