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
package uk.ac.manchester.cs.diff.justifications;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.api.ExplanationManager;
import org.semanticweb.owl.explanation.impl.laconic.LaconicExplanationGeneratorFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class LaconicJustificationFinder {
	private OWLReasonerFactory rf;
	private ExplanationGeneratorFactory<OWLAxiom> regFac, lacFac;
	private OWLOntology ent, just;
	private final long timeout = 500;	// milliseconds
	
	/**
	 * Constructor
	 * @param ent	Entailment
	 * @param just	Justification
	 */
	public LaconicJustificationFinder(OWLOntology ent, OWLOntology just) {
		this.ent = ent;
		this.just = just;
		rf = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();
		regFac = ExplanationManager.createExplanationGeneratorFactory(rf);
		lacFac = new LaconicExplanationGeneratorFactory<OWLAxiom>(regFac);
	}
	
	
	/**
	 * Get laconic justification
	 * @return Justification
	 */
	public Explanation<OWLAxiom> getLaconicJustification() {
		// Set timeout
		Timer t = new Timer(true);
		t.schedule(interrupt, timeout);
		// Find laconic justs
		ExplanationGenerator<OWLAxiom> lacGen = lacFac.createExplanationGenerator(just.getAxioms());
		Set<Explanation<OWLAxiom>> results = new HashSet<Explanation<OWLAxiom>>();
		Set<OWLLogicalAxiom> axs = ent.getLogicalAxioms();
		for(OWLLogicalAxiom ax : axs) {
			try {
				results.addAll(lacGen.getExplanations(ax, 1));
			} catch(OutOfMemoryError | IllegalArgumentException e) { /* Do nothing */ }
		}
		return results.iterator().next();
	}
	
	
	/**
	 * Get laconic justification as an ontology string
	 * @return Laconic justification as an ontology string
	 */
	public String getLaconicJustificationAsString() {
		return getOntologyAsString(getLaconicJustification().getAxioms());
	}
	
	
	/**
	 * Get set of axioms as an ontology string
	 * @param axioms	Set of axioms
	 * @return String representing an ontology containing the specified axioms 
	 */
	private String getOntologyAsString(Set<OWLAxiom> axioms) {
		String out = "Ontology(";
		for(OWLAxiom ax : axioms)
			out += ax.toString() + "\n";
		out += ")";
		return out;
	}
	
	
	/**
	 * Interrupt trigger	
	 */
	private TimerTask interrupt = new TimerTask() {
		@Override
		public void run() {
//			System.out.print("\n\tLaconic Justification Finder: Task exceeded timeout");
			System.exit(0);
		}
	};
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String entailment = args[0];
		String justification = args[1];
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ent = null, just = null;
		try {
			ent = man.loadOntologyFromOntologyDocument(new StringDocumentSource(entailment));
			just = man.loadOntologyFromOntologyDocument(new StringDocumentSource(justification));
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		LaconicJustificationFinder finder = new LaconicJustificationFinder(ent, just);
		String out = finder.getLaconicJustificationAsString();
		System.out.println(out);
	}
}