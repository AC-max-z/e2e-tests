package ru.belkacar.core.test

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.WKBReader
import org.locationtech.jts.io.WKBWriter

data class WKB(val value: ByteArray) {

    fun toGeometry(geometryFactory: GeometryFactory): Geometry {
        try {
            return with(WKBReader(geometryFactory)) {
                read(value)
            }
        } catch (e : Exception) {
            throw GeometryParseException(e.message, e)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WKB

        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }

    override fun toString(): String {
        return "WKB(size=${value.size})"
    }

    companion object {
        fun fromGeometry(geometry: Geometry): WKB {
            return with(WKBWriter(2, geometry.srid != 0)) {
                WKB(write(geometry))
            }
        }
    }
}

class GeometryParseException(override val message: String?, override val cause: Throwable?) : RuntimeException()
