GPX Animator
============

Introduction
------------

GPX Animator generates video from GPX files.
More information can be found at https://gpx-animator.app.

For program help run:

```
java -jar GpxAnimator.jar --help
```

Basic usage
-----------

```
java -jar GpxAnimator.jar --input track.gpx
```

Build
-----------

GPX Animator uses the [Maven](https://maven.apache.org/) build system to create the JAR file. You need to have Maven installed on your system. If you don't have Maven installed, consider using [SDKMAN](https://sdkman.io/) to install it, first.

```
./mvnw clean package
```

After a successful build, the JAR file can be found in the `target` directory.

Test
-----------

In the directory `src/test/` tests are located that can be run with the following command:

```
./mvnw test
```

Features
--------
* supports multiple GPX tracks with multiple track segments
* skipping idle parts
* configurable color, label, width and time offset per track
* configurable video size, fps and speedup or total video time
* background map from any public TMS server

Tutorials
--------
- [GPS Tracks Animation mit "GPX Animator" (Marcus Bersheim)](https://www.youtube.com/watch?v=AtcBVrbB6bg) :de:
