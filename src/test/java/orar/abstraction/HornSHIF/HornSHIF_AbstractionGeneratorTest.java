package orar.abstraction.HornSHIF;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import junit.framework.Assert;
import orar.abstraction.AbstractionGenerator;
import orar.abstraction.BasicTypeComputor;
import orar.abstraction.PairOfSubjectAndObject;
import orar.abstraction.TypeComputor;
import orar.data.AbstractDataFactory;
import orar.data.DataForTransferingEntailments;
import orar.data.DataForTransferringEntailmentInterface;
import orar.data.MetaDataOfOntology;
import orar.indexing.IndividualIndexer;
import orar.modeling.ontology2.MapbasedOrarOntology2;
import orar.modeling.ontology2.OrarOntology2;
import orar.type.IndividualType;
import orar.util.DefaultTestDataFactory;
import orar.util.PrintingHelper;

public class HornSHIF_AbstractionGeneratorTest {
	DefaultTestDataFactory testData = DefaultTestDataFactory.getInsatnce();
	IndividualIndexer indexer = IndividualIndexer.getInstance();

	/*
	 * Signature
	 */
	Integer a = indexer.getIndexOfOWLIndividual(testData.getIndividual("a"));
	Integer a1 = indexer.getIndexOfOWLIndividual(testData.getIndividual("a1"));
	Integer a2 = indexer.getIndexOfOWLIndividual(testData.getIndividual("a2"));

	Integer b = indexer.getIndexOfOWLIndividual(testData.getIndividual("b"));
	Integer b1 = indexer.getIndexOfOWLIndividual(testData.getIndividual("b1"));
	Integer b2 = indexer.getIndexOfOWLIndividual(testData.getIndividual("b2"));

	Integer c = indexer.getIndexOfOWLIndividual(testData.getIndividual("c"));
	Set<Integer> a1a2 = new HashSet<Integer>();
	Set<Integer> b1b2 = new HashSet<Integer>();
	Set<Integer> a1a2b1b2 = new HashSet<Integer>();
	Set<Integer> b1Set = new HashSet<Integer>();
	Set<Integer> a1Set = new HashSet<Integer>();

	OWLClass A = testData.getConcept("A");
	OWLClass A1 = testData.getConcept("A1");
	OWLClass A2 = testData.getConcept("A2");

	OWLClass B = testData.getConcept("B");
	OWLClass B1 = testData.getConcept("B1");
	OWLClass B2 = testData.getConcept("B2");

	OWLClass C = testData.getConcept("C");

	OWLObjectProperty R = testData.getRole("R");
	OWLObjectProperty R1 = testData.getRole("R1");
	OWLObjectProperty R2 = testData.getRole("R2");

	OWLObjectProperty S = testData.getRole("S");

	/*
	 * Others
	 */

	OWLObjectProperty funcRole = testData.getRole("funcRole");
	OWLObjectProperty invFuncRole = testData.getRole("invFuncRole");
	MetaDataOfOntology sharedData = MetaDataOfOntology.getInstance();
	DataForTransferringEntailmentInterface sharedMap = DataForTransferingEntailments.getInstance();
	AbstractDataFactory abstractDataFactory = AbstractDataFactory.getInstance();

	@Before
	public void init() {
		indexer.clear();
		a1a2.add(a1);
		a1a2.add(a2);

		b1b2.add(b1);
		b1b2.add(b2);

		a1a2b1b2.add(a1);
		a1a2b1b2.add(a2);
		a1a2b1b2.add(b1);
		a1a2b1b2.add(b2);

		b1Set.add(b1);
		a1Set.add(a1);
	}

