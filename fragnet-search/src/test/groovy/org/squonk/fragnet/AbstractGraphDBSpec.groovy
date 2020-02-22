package org.squonk.fragnet

import org.squonk.fragnet.service.GraphDB
import spock.lang.Shared
import spock.lang.Specification

abstract class AbstractGraphDBSpec extends Specification {

    @Shared
    protected GraphDB graphDB

    def setupSpec() { // run before the first feature method
        graphDB = new GraphDB()
    }
    def cleanupSpec() { // run after the last feature method
        graphDB?.close()
    }
}
