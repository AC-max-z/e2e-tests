package ru.belkacar.core.test

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.geojson.GeoJsonReader

class GeometryGenerator : ObjectGenerator<Geometry> {
    //    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
    private var geometryType: GeometryType = GeometryType.DEFAULT

    private val defaultPolygon = """
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

    private val biggerThanDefaultPolygon = """
        {
        "type": "Polygon",
        "coordinates": [
          [
            [
              37.254638671875,
              55.59076338488528
            ],
            [
              38.0841064453125,
              55.59076338488528
            ],
            [
              38.0841064453125,
              55.88763544617004
            ],
            [
              37.254638671875,
              55.88763544617004
            ],
            [
              37.254638671875,
              55.59076338488528
            ]
          ]
        ]
      }
    """.trimIndent()

    private val smallerThanDefaultPolygon = """
        {
        "type": "Polygon",
        "coordinates": [
          [
            [
              37.62169361114502,
              55.75443355110991
            ],
            [
              37.62993335723876,
              55.75443355110991
            ],
            [
              37.62993335723876,
              55.75783858380449
            ],
            [
              37.62169361114502,
              55.75783858380449
            ],
            [
              37.62169361114502,
              55.75443355110991
            ]
          ]
        ]
      }
    """.trimIndent()

    private val boobLick = """
        {
                "type": "Polygon",
                "coordinates": [
                    [
                        [37.52706027441407, 55.755151153651255],
                        [37.48998141699218, 55.73965922168283],
                        [37.48311496191406, 55.712533473263065],
                        [37.51744723730469, 55.69237066441155],
                        [37.56139254980469, 55.68461295597482],
                        [37.64653659277343, 55.675301665392965],
                        [37.705588106445305, 55.69857571830571],
                        [37.73580050878906, 55.71563451667248],
                        [37.722067598632805, 55.75127874980543],
                        [37.70421481542969, 55.78766410628874],
                        [37.65477633886719, 55.7930802842255],
                        [37.588858370117194, 55.795401271825455],
                        [37.54353976660157, 55.78921166287346],
                        [37.52706027441407, 55.755151153651255]
                    ],
                    [
                        [37.57924533300781, 55.72493616363533],
                        [37.58061862402344, 55.75127874980543],
                        [37.664389375976576, 55.75747441064754],
                        [37.67125583105469, 55.71408402587071],
                        [37.62319064550781, 55.70555522164443],
                        [37.57924533300781, 55.72493616363533]
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

    private fun getGeo(): String {
        return when (geometryType) {
            GeometryType.LESSER -> smallerThanDefaultPolygon
            GeometryType.BIGGER -> biggerThanDefaultPolygon
            GeometryType.DEFAULT -> defaultPolygon
            GeometryType.POINT -> point
            GeometryType.LINESTRING -> linestring
            GeometryType.PINNED_OUT -> boobLick
        }
    }


    private var geometry: () -> Geometry = { decodeGeoJson(getGeo()) }

    fun withType(geometryType: GeometryType) = apply { this.geometryType = geometryType }

    override fun generate(): Geometry {
        return geometry()
    }
}

enum class GeometryType {
    DEFAULT, BIGGER, LINESTRING, POINT, LESSER, PINNED_OUT
}
