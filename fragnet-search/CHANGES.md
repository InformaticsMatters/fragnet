# History of changes

## 0.1
### 0.1.0 - April 2019
* Original v1 REST API

## 0.2
### 0.2.0 04-JUN-2019
* Initial v2 REST API
* Added supplier search capability
* Added `apiVersion` property to result JSON

### 0.2.1 10-JUN-2019
* Fixed problem with supplier search
* Added `nodeCount`, `edgeCount` and `groupCount` properties to result JSON

### 16-AUG-2019
* Added availability endpoint for fetching information about molecule availability from suppliers

### 26-AUG-2019
* Initial MCS implementation for groups giving the `refmolAtomsMissing` property

### 29-AUG-2019
* Better handling of duplicate paths when generating groups
* Added SMARTS for MCS to help with debugging 'duplicate' groups

### 09-SEP-2019
* Major change to grouping algorithm using MCS determinations where needed

### 11-SEP-2019
* Query which finds no results now throws 404 response

### 17-SEP-2019
* Messages provided when path limit is reached

### 24-SEP-2019
* Group members are sorted (by HAC then MW)
* Added `groupLimit` parameter to restrict results to a max number of group members per group 

### 05-OCT-2019
* memberCount property added for the original number of group members before `groupLimit` is applied