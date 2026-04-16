# Generated Sketch Source Tree

This folder is for reviewed generated sketches that you want Maven to compile with the app without mixing them into the main handwritten source tree under `src/main/java`.

Recommended workflow:

1. Run the PDE converter so it writes raw output into `target/pde-output/...`
2. Review the generated Java and migration report
3. Copy a reviewed sketch class into this folder if you want to try it in the app
4. Rebuild with Maven
5. Launch with `-Dprocessing.sketch-class=...`

Example launch:

```powershell
java "-Dprocessing.sketch-class=com.processing.server.ProcessingSketchGenerated" -jar .\target\processing-server-1.0-SNAPSHOT.jar
```

This folder is compiled by Maven through the `build-helper-maven-plugin` configuration in `pom.xml`.
