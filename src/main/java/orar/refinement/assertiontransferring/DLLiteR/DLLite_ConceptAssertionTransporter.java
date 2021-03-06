package orar.refinement.assertiontransferring.DLLiteR;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import orar.config.Configuration;
import orar.config.DebugLevel;
import orar.data.AbstractDataFactory;
import orar.data.DataForTransferingEntailments;
import orar.data.DataForTransferringEntailmentInterface;
import orar.modeling.ontology.OrarOntology;
import orar.modeling.ontology2.OrarOntology2;
import orar.modeling.roleassertion2.IndexedRoleAssertionList;
import orar.refinement.abstractroleassertion.AbstractRoleAssertionBox;
import orar.refinement.abstractroleassertion.RoleAssertionList;
import orar.refinement.assertiontransferring.AssertionTransporter;
import orar.refinement.assertiontransferring.AssertionTransporterTemplate;
import orar.util.PrintingHelper;

public class DLLite_ConceptAssertionTransporter implements AssertionTransporter {
	// original ontology
	protected final OrarOntology2 orarOntology;
	// entailments of the abstraction
	protected final Map<OWLNamedIndividual, Set<OWLClass>> abstractConceptAssertionsAsMap;

	// flag for abox updating
	protected boolean isABoxExtended;
	// debugging
	protected final Configuration config;
	private static final Logger logger = Logger.getLogger(AssertionTransporterTemplate.class);
	// map/data for transferring assertions
	protected final DataForTransferringEntailmentInterface dataForTransferingEntailments;
	protected final AbstractDataFactory abstractDataFactory;

	public DLLite_ConceptAssertionTransporter(OrarOntology2 orarOntoloy,
			Map<OWLNamedIndividual, Set<OWLClass>> entailedAbstractConceptAssertions) {
		this.orarOntology = orarOntoloy;
		// this.abstractConceptAssertionsAsMap = new HashMap<>();
		// this.abstractRoleAssertionBox = new AbstractRoleAssertionBox();
		// this.abstractSameasMap = new HashMap<>();
		this.isABoxExtended = false;
		this.config = Configuration.getInstance();
		this.dataForTransferingEntailments = DataForTransferingEntailments.getInstance();
		this.abstractDataFactory = AbstractDataFactory.getInstance();
		this.abstractConceptAssertionsAsMap = entailedAbstractConceptAssertions;
	}

	@Override
	public void updateOriginalABox() {
		transferConceptAssertions();// not change

	}

	/**
	 * add concept assertions based on concept assertions of representatives X
	 */
	protected void transferConceptAssertions() {
		Iterator<Entry<OWLNamedIndividual, Set<OWLClass>>> iterator = this.abstractConceptAssertionsAsMap.entrySet()
				.iterator();
		while (iterator.hasNext()) {
			Entry<OWLNamedIndividual, Set<OWLClass>> entry = iterator.next();
			OWLNamedIndividual abstractInd = entry.getKey();
			/*
			 * only transferring assertions of X-abstract-individual
			 */
			if (!this.abstractDataFactory.getXAbstractIndividuals().contains(abstractInd)) {
				continue;
			}
			Set<OWLClass> concepts = entry.getValue();
			if (concepts != null) {
				Set<Integer> originalIndividuals = this.dataForTransferingEntailments
						.getOriginalIndividuals(abstractInd);
				for (Integer originalInd : originalIndividuals) {
					/*
					 * debug:begin
					 */
					if (config.getDebuglevels().contains(DebugLevel.UPDATING_CONCEPT_ASSERTION)) {
						logger.info("***DEBUG Update concept assertions in the original ABox ***");
						logger.info("Individual:" + originalInd);
						logger.info("has new concepts:" + concepts);
						logger.info("Reason: get from concept assertion of the abstract individual:" + abstractInd);
						logger.info("*=====================================================*");
					}
					/*
					 * debug:end
					 */
					Set<OWLClass> existingAssertedConcept = new HashSet<OWLClass>();
					if (this.config.getDebuglevels().contains(DebugLevel.TRANSFER_CONCEPTASSERTION)) {
						existingAssertedConcept.addAll(this.orarOntology.getAssertedConcepts(originalInd));
					}
					if (this.orarOntology.addManyConceptAssertions(originalInd, concepts)) {
						this.isABoxExtended = true;
						if (this.config.getDebuglevels().contains(DebugLevel.TRANSFER_CONCEPTASSERTION)) {
							logger.info("***DEBUG***TRANSFER_CONCEPTASSERTION:");
							logger.info("For individual:" + originalInd);
							logger.info("Existing asserted concepts:");
							PrintingHelper.printSet(existingAssertedConcept);
							logger.info("Newly added asserted concepts:" + concepts);
							logger.info("updated=true");
						}
					}
				}
			}
		}

	}

	@Override
	public boolean isABoxExtended() {

		return this.isABoxExtended;
	}

	@Override
	public IndexedRoleAssertionList getNewlyAddedRoleAssertions() {
		return new IndexedRoleAssertionList();
	}

	@Override
	public Set<Set<Integer>> getNewlyAddedSameasAssertions() {
		return new HashSet<Set<Integer>>();
	}

	@Override
	public boolean isABoxExtendedViaX() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isABoxExtendedViaY() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isABoxExtendedViaZ() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isABoxExtendedWithNewSpecialRoleAssertions() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isABoxExtendedWithNewSameasAssertions() {
		// TODO Auto-generated method stub
		return false;
	}
	public Set<Integer> getIndividualsHavingNewAssertions(){
		return new HashSet<>();
	}
}
