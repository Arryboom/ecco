/*******************************************************************************
 * This file is part of ecco.
 * 
 * ecco is distributed under the terms of the GNU Lesser General Public License (LGPL), Version 3.0.
 *  
 * Copyright 2011-2013, The University of Manchester
 *  
 * ecco is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *  
 * ecco is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even 
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser 
 * General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public License along with ecco.
 * If not, see http://www.gnu.org/licenses/.
 ******************************************************************************/
package uk.ac.manchester.cs.diff;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLEntityURIConverter;
import org.semanticweb.owlapi.util.OWLEntityURIConverterStrategy;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.w3c.dom.Document;

import uk.ac.manchester.cs.diff.axiom.AxiomDiff;
import uk.ac.manchester.cs.diff.axiom.CategoricalDiff;
import uk.ac.manchester.cs.diff.axiom.LogicalDiff;
import uk.ac.manchester.cs.diff.axiom.StructuralDiff;
import uk.ac.manchester.cs.diff.output.XMLReport;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class EccoRunner {
	private static final String versionInfo = "2.0b";
	private static final String releaseDate = "7/05/2013";
	private static final String PROGRAM_TITLE = 
			"-------------------------------------------------------------------\n" +
			"	     ecco: a diff tool for OWL ontologies\n" +
			"	         v" + versionInfo + " released on " + releaseDate + "\n" +		
			"-------------------------------------------------------------------\n" +
			"by Rafael Goncalves. Copyright 2011-2013, University of Manchester";
	
	private boolean processImports, ignoreAbox, transform, verbose, normalizeURIs;
	private static String sep = File.separator;
	private OWLOntologyManager man;
	private OWLOntologyLoaderConfiguration config;
	private String outputDir;
	
	
	/**
	 * Constructor
	 * @param processImports	true if imports should be processed, false otherwise
	 * @param ignoreAbox	true if abox axioms should be ignored, false otherwise
	 * @param transform	true if XML files should be transformed to HTML
	 * @param normalizeURIs	true if namespaces of shared entities should be forced to be similar, false otherwise
	 * @param verbose	true if detailed messages should be output, false otherwise
	 */
	public EccoRunner(boolean processImports, boolean ignoreAbox, boolean transform, boolean normalizeURIs, boolean verbose) {
		this.processImports = processImports;
		this.ignoreAbox = ignoreAbox;
		this.transform = transform;
		this.normalizeURIs = normalizeURIs;
		this.verbose = verbose;
		man = OWLManager.createOWLOntologyManager();
		config = new OWLOntologyLoaderConfiguration();
		config.setLoadAnnotationAxioms(false);
	}
	
	
	/**
	 * Compute diff and output resulting files
	 * @param ont1	Ontology 1
	 * @param ont2	Ontology 2
	 * @param structDiff	true if only structural diff should be performed, false otherwise
	 * @param logDiff	true if only logical diff should be performed, false otherwise
	 * @param serializeOutput	true if XML and CSV files should be output
	 * @throws TransformerException
	 * @throws UnsupportedEncodingException
	 */
	public XMLReport computeDiff(OWLOntology ont1, OWLOntology ont2, boolean structDiff, boolean logDiff, boolean serializeOutput, String xsltPath) 
			throws TransformerException, UnsupportedEncodingException {
		if(normalizeURIs) normalizeEntityURIs(ont1, ont2);
	
		System.out.println("\nComputing diff...");
		long start = System.currentTimeMillis();
		
		AxiomDiff diff = null;
		if(logDiff)
			diff = new LogicalDiff(ont1, ont2, verbose);
		else if(structDiff) 
			diff = new StructuralDiff(ont1, ont2, verbose);
		else
			diff = new CategoricalDiff(ont1, ont2, verbose);
		
		XMLReport report = diff.getXMLReport();
		if(serializeOutput) 
			processOutput(report, diff.getCSVChangeReport(), xsltPath);
		
		long end = System.currentTimeMillis();
		System.out.println("finished (total diff time: " + (end-start)/1000.0 + " seconds)");
		return report;
	}
	
	
	/**
	 * Process local output by saving XML change sets, CSV log, and, if applicable, the HTML transformation 
	 * @param report	XML report instance
	 * @param csvFile	CSV log
	 * @throws UnsupportedEncodingException
	 * @throws TransformerException
	 */
	public void processOutput(XMLReport report, String csvFile, String xsltPath) 
			throws UnsupportedEncodingException, TransformerException {
		saveDocumentToFile(report, report.getXMLDocumentReport(), outputDir, "", xsltPath);	// Entity name based document
		saveDocumentToFile(report, report.getXMLDocumentReportUsingLabels(), outputDir, "_labels", xsltPath);	// Label based document
		saveDocumentToFile(report, report.getXMLDocumentReportUsingGenSyms(), outputDir, "_gensyms", xsltPath);	// Gensym based document
		saveStringToFile(outputDir, "EccoLog.csv", csvFile, sep);
	}
	
	
	/**
	 * Serialize the given XML file and, if applicable, transform it to HTML and serialize the file
	 * @param report	XMLReport instance
	 * @param doc	XML document
	 * @param outputDir	Output directory
	 * @param suffix	Suffix for the different variants of the XML report
	 * @param xsltPath	Path to the XSL transformation sheet
	 * @throws TransformerException
	 */
	private void saveDocumentToFile(XMLReport report, Document doc, String outputDir, String suffix, String xsltPath) 
			throws TransformerException {
		String docString = report.getReportAsString(doc);
		saveStringToFile(outputDir, "EccoChangeSet" + suffix + ".xml", docString, sep);
		if(transform) {
			String html = report.getReportAsHTML(doc, xsltPath);
			saveStringToFile(outputDir, "index" + suffix + ".html", html, sep);
		}
	}
	
	
	/*
	 * Load ontology
	 */
	public OWLOntology loadOntology(int ontNr, String filepath, boolean localFile) throws OWLOntologyCreationException {
		if(!localFile) sep = "/";
		String filename = filepath.substring(filepath.lastIndexOf(sep)+1, filepath.length());
		System.out.println("Input " + ontNr + ": " + filename + " (" + filepath + ")");
		
		// Load ontology
		OWLOntology ont = null;
		try {
			if(localFile) ont = man.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create("file:" + filepath)), config);
			else ont = man.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(filepath)), config);
		} catch (OWLOntologyCreationException e) {
			System.out.println("\n! [Invalid IRI]\tUnable to load ontology " + ontNr + "\n");
		}
		
		Set<OWLAxiom> result = null;
		if(ont != null) {
			if(verbose) System.out.println("\tLoaded ontology " + ontNr + " (" + ont.getLogicalAxiomCount() + " logical axioms)");
			if(ignoreAbox) removeAbox(ont);
			result = ont.getAxioms();
			if(processImports) {
				if(!ont.getImportsDeclarations().isEmpty()) {
					for(OWLImportsDeclaration d : ont.getImportsDeclarations()) {
						try {
							OWLOntology imported = man.loadOntology(d.getIRI());
							if(verbose) System.out.println("\tLoaded imported ontology (" + imported.getLogicalAxiomCount() + " logical axioms)");
							result.addAll(imported.getLogicalAxioms());
							man.removeOntology(imported);
						} catch (OWLOntologyCreationException e) {
							System.out.println("\n! [Invalid IRI]\tUnable to load the imported ontology: " + d.getURI() + "\n");
						}
					}
				}
			}
			normalize(result);
			man.removeOntology(ont);
		}
		if(result != null) return man.createOntology(result);
		else return null;
	}
	
	
	/*
	 * Load ontology
	 */
	public OWLOntology loadOntology(int ontNr, InputStream stream, boolean localFile) throws OWLOntologyCreationException {
		if(!localFile) sep = "/";
		
		OWLOntology ont = null;
		try {
			ont = man.loadOntologyFromOntologyDocument(new StreamDocumentSource(stream), config);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		
		Set<OWLAxiom> result = null;
		if(ont != null) {
			if(ignoreAbox) removeAbox(ont);
			result = ont.getAxioms();
			if(processImports) {
				if(!ont.getImportsDeclarations().isEmpty()) {
					for(OWLImportsDeclaration d : ont.getImportsDeclarations()) {
						try {
							OWLOntology imported = man.loadOntology(d.getIRI());
							result.addAll(imported.getLogicalAxioms());
							man.removeOntology(imported);
						} catch (OWLOntologyCreationException e) {
							e.printStackTrace();
						}
					}
				}
			}
			normalize(result);
			man.removeOntology(ont);
		}
		return man.createOntology(result);
	}
	
	
	/**
	 * Normalize entity URIs, e.g.: if there exists an entity "A" in sig(O1) and sig(O2) with different URIs
	 * the diff will report changes involving "A" -- not desirable
	 * @param ont1	Ontology 1
	 * @param ont2	Ontology 2
	 * @param verbose	Verbose mode
	 */
	private void normalizeEntityURIs(OWLOntology ont1, OWLOntology ont2) {
		if(verbose) System.out.print("  Normalizing entity URIs... ");
		final SimpleShortFormProvider sf = new SimpleShortFormProvider();
		Set<OWLOntology> ontSet = new HashSet<OWLOntology>();
		ontSet.add(ont1); ontSet.add(ont2);

		OWLEntityURIConverter converter = new OWLEntityURIConverter(man, ontSet, new OWLEntityURIConverterStrategy() {
			@Override
			public IRI getConvertedIRI(OWLEntity arg0) {
			String entityName = sf.getShortForm(arg0);
				IRI iri = IRI.create("http://owl.cs.manchester.ac.uk/ecco#" + entityName);
				return iri;
			}
		});
		man.applyChanges(converter.getChanges());
		if(verbose) System.out.println("done");
	}
	
	
	/**
	 * Given a set of axioms, remove unary disjointness axioms
	 * @param set	Set of axioms to analyse
	 * @return Set of axioms without unary disjointness axioms
	 */
	private Set<OWLAxiom> normalize(Set<OWLAxiom> set) {
		Set<OWLAxiom> toRemove = new HashSet<OWLAxiom>();
		for(OWLAxiom ax : set) {
			if(ax.isOfType(AxiomType.DISJOINT_CLASSES)) {
				OWLDisjointClassesAxiom dis = (OWLDisjointClassesAxiom)ax;
				if(dis.getClassesInSignature().size() < 2)
					toRemove.add(ax);
			}
		}
		set.removeAll(toRemove);
		return set;
	}
	
	
	/**
	 * Remove Abox axioms from given ontology
	 * @param ont	Ontology to remove axioms from
	 */
	private void removeAbox(OWLOntology ont) {
		Set<OWLAxiom> aboxAxs = ont.getABoxAxioms(true);
		ont.getOWLOntologyManager().removeAxioms(ont, aboxAxs);
	}
	
	
	/**
	 * Set the output directory
	 * @param outputDirectory	Output directory
	 */
	public void setOutputDirectory(String outputDirectory, boolean verbose) {
		outputDir = outputDirectory;
		if(verbose) System.out.println("Output directory: " + outputDir + "\n");
	}
	
	
	/**
	 * Set the output directory
	 * @param outputDirectory	Output directory
	 * @param ontFile	Ontology filepath
	 * @param localOnt	true if ontology is hosted locally, false otherwise
	 */
	public void setOutputDirectory(String outputDirectory, String ontFile, boolean localOnt) {
		ontFile = ontFile.substring(0, ontFile.lastIndexOf(sep)+1);
		outputDir = ontFile + "out" + sep;
		if(verbose) System.out.println("Output directory: " + outputDir + "\n");
	}
	
	
	/**
	 * Serialize a given string to the specified path
	 * @param outputDir	Output directory
	 * @param filename	File name
	 * @param content	String to output
	 * @param sep	File system separator
	 */
	private void saveStringToFile(String outputDir, String filename, String content, String sep) {
        try {
            new File(outputDir).mkdirs();
            FileWriter fw = new FileWriter(outputDir + sep + filename);
            fw.write(content);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	
	/**
	 * Print usage message 
	 */
	private static void printUsage() {
		System.out.println(" Usage:\n\t-ont1 [ONTOLOGY] -ont2 [ONTOLOGY] [OPTIONS]");
		System.out.println();
		System.out.println("	[ONTOLOGY]	An input ontology file path or URL");
		System.out.println();
		System.out.println("	[OPTIONS]");
		System.out.println("	-o		Output directory for the XML change set and CSV log");
		System.out.println("	-t		Transform resulting XML report into HTML");
		System.out.println("	-s		Perform structural diff only");
		System.out.println("	-l		Perform logical diff only");
		System.out.println("	-r		Analyze the root ontologies only, not any of their imports");
		System.out.println("	-n		Normalize entity URIs, i.e. if two ontologies have the same entity");
		System.out.println("			names in a different namespace, this trigger establishes a common namespace");
		System.out.println("	-x		File path to XSL Transformation file");
		System.out.println("	-i		Ignore Abox axioms");
		System.out.println("	-v		Verbose mode");
		System.out.println("	-h -help	Print this help message\n");
	}
	
	
	/**
	 * Main
	 * @param args
	 * @throws OWLOntologyCreationException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws TransformerException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException, ParserConfigurationException, IOException, TransformerException {
		boolean hasOnt1 = false, hasOnt2 = false, processImports = true, normalizeURIs = false, ignoreAbox = false, 
				structDiff = false, logDiff = false, verbose = false, transform = false, localOnt1 = true, localOnt2 = true;
		System.out.println(PROGRAM_TITLE);
		String outputDir = "", xsltPath = null, f1 = null, f2 = null;
		
		for(int i = 0; i < args.length; i++) {
			String arg = args[i].trim();
			// Ontology 1
			if (arg.equalsIgnoreCase("-ont1")) {
				if(hasOnt1) throw new RuntimeException("\nToo many -ont1 options provided.\n");
				if(++i == args.length) throw new RuntimeException("\n-ont1 must be followed by an ontology URI.\n");
				f1 = args[i].trim(); hasOnt1 = true;
				if(f1.contains("http")) localOnt1 = false;
			} 
			// Ontology 2
			else if(arg.equalsIgnoreCase("-ont2")) {
				if(hasOnt2) throw new RuntimeException("\nToo many -ont2 options provided.\n");
				if(++i == args.length) throw new RuntimeException("\n-ont2 must be followed by an ontology URI.\n");
				f2 = args[i].trim(); hasOnt2 = true; 
				if(f2.contains("http")) localOnt2 = false;
			}
			// Output directory for change report
			else if(arg.equalsIgnoreCase("-o")) {
				if(++i == args.length) throw new RuntimeException("\n-o must be followed by an output directory.\n");
				arg = args[i].trim(); outputDir = arg;
				if(!outputDir.endsWith(sep)) outputDir += sep;
			}
			// XSLT file path
			else if(arg.equalsIgnoreCase("-x")) {
				if(++i == args.length) throw new RuntimeException("\n-x must be followed by a file path.\n");
				arg = args[i].trim(); xsltPath = arg;
			}
			else if(arg.equalsIgnoreCase("-s"))		// Structural diff only
				structDiff = true;
			else if(arg.equalsIgnoreCase("-l"))		// Logical diff only
				logDiff = true;
			else if(arg.equalsIgnoreCase("-n"))		// Normalize entity namespaces
				normalizeURIs = true;
			else if(arg.equalsIgnoreCase("-t"))		// Transform XML into HTML
				transform = true;
			else if(arg.equalsIgnoreCase("-r"))		// Process root ontologies only
				processImports = false; 
			else if(arg.equalsIgnoreCase("-i"))		// Ignore Abox axioms
				ignoreAbox = true;
			else if(arg.equalsIgnoreCase("-v"))		// Verbose mode
				verbose = true;
			else if(arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("-help")) {		// Print help message
				printUsage(); System.exit(0);
			}
			else if(arg.startsWith("-")) {		// Unknown argument
				System.out.println("\n! Unrecognized option: " + arg + "\n");
				printUsage(); System.exit(0);
			} 
			else {
				System.out.println("\n! Missing arguments\n");
				printUsage(); System.exit(0);
			}
		}
		System.out.println("");
		if(f1 != null && f2 != null) {
			if(!normalizeURIs) 
				System.out.println("* For accurate results ensure that the namespace fragment of shared terms\n" +
					"across ontologies is the same. If not, execute again with the -n flag *\n");
			
			EccoRunner runner = new EccoRunner(processImports, ignoreAbox, transform, normalizeURIs, verbose);
			
			if(outputDir.equals("ont1"))
				runner.setOutputDirectory(outputDir, f1, localOnt1);
			else if(outputDir.equals("ont2"))
				runner.setOutputDirectory(outputDir, f2, localOnt2);
			else if(!outputDir.equals(""))
				runner.setOutputDirectory(outputDir, true);
			else {
				runner.setOutputDirectory("out" + sep, false);
				System.out.println("Using default output directory: [ecco_folder]" + sep + "out" + sep + "\n");
			}

			OWLOntology ont1 = runner.loadOntology(1, f1, localOnt1);
			OWLOntology ont2 = runner.loadOntology(2, f2, localOnt2);

			if(xsltPath == null)
				xsltPath = "out" + sep + "xslt_client.xsl";
			
			if(ont1 != null && ont2 != null) runner.computeDiff(ont1, ont2, structDiff, logDiff, true, xsltPath);
		}
		else if(f1 == null) {
			System.out.println("\n! Invalid or missing -ont1 input\n");
			printUsage();
		}
		else if(f2 == null) {
			System.out.println("\n! Invalid or missing -ont2 input\n");
			printUsage();
		}
	}
}