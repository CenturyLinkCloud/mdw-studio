## Build Assembly
1. Open Gradle tool window
2. Run task mdw-studio/build/assemble

## Deploy locally
1. Launch IntelliJ IDEA
2. Settings > Plugins > Install Plugin from Disk
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
1. Update version in build.gradle and resources/META-INF/plugin.xml.
2. Run Gradle task intellij/publishPlugin.