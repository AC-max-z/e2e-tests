package ru.belkacar.telematics.geofence

import proto.belka.telematics.geofence.UUID
import proto.belka.telematics.geofence.v1.Geofence

fun Geofence.Id.toUUID(): java.util.UUID = java.util.UUID.fromString(this.value.uuidString)