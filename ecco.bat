:: This script is untested! Please let me know if any problems occur
:: 
:: ecco: a diff tool for OWL ontologies
:: Copyright 2011-2014, The University of Manchester
::
:: This script builds (if necessary) and runs ecco. This requires 
:: Java 1.6 installed, and the default JRE. Additionally, if building 
:: from sources, ant must be installed.
:: 
:: Last updated: 14-Feb-14
:: 
:: Compile sources and produce the jar and javadocs (if ecco.jar does not exist)
if not exist ecco.jar echo Building ecco from sources... && ant
::
:: Run ecco with the specified arguments
:: Maximum heap space is set to: 8GB, and Java library path to: .\lib
java -Xmx8G -Djava.library.path=".\lib" -DentityExpansionLimit=100000000 -jar ecco.jar %*