## Build Assembly
1. Set version in mdw-draw/build.gradle.kts and mdw-studio/build.gradle.kts
2. Open Gradle tool window
3. Run task mdw-studio/build/assemble

## Deploy locally
1. Launch IntelliJ IDEA
2. Settings/Preferences > Plugins > Install Plugin from Disk
3. Browse to mdw-studio/build/distributions/mdw-studio-X.X.X.zip
4. Restart IntellJ when prompted
5. If testing is okay, commit build.gradle.kts changes

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
2. (Release build only) - Comment out the PublishTask Channels entry (but do not commit)
3. Run Gradle task intellij/publishPlugin.
4. After success:
   - Revert PublishTask comment-out
   - Set next SNAPSHOT in build.gradle.kts x2, and commit