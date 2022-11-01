rootProject.name = "telematics-e2e-tests"

include(
    ":kafka",
    ":broadcasting-platform",
    ":broadcasting-platform-tests",
    ":test-data-generators",
    ":bootstrap",
    ":domain-api",
    ":grpc",
    ":geofencing",
    ":geofence-services-tests",
    ":test-matchers",
    ":test-helpers"
)
