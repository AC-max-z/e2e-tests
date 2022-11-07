# E2E tests

Launch options:

- `./gradlew test` - runs all tests;
- `./gradlew test -DincludeTags="${tagExp}"` - runs tests only including specified JUnit tag expression;
- `./gradlew test -DexcludeTags="${tagExp}"` - runs tests excluding specified JUnit tag expression (ignored if DincludeTags already specified);
- `./gradlew geofence-tests` - runs only tests including tag `geofence-services`;
- `./gradlew broadcasting-tests` - runs only tests including tag `broadcasting-services`;
- `./gradlew detector-tests` - runs only tests including tag `detector`;
- `./gradlew hash-preprocessor-tests` - runs only tests including tag `hash-preprocessor`;
- `./gradlew e2e-tests` - runs only tests including tag `e2e`.