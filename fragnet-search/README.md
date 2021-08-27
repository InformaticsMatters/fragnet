# Fragnet Search

This module provides REST web services for searching the fragment network graph database.

The aim is to provide strongly opinionated searches against specific types of dataset and provide a common object model
that is independent of the Neo4j graph model that can be used on the client side and that somewhat insulates client code
for the exact details of the Neo4j data model.

## API versions

The APIs are versioned. Currently `v2` is in use, but the older `v1` is still supported. See [README_v1.md]() for details.
The API version is included in the API URL endpoints and is implemented using classes in version specific packages.

### v1 -> v2 changes
Significant changes:

* Handle updated labels in the database (MOL -> Mol and F2EDGE -> FRAG)
* Handle supplier information as part of neighbourhood search
* Endpoint to return suppliers that are present
* Don't include query structure in results

## Search types

The current search types that are supported are:

1. Supplier search - find the different suppliers that are in the database. Other searches can be
restricted to specific suppliers.
2. Molecule search - is the specified molecule part of the fragment network.
3. Molecule neighbourhood search - find the local graph network surrounding a specific molecule.
4. Availability search - find the forms of a molecule from the fragment network that are available from suppliers 
5. Expansion search - expand out a single molecule returning isomeric molecules that can be purchased
6. Expansion multi search - expand out a set of molecules returning isomeric molecules that can be purchased


### Supplier search

This is available from the `fragnet-search/rest/v2/search/suppliers` endpoint. There are no parameters for this request.

#### Supplier results

This is a simple JSON array of supplier objects. e.g. `[{"name":"MolPort","label":"V_MP"},{"name":"eMolecules","label":"V_EMOLS"}]`.
Each supplier object has a name and label property.
If restricting searches to specific suppliers then specify the suppliers query parameter and give it the value
of a comma separated list of supplier names. These must be specified __exactly__ as found in the result of this query.

### Molecule search

This allows you to find out if the specified molecule is part of the fragment network.
This is available from the `fragnet-search/rest/v2/search/molecule/{smiles}` endpoint.
If the molecule is not present you get a 404 response. If it is present you get back a 200 response containing JSON
with basic information about the molecule, e.g.
```
{
  "id": 3101538,
  "smiles": "OC(Cn1ccnn1)C1CC1",
  "molType": "NET_FRAG",
  "labels": [
    "F2"
  ],
  "props": {
    "inchik": "RRDAVGHEBCZBLM-UHFFFAOYNA-N",
    "osmiles": "OC(CC1CCCC1)C1CC1",
    "chac": 8,
    "hac": 11,smiles
    "inchis": "InChI=1/C7H11N3O/c11-7(6-1-2-6)5-10-4-3-8-9-10/h3-4,6-7,11H,1-2,5H2MA"
  }
}
```
The molecule is typically specified as SMILES as illustrated above, but it can be specified as in Molfile format in which 
case the molfile must be POSTed to the `fragnet-search/rest/v2/search/molecule` endpoint AND the mime-type must be set to
`chemical/x-mdl-molfile` using the `Content-type` header.

### Molecule neighbourhood search

This is available from the `fragnet-search/rest/v2/search/neighbourhood/{smiles}` endpoint.

Parameters:

| Name       | Type  | Required | Description |
|------------|-------|----------|-------------|
| smiles     | URL   | Yes      | The smiles string for the molecule to look for. See below for requirements about standardisation and canonicalisation. |
| hac        | Query | No       | The difference in heavy atom count compared to the query that is allowed it the result molecules. |
| rac        | Query | No       | The difference in ring atom count compared to the query that is allowed it the result molecules. |
| hops       | Query | No       | The number of graph edges to traverse from the query molecule. Must be 1 or 2. Default is 1. |
| calcs      | Query | No       | Comma separated list of the calculations to be performed on the resulting molecules. See below for details. |
| suppliers  | Query | No       | Comma separated list of suppliers to restrict results to. |
| pathLimit  | Query | No       | The maximum number of paths to return from the graph query. Default is 1000 and this is usually more than enough. Values greater than 5000 are not permitted. | 
| groupLimit | Query | No       | The maximum number of members in each group. | 

