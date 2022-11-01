package ru.belkacar.core

import proto.belka.telematics.geofence.v1.Geofence
import ru.belkacar.telematics.geofence.GeofenceDetectorKafkaState

class GeofenceMatchers {
    class DetectorState {

        class CurrentPointGeofences {
            fun hasAtLeastOneGeofence(geofence: Geofence, state: GeofenceDetectorKafkaState): Boolean {
                return state.currentPoinGeofences.any { i -> i.id.value.toString() == geofence.id.value.uuidString }
            }

            fun hasExactlyOneGeofence(geofence: Geofence, state: GeofenceDetectorKafkaState): Boolean {
                return state.currentPoinGeofences
                    .filter { i -> i.id.value.toString() == geofence.id.value.uuidString }
                    .size == 1
            }

            fun hasNoGeofence(geofence: Geofence, state: GeofenceDetectorKafkaState): Boolean {
                return state.currentPoinGeofences.none { i -> i.id.value.toString() == geofence.id.value.uuidString }
            }
        }

        class LastPointGeofences {
            fun hasNoGeofence(geofence: Geofence, state: GeofenceDetectorKafkaState): Boolean {
                return state.lastPointGeofences.none { i -> i.id.value.toString() == geofence.id.value.uuidString }
            }

            fun hasExactlyOneGeofence(geofence: Geofence, state: GeofenceDetectorKafkaState): Boolean {
                return state.lastPointGeofences
                    .filter { i -> i.id.value.toString() == geofence.id.value.uuidString }
                    .size == 1
            }

            fun hasAtLeastOneGeofence(geofence: Geofence, state: GeofenceDetectorKafkaState): Boolean {
                return state.lastPointGeofences
                    .any { i -> i.id.value.toString() == geofence.id.value.uuidString }
            }
        }
    }
}