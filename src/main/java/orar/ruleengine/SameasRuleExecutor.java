package orar.ruleengine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;

import orar.abstraction.TypeComputor;
import orar.abstraction2.BasicTypeComputor_Increment;
import orar.data.DataForTransferingEntailments;
import orar.modeling.ontology2.OrarOntology2;
import orar.modeling.roleassertion2.IndexedRoleAssertion;
import orar.type.IndividualType;

public class SameasRuleExecutor implements RuleExecutor {

	private final OrarOntology2 orarOntology;
	private final Set<Set<Integer>> newSameasAssertions;
	private boolean isABoxExtended;
	private boolean isIncrementalStepAfterFirstAbstraction = false;
	// private Queue<Set<OWLNamedIndividual>> localTodoSameas;
	// private final Logger logger = Logger.getLogger(SameasRuleExecutor.class);
	Logger logger = Logger.getLogger(SameasRuleExecutor.class);
	/*
	 * for incremental type computation
	 */
	private final TypeComputor typeComputor;
	private final Map<IndividualType, Set<Integer>> mapType2Individuals;
	public SameasRuleExecutor(OrarOntology2 orarOntology) {
		this.orarOntology = orarOntology;
		this.isABoxExtended = false;
		this.newSameasAssertions = new HashSet<>();
		this.typeComputor = new BasicTypeComputor_Increment(this.orarOntology);
		this.mapType2Individuals = DataForTransferingEntailments.getInstance().getMapType_2_Individuals();
	}

	@Override
	public void materialize() {
		// long startTime = System.currentTimeMillis();
		// compute connect components
		List<Set<Integer>> components = computeConnectedComponents();
		// put components to the map.
		for (Set<Integer> component : components) {
			for (Integer ind : component) {
				if (this.orarOntology.addManySameAsAssertions(ind, component)) {
					this.isABoxExtended = true;
				}
			}
		}
		// long endTime = System.currentTimeMillis();
		// long time = (endTime-startTime)/1000;
		// logger.info("time in materializer step: "+ time);
	}

	private List<Set<Integer>> computeConnectedComponents() {
		Set<Integer> allIndividualsInSameasMap = this.orarOntology.getSameasBox().getAllIndividuals();
		Queue<Integer> todoIndividuals = new LinkedList<Integer>(allIndividualsInSameasMap);
		List<Set<Integer>> components = new ArrayList<Set<Integer>>();
		while (!todoIndividuals.isEmpty()) {
			Integer a = todoIndividuals.poll();
			// compute component for each individual a in todoIndividuals.
			Set<Integer> newComponent = new HashSet<Integer>();
			newComponent.add(a);
			Stack<Integer> stackForDFS = new Stack<Integer>();
			stackForDFS.push(a);
			while (!stackForDFS.isEmpty()) {
				Integer ind = stackForDFS.pop();
				Set<Integer> sameasOf_a = this.orarOntology.getSameIndividuals(ind);
				sameasOf_a.removeAll(newComponent);
				newComponent.addAll(sameasOf_a);
				stackForDFS.addAll(sameasOf_a);
				todoIndividuals.removeAll(sameasOf_a);
			}
			components.add(newComponent);
		}
		return components;
	}

	@Override
	public Set<Set<Integer>> getNewSameasAssertions() {

		// return this.newSameasAssertions;
		return new HashSet<>();
	}

	@Override
	public Set<IndexedRoleAssertion> getNewRoleAssertions() {
		// return empty set in this rule
		return new HashSet<IndexedRoleAssertion>();
	}

	@Override
	public boolean isABoxExtended() {

		return this.isABoxExtended;
	}
	private void removeIndividuslFromOldMapType2Individuals(IndividualType oldTYpe, Set<Integer> currentIndividuals,
			Integer individualToBeRemoved) {
		currentIndividuals.remove(individualToBeRemoved);
		if (currentIndividuals.isEmpty()) {
			this.mapType2Individuals.remove(oldTYpe);
		}
	}
	@Override
	public void incrementalMaterialize(Set<Integer> setOfSameasIndividuals) {
		// logger.info("SameasRuleExecutor.incrementalMaterialize");
		// get union of equivalent individuals in the set.
		Set<Integer> accumulatedSameasIndividuals = new HashSet<Integer>();
		for (Integer ind : setOfSameasIndividuals) {
			accumulatedSameasIndividuals.addAll(this.orarOntology.getSameIndividuals(ind));
		}
		if (accumulatedSameasIndividuals.size() == 0) {
			Integer firstIndiv=null;
			if (this.isIncrementalStepAfterFirstAbstraction){
			 firstIndiv = accumulatedSameasIndividuals.iterator().next();
			IndividualType oldType = this.typeComputor.computeType(firstIndiv);
			Set<Integer> oldIndividuals = this.mapType2Individuals.get(oldType);
			removeIndividuslFromOldMapType2Individuals(oldType, oldIndividuals, firstIndiv);
			}
			
			this.orarOntology.addNewManySameAsAssertions(setOfSameasIndividuals);
			if (this.isIncrementalStepAfterFirstAbstraction){
			this.typeComputor.computeType(firstIndiv);
			}
			this.isABoxExtended = true;
			
			
		} else {
			accumulatedSameasIndividuals.addAll(setOfSameasIndividuals);
			
			Integer firstIndiv=null;
			IndividualType oldType =null;
			Set<Integer> oldIndividuals =null;
			if (this.isIncrementalStepAfterFirstAbstraction){
			 firstIndiv = accumulatedSameasIndividuals.iterator().next();
			oldType = this.typeComputor.computeType(firstIndiv);
			oldIndividuals = this.mapType2Individuals.get(oldType);
			
			}
			
			if (!setOfSameasIndividuals.containsAll(accumulatedSameasIndividuals)) {
				
				if(this.isIncrementalStepAfterFirstAbstraction){
					removeIndividuslFromOldMapType2Individuals(oldType, oldIndividuals, firstIndiv);
				}
				
				this.orarOntology.addNewManySameAsAssertions(accumulatedSameasIndividuals);
				
				if(this.isIncrementalStepAfterFirstAbstraction){
					this.typeComputor.computeTypeIncrementally(firstIndiv);
				}
				
				this.isABoxExtended = true;
				/*
				 * We don't need to add new sameas assertions to todo because
				 * when other rule consider setOfSameasIndividuals, obtained
				 * from global toto, this new sameas is taken into account. The
				 * reason is that while executing, other rules take
				 * setOfSameasIndividuals sameas assertions currently in the
				 * ontology.
				 */
				// this.newSameasAssertions.add(accumulatedSameasIndividuals);
			}
		}
		// // update the map
		// for (Integer eachIndividual : accumulatedSameasIndividuals) {
		// if (this.orarOntology.addManySameAsAssertions(eachIndividual,
		// accumulatedSameasIndividuals)) {
		// this.isABoxExtended = true;
		// }

	}

	@Override
	public void clearOldBuffer() {
		// nothing to clear

	}

	@Override
	public void incrementalMaterialize(IndexedRoleAssertion roleAssertion) {
		// nothing to to

	}

	@Override
	public void setIncrementalAfterFirstAbstraction() {
		this.isIncrementalStepAfterFirstAbstraction=true;
		
	}
}
