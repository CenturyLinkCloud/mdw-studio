## Build Assembly
1. Set version in mdw-draw/build.gradle.kts, mdw-studio/build.gradle.kts and mdw-studio/resources/META-INF/plugin.xml
2. Open Gradle tool window
3. Run task mdw-studio/build/assemble

## Deploy locally
1. Launch IntelliJ IDEA
2. Settings/Preferences > Plugins > Install Plugin from Disk
3. Browse to mdw-studio/build/distributions/mdw-studio-X.X.X.zip
4. Restart IntellJ when prompted
5. Test features/functionality.

## Publish
1. Log in to JetBrains plugin site:
   https://plugins.jetbrains.com/plugin/11095-mdw-studio
2. Click "Upload Update" and browse to mdw-studio/build/distributions/mdw-studio-X.X.X.zip
3. Upload to Stable channel
4. Commit/push changes.
5. Tag Git master branch (eg: 2.x.x):
    ```
    git tag -a 2.x.x -m "2.x.x"
    git tag
    git push origin --tags
    ```   
6. Changelog (in top-level mdw-studio):
   - Close milestone in GitHub.
   - Generate changelog for milestone.
   ```
   github_changelog_generator --no-pull-request  --filter-by-milestone --future-release '2.x.x' --exclude-labels duplicate,invalid,wontfix,question --output changes.md
   ```
   - Merge generated changes.md into top of CHANGELOG.md.
   - Commit and push.
7. Next snapshot
   - Set next SNAPSHOT in build.gradle.kts x2 + plugin.xml
   - Commit and push snapshot versions.
8. Studio upgrade will be available once new version is accepted by JetBrains.   

