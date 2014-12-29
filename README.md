groovy-maven
============

Groovy cli tool to make requests on maven repository.

With this tool **you can**:

1. Get newest version of your artifact from maven repository.
2. Update version of your artifact in pom.xml.

**When it's useful**:

1. When you can't use range in your pom.xml.
2. When you would like to automate a process of you pom.xml version maintenance.

**How it works**:

Script depends on [__Aether__](http://eclipse.org/aether/) libraries to search artifacts and get newest version of specified range.


Usage:
============

```sh
$groovy maven-latest-version.groovy -h

usage: maven-latest-version
 -a,--artifacts <arg>      [REQUIRED] list of artifacts to get latest
                           version. Format:
                           groupId:artifactId:version;groupId:artifactId:v
                           ersion. Example: log4j:log4j:[1,).
 -h,--help                 prints this help
 -r,--repositories <arg>   [REQUIRED] list of repositories where to search
                           artifact. Format: id1=url1;id2=url2. Example:
                           central=http://central.maven.org/maven2/
 -u,--update <arg>         path to pom.xml file to update version of
                           specified artifact. May be directory to search
                           pom.xml files recursively.
```

Example:
============
```sh
$groovy maven-latest-version.groovy -a "log4j:log4j:[1.0,1.2.5];org.apache.commons:commons-io:[0,)" -r cental=http://repo1.maven.org/maven2/ -u ../../pom.xml
Querying repos...
====> log4j.log4j: 1.2.5 -> cental (http://repo1.maven.org/maven2/, default, releases+snapshots)
====> org.apache.commons.commons-io: 1.3.2 -> cental (http://repo1.maven.org/maven2/, default, releases+snapshots)
Updating files...
Processing /path_to_your_pom/pom.xml file
====> Changing version: log4j.log4j 1.2.3 => 1.2.5
```
