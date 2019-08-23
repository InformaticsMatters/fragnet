package org.squonk.fragnet.service

import spock.lang.Specification

class GraphDBSpec extends Specification {

    void "wait for driver"() {

        GraphDB db = new GraphDB()

        when:
        long t0 = new Date().getTime()
        def driver = db.getDriver()
        long t1 = new Date().getTime()
        println "Getting driver took ${t1 - t0}ms"

        then:
        driver != null

        cleanup:
        db?.close()
    }

    void "wait for session"() {

        GraphDB db = new GraphDB()

        when:
        long t0 = new Date().getTime()
        def session = db.getSession()
        long t1 = new Date().getTime()
        println "Getting driver took ${t1 - t0}ms"

        then:
        session != null

        cleanup:
        db?.close()
    }
}
