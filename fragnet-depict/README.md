# Fragnet Depict

This module provides the ability to depict smiles strings as SVG.
It is based on CDK rendering which provides high quality layout and depiction
of molecules.
The code is largely based on the rendering code used in Squonk:
https://github.com/InformaticsMatters/squonk

See the docs for the [CdkMolDepictServlet](fragnet-depict/src/main/java/org/squonk/fragnet/depict/CdkMolDepictServlet.java) 
class for details of how to use and for a description of the parameters.

Example usage (depending on the server it is deployed to and the context):
http://localhost:8080/fragnet-depict/moldepict?&w=75&h=75&bg=0x33FFFF00&mol=CN1C%3DNC2%3DC1C(%3DO)N(C)C(%3DO)N2C


## History of Changes

31-JUL-2019 Initial commit with basic rendering servlet code.
01-AUG-2019 war file deployed to tomcat in Docker (alongside fragnet-search)
