# TMC for IntelliJ IDEA

TMC for IntelliJ IDEA integrates the University of Helsinki Test My Code service with modern IntelliJ IDEA versions. It downloads course exercises, runs the bundled tests, submits solutions, and displays submission feedback inside the IDE. The main target is the University of Helsinki [Java Programming MOOC](https://java-programming.mooc.fi/).

This fork targets IntelliJ IDEA 2026.1 and newer (`since-build 261`) and produces Java 21-compatible plugin bytecode. Both Community and Ultimate editions are supported when the bundled Java plugin is available.

## Build and test

Requirements:

- JDK 26
- Internet access on the first build so Gradle can download IntelliJ IDEA and test dependencies

The Gradle wrapper pins the build tool version:

```bash
./gradlew clean test buildPlugin verifyPlugin
```

The installable ZIP is created under `build/distributions/`. To install it, open IntelliJ IDEA, choose **Settings > Plugins**, use the gear menu, and select **Install Plugin from Disk**.

To start an isolated development IDE:

```bash
./gradlew runIde
```

To test with a specific local IntelliJ IDEA installation instead of the default 2026.1 test platform:

```bash
./gradlew -PlocalPlatformPath=/absolute/path/to/intellij-idea runIde
```

The old TMC Maven repository is no longer available, so the historical TMC Core runtime and its transitive dependencies remain vendored in `libs/`. Current build and test dependencies are resolved normally by Gradle.

## Using the plugin

1. Open **Settings > TMC Settings** and sign in with your TMC account.
2. Select the University of Helsinki organization and the desired MOOC course.
3. Download exercises from the TMC menu or toolbar.
4. Open a downloaded exercise and use **Run tests**.
5. Submit after the tests pass and review the result in the TMC Test Results tool window.

TMC server access requires a valid account. Unit tests and IDE compatibility checks do not require credentials.

## Project history

The original plugin was developed for the University of Helsinki's RAGE team during the Software Production Project course in summer 2016. This fork preserves that implementation while updating its IntelliJ Platform integration and build for current IDE releases.

Upstream projects:

- [testmycode/tmc-intellij](https://github.com/testmycode/tmc-intellij)
- [thomaslabeyrie/tmc-intellij](https://github.com/thomaslabeyrie/tmc-intellij)