	@Test
	public void shouldGenerateOneAbstractionAndMapProperly() {
		sharedData.clear();
		sharedMap.clear();
		abstractDataFactory.clear();
		indexer.clear();
		/*
		 * create ontology
		 */
		OrarOntology2 orarOntology = new MapbasedOrarOntology2();
		sharedData.getFunctionalRoles().add(funcRole);
		sharedData.getInverseFunctionalRoles().add(invFuncRole);

		/*
		 * create ontology: A(a1), funcRole(a1,b1), A(a2), funcRole(a2,b2)
		 */
		orarOntology.addConceptAssertion(a1, A);
		orarOntology.addConceptAssertion(a2, A);
		orarOntology.addRoleAssertion(a1, funcRole, b1);
		orarOntology.addRoleAssertion(a2, funcRole, b2);
		/*
		 * we only want to test for a1, a2, therefore we add a1,a2 to the
		 * signature of the ontology
		 */

		orarOntology.addIndividualsToSignature(a1a2);

		/*
		 * compute type and generate the abstraction
		 */

		TypeComputor typeComputor = new BasicTypeComputor(orarOntology);
		Map<IndividualType, Set<Integer>> typeMap2Individuals = typeComputor.computeTypes();
		AbstractionGenerator abstractionGenerator = new HornSHIF_AbstractionGenerator(orarOntology,
				typeMap2Individuals);
		OWLOntology abstraction = abstractionGenerator.getAbstractOntology();

		/*
		 * compare abstraction
		 */
		OWLNamedIndividual x1 = testData.getAbstractIndividual("X1");
		OWLNamedIndividual y1 = testData.getAbstractIndividual("Y1");
		OWLClassAssertionAxiom A_x1 = testData.getConceptAssertion(A, x1);
		OWLObjectPropertyAssertionAxiom funcRole_x1y1 = testData.getRoleAssertion(x1, funcRole, y1);
		Set<OWLAxiom> expectedAxioms = new HashSet<OWLAxiom>();
		expectedAxioms.add(A_x1);
		expectedAxioms.add(funcRole_x1y1);

		PrintingHelper.printSet(abstraction.getAxioms());
		Assert.assertEquals(expectedAxioms, abstraction.getAxioms());
		/*
		 * compare mappings for x
		 */
		Assert.assertEquals(a1a2, sharedMap.getMap_XAbstractIndiv_2_OriginalIndivs().get(x1));
		/*
		 * compare mappings for y
		 */
		Assert.assertEquals(b1b2, sharedMap.getMap_YAbstractIndiv_2_OriginalIndivs().get(y1));
		/*
		 * compare mapping for x1y1
		 */
		PairOfSubjectAndObject x1y1 = new PairOfSubjectAndObject(x1, y1);
		Assert.assertEquals(funcRole, sharedMap.getMap_XY_2_Role().get(x1y1));
	}

