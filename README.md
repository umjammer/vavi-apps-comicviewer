[![Release](https://jitpack.io/v/umjammer/vavi-apps-comicviewer.svg)](https://jitpack.io/#umjammer/vavi-apps-comicviewer)
[![Java CI](https://github.com/umjammer/vavi-apps-comicviewer/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-apps-comicviewer/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-apps-comicviewer/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/vavi-apps-comicviewer/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-8-b07219)

# vavi-apps-comicviewer

<a href="https://brandmark.io/"><image src="https://repository-images.githubusercontent.com/534397011/27e695b5-6224-4edd-8fb8-d8dbf8bd14b8" width="640"/></a>

the comic viewer avif supported.<br/>
this is a stopgap until Ventura (support avif natively) release.

## Usage

 * open ...
   * drop an archive or a folder into the application window
   * via open menus
 * next ... (with shift key ... shift 1 page next)
   * ^N
   * click the left page
   * left arrow
 * prev ... (with shift key ... shift 1 page prev)
   * ^P
   * click the right page
   * right arrow
 * magnify ...
   * click w/ the command key and drag
 * jump...
   * move cursor left top corner, the paging slider will appear
 * full screen...
   * ⌘F toggle
   * via view menu toggle

## Remarkable Points

 * avif
 * drop and open via dock icon (how to use mac specific capability on java)
 * resizing smaller/larger keeping aspect ratio component w/ magnifying glass
 * brought you as mac .app by jpackager (how to create mac .app by java)

## TODO

 * ~~resizing when the window is larger than an image~~
 * [jpackager](https://github.com/fvarrui/JavaPackager)
   * application title
     * ~~`-Dapple.awt.application.name=Foo` doesn't work~~
     * use `macConfig.infoPlist.additionalEntries` key:`CFBundleName`
   * `macConfig.icnsFile` doesn't work?
   * `macConfig.codesignApp` true -> need `sudo` on my env -> dir removal failure
     * CONCLUSION: no need to sign
   * entitlement 
     * `macConfig.entitlements` no value: use default
     * Info.plist [sandbox](https://developer.apple.com/library/archive/documentation/Miscellaneous/Reference/EntitlementKeyReference/Chapters/EnablingAppSandbox.html)
     * CONCLUSION: no need to prepare entitlement
 * drop into mac application
   * info.plist? -> right, use `CFBundleTypeExtensions` for accepting to drop
   * `CFProcessPath`? -> env
   * info.plist `:Javax:JVMVersion` doesn't work -> bundle jdk
 * ~~recent opened files menu~~
   * wip: not work on .app
 * ~~sibling files menu~~
   * wip: not work on .app
 * coverflow
   * https://github.com/umjammer/jflow 
 * https://github.com/ymasory/OrangeExtensions
 * https://github.com/dmMaze/comic-text-detector
 * suspend dnnsuperres bec not effective