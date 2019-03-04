## Build Assembly
1. Set version in mdw-draw/build.gradle.kts, mdw-studio/build.gradle.kts and mdw-studio/resources/META-INF/plugin.xml
2. Open Gradle tool window
3. Run task mdw-studio/build/assemble

## Deploy locally
1. Launch IntelliJ IDEA
2. Settings/Preferences > Plugins > Install Plugin from Disk
3. Browse to mdw-studio/build/distributions/mdw-studio-X.X.X.zip
4. Restart IntellJ when prompted

## Beta Channel
  - http://www.jetbrains.org/intellij/sdk/docs/plugin_repository/custom_channels.html
 
## Plugin URL
  - https://plugins.jetbrains.com/plugin/11095-mdw-studio
  
## Publish
0. (One-time step) Add valid credentials in ~/.gradle/gradle.properties (in your HOME directory):
   ```
   intellijPublishUsername=myusername
   intellijPublishPassword=mypassword
   ```
1. Update version in build.gradle.kts (x2) and resources/META-INF/plugin.xml.
2. Remove previous snapshot from beta channel if present.
3. (Release build only) - Comment out the PublishTask Channels entry in mdw-studio/build.gradle.kts (but do not commit)
4. Run Gradle task intellij/publishPlugin.
5. After success:
6. Commit/push changes.
7. Tag Git master branch (eg: 1.2.0):
    ```
    git tag -a 1.2.0 -m "1.2.0"
    git tag
    git push origin --tags
    ```   
   - Revert PublishTask Channels comment-out
   - Commit new versions.
8. Changelog (in top-level mdw-studio):
   - Close milestone in GitHub.
   - Generate changelog for milestone.
   ```
   github_changelog_generator --no-pull-request  --filter-by-milestone --future-release '1.x.x' --exclude-labels duplicate,invalid,wontfix,question --output changes.md
   ```
   - Merge changes.md into top of CHANGELOG.md.
   - Commit and push.
9. Next snapshot
   - Set next SNAPSHOT in build.gradle.kts x2 + plugin.xml
   - Commit and push snapshot versions.
