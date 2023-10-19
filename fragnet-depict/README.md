# Fragnet Depict

This module provides the ability to depict smiles or molfile strings as SVG.
It is based on CDK rendering which provides high quality layout and depiction of molecules.
The code is largely based on the rendering code used in Squonk:
https://github.com/InformaticsMatters/squonk

Example usage with SMILES (depending on the server it is deployed to and the context):
http://localhost:8080/fragnet-depict/moldepict?&w=75&h=75&bg=0x33FFFF00&mol=CN1C%3DNC2%3DC1C(%3DO)N(C)C(%3DO)N2C

## Parameters

The rendering service is accessed using HTTP GET commands with all parameters being specified as
query parameters.

Only the `mol` parameter is required. Sensible defaults are used for other parameters if not specified.

| name           | purpose                                                            | default |
| -------------- | ------------------------------------------------------------------ | ------- |
| mol            | SMILES to render                                                   | none    |
| w              | image width (in mm as integer number)                              | 50      | 
| h              | image height (in mm as integer number)                             | 50      | 
| m              | image margin (in mm as decimal number)                             | 0       |
| expand         | expand molecule to fit                                             | true    |
| bg             | background colour as RGBA hex value                                | white   |
| colorScheme    | atom colouring scheme                                              | cdk2d   |
| explicitHOnly  | Show only explicit hydrogens (e.g. for a query structure)          | false   |
| highlightAtoms | comma separated list of atom numbers to highlight                  | none    |
| highlightColor | colour to use for highlighted atoms and bonds                      | red     |
| outerGlow      | highlight using "outer glow" rather than colouring atoms and bonds | false   |
| mcs            | SMILES to use for MCS determination                                | none    |
| mcsColor       | Color to use for MCS highlighting                                  | none    |
| noStereo       | Display as achiral (no wedge/dash/squiggle bonds etc)              | false   |

### Examples

Most of these examples use caffeine as the molecule. Make sure that the SMILES is URL encoded.

**Specify image size and background colour**:
http://localhost:8080/fragnet-depict/moldepict?&w=75&h=60&bg=0x33FFFF00&mol=CN1C%3DNC2%3DC1C(%3DO)N(C)C(%3DO)N2C

**Display explicit hydrogens only e.g.when representing a query structure**:
http://localhost:8080/fragnet-depict/moldepict?mol=CN1C%3DNC2%3DC1C(%3DO)N(C)C(%3DO)N2C&explicitHOnly=true

**Do not expand to fit**:
http://localhost:8080/fragnet-depict/moldepict?mol=CN1C%3DNC2%3DC1C(%3DO)N(C)C(%3DO)N2C&expand=false

**Display using a different atom colour scheme** (options are jmol, cdk2d, partialAtomicCharge, rasmol, white, black):
http://localhost:8080/fragnet-depict/moldepict?mol=CN1C%3DNC2%3DC1C(%3DO)N(C)C(%3DO)N2C&colorscheme=rasmol

**Highlight atoms** (atom numbers in the order that is in the SMILES and start from zero, bonds between highlighted atoms are also highlighted):
http://localhost:8080/fragnet-depict/moldepict?mol=CN1C%3DNC2%3DC1C(%3DO)N(C)C(%3DO)N2C&highlightAtoms=0,1,2,3

**Highlight atoms using outer glow**:
http://localhost:8080/fragnet-depict/moldepict?mol=CN1C%3DNC2%3DC1C(%3DO)N(C)C(%3DO)N2C&highlightAtoms=0,1,2,3&outerGlow=true&highlightColor=0x33FFFF00

**Highlight MCS**:
http://localhost:8080/fragnet-depict/moldepict?&w=75&h=75&mol=%5BH%5DC1NCC%28C%29C%28N%29%3DC1&mcs=C1NCCC%3DC1&mcsColor=0xFFFF0000

