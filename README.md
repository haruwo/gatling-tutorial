gatling-tutorial
================

1. install sbt, jenv, jenv 1.8
2. run https://git.dmm.com/yajima-yusuke/gatling-sample-app
3. run `sbt "gatling:testOnly LoginSimulation"`

CAUTION!
- gatling must not run >= java9. if run with java9, gatling will throw error `java.lang.ClassCastException: [B cannot be cast to [C`.
  like issue: https://github.com/akka/akka/issues/23702
