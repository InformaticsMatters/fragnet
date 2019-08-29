package org.squonk.fragnet.search.model.v2

import spock.lang.Specification

class GroupMemberSpec extends Specification {

    void "one edge between nodes one hop"() {

        // This tests the case where there is a single one hop path between a pair on nodes
        // It is the counterpart of the "two edges between nodes" test which is the same transform

        // The member
        //"key":"Oc1ccc(-c2ccc([Xe])cc2)cc1","classification":"ADDITION","prototype":"Oc1ccc(-c2ccc(I)cc2)cc1","members":
        // [{"id":123523916,"smiles":"Oc1ccc(-c2ccc(I)cc2)cc1","edgeIds":[[-636668291]]}

        // The edge
        //{"id":123523916,"parentId":123523916,"childId":123525973,
        // "label":"FG|I[Xe]|I[102Xe]|RING|Oc1ccc(-c2ccc([Xe])cc2)cc1|OC1CCC(C2CCC([102Xe])CC2)CC1"}

        when:
        NeighbourhoodGraph.GroupMember member = createMember(123523916, "Oc1ccc(-c2ccc(I)cc2)cc1")

        member.addEdges([new MoleculeEdge(123523916, 123523916, 123525973,
                "FG|I[Xe]|I[102Xe]|RING|Oc1ccc(-c2ccc([Xe])cc2)cc1|OC1CCC(C2CCC([102Xe])CC2)CC1")] as MoleculeEdge[])

        def key = member.generateGroupingKey()
        //println key

        then:
        key == 'Oc1ccc(-c2ccc([Xe])cc2)cc1'
    }

    // "key":"Oc1ccc(-c2ccc([Xe])cc2)cc1","classification":"ADDITION","prototype":"Oc1ccc(-c2ccc(I)cc2)cc1","members":[{"id":123523916,"smiles":"Oc1ccc(-c2ccc(I)cc2)cc1","edgeIds":[[-636668291]]}

    void "two edges between nodes one hop"() {

        // this tests the case where there are 2 one hop paths between a pair on nodes
        // the bug was that this was ending up with a key like Oc1ccc(-c2ccc([Xe])cc2)cc1$$Oc1ccc(-c2ccc([Xe])cc2)cc1
        // (the bits before and after the $$ are the same.
        // The fix is generate a key like Oc1ccc(-c2ccc([Xe])cc2)cc1 (single value)

        // The buggy group member, all in its own group even though there are other substitutions of the same type
        //{"key":"Oc1ccc(-c2ccc([Xe])cc2)cc1$$Oc1ccc(-c2ccc([Xe])cc2)cc1","classification":"ADDITION",
        // "prototype":"Oc1ccc(-c2ccc(O)cc2)cc1","refmolAtomsMissing":0,
        // "members":[{"id":123523948,"smiles":"Oc1ccc(-c2ccc(O)cc2)cc1","edgeIds":[[-636668989],[-636668988]]}]}

        // The first edge
        //{"id":636668989,"parentId":123523948,"childId":123525973,
        // "label":"FG|O[Xe]|O[102Xe]|RING|Oc1ccc(-c2ccc([Xe])cc2)cc1|OC1CCC(C2CCC([102Xe])CC2)CC1"}

        // The second edge
        //{"id":636668988,"parentId":123523948,"childId":123525973,
        // "label":"FG|O[Xe]|O[100Xe]|RING|Oc1ccc(-c2ccc([Xe])cc2)cc1|OC1CCC(C2CCC([100Xe])CC2)CC1"}

        when:
        NeighbourhoodGraph.GroupMember member = createMember(123523948, "Oc1ccc(-c2ccc(O)cc2)cc1")

        member.addEdges([new MoleculeEdge(636668989, 123523948, 123525973,
                "FG|O[Xe]|O[102Xe]|RING|Oc1ccc(-c2ccc([Xe])cc2)cc1|OC1CCC(C2CCC([102Xe])CC2)CC1")] as MoleculeEdge[])
        member.addEdges([new MoleculeEdge(636668988, 123523948, 123525973,
                "FG|O[Xe]|O[100Xe]|RING|Oc1ccc(-c2ccc([Xe])cc2)cc1|OC1CCC(C2CCC([100Xe])CC2)CC1")] as MoleculeEdge[])

        def key = member.generateGroupingKey()
        //println key

        then:
        key == 'Oc1ccc(-c2ccc([Xe])cc2)cc1'
    }


    void "two routes between nodes two hops 65424083"() {

        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2)
        // WHERE m.smiles='Oc1ccc(-c2ccccc2)cc1' AND e.smiles='CCc1cccc(-c2ccc(O)c(C)c2)c1' RETURN p LIMIT 1000

        when:
        NeighbourhoodGraph.GroupMember member = createMember(65424083, "CCc1cccc(-c2ccc(O)c(C)c2)c1")

        member.addEdges([
                new MoleculeEdge(303613227, 65424086, 123525973,
                        "FG|CC[Xe]|CC[100Xe]|RING|Oc1ccc(-c2cccc([Xe])c2)cc1|OC1CCC(C2CCCC([100Xe])C2)CC1"),
                new MoleculeEdge(303613207, 65424083, 65424086,
                        "FG|C[Xe]|C[103Xe]|RING|CCc1cccc(-c2ccc(O)c([Xe])c2)c1|CCC1CCCC(C2CCC(O)C([103Xe])C2)C1")
        ] as MoleculeEdge[])
        member.addEdges([
                new MoleculeEdge(445521018, 90077975, 123525973,
                        "FG|C[Xe]|C[100Xe]|RING|Oc1ccc(-c2ccccc2)cc1[Xe]|OC1CCC(C2CCCCC2)CC1[100Xe]"),
                new MoleculeEdge(303613210, 65424083, 90077975,
                        "FG|CC[Xe]|CC[100Xe]|RING|Cc1cc(-c2cccc([Xe])c2)ccc1O|CC1CC(C2CCCC([100Xe])C2)CCC1O")
        ] as MoleculeEdge[])
        def key = member.generateGroupingKey()
        println key

        then:
        key == 'Oc1ccc(-c2cccc([Xe])c2)cc1$$Oc1ccc(-c2ccccc2)cc1[Xe]'
    }

    void "three routes between nodes two hops 35995193"() {

        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2)
        // WHERE m.smiles='Oc1ccc(-c2ccccc2)cc1' AND e.smiles='C=CCc1cccc(-c2ccccc2)c1' RETURN p LIMIT 1000

        when:
        NeighbourhoodGraph.GroupMember member = createMember(35995193, "C=CCc1cccc(-c2ccccc2)c1")

        member.addEdges([
                new MoleculeEdge(151547523, 35995159, 123525973,
                        "FG|C=CC[Xe]|CCC[100Xe]|RING|Oc1ccc(-c2cccc([Xe])c2)cc1|OC1CCC(C2CCCC([100Xe])C2)CC1"),
                new MoleculeEdge(151547520, 35995159, 35995193,
                        "FG|O[Xe]|O[102Xe]|RING|C=CCc1cccc(-c2ccc([Xe])cc2)c1|CCCC1CCCC(C2CCC([102Xe])CC2)C1")
        ] as MoleculeEdge[])
        member.addEdges([
                new MoleculeEdge(636675733, 123525973, 123867746,
                        "FG|O[Xe]|O[100Xe]|RING|[Xe]c1ccc(-c2ccccc2)cc1|[100Xe]C1CCC(C2CCCCC2)CC1"),
                new MoleculeEdge(151547741, 35995193, 123867746,
                        "FG|C=CC[Xe]|CCC[100Xe]|RING|[Xe]c1cccc(-c2ccccc2)c1|[100Xe]C1CCCC(C2CCCCC2)C1")
        ] as MoleculeEdge[])
        member.addEdges([
                new MoleculeEdge(151454568, 36120704, 123525973,
                        "FG|C=CC[Xe]|CCC[100Xe]|RING|Oc1ccc(-c2ccccc2)cc1[Xe]|OC1CCC(C2CCCCC2)CC1[100Xe]"),
                new MoleculeEdge(151454565, 36120704, 35995193,
                        "FG|O[Xe]|O[102Xe]|RING|C=CCc1cc(-c2ccccc2)ccc1[Xe]|CCCC1CC(C2CCCCC2)CCC1[102Xe]")
        ] as MoleculeEdge[])
        def key = member.generateGroupingKey()
        println key

        then:
        key == 'Oc1ccc(-c2cccc([Xe])c2)cc1$$Oc1ccc(-c2ccccc2)cc1[Xe]$$[Xe]c1ccc(-c2ccccc2)cc1'
    }

    static NeighbourhoodGraph.GroupMember createMember(long id, String smiles) {
        MoleculeNode molNode = new MoleculeNode(id, smiles, MoleculeNode.MoleculeType.NET_MOL, [], [:])
        new NeighbourhoodGraph.GroupMember(molNode)
    }

}