Note: pathLimit was previously named limit.

An example query run with [curl], where the Fragnet server address (and port) is
set in the `FRAGNET_SERVER` environment variable (e.g. `export FRAGNET_SERVER=http://localhost:8080`),
might look like this:
```
curl "${FRAGNET_SERVER}/fragnet-search/rest/v2/search/neighbourhood/COc1ccccc1CN1CCCC1?hac=3&rac=1&hops=2&calcs=LOGP,SIM_RDKIT_TANIMOTO"
``` 

>   Remember that the Fragnet server may not be running on the default HTTP
    port. It can normally be found on port `8080`.

Note that the query molecule is specified as smiles and must by URL encoded. This can, for instance,
be performed [here](https://www.urlencoder.org/).

Results are of type **Fragment Neighbourhood results** described below.  

#### Search details

##### Query molecule standardisation and canonicalisation

Molecules  in the fragment network are standardised and canonicalised and query matches are by exact matches of smiles
strings. This means that the query molecule must be standardised and canonicalised in the same way as was done for the 
generation of the fragment network. If not then 'equivalent' but non 'identical' smiles molecules will not match.
  
Currently query molecules for the neighbourhood and expansion searches are standardised using the same rules as are
applied to the fragment network, so in principle you do not need to worry about converting your molecules to standard form.
This involves basic molecule sketching conventions (e.g. covalently attached metals), conversion to neutral form and
canonicalisation. If however you unexpectedly find no hits for a molecules you expect to be in the database then it is
possible that this is a standardisation issue so try specifying your query in the 'standard' form. 

Details of the standardisation can be found
[here](https://github.com/InformaticsMatters/fragalysis/blob/master/frag/utils/rdkit_utils.py#L245-L268).
You can use that Python method to perform exactly the same standardisation if you wish. Otherwise here are some simple
rules that should suffice in most cases:

1. Sketch the molecule 'correctly' e.g. without covalently bonded metal atoms.
1. Do not include salts.
1. Sketch in neutral form e.g. carboxylic acid not carboxylate.

##### Calculations

We have a small number of molecular calculations built in that are available to include in query results. Heavy atom count
(`hac` property) and ring atom count (`chac` property - NOTE: we plan to rename this to `rac`) are present in the fragment 
network data and always included. You can optionally include the following additional calculation by adding the `calcs` query parameter.

| Name | Description |
|------|-------------|
| MW   | Molecular weight |
| LOGP | cLogP |
| ROTB | Rotatable bonds |
| HBD  | Hydrogen bond donors |
| HBA  | Hydrogen bond acceptors |
| TPSA | Topological polar surface area |
| SIM_RDKIT_TANIMOTO | Tanimoto similarity to query molecule using standard RDKit fingerprints |
| SIM_MORGAN2_TANIMOTO | Tanimoto similarity to query molecule using Morgan fingerprints of radius 2 |
| SIM_MORGAN3_TANIMOTO | Tanimoto similarity to query molecule using Morgan fingerprints of radius 3 |

All these properties are calculated with RDKit.

The list of available calculations can be fetched using the `calcs` endpoint that is described below.
It expected that additional calculations may be added in the future. 

Specify the properties to caclulate as a comma separated list of values for the `calcs` property for queries that support
this property. e.g. `calcs=LOGP,SIM_RDKIT_TANIMOTO`

#### Fragment Graph results.

This is a JSON serialised form of the 
[org.squonk.fragnet.search.model.v2.FragmentGraph](src/main/java/org/squonk/fragnet/search/model/v2/FragmentGraph.java)
Java class.

This has the following top level properties describing the query:

1. **query** - the Neo4j Cypher query that was executed.
1. **parameters** - the parameters for the query.
1. **resultAvailableAfter** - the time in milliseconds for the Cypher query to return results.
1. **processingTime** - the time in milliseconds taken for processing the Cypher query results and generating these Fragment Graph results. 
1. **calculationTime** - the time in milliseconds for any calculations that were specified.
1. **shortMessage** - brief warning message about the query execution 
1. **longMessage** - more verbose warning message about the query execution 

The Nodes and Edges are present as top level properties.

Nodes are present as the **nodes** property which is an array of JSON serialised
[org.squonk.fragnet.search.model.v2.MoleculeNode](src/main/java/org/squonk/fragnet/search/model/v2/MoleculeNode.java) instances
which provide the following properties: 

* **id** - a unique ID for the node. Only to be used internally. The ID for a particular molecule will change between different databases. 
* **smiles** - the canonical standardised SMILES for the molecule in the fragment network.
* **molType** - the type of molecule: **NET_MOL** for a real molecule that is part of the non-isomeric fragment network, **NET_FRAG**
for a fragment that is part of the non-isomeric fragment network and **ISO_MOL** for an isomeric molecule that is not a direct 
part of the fragment network but links into it through its **NET_MOL** non-isomeric counterpart.
* **labels** - labels that are present in the Neo4j node. Probably not to be used. 
* **props** - a map of properties such as `hac` and any calculations that were requested.

Edges are present as the `edges` property which is an array of JSON serialised
[org.squonk.fragnet.search.model.v2.MoleculeEdge](src/main/java/org/squonk/fragnet/search/model/v2/MoleculeEdge.java) instances
which provide the following properties:

* **id** - a unique ID for the edge. Only to be used internally. The ID for a particular molecule will change between different databases. 
* **parentId** - the ID of the parent node.
* **childId** - the ID of the child node.
* **label** - the label in the Neo4j edge that describes the type of transform (as described in the Astex paper). 

NOTE: the IDs should only be used for reconstructing the dataset. They are not guaranteed to be stable between searches.
For longer term persistence use the SMILES string of the node.

#### Fragment Neighbourhood results.

An example results file can be found [here](neighbourhood-search.json).

This is a JSON serialised form of the 
[org.squonk.fragnet.search.model.v2.NeighbourhoodGraph](src/main/java/org/squonk/fragnet/search/model/v2/NeighbourhoodGraph.java)
Java class which extends from the `org.squonk.fragnet.search.model.v2.FragmentGraph` class described above.
Correspondingly it contains all the properties of the Fragment Graph plus these additional ones that handle the
grouping of the result molecules based on the type of change. This is as described in the Astex paper, but extended
slightly to better handle the fact that traversing 2 edges can allow multiple paths between the query molecule and a result
molecule e.g. a result that has 2 additions to the query can be traversed through a pair of edges with the substitutions 
in either order.

Additional properties are:

* **refmol** - the query smiles that formed the basis of the search
* **groups** - an array of groupings of the nodes according to the type of change.

Each **group** is a JSON serialised `org.squonk.fragnet.search.model.v2.NeighbourhoodGraph.Group`
instance (this is an inner class of `NeighbourhoodGraph`) and has properties for:

* **key** - a generated key for the group based on the traversal path
* **classification** - a classification of the type of change e.g. addition, substitution (NOTE: currently this classification 
is not yet full optimised)
* **prototype** - a prototype structure for the group (currently the structure with the smallest number of atoms)
* **refmolAtomsMissing** - number of atoms from the query that are NOT present in the group members
* **members** - an array of members of the group.

The members have these properties:

* **id** - the ID of the node that can be found in the **nodes** section.
* **smiles** - the SMILES of the node. NOTE: this is currently provided for convenience and might be removed as it can be obtained 
from the node.
* **edgeIds** - an array of arrays of **edge** ID that traverse from the query molecule to this node. There is one outer array for
each path from the query. If there are multiple paths then there are multiple elements. The value of each path element is itself an
array of **edge** IDs that defines the path. Positive values describe the ID of a parent-to-child **edge** and negative values describe 
the negative ID of a child-to-parent **edge**.

### Calculations search

The list of calculations that can be requests is available from the `fragnet-search/rest/v2/search/calcs` endpoint.
The results look like this:
```
[
{"id":"MW","name":"molw","description":"Mol weight","type":"number"},
{"id":"LOGP","name":"logp","description":"cLogP","type":"number"},
{"id":"ROTB","name":"rotb","description":"RotBonds","type":"integer"},
{"id":"HBD","name":"hbd","description":"HBond donors","type":"integer"},
{"id":"HBA","name":"hba","description":"HBond acceptors","type":"integer"},
{"id":"TPSA","name":"tpsa","description":"TPSA","type":"number"},
{"id":"SIM_RDKIT_TANIMOTO","name":"sim_rdkit_tanimoto","description":"Tanimoto sim (RDKit)","type":"number"},
{"id":"SIM_MORGAN2_TANIMOTO","name":"sim_morgan2_tanimoto","description":"Tanimoto sim (Morgan2)","type":"number"},
{"id":"SIM_MORGAN3_TANIMOTO","name":"sim_morgan3_tanimoto","description":"Tanimoto sim (Morgan3)","type":"number"}
]
```
Additional calculations may be added in the future.

The properties are as follows:

* **id**: The ID of the calculation. 
* **name** The name of the property in the query results
* **description** A human friendly display name
* **type** The JSON Schema data type of the result

When requesting calculations to be performed as part of a query you need to specify the id properties as a comma
separated list e.g. `calcs=LOGP,SIM_RDKIT_TANIMOTO`.

### Availability search

This is available from the `fragnet-search/rest/v2/search/availability/{smiles}` endpoint.

This endpoint allows to fetch details of compound availability. The molecules in the fragment network are standardized
and achiral which means that additional info such as salt forms and stereoisomers is not present. You need to use the
availability search to find the exact forms that are available from the various suppliers.

The only parameter for this endpoint is the SMILES string that is appended to the path. This is the SMILES for a molecule
in the fragment network.

The results are in JSON format and look like this:
```
{"smiles":"CC(C)OCc1ccc(CNC(=O)CCCCNC(N)=O)cc1","items":[
{"supplier":"MolPort","code":"MOLPORT:020-059-849","smiles":"CC(C)OCC1=CC=C(CNC(=O)CCCCNC(N)=O)C=C1"}
]}
```
The `items` property will contain one record for each form that is available. The `items.smiles` property describes
the actual form that is available (e.g. with stereochemistry and salts if present).
The suppliers code for that form is present in the `items.code` property. The part after the colon is the actual
identifier that will be recognised by the supplier.
The `smiles` that is the first property you see (e.g. as a top level property) is the SMILES that was queried.

#### Expansion search

This is a simple API that fetches molecules related to a query molecule. Many aspects are the same as the 
Neighbourhood search, except for:

* standardised chiral molecules are returned whereas neighbourhood search returns achiral forms
* no calculations can be performed
* grouping is not performed

This API is expected to be used for expanding a query molecule to get related molecules that can be further analysed.

This is available from the `fragnet-search/rest/v2/search/expand/{smiles}` endpoint.

Parameters:

| Name       | Type  | Required | Description |
|------------|-------|----------|-------------|
| smiles     | URL   | Yes      | The smiles string for the molecule to look for. See below for requirements about standardisation and canonicalisation. |
| hacMin     | Query | No       | The lower difference in heavy atom count compared to the query that is allowed it the result molecules. |
| hacMax     | Query | No       | The upper difference in heavy atom count compared to the query that is allowed it the result molecules. |
| racMin     | Query | No       | The lower difference in ring atom count compared to the query that is allowed it the result molecules. |
| racMax     | Query | No       | The upper difference in ring atom count compared to the query that is allowed it the result molecules. |
| hops       | Query | No       | The number of graph edges to traverse from the query molecule. Must be 1, 2 or 3. Default is 1. |
| suppliers  | Query | No       | Comma separated list of suppliers to restrict results to. |
| pathLimit  | Query | No       | The maximum number of paths to return from the graph query. Default is 1000 and this is usually more than enough. Values greater than 5000 are not permitted. | 

These parameters have the same meaning as in neighbourhood search, except that the hac and rac queries can specify a min
and a max value, whereas in neighbourhood search the same value is used for the lower and upper bound.
For example, if your query molecule has 10 heavy atoms, you can specify hacMin=0 and hacMax=5 to restrict the search to
molecules with 10-15 heavy atoms. Negative values are allowed, so hacMin=-2 and hacMax=5 will result in molecules with 
12-15 heavy atoms.

An example query run with [curl] might look like this:
```
 curl "$FRAGNET_SERVER/fragnet-search/rest/v2/search/expand/OC(Cn1ccnn1)C1CC1?hacMin=5&hacMax=10&racMin=3&racMax=3&hops=2"
```

#### Expansion search results

This is a simple JSON datastructure as follows:

```json
{
  "query": "MATCH p=(m:F2)-[:FRAG*1..2]-(e:Mol)<-[:NonIso*0..1]-(c:Mol)\nWHERE m.smiles=$smiles AND e.smiles <> $smiles AND m.hac - e.hac <= $hacMin AND e.hac - m.hac <= $hacMax AND m.chac - e.chac <= $racMin AND e.chac - m.chac <= $racMax\nRETURN p LIMIT $limit",
  "parameters": {
    "racMin": 3,
    "smiles": "OC(Cn1ccnn1)C1CC1",
    "hacMin": 5,
    "hacMax": 10,
    "limit": 5000,
    "racMax": 3
  },
  "refmol": "OC(Cn1ccnn1)C1CC1",
  "resultAvailableAfter": 3,
  "processingTime": 35,
  "pathCount": 17,
  "size": 17,
  "members": [
    {
      "smiles": "OC(CN1CCCC1)C1CC1",
      "props": {
        "chac": 8,
        "hac": 11
      },
      "cmpd_ids": [
        "REAL:Z2311621453"
      ]
    },
    {
      "smiles": "OC(CN1CCCCC1)C1CC1",
      "props": {
        "chac": 9,
        "hac": 12
      },
      "cmpd_ids": [
        "REAL:Z2311631443"
      ]
    },
    {
      "smiles": "OC(CN1CCSCC1)C1CC1",
      "props": {
        "chac": 9,
        "hac": 12
      },
      "cmpd_ids": [
        "REAL:Z2311658997"
      ]
    },
    {
      "smiles": "O=C(CCc1c[nH]nn1)NC1CC1",
      "props": {
        "chac": 8,
        "hac": 13
      },
      "cmpd_ids": [
        "REAL:Z2763035825"
      ]
    },
    {
      "smiles": "O=C(CCn1ccnn1)NC1CC1",
      "props": {
        "chac": 8,
        "hac": 13
      },
      "cmpd_ids": [
        "REAL:Z295950954"
      ]
    },
 
    ...
  ]
}

```

The main part is the members property that holds an array of the related molecules.

#### Expansion multi search

This is a simplified version of expansion search allowing multiple queries to be expanded in one go.

The easiest way to specify input is as SMILES that is POSTed. An example is shown below.
```
CC(C)(C(=O)N1CCCC1)c1ccccc1	1
CC(C)(C(=O)N1CCCC1)c1cccc(F)c1	2
OCC(O)CN1CCCC1	3
OCCNc1ccc(F)cn1	4
OCCNc1ccc(Cl)cn1	5
```
The syntax is plain text with one molecule per line. Following the molecule, separated by space or tab is an ID of the 
molecules. It is recommended to include an ID, but if none is supplied the the numeric index (starting from 1) is used 
for the ID.

An example execution using [curl] is:
```
 curl --data-binary "@queries.smi" -H "Content-Type: chemical/x-daylight-smiles" "$FRAGNET_SERVER/fragnet-search/rest/v2/search/expand-multi?hacMin=5&hacMax=5&racMin=2&racMax=2&hops=2"
```

Specifying the `Content-Type` is required. Data can also be sent in SDF format, in which case the `Content-Type`
must be `chemical/x-mdl-sdfile`. When using SDF you can also specify the `id_prop` header to choose the SDF field
that will be used for the molecule IDs. Either specify a data field or use `_Name` if you want to use the molecule
name (the first line in the record.)

**NOTE:** These queries can fetch large amounts of results. It is best to run them initially with strict query criteria before
loosening them (in particular for the `hops` parameter only use a value of 3 if you find you do not get many results with
a value of 2.)

#### Expansion multi search results

The searches are executed using the same mechanism as the standard expansion search with results being aggregated. 
A result molecule can be found by multiple queries so the results include the ID of the query molecules that found the
hit as well as various information about the query. Example output (part of data is removed) is shown below:

```json
{
  "executionDate": "2020/02/27 11:37:11",
  "executionTimeMillis": 149,
  "resultCount": 2770,
  "parameters": {
    "hops": 2,
    "hac": 5,
    "rac": 2
  },
  "queries": {
    "molecules": [
      {
        "id": "1",
        "originalMol": "CC(C)(C(=O)N1CCCC1)C1=CC=CC=C1",
        "smiles": "CC(C)(C(=O)N1CCCC1)c1ccccc1"
      },
      {
        "id": "2",
        "originalMol": "CC(C)(C(=O)N1CCCC1)C1=CC=CC(F)=C1",
        "smiles": "CC(C)(C(=O)N1CCCC1)c1cccc(F)c1"
      },
      {
        "id": "3",
        "originalMol": "OCC(O)CN1CCCC1",
        "smiles": "OCC(O)CN1CCCC1"
      },
      {
        "id": "4",
        "originalMol": "OCCNC1=CC=C(F)C=N1",
        "smiles": "OCCNc1ccc(F)cn1"
      },
      {
        "id": "5",
        "originalMol": "OCCNC1=CC=C(Cl)C=N1",
        "smiles": "OCCNc1ccc(Cl)cn1"
      }
    ],
    "mimeType": "chemical/x-daylight-smiles"
  },
  "hitCounts": {
    "1": 650,
    "2": 671,
    "3": 1183,
    "4": 289,
    "5": 6
  },
  "results": [
    {
      "smiles": "CC(C)(C(=O)N1CCCC1)c1ccc(F)cc1",
      "vendorIds": [
        "REAL:Z1102056262"
      ],
      "sourceMols": [
        "1",
        "2"
      ]
    },
    {
      "smiles": "CC(C)(C(=O)N1CCCC1)c1ccc(F)cc1F",
      "vendorIds": [
        "REAL:Z2833058319"
      ],
      "sourceMols": [
        "1"
      ]
    },
    {
      "smiles": "CC(C)(C(=O)N1CCCC1)c1ccccc1Br",
      "vendorIds": [
        "REAL:Z1985969847"
      ],
      "sourceMols": [
        "1",
        "2"
      ]
    },
    {
      "smiles": "CC(C)(C(=O)N1CCC(CN)C1)c1ccccc1",
      "vendorIds": [
        "REAL:Z2440100621"
      ],
      "sourceMols": [
        "1",
        "2"
      ]
    },
    {
      "smiles": "CC1CC(CN)CN1C(=O)C(C)(C)c1ccccc1",
      "vendorIds": [
        "REAL:Z2393821782"
      ],
      "sourceMols": [
        "1"
      ]
    },
    {
      "smiles": "CC1(CN)CCN(C(=O)C(C)(C)c2ccccc2)C1",
      "vendorIds": [
        "REAL:Z2103219073"
      ],
      "sourceMols": [
        "1"
      ]
    },
    {
      "smiles": "CC(C)(C(=O)N1CCCC1)c1ccc(C#N)cc1",
      "vendorIds": [
        "REAL:Z2697275895"
      ],
      "sourceMols": [
        "1",
        "2"
      ]
    },
    {
      "smiles": "CC(C)(C(=O)N1CCC(N)C1)c1ccc(C#N)cc1",
      "vendorIds": [
        "REAL:Z2825649014"
      ],
      "sourceMols": [
        "1"
      ]
    },
    {
      "smiles": "Cc1ccccc1C(C)(C)C(=O)N1CCCC1",
      "vendorIds": [
        "REAL:Z941548126"
      ],
      "sourceMols": [
        "1",
        "2"
      ]
    },
    {
      "smiles": "CC(CC1CCCC1)C(=O)N1CCCC1",
      "vendorIds": [
        "REAL:Z1204326216"
      ],
      "sourceMols": [
        "1"
      ]
    },

    ...

  ]
}
```
  
### Fragments search

This search returns all the child fragments (recursive) of a molecule. The input is the same as the Molecule search,
comprising a single molecule in SMILES or Molfile format.

If that molecule is not found your get a 404 response. If it is found you get a 200 response containing a list of child 
fragments in JSON format.

Typical execution looks like this:
```
curl "$FRAGNET_SERVER/fragnet-search/rest/v2/search/fragments/OC(Cn1ccnn1)C1CC1"
```

The response would look like this:
```
[
  "[Xe]C1CC1",
  "C1CC1",
  "c1c[nH]nn1",
  "CC(O)[Xe]",
  "[Xe]C1CC1.[Xe]n1ccnn1",
  "OC(C[Xe])C1CC1",
  "[Xe]n1ccnn1",
  "OC([Xe])Cn1ccnn1",
  "OC([Xe])C[Xe]",
  "OCC[Xe]"
]
```

### Synthon expansion search

This search finds molecules in the neighbourhood of the query molecule that incorporate a specific "synthon". Those 
synthons are child fragments that can be found using the fragments search endpoint. This can be useful to generate
"fragment merges", molecules that incorporate parts of 2 different molecules.

Parameters:

| Name       | Type  | Required | Description |
|------------|-------|----------|-------------|
| smiles     | URL   | Yes      | The smiles string for the query molecule. |
| synthon    | Query | Yes      | The smiles string for the synthon molecule. |
| hacMin     | Query | No       | The minimum heavy atom count of the resulting molecules. |
| hacMax     | Query | No       | The maximum heavy atom count of the resulting molecules. |
| racMin     | Query | No       | The minimum ring atom count of the resulting molecules. |
| racMax     | Query | No       | The maximum ring atom count of the resulting molecules. |
| hops       | Query | Yes      | The number of graph edges to traverse from the query molecule. Typically use 3 or 4. |

Note that these searches can be quite slow. Start with a small number of hops, and increase if you have no results.

Typcial execution:
```
curl "$FRAGNET_SERVER/fragnet-search/rest/v2/search/synthon-expand/OC(Cn1ccnn1)C1CC1?synthon=%5BXe%5Dn1ccnn1&hops=3&hacMin=14&hacMax=18"
```

Typcial results:
```
[
  "NC(CCCNCCn1ccnn1)=NO",
  "NN=C(NCCn1ccnn1)NC1CC1",
  "CCC(NCCn1ccnn1)C(C)(C)C",
  "CC(CC(N)=O)NCCCn1ccnn1",
  "O=C(O)C(=O)C(=O)NCCn1ccnn1",
  "NN=C(NCCCn1ccnn1)NC1CC1",
  "CCOC(C)(C)CNCCn1ccnn1",
  "CCNC(C)CNC(=O)Cn1ccnn1",
  "CCC(CN)NC(=O)CCn1ccnn1",
  "CN(C(=O)Cn1ccnn1)C(C)(C)C#N",
  "O=C(O)C=CC(=O)NCCn1ccnn1",
  "COCCNC(=NN)NCCn1ccnn1",
  "CC(NC(=O)CCn1ccnn1)C(N)=O",
  "CC(C)N(CC#N)C(=O)Cn1ccnn1",
  "CCC(NCCCn1ccnn1)C(N)=NO",
  "CN(CCC(=O)O)C(=O)CCn1ccnn1",
  "NCC=CCNC(=O)CCn1ccnn1",
  "COC(=O)C=CNCCCn1ccnn1",
  "NC(CCCCNCCn1ccnn1)=NO"
]
```

## Authentication

### Linux or Mac

In the production environment access to the search service will require authentication.
To achieve this you will need to:
 
1. Get a login to the Squonk systems. Contact info@informaticsmatters to get an account set up.
2. Request access to fragnet-search. You need to be given the `fragnet-search` role to be able to use this.
3. Generate an authentication token.
4. Run the query passing in the authentication token.

For instance, assuming have [curl] and [jq] installed, to perform step 3 you will need to do something like this:

```
token=$(curl -d "grant_type=password" -d "client_id=fragnet-search" -d "username=<username>" -d "password=<password>"\
  https://squonk.it/auth/realms/squonk/protocol/openid-connect/token 2> /dev/null \
  | jq -r '.access_token')
```
Replace `<username>` and `<password>` with the appropriate values.
You can use `echo $token` to make sure you have obtained a token.

If using a different client then this is a HTTP POST operation to
`https://squonk.it/auth/realms/squonk/protocol/openid-connect/token` using 
multipart form data (`Content-Type` header of `application/x-www-form-urlencoded`)
passing in form parameters equivalent to the `-d` parameters set by curl. 

To perform a search of the fragment network you will now need to do something like this:
```
curl -LH "Authorization: bearer $token" "${FRAGNET_SERVER}/fragnet-search/rest/v2/search/neighbourhood/c1ccc%28Nc2nc3ccccc3o2%29cc1?hac=3&rac=1&hops=2&calcs=LOGP,SIM_RDKIT_TANIMOTO"
```
Notice how the token is sent with the request.

### Windows

If you are unfortunate enough to have to use Windows then try something like this (you need to have `curl` installed, but 
this is now included in Windows 10):

1. Get the token:
```
curl -d "grant_type=password" -d "client_id=fragnet-search" -d "username=username" -d "password=password" https://squonk.it/auth/realms/squonk/protocol/openid-connect/token
```
(change username and password accordingly).

2. Then from the JSON that you get back copy out the value of the access_token property and set it to a variable like this:
```
set token=<paste-token-here>
```

3. Check it like this:
```
echo %token%
```

4. Use it like this:

```
curl -LH "Authorization: bearer %token%" "${FRAGNET_SERVER}/fragnet-search/rest/v2/search/neighbourhood/c1ccc%28Nc2nc3ccccc3o2%29cc1?hac=3&rac=1&hops=2&calcs=LOGP,SIM_RDKIT_TANIMOTO"
```

## Expected use

The Fragment Neighbourhood results are typically expected to be used to generate a UI that depicts the neighbours of the 
query structure grouped according to the transformation type. See for instance figure 2 in the Astex paper.

For instance this could be a web client that allows to specify a query structure, ran the search and then processed the 
results to generate a depiction like that of figure 2 in the Astex paper.

Processing would proceed as follows:

1. Process the **nodes** and create a Map of node instances keyed by ID.
1. Optionally process the **edges** in a similar way.
1. Process the **groups**, associating the group members with the nodes by using the node ID.

---

[curl]: https://github.com/curl/curl
[jq]: https://stedolan.github.io/jq/