	@Test
	public void shouldGenerateTwoAbstractions() {
		sharedData.clear();
		sharedMap.clear();
		abstractDataFactory.clear();
		/*
		 * create ontology
		 */
		sharedData.getFunctionalRoles().add(funcRole);
		sharedData.getInverseFunctionalRoles().add(invFuncRole);

		/*
		 * create ontology: A(a1), funcRole(a1,b1), A(a2), funcRole(a2,b2)
		 */
		OrarOntology2 orarOntology = new MapbasedOrarOntology2();
		orarOntology.addConceptAssertion(a1, A);
		orarOntology.addConceptAssertion(a2, A);
		orarOntology.addRoleAssertion(a1, funcRole, b1);
		orarOntology.addRoleAssertion(a2, funcRole, b2);
		/*
		 * we only want to test for a1, a2, therefore we add a1,a2 to the
		 * signature of the ontology
		 */
		orarOntology.addIndividualsToSignature(a1a2b1b2);

		/*
		 * compute type and generate the abstraction
		 */

		TypeComputor typeComputor = new BasicTypeComputor(orarOntology);
		Map<IndividualType, Set<Integer>> typeMap2Individuals = typeComputor.computeTypes();
		AbstractionGenerator abstractionGenerator = new HornSHIF_AbstractionGenerator(orarOntology,
				typeMap2Individuals);
		List<OWLOntology> abstractions = abstractionGenerator.getAbstractOntologies(1);

		/*
		 * compare number of abstractions
		 */
		Assert.assertEquals(2, abstractions.size());

		/*
		 * compare content of abstractions
		 */
		OWLNamedIndividual x1 = testData.getAbstractIndividual("X1");
		OWLNamedIndividual y1 = testData.getAbstractIndividual("Y1");
		OWLNamedIndividual x2 = testData.getAbstractIndividual("X2");
		OWLNamedIndividual z1 = testData.getAbstractIndividual("Z1");
		OWLClassAssertionAxiom A_x1 = testData.getConceptAssertion(A, x1);
		OWLObjectPropertyAssertionAxiom funcRole_x1y1 = testData.getRoleAssertion(x1, funcRole, y1);
		OWLObjectPropertyAssertionAxiom funcRole_z1X2 = testData.getRoleAssertion(z1, funcRole, x2);

		Set<OWLAxiom> expectedAxioms = new HashSet<OWLAxiom>();
		expectedAxioms.add(A_x1);
		expectedAxioms.add(funcRole_x1y1);
		expectedAxioms.add(funcRole_z1X2);

		Set<OWLAxiom> actualAxioms = new HashSet<OWLAxiom>();

		for (OWLOntology abstraction : abstractions) {
			System.out.println("====Abstraction====");
			PrintingHelper.printSet(abstraction.getAxioms());
			actualAxioms.addAll(abstraction.getAxioms());
		}

		Assert.assertEquals(expectedAxioms, actualAxioms);

		/*
		 * compare mappings for x
		 */
		Assert.assertEquals(a1a2, sharedMap.getMap_XAbstractIndiv_2_OriginalIndivs().get(x1));
		/*
		 * compare mappings for y
		 */
		Assert.assertEquals(b1b2, sharedMap.getMap_YAbstractIndiv_2_OriginalIndivs().get(y1));
		/*
		 * compare mapping for x1y1
		 */
		PairOfSubjectAndObject x1y1 = new PairOfSubjectAndObject(x1, y1);
		Assert.assertEquals(funcRole, sharedMap.getMap_XY_2_Role().get(x1y1));

		/*
		 * compare marking for x1
		 */
		Assert.assertEquals(testData.getSetOfIndividuals(x1), sharedMap.getxAbstractHavingFunctionalRole());
	}

	@Test
	public void shouldCreateMappingPropertly() {
		sharedData.clear();
		sharedMap.clear();
		abstractDataFactory.clear();
		/*
		 * create ontology
		 */
		OrarOntology2 orarOntology = new MapbasedOrarOntology2();
		sharedData.getFunctionalRoles().add(funcRole);
		sharedData.getInverseFunctionalRoles().add(invFuncRole);

		/*
		 * create ontology: A(a1), funcRole(a1,b1), A(a2), funcRole(a2,b2)
		 */
		orarOntology.addRoleAssertion(a1, invFuncRole, b1);
		/*
		 * we only want to test for a1, a2, therefore we add a1,a2 to the
		 * signature of the ontology
		 */
		orarOntology.addIndividualsToSignature(b1Set);

		/*
		 * compute type and generate the abstraction
		 */

		TypeComputor typeComputor = new BasicTypeComputor(orarOntology);
		Map<IndividualType, Set<Integer>> typeMap2Individuals = typeComputor.computeTypes();
		AbstractionGenerator abstractionGenerator = new HornSHIF_AbstractionGenerator(orarOntology,
				typeMap2Individuals);
		OWLOntology abstraction = abstractionGenerator.getAbstractOntology();

		/*
		 * compare mappings for x,z
		 */

		OWLNamedIndividual x1 = testData.getAbstractIndividual("X1");
		OWLNamedIndividual z1 = testData.getAbstractIndividual("Z1");
		PrintingHelper.printSet(abstraction.getAxioms());
		Assert.assertEquals(a1Set, sharedMap.getMap_ZAbstractIndiv_2_OriginalIndivs().get(z1));
		Assert.assertEquals(b1Set, sharedMap.getMap_XAbstractIndiv_2_OriginalIndivs().get(x1));

		/*
		 * compare mapping for z1x1
		 */
		PairOfSubjectAndObject z1x1 = new PairOfSubjectAndObject(z1, x1);
		Assert.assertEquals(invFuncRole, sharedMap.getMap_ZX_2_Role().get(z1x1));

		/*
		 * compare marking for z1
		 */
		Assert.assertEquals(testData.getSetOfIndividuals(z1), sharedMap.getzAbstractHavingInverseFunctionalRole());
	}

}