User specified highlighting and MCS highlighting can be combined, with the user specified colour being used when atoms are
in both sets. The `outer glow` option applies to both. e.g. It is NOT possible to highlight MCS by colouring atoms and 
bonds and highlight the user specified atoms with outer glow. But it is possible to use different colours.


## Rendering molfiles

MDL Molfiles can also be rendered. If using a get operation you must specify `format=mol` and make sure the molfile is
encoded (in particular spaces replaced by %20 and newlines by %0A). Alternatively you can use a POST operation with the
molfile being set as the body of the POST operation. In this case the format is assumed to be molfile, so there is no
need to specify `format=mol`.

**Example using GET**
http://localhost:8080/fragnet-depict/moldepict?&w=75&h=60&bg=0x33FFFF00&format=mol&mol=%0A%20%20%20%20%20RDKit%20%20%20%20%20%20%20%20%20%203D%0A%0A%2011%2011%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200999%20V2000%0A%20%20%2010.1040%20%20%20-0.6810%20%20%2022.5530%20C%20%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%0A%20%20%20%209.2070%20%20%20-0.0590%20%20%2021.4840%20C%20%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%0A%20%20%20%209.5320%20%20%20%200.9550%20%20%2020.9690%20O%20%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%0A%20%20%20%207.9590%20%20%20-0.7020%20%20%2021.1090%20N%20%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%0A%20%20%20%207.1050%20%20%20-0.1160%20%20%2020.0880%20C%20%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%0A%20%20%20%207.4770%20%20%20-0.1810%20%20%2018.7520%20C%20%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%0A%20%20%20%206.7240%20%20%20%200.3380%20%20%2017.8000%20N%20%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%0A%20%20%20%205.5930%20%20%20%200.9420%20%20%2018.0910%20C%20%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%0A%20%20%20%205.1410%20%20%20%201.0620%20%20%2019.3980%20C%20%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%0A%20%20%20%205.9120%20%20%20%200.5170%20%20%2020.4250%20C%20%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%0A%20%20%20%205.4700%20%20%20%200.6210%20%20%2021.8830%20C%20%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%20%200%0A%20%202%20%201%20%201%20%200%0A%20%203%20%202%20%202%20%200%0A%20%204%20%202%20%201%20%200%0A%20%205%20%204%20%201%20%200%0A%20%206%20%205%20%202%20%200%0A%20%207%20%206%20%201%20%200%0A%20%208%20%207%20%202%20%200%0A%20%209%20%208%20%201%20%200%0A%2010%20%209%20%202%20%200%0A%2010%20%205%20%201%20%200%0A%2011%2010%20%201%20%200%0AM%20%20END

**Example using POST**
```
curl --data-binary "@test-data/ligand2d.mol" "http://localhost:8080/fragnet-depict/moldepict?&w=75&h=60&bg=0x33FFFF00"
```


## Format conversion

A simple service to generate a MDL Molfile from a SMILES string is available at:
http://localhost:8080/fragnet-depict/smiles2molfile

Use it like this by specifying the input as the `smiles` query parameter e.g.
http://localhost:8080/fragnet-depict/smiles2molfile?smiles=CN1C%3DNC2%3DC1C(%3DO)N(C)C(%3DO)N2C 

## History of Changes

* 31-JUL-2019 Initial commit with basic rendering servlet code.
* 01-AUG-2019 war file deployed to tomcat in Docker (alongside fragnet-search)
* 13-AUG-2019 Atom/bond highlighting and show explicit hydrogens only added (v0.3.2)
* 15-AUG-2019 MCS highlighting for depiction
* 21-AUG-2019 Added noStereo option for depicting molecules as achiral
* 27-AUG-2019 Handle case where MCS or alignment causes exception
* 28-AUG-2019 Switch to legacy (deprecated) CDK code for SMSD as external code is buggy and not maintained
* 18-SEP-2019 Added smiles to molfile conversion endpoint
