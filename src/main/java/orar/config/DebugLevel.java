package orar.config;

public enum DebugLevel {
	MERGING_INDIVIDUALS_DIRECTLY, MERGING_INDIVIDUALS_BYABSTRACTION, DL_FRAGMENT_VALIDATING, //
	REASONING, ABSTRACTION_CREATION, MARKING_INDIVIDUALS_TOCHECK_SAMEAS, UPDATING_CONCEPT_ASSERTION, //
	/*
	 * print the normalized TBox
	 */
	PRINT_NORMALIZATION, 
	/*
	 * save the normalized ontology to a file: normalizedOntology.datetime.owl
	 */
	SAVE_NORMALIZED_ONTOLOGY,
	/*
	 * print assertions when doing rule reasoing
	 */
	PRINT_ASSERTION_IN_RULE_ENGINE,
	PRINT_TYPES, TRANSITIVITY_ELIMINATION, MARKING_INDIVIDUAL_OF_SINGLETONCONCEPT_AND_HAVINGCOUNTINGCONCEPT, //
	REASONING_ABSTRACTONTOLOGY, ADDING_MARKING_AXIOMS, STREAM_PARSING, PRINT_MARKING_INDIVIDUALS,//
	TRANSFER_SAMEAS, TRANSFER_ROLEASSERTION,TRANSFER_CONCEPTASSERTION;
}
