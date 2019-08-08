# Fragnet Search

This module provides REST web services for searching the fragment network graph database.

The aim is to provide strongly opinionated searches against specific types of dataset and provide a common object model
that is independent of the Neo4j graph model that can be used on the client side and that somewhat insulates client code
for the exact details of the Neo4j data model.

Currently this work is at an early stage and much more is planned.

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
1. Molecule neighbourhood search - find the local graph network surrounding a specific molecule.

Planned searches are:

1. Activity island searches - find clusters of molecules with activity.
2. Compound availability search - find out who you can purchase molecules from.

### Supplier search

This is available from the `fragnet-search/rest/v2/search/suppliers` endpoint. There are no parameters for this request.

The result is a JSON array of supplier objects as described in the [Supplier results](#supplier-results) section.

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
| limit      | Query | No       | The maximum number of paths to return from the graph query. Default is 1000 and this is usually more than enough. Values greater than 5000 are not permitted. | 

An example query run with [curl], where the Fragnet server address (and port) is
set in the `FRAGNET_SERVER` environment variable (e.g. `export FRAGNET_SERVER=http://localhost:8080`),
might look like this:
```
curl "${FRAGNET_SERVER}/fragnet-search/rest/v2/search/neighbourhood/c1ccc%28Nc2nc3ccccc3o2%29cc1?hac=3&rac=1&hops=2&calcs=LOGP,SIM_RDKIT_TANIMOTO"
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
  
Currently query molecules are canonicalised but not standardised so you need to provide your query molecule with some care.
Details of the standardisation can be found
[here](https://github.com/InformaticsMatters/fragalysis/blob/master/frag/utils/rdkit_utils.py#L245-L268).
You can use that Python method to perform exactly the same standardisation if you wish. Otherwise here are some simple
rules that should suffice in most cases:

1. Sketch the molecule 'correctly' e.g. without covalently bonded metal atoms.
1. Do not include salts.
1. Sketch in neutral form e.g. carboxylic acid not carboxylate.

Once the new RDKit standardisation code (first introduced in the 2018_09 release) is available from Java we will also be
able to perform standardisation.

##### Calculations

We have a small number of molecular calculations built in that are available to include in query results. Heavy atom count
(`hac` property) and ring atom count (`chac` property - NOTE: we plan to rename this to `rac`) are present in the fragment 
network data and always included. You can optionally include the following additional calculation by adding the `calcs` query parameter.

| Name | Description |
|------|-------------|
| LOGP | cLogP       |
| TPSA | Topological polar surface area |
| SIM_RDKIT_TANIMOTO | Tanimoto similarity to query molecule using standard RDKit fingerprints |
| SIM_MORGAN2_TANIMOTO | Tanimoto similarity to query molecule using Morgan fingerprints of radius 2 |
| SIM_MORGAN3_TANIMOTO | Tanimoto similarity to query molecule using Morgan fingerprints of radius 3 |

All these properties are calculated with RDKit.

Specify the properties to caclulate as a comma separated list of values for the `calcs` property for queries that support
this property. e.g. `calcs=LOGP,SIM_RDKIT_TANIMOTO`

### Authentication

#### Linux or Mac

In the Squonk production environment access to the search service will require authentication.
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

#### Windows

If you are unfortunate enough to have to use Windows then try something like this (you need to have `curl` installed):

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

## Result Details

Results are returned in JSON format.

### Supplier results

This is a simple JSON array of supplier objects. e.g. `[{"name":"MolPort","label":"V_MP"},{"name":"eMolecules","label":"V_EMOLS"}]`.
Each supplier object has a name and label property.
If restricting searches to specific suppliers then specify the suppliers query parameter and give it the value
of a comma separated list of supplier names. These must be specified __exactly__ as found in the result of this query. 

### Fragment Graph results.

This is a JSON serialised form of the 
[org.squonk.fragnet.search.model.v2.FragmentGraph](src/main/java/org/squonk/fragnet/search/model/v2/FragmentGraph.java)
Java class.

This has the following top level properties describing the query:

1. **query** - the Neo4j Cypher query that was executed.
1. **parameters** - the parameters for the query.
1. **resultAvailableAfter** - the time in milliseconds for the Cypher query to return results.
1. **processingTime** - the time in milliseconds taken for processing the Cypher query results and generating these Fragment Graph results. 
1. **calculationTime** - the time in milliseconds for any calculations that were specified.

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

### Fragment Neighbourhood results.

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
* **members** - an array of members of the group.

The members have these properties:

* **id** - the ID of the node that can be found in the **nodes** section.
* **smiles** - the SMILES of the node. NOTE: this is currently provided for convenience and might be removed as it can be obtained 
from the node.
* **edgeIds** - an array of arrays of **edge** ID that traverse from the query molecule to this node. There is one outer array for
each path from the query. If there are multiple paths then there are multiple elements. The value of each path element is itself an
array of **edge** IDs that defines the path. Positive values describe the ID of a parent-to-child **edge** and negative values describe 
the negative ID of a child-to-parent **edge**.

#### Expected use

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
