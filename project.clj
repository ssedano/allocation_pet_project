(defproject allocation_clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [korma "0.3.0-beta10"]
                 [log4j "1.2.14" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [mysql/mysql-connector-java "5.1.6"]
                 [org.jclouds.provider/jclouds-abiquo "2.2-SNAPSHOT"]
                 [org.jclouds/jclouds-compute "1.4.0"]
                 [ch.qos.logback/logback-classic "1.0.0"]
                 [faker "0.2.2"]
                 [org.clojars.tavisrudd/redis-clojure "1.3.1"]]
  :main allocation_clj.core)
