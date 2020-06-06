If you've downloaded the source code of the **ProGuardCORE** library, you can
build it yourself with Gradle:

- Build the artifacts:

        ./gradlew assemble
    
- Publish the artifacts to your local Maven cache (something like `~/.m2/`):

        ./gradlew publishToMavenLocal
    
- Build tar and zip archives with the binaries and documentation:

        ./gradlew distTar distZip

- Build the complete API documentation with

        ./gradlew javadoc

You can then find the [API documentation](api/index.html) in `docs/md/api`.
