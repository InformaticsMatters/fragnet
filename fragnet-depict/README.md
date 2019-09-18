# Fragnet Depict

This module provides the ability to depict smiles strings as SVG.
It is based on CDK rendering which provides high quality layout and depiction of molecules.
The code is largely based on the rendering code used in Squonk:
https://github.com/InformaticsMatters/squonk

Example usage (depending on the server it is deployed to and the context):
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
