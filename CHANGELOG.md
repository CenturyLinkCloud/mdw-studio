# Change Log

## [2.1.1](https://github.com/CenturyLinkCloud/mdw-studio/tree/2.1.1) (2020-02-18)
[Full Changelog](https://github.com/CenturyLinkCloud/mdw-studio/compare/2.1.0...2.1.1)

**Implemented enhancements:**

- General purpose Configurator help links [\#122](https://github.com/CenturyLinkCloud/mdw-studio/issues/122)
- Inspection to prevent activity logging bypass [\#121](https://github.com/CenturyLinkCloud/mdw-studio/issues/121)

**Fixed bugs:**

- Process export to PDF does not render markdown documentation content [\#120](https://github.com/CenturyLinkCloud/mdw-studio/issues/120)
- Process export to PDF/HTML/PNG does not show icons for built-in implementors [\#119](https://github.com/CenturyLinkCloud/mdw-studio/issues/119)
- Hardcoded Maven Central base URL uses HTTP [\#117](https://github.com/CenturyLinkCloud/mdw-studio/issues/117)
- ObjectNotDisposedException from Editor Widget [\#115](https://github.com/CenturyLinkCloud/mdw-studio/issues/115)

## [2.1.0](https://github.com/CenturyLinkCloud/mdw-studio/tree/2.1.0) (2020-01-24)
[Full Changelog](https://github.com/CenturyLinkCloud/mdw-studio/compare/2.0.3...2.1.0)

**Implemented enhancements:**

- Allow -SNAPSHOT versions in package.yaml [\#114](https://github.com/CenturyLinkCloud/mdw-studio/issues/114)
- Support package.yaml dependencies [\#113](https://github.com/CenturyLinkCloud/mdw-studio/issues/113)

## [2.0.3](https://github.com/CenturyLinkCloud/mdw-studio/tree/2.0.3) (2020-01-17)
[Full Changelog](https://github.com/CenturyLinkCloud/mdw-studio/compare/2.0.2...2.0.3)

**Implemented enhancements:**

- Ability to increment activity ids within a process [\#86](https://github.com/CenturyLinkCloud/mdw-studio/issues/86)

**Fixed bugs:**

- GitLab asset discovery limited by default per\_page parameter value [\#112](https://github.com/CenturyLinkCloud/mdw-studio/issues/112)
- Some CLI-based actions broken due to Maven Central HTTPS requirement [\#110](https://github.com/CenturyLinkCloud/mdw-studio/issues/110)

## [2.0.2](https://github.com/CenturyLinkCloud/mdw-studio/tree/2.0.2) (2020-01-10)
[Full Changelog](https://github.com/CenturyLinkCloud/mdw-studio/compare/2.0.1...2.0.2)

**Implemented enhancements:**

- Support custom task pagelet assets [\#109](https://github.com/CenturyLinkCloud/mdw-studio/issues/109)
- Debug Groovy script activities [\#31](https://github.com/CenturyLinkCloud/mdw-studio/issues/31)

**Fixed bugs:**

- Kotlin script activity right-click does not show Open Kotlin menu item [\#101](https://github.com/CenturyLinkCloud/mdw-studio/issues/101)

## [2.0.1](https://github.com/CenturyLinkCloud/mdw-studio/tree/2.0.1) (2020-01-06)
[Full Changelog](https://github.com/CenturyLinkCloud/mdw-studio/compare/1.3.7...2.0.1)

**Implemented enhancements:**

- Snap-to-grid option for process design canvas [\#108](https://github.com/CenturyLinkCloud/mdw-studio/issues/108)
- Compatibility with IntelliJ 2019.3 [\#105](https://github.com/CenturyLinkCloud/mdw-studio/issues/105)
- Default to saving yaml-format processes if app uses \> MDW 6.1.23 [\#104](https://github.com/CenturyLinkCloud/mdw-studio/issues/104)
- Display milestone activity colors in process canvas [\#99](https://github.com/CenturyLinkCloud/mdw-studio/issues/99)

**Fixed bugs:**

- Canvas entities not selectable when zoomed [\#103](https://github.com/CenturyLinkCloud/mdw-studio/issues/103)
- Bad @Activity pagelet path prevents Toolbox from loading [\#100](https://github.com/CenturyLinkCloud/mdw-studio/issues/100)

## [1.3.7](https://github.com/CenturyLinkCloud/mdw-studio/tree/1.3.7) (2019-10-04)
[Full Changelog](https://github.com/CenturyLinkCloud/mdw-studio/compare/1.3.6...1.3.7)

**Fixed bugs:**

- Groovy assets in temp dir break activity annotations scan [\#98](https://github.com/CenturyLinkCloud/mdw-studio/issues/98)

## [1.3.6](https://github.com/CenturyLinkCloud/mdw-studio/tree/1.3.6) (2019-09-24)
[Full Changelog](https://github.com/CenturyLinkCloud/mdw-studio/compare/1.3.5...1.3.6)

**Implemented enhancements:**

- Recognize @Activity annotations that are not asset-based [\#97](https://github.com/CenturyLinkCloud/mdw-studio/issues/97)

## [1.3.5](https://github.com/CenturyLinkCloud/mdw-studio/tree/1.3.5) (2019-07-25)
[Full Changelog](https://github.com/CenturyLinkCloud/mdw-studio/compare/1.3.4...1.3.5)

**Implemented enhancements:**

- Asset vercheck and auto-increment should use actual Git branch [\#96](https://github.com/CenturyLinkCloud/mdw-studio/issues/96)
- Compatibility with IntelliJ 2019.2 [\#95](https://github.com/CenturyLinkCloud/mdw-studio/issues/95)

**Fixed bugs:**

- Transition retry count cannot be removed [\#91](https://github.com/CenturyLinkCloud/mdw-studio/issues/91)
- Refresh Toolbox after asset import [\#90](https://github.com/CenturyLinkCloud/mdw-studio/issues/90)

## [1.3.4](https://github.com/CenturyLinkCloud/mdw-studio/tree/1.3.4) (2019-07-23)
[Full Changelog](https://github.com/CenturyLinkCloud/mdw-studio/compare/1.3.3...1.3.4)

**Implemented enhancements:**

- Support yaml-format .proc assets [\#92](https://github.com/CenturyLinkCloud/mdw-studio/issues/92)
- Processes viewed from VCS History should open in canvas editor [\#88](https://github.com/CenturyLinkCloud/mdw-studio/issues/88)
- Compatibility with Java 11 [\#77](https://github.com/CenturyLinkCloud/mdw-studio/issues/77)
- Support editing of dynamic java activity code opened automatically by debugger or Find Usages [\#72](https://github.com/CenturyLinkCloud/mdw-studio/issues/72)
- Find usages and refactoring in dynamic Java and Groovy activities [\#32](https://github.com/CenturyLinkCloud/mdw-studio/issues/32)

**Fixed bugs:**

- Service Process checkbox value does not stick [\#93](https://github.com/CenturyLinkCloud/mdw-studio/issues/93)

## [1.3.3](https://github.com/CenturyLinkCloud/mdw-studio/tree/1.3.3) (2019-06-03)
[Full Changelog](https://github.com/CenturyLinkCloud/mdw-studio/compare/1.3.2...1.3.3)

**Implemented enhancements:**

- Ability to specify package version [\#62](https://github.com/CenturyLinkCloud/mdw-studio/issues/62)
- Wizard to create external event handler [\#49](https://github.com/CenturyLinkCloud/mdw-studio/issues/49)
- Checkbox and DateTime widgets should have a way to enter expression values [\#19](https://github.com/CenturyLinkCloud/mdw-studio/issues/19)
- Process context action: Find Usages [\#15](https://github.com/CenturyLinkCloud/mdw-studio/issues/15)

**Fixed bugs:**

- Cannot render Design tab for Event wait with expression driven timeout value [\#78](https://github.com/CenturyLinkCloud/mdw-studio/issues/78)
- New project wizard should use import gradle structure [\#60](https://github.com/CenturyLinkCloud/mdw-studio/issues/60)
- Cannot enter numbers greater 1000 for transition delay [\#85](https://github.com/CenturyLinkCloud/mdw-studio/issues/85)
- Dynamic Java activity copy/paste results in cross-up source code attributes [\#82](https://github.com/CenturyLinkCloud/mdw-studio/issues/82)
- Task editor incorrectly populating default notifier class and asset version [\#81](https://github.com/CenturyLinkCloud/mdw-studio/issues/81)

## [1.3.2](https://github.com/CenturyLinkCloud/mdw-studio/tree/1.3.2) (2019-04-04)
[Full Changelog](https://github.com/CenturyLinkCloud/mdw-studio/compare/1.3.1...1.3.2)

**Fixed bugs:**

- Activity dynamic Java intermittently becomes read-only [\#75](https://github.com/CenturyLinkCloud/mdw-studio/issues/75)
- Suppress MDW console tool window for non-MDW projects [\#76](https://github.com/CenturyLinkCloud/mdw-studio/issues/76)

## [1.3.1](https://github.com/CenturyLinkCloud/mdw-studio/tree/1.3.1) (2019-04-02)
[Full Changelog](https://github.com/CenturyLinkCloud/mdw-studio/compare/1.3.0...1.3.1)

**Fixed bugs:**

- Asset vercheck may require Git credentials [\#74](https://github.com/CenturyLinkCloud/mdw-studio/issues/74)

## [1.3.0](https://github.com/CenturyLinkCloud/mdw-studio/tree/1.3.0) (2019-03-31)
[Full Changelog](https://github.com/CenturyLinkCloud/mdw-studio/compare/1.2.1...1.3.0)

**Implemented enhancements:**

- Support IntelliJ 2019 [\#73](https://github.com/CenturyLinkCloud/mdw-studio/issues/73)
- Automatically mark temp and node\_modules dirs as excluded [\#67](https://github.com/CenturyLinkCloud/mdw-studio/issues/67)
- Dialog widgets as item types in pagelet [\#64](https://github.com/CenturyLinkCloud/mdw-studio/issues/64)
- Automatic asset version check/fix to prevent import conflicts [\#55](https://github.com/CenturyLinkCloud/mdw-studio/issues/55)

**Fixed bugs:**

- Incorrect transition delay attributes [\#71](https://github.com/CenturyLinkCloud/mdw-studio/issues/71)
- Issue with dragging activity into embedded subflow [\#70](https://github.com/CenturyLinkCloud/mdw-studio/issues/70)
- Cannot debug dynamic java activity [\#69](https://github.com/CenturyLinkCloud/mdw-studio/issues/69)
- Dynamic java/groovy script contents jump from one editor to another [\#68](https://github.com/CenturyLinkCloud/mdw-studio/issues/68)

## [1.2.1](https://github.com/CenturyLinkCloud/mdw-studio/tree/1.2.1) (2019-03-04)
[Full Changelog](https://github.com/CenturyLinkCloud/mdw-studio/compare/1.2.0...1.2.1)

**Fixed bugs:**

- Discovery GitImport failure when subpackages included [\#65](https://github.com/CenturyLinkCloud/mdw-studio/issues/65)
- Discovery errors on Windows [\#63](https://github.com/CenturyLinkCloud/mdw-studio/issues/63)

## [1.2.0](https://github.com/CenturyLinkCloud/mdw-studio/tree/1.2.0) (2019-02-25)
**Implemented enhancements:**

- Disable AssetFileListener from updating asset versions during a git pull operation [\#61](https://github.com/CenturyLinkCloud/mdw-studio/issues/61)
- Discover/Import asset packages from Git [\#59](https://github.com/CenturyLinkCloud/mdw-studio/issues/59)

**Fixed bugs:**

- Adapter activities do not show Output documents lists in Script tab [\#58](https://github.com/CenturyLinkCloud/mdw-studio/issues/58)
- Single invalid package.yaml file should not prevent MDW plugin from loading [\#57](https://github.com/CenturyLinkCloud/mdw-studio/issues/57)
- Auto version updates FileListener anomalies [\#56](https://github.com/CenturyLinkCloud/mdw-studio/issues/56)

## [1.1.6](https://github.com/CenturyLinkCloud/mdw-studio/tree/1.1.6) (2019-01-29)
**Implemented enhancements:**

- Capture all activity timings via process-level checkbox [\#53](https://github.com/CenturyLinkCloud/mdw-studio/issues/53)
- Pagelet content in @Activity annotations can be externalized [\#47](https://github.com/CenturyLinkCloud/mdw-studio/issues/47)
- Upgrade to IntelliJ 2018.3.x [\#45](https://github.com/CenturyLinkCloud/mdw-studio/issues/45)

**Fixed bugs:**

- Restore Dummy Activity to toolbox [\#51](https://github.com/CenturyLinkCloud/mdw-studio/issues/51)
- ClassCastException in dynamic Java update listener [\#48](https://github.com/CenturyLinkCloud/mdw-studio/issues/48)

## [1.1.5](https://github.com/CenturyLinkCloud/mdw-studio/tree/1.1.5) (2018-12-06)
**Implemented enhancements:**

- Search or filter the toolbox [\#28](https://github.com/CenturyLinkCloud/mdw-studio/issues/28)
- Error reporting to mdw-central [\#18](https://github.com/CenturyLinkCloud/mdw-studio/issues/18)

**Fixed bugs:**

- Adapter activity pre/post script editing anomaly [\#46](https://github.com/CenturyLinkCloud/mdw-studio/issues/46)
- Dynamic Java breakpoints not enabled for processes with spaces in their names [\#44](https://github.com/CenturyLinkCloud/mdw-studio/issues/44)

## [1.1.4](https://github.com/CenturyLinkCloud/mdw-studio/tree/1.1.4) (2018-11-26)
**Implemented enhancements:**

- Add mdwHub and mdwSyncServer actions to the Tools menu [\#20](https://github.com/CenturyLinkCloud/mdw-studio/issues/20)

**Fixed bugs:**

- Ignore .evth files in autoversioning [\#41](https://github.com/CenturyLinkCloud/mdw-studio/issues/41)
- Ignore line ending diffs in AssetFileListener comparison [\#40](https://github.com/CenturyLinkCloud/mdw-studio/issues/40)
- Auto version file updates should Ignore .impl and .evth files [\#39](https://github.com/CenturyLinkCloud/mdw-studio/issues/39)

**Closed issues:**

- Task editor should allow option entry for ParameterizedStrategies [\#23](https://github.com/CenturyLinkCloud/mdw-studio/issues/23)

## [1.1.3](https://github.com/CenturyLinkCloud/mdw-studio/tree/1.1.3) (2018-11-02)
**Implemented enhancements:**

- Export to PDF [\#36](https://github.com/CenturyLinkCloud/mdw-studio/issues/36)
- Honor .mdwignore [\#33](https://github.com/CenturyLinkCloud/mdw-studio/issues/33)
- Option to open dynamic java/script content in Editor tab [\#29](https://github.com/CenturyLinkCloud/mdw-studio/issues/29)
- Debug dynamic java activities [\#27](https://github.com/CenturyLinkCloud/mdw-studio/issues/27)
- Custom baseline data [\#26](https://github.com/CenturyLinkCloud/mdw-studio/issues/26)
- Toolbox context actions: New..., Find Usages, View Source [\#16](https://github.com/CenturyLinkCloud/mdw-studio/issues/16)
- Task template editor [\#3](https://github.com/CenturyLinkCloud/mdw-studio/issues/3)
- Enable cf push without .git [\#2](https://github.com/CenturyLinkCloud/mdw-studio/issues/2)
- Fork from java-buildpack [\#1](https://github.com/CenturyLinkCloud/mdw-studio/issues/1)

**Fixed bugs:**

- Handle $DefaultNotices in legacy manual task templates [\#37](https://github.com/CenturyLinkCloud/mdw-studio/issues/37)
- Exception configuring newly-added single process invoke activity [\#35](https://github.com/CenturyLinkCloud/mdw-studio/issues/35)
- Ignore Archive folder [\#25](https://github.com/CenturyLinkCloud/mdw-studio/issues/25)
- Newly-added @Activity implementors don't show up immediately in Toolbox [\#21](https://github.com/CenturyLinkCloud/mdw-studio/issues/21)
- BPMN export/import not working from Studio [\#17](https://github.com/CenturyLinkCloud/mdw-studio/issues/17)
- Error when deleting asset [\#14](https://github.com/CenturyLinkCloud/mdw-studio/issues/14)
- Newly-added manual task activity causes runtime error [\#9](https://github.com/CenturyLinkCloud/mdw-studio/issues/9)

**Closed issues:**

- Activity right-click \> Go To \> Declaration [\#34](https://github.com/CenturyLinkCloud/mdw-studio/issues/34)
- New project wizard [\#22](https://github.com/CenturyLinkCloud/mdw-studio/issues/22)
- New activity implementor wizard [\#13](https://github.com/CenturyLinkCloud/mdw-studio/issues/13)
- Version control "revert" takes x 2 [\#6](https://github.com/CenturyLinkCloud/mdw-studio/issues/6)

\* *This Change Log was automatically generated by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*