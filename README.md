# zmq-jni

[![Build Status](https://travis-ci.org/zeromq/zmq-jni.png)](https://travis-ci.org/zeromq/zmq-jni)

Simple High Performance JNI Wrapper for ØMQ

## Motivation

The goal of this project is to simplify and ease the development of high
performance ØMQ binding on the JVM. Ideally when this project stabilizes, it
will underpin libraries such as: [jzmq](https://github.com/zeromq/jzmq),
[cljzmq](https://github.com/zeromq/cljzmq) and
[zmq-async](https://github.com/lynaghk/zmq-async).

## Building

```bash
cmake .
mvn compile
mvn clojure:test
```
## Deploy locally

```bash
mvn install
```

## Troubleshooting

If cmake . fails with an error like "Could NOT find JNI (missing: JAVA_INCLUDE_PATH JAVA_INCLUDE_PATH2
JAVA_AWN_INCLUDE_PATH)", the most likely culprit seems to be an issue with your JAVA_HOME environment
variable.

## TODO

* Build 32 and 64 bit libraries for Windows, Linux and Mac OS X. Currently I
  only include Linux/amd64 inside the JAR

## Contributions

zmq-jni uses the
[C4.1 (Collective Code Construction Contract)](http://rfc.zeromq.org/spec:22)
process for contributions.

## Acknowledgements

YourKit is kindly supporting ZeroMQ project with its full-featured [Java Profiler](http://www.yourkit.com/java/profiler/index.jsp).

## License

Copyright © 2013-2014 Trevor Bernard

This software is licensed under the [MPL] 2.0:

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/.

[MPL]: http://www.mozilla.org/MPL/2.0/
