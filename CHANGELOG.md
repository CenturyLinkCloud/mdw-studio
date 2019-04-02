# Change Log

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