# Building

This snapshot is not yet a normal Gradle Android project.

The original prototype was built manually with:

- `javac`
- Android `d8`
- Android `aapt`
- `zipalign`
- `apksigner`

Recommended cleanup before public release:

1. Create a Gradle project for each app or a multi-module Android project.
2. Add Gradle wrapper files.
3. Move `src` and `AndroidManifest.xml` into standard Android source sets.
4. Configure debug signing locally, not in the repository.
5. Document install commands for:
   - the glasses app
   - the phone app
   - the manager app

Until then, treat this repository as source code for review and archival, not as a one-command buildable release.
