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

    private var geometry: () -> Geometry = when (geometryType) {
        GeometryType.DEFAULT -> {
            { decodeGeoJson((defaultPolygon)) }
        }

        GeometryType.BIGGER -> {
            { decodeGeoJson(biggerThanDefaultPolygon) }
        }

        GeometryType.LINESTRING -> {
            { decodeGeoJson(linestring) }
        }

        GeometryType.POINT -> {
            { decodeGeoJson(point) }
        }

        GeometryType.LESSER -> {{decodeGeoJson(smallerThanDefaultPolygon)}}
    }

    fun withType(geometryType: GeometryType) = apply { this.geometryType = geometryType }

    override fun generate(): Geometry {
        return geometry()
    }
}

enum class GeometryType {
    DEFAULT, BIGGER, LINESTRING, POINT, LESSER
}
