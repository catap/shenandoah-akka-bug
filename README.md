# Shenandoah akka bug

This repository contains a minimal test case to reproduce a bug that is
originally was reported to akka https://github.com/akka/akka/issues/28601

This test case can be run by
```
mvn clean package
docker-compose up --build --remove-orphans
```

The case will run until it doesn't get the bug then it exists with a code `66`.
At output you can find logs like:
```
server_1  | [setNext] (Node(2036)) ; newNext: Node(2043); next: Node(2043); next(): Node(2043)
server_1  | [pollNode] (Node(2036)); tail.next(): null; tail.next: null
```

That shown than `next` field at a node was setup to some value, but was lost.

Thus bug is reproduced at jvm:
```
openjdk version "1.8.0-builds.shipilev.net-openjdk-shenandoah-jdk8-fastdebug"
OpenJDK Runtime Environment (build 1.8.0-builds.shipilev.net-openjdk-shenandoah-jdk8-fastdebug-b571-20200223)
OpenJDK 64-Bit Server VM (build 25.71-b571-20200223-fastdebug, mixed mode)
```
