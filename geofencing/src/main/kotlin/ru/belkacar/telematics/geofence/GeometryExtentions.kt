package ru.belkacar.telematics.geofence

import com.google.protobuf.ByteString
import org.locationtech.jts.geom.Geometry
import proto.belka.telematics.geofence.geometry


fun Geometry.toProto(): proto.belka.telematics.geofence.Geometry {
    return geometry {
        wkbValue = ByteString.copyFrom(
            ru.belkacar.core.test.WKB.fromGeometry(this@toProto).value
        )
    }
}