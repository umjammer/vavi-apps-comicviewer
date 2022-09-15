[![Release](https://jitpack.io/v/umjammer/vavi-apps-comicviewer.svg)](https://jitpack.io/#umjammer/vavi-apps-comicviewer)
[![Java CI](https://github.com/umjammer/vavi-apps-comicviewer-avif/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-apps-comicviewer-avif/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-apps-comicviewer/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/vavi-apps-comicviewer/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-8-b07219)

# vavi-apps-comicviewer

<a href="https://brandmark.io/"><image src="https://repository-images.githubusercontent.com/534397011/27e695b5-6224-4edd-8fb8-d8dbf8bd14b8" width="640"/></a>

the comic viewer avif supported.<br/>
this is a stopgap until Ventura (support avif natively) release.

## Usage

 * open ... drop an archive or a folder into the application window
 * next ... ^N, click the left page, left arrow (with shift key ... shift 1 page next)
 * prev ... ^P, click the right page, right arrow (with shift key ... shift 1 page prev)
 * magnify ... click w/ the command key and drag

## TODO

 * ~~resizing when the window is larger than an image~~
 * [jpackager](https://github.com/fvarrui/JavaPackager)
   * application title
     * ~~`-Dapple.awt.application.name=Foo` doesn't work~~
     * use `macConfig.infoPlist.additionalEntries` key:`CFBundleName`
   * `macConfig.icnsFile` doesn't work?
   * drop into mac application
     * info.plist? -> right, use `CFBundleTypeExtensions` for accepting to drop
     * `CFProcessPath`? -> env
     * info.plist `:Javax:JVMVersion` doesn't work
   * recent opened files menu 
