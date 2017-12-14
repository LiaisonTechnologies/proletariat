# Proletariat

> [![Build Status](https://travis-ci.org/LiaisonTechnologies/proletariat.svg?branch=master)](https://travis-ci.org/LiaisonTechnologies/proletariat) | [API Docs](https://liaisontechnologies.github.io/proletariat)

Library of the Commons.

A hard-working library of common utilities.

![Proletariat](docs/proletariat.jpg "workers unite!")

## Add Dependency

Add the dependency using the code below that fits your build framework:

Leiningen

```clojure
[com.liaison/proletariat "0.7.3"]
```

Maven

```xml
<dependency>
    <groupId>com.liaison</groupId>
    <artifactId>proletariat</artifactId>
    <version>0.7.3</version>
</dependency>
```

Gradle

```groovy
compile group: 'com.liaison', name: 'proletariat', version: '0.7.3'
```

## User Guide

Proletariat relies on [spec](https://clojure.org/guides/spec) so it will pull in
the latest alpha build of Clojure. The good part is that the library is almost
entirely spec'ed, which is nice. The bad part is that you pull in an alpha
version of Clojure. Luckily Clojure is really good about being
backward-compatible, so you shouldn't run into any major issues.

This library is being actively developed with new features and functionality, so
do check out the [API docs](https://liaisontechnologies.github.io/proletariat)
for an overview of what it offers.

If you find any errors or are confused about something, open an issue on GitHub
and we'll try to help clear things up.

## License

Copyright © 2017 Liaison Technologies

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
