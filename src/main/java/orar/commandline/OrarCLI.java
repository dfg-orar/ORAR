package orar.commandline;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import orar.config.Configuration;
import orar.config.LogInfo;
import orar.config.StatisticVocabulary;
import orar.io.ontologyreader.HornSHOIF_OntologyReader;
import orar.io.ontologyreader.OntologyReader;
import orar.materializer.Materializer;
import orar.materializer.DLLiteExtensions.DLLiteExtension_Materializer_Hermit;
import orar.materializer.DLLiteExtensions.DLLiteExtension_Materializer_Konclude;
import orar.materializer.HornSHIF.HornSHIF_Materializer_Hermit;
import orar.materializer.HornSHIF.HornSHIF_Materializer_KoncludeOptimized;
import orar.materializer.HornSHOIF.HornSHOIF_Materializer_Hermit;
import orar.materializer.HornSHOIF.HornSHOIF_Materializer_KoncludeOptimized;
import orar.modeling.ontology2.MapbasedOrarOntology2;
import orar.modeling.ontology2.OrarOntology2;
import orar.strategyindentifying.StrategyIdentifier;
import orar.strategyindentifying.StrategyIdentifierImpl;
import orar.strategyindentifying.StrategyName;

public class OrarCLI {
	private static Configuration config = Configuration.getInstance();
	private static Logger logger = Logger.getLogger(OrarCLI.class);
	private static long totalLoadingTime = 0;

	public static void main(String[] args) {
		// PropertyConfigurator.configure("src/main/resources/log4j.properties");

		Options options = new Options();
		/*
		 * Boolean options
		 */
		Option parsingTime = new Option(Argument.LOADING_TIME, false, "print loading time");
		Option runningTime = new Option(Argument.REASONING_TIME, false, "print time for materialization");

		Option totalTime = new Option(Argument.TOTAL_TIME, false,
				"print total time of the system,e.g. loading time + materialization time");

		Option statistic = new Option(Argument.STATISTIC, false,
				"print statistic information of the (materialized) ontology");

		Option help = new Option(Argument.HELP, false, "print help");

		Option test = new Option(Argument.PRINT_RESULT, false, "print results");
		Option performance = new Option(Argument.PERFORMANCE, false, "print detailed time in each step");
		/*
		 * Argument options
		 */
		Option tbox = new Option(Argument.TBOX, true, "input TBox OWL file in any format supported by OWLAPI");
		Option aboxes = new Option(Argument.ABOX, true,
				"an input text file containing the list of ABox files in RDF/XML, N3, or Turtle format; one file per line");
		Option ontology = new Option(Argument.ONTOLOGY, true,
				"OWL ontology file containing both TBox and ABox in any format supported by OWLAPI");
		// Option reasoner = new Option(
		// REASONER,
		// true,
		// "set the reasoner used in the system. If you choose konclude, then
		// the path to Konclude reasoner must be provided");
		//
		StringBuilder reasonerDescription = new StringBuilder();
		reasonerDescription.append("the (inner) reasoner used to materialize the abstractions:");
		// reasonerDescription.append(
		// Argument.KONCLUDE + ", " + Argument.HERMIT + ", " + Argument.PELLET +
		// ", " + Argument.FACT + ".\n");
		reasonerDescription.append(Argument.KONCLUDE + ", " + Argument.HERMIT + ", " + Argument.PELLET + ".\n");
		reasonerDescription.append(
				"If choosing konclude, then the path to the Konclude binary file and the port for the Konclude server must be provided");
		// Option reasoner =
		// Option.builder(Argument.REASONER).required().desc(reasonerDescription.toString()).hasArg(true)
		// .build();
		Option reasoner = Option.builder(Argument.REASONER).desc(reasonerDescription.toString()).hasArg(true).build();

		Option konclude = new Option(Argument.KONCLUDEPATH, true,
				"Konclude reasoner file; only required when choosing Konclude as an inner reasoner");
		Option port = new Option(Argument.PORT, true,
				"port number of the Konclude server in case the inner reasoner is Konclude");
		Option outputABox = new Option(Argument.OUTPUTABOX, true,
				"the output file containing all entailed atomic assertions in OWL/XML format");

		// Option dl = Option.builder(Argument.DL).required()
		// .desc("set the reasoner used in the system. If you choose konclude,
		// then the path to Konclude reasoner must be provided")
		// .hasArg(true).build();

		Option split = new Option(Argument.SPLITTING, true, "number of types per abstract ABox; experimental feature");

		/*
		 * add options
		 */

		// options.addOption(totalTime);
		options.addOption(parsingTime);
		options.addOption(runningTime);
		options.addOption(statistic);
		options.addOption(tbox);
		options.addOption(aboxes);
		options.addOption(ontology);
		options.addOption(reasoner);
		options.addOption(konclude);
		// options.addOption(split);
		options.addOption(port);
		options.addOption(outputABox);
		options.addOption(help);
		options.addOption(test);
		options.addOption(performance);
		// create the parser
		CommandLineParser parser = new DefaultParser();
		try {
			/*
			 * parse the command line arguments
			 */
			CommandLine commandLine = parser.parse(options, args);

			/*
			 * print help
			 */
			if (commandLine.hasOption(Argument.HELP)) {
				printHelp(options);
				return;
			}

			/*
			 * Check the validity of the arguments
			 */
			if (!argumentsAreValid(commandLine)) {
				logger.info("Missing/invalid arguments!");
				printHelp(options);
				return;
			}
			/*
			 * get arguments and config the system accordingly
			 */
			setConfigWithBooleanArguments(commandLine);

			logger.info("Run abstraction with " + commandLine.getOptionValue(Argument.REASONER));

			OrarOntology2 orarOntology = getOrarOntology(commandLine);
			Materializer materializer = getMaterializer(commandLine, orarOntology);
			logger.info("Run " + materializer.getClass());
			runMaterializer(materializer, commandLine);
			if (config.getLogInfos().contains(LogInfo.LOADING_TIME)) {
				logger.info(StatisticVocabulary.TIME_LOADING_IN_ALL_STEPS + totalLoadingTime);
			}

			if (config.getLogInfos().contains(LogInfo.REASONING_TIME)) {
				logger.info(StatisticVocabulary.TIME_REASONING_BY_INNER_REASONER
						+ materializer.getReasoningTimeOfInnerReasonerInSeconds());
				
				logger.info(StatisticVocabulary.TIME_REASONING_BY_DEDUCTIVE_RULES
						+ materializer.getReasoningTimeOfDeductiveRules());
				
				logger.info(StatisticVocabulary.TOTAL_REASONING_TIME
						+ materializer.getReasoningTimeInSeconds());
			}
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());

			printHelp(options);
			printExampleRun();
		}
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("Orar", options);
		printExampleRun();
	}

	private static void printExampleRun() {
		System.out.println("");
		System.out.println("Example run with HermiT as an inner reasoner:");
		System.out
				.println("java -jar -Xmx2G Orar.jar -reasoner hermit -statistic -tbox ./tbox.owl -abox ./aboxList.txt");
		System.out.println("");
		System.out.println("Example run with Konclude as an inner reasoner:");
		System.out.println(
				"java -jar -Xmx2G Orar.jar -reasoner konclude -koncludepath ./Konclude -port 9090 -statistic -tbox ./tbox.owl -abox ./aboxList.txt");
	}

	private static long runMaterializer(Materializer materializer, CommandLine commandLine) {

		String reasonerName = commandLine.getOptionValue(Argument.REASONER);
		logger.info("Runnig Abstraction Refinement Using :" + reasonerName + " ...");
		materializer.materialize();

		if (commandLine.hasOption(Argument.PRINT_RESULT)) {
			OrarOntology2 materializedOrarOntology = materializer.getOrarOntology();
			Map<OWLClass, Set<OWLNamedIndividual>> map = materializedOrarOntology
					.getOWLAPIConcepAssertionMapWITHOUTNormalizationSymbols();
			Iterator<Entry<OWLClass, Set<OWLNamedIndividual>>> iterator = map.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<OWLClass, Set<OWLNamedIndividual>> entry = iterator.next();
				logger.info("Instances of concept:" + entry.getKey().getIRI().toString());
				logger.info("Number of instances:" + entry.getValue().size());
			}
		}

		if (commandLine.hasOption(Argument.OUTPUTABOX)) {
			String aboxFile = commandLine.getOptionValue(Argument.OUTPUTABOX);
			Set<OWLAxiom> entailedAssertions = materializer.getOrarOntology().getOWLAPIMaterializedAssertions();
			saveAssertionsToFile(entailedAssertions, aboxFile);
		}
		totalLoadingTime += materializer.getAbstractOntologyLoadingTime();
		return materializer.getReasoningTimeInSeconds();

	}

	private static void saveAssertionsToFile(Set<OWLAxiom> assertions, String fileName) {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

		logger.info("Saving entailed assertions to a file ...");
		
		// OWLDocumentFormat format = new OWLXMLDocumentFormat();
		OWLXMLOntologyFormat format=new OWLXMLOntologyFormat();
		File file = new File(fileName);
		IRI iriDocument = IRI.create(file.toURI());
		try {
			OWLOntology ontology = manager.createOntology();
			manager.addAxioms(ontology, assertions);
			long startSavingTime = System.currentTimeMillis();
			manager.saveOntology(ontology, format, iriDocument);
			long endSavingTime = System.currentTimeMillis();
			long savingTimeInSeconds = (endSavingTime - startSavingTime) / 1000;
			if (config.getLogInfos().contains(LogInfo.LOADING_TIME)) {
				logger.info("Time for saving the entailed assertions (in seconds):" + savingTimeInSeconds);

			}
			logger.info("                                     ...done!");
		} catch (OWLOntologyCreationException e) {

			e.printStackTrace();
		}

		catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}

	static private Materializer getMaterializer(CommandLine commandLine, OrarOntology2 orarOntology) {
		Materializer materializer;
		String reasoner = commandLine.getOptionValue(Argument.REASONER);
		materializer = getHornReasoner(commandLine, reasoner, orarOntology);
		return materializer;
	}

	static private Materializer getHornReasoner(CommandLine commandLine, String reasonerName,
			OrarOntology2 orarOntology) {
		Materializer materializer = null;
		// logger.info("Info: Some DL Constructors in the validated ontology: "
		// + orarOntology.getActualDLConstructors());
		StrategyIdentifier strategyIdentifier = new StrategyIdentifierImpl(orarOntology.getTBoxAxioms());
		StrategyName strategyName = strategyIdentifier.getStrategyName();
		if (config.getLogInfos().contains(LogInfo.TIME_STAMP_FOR_EACH_STEP)) {
			logger.info("Strategy:" + strategyName);
		}
		if (reasonerName.equals(Argument.HERMIT)) {

			switch (strategyName) {
			case DLLITE_EXTENSION_STRATEGY:
				materializer = new DLLiteExtension_Materializer_Hermit(orarOntology);
				break;
			case HORN_SHIF_STRATEGY:
				materializer = new HornSHIF_Materializer_Hermit(orarOntology);
				break;
			case HORN_SHOIF_STRATEGY:
				materializer = new HornSHOIF_Materializer_Hermit(orarOntology);
				break;
			}
		}

		if (reasonerName.equals(Argument.KONCLUDE)) {
			String koncludePath = commandLine.getOptionValue(Argument.KONCLUDEPATH);
			config.setKONCLUDE_BINARY_PATH(koncludePath);
			String port = commandLine.getOptionValue(Argument.PORT);
			int intPort = Integer.parseInt(port);

			switch (strategyName) {
			case DLLITE_EXTENSION_STRATEGY:
				materializer = new DLLiteExtension_Materializer_Konclude(orarOntology, intPort);
				break;
			case HORN_SHIF_STRATEGY:
				materializer = new HornSHIF_Materializer_KoncludeOptimized(orarOntology, intPort);
				break;
			case HORN_SHOIF_STRATEGY:
				materializer = new HornSHOIF_Materializer_KoncludeOptimized(orarOntology, intPort);
				break;
			}
			// if
			// (orarOntology.getActualDLConstructors().contains(DLConstructor.NOMINAL))
			// {
			// materializer = new HornSHOIF_Materializer_Konclude(orarOntology,
			// intPort);
			// } else {
			// materializer = new HornSHIF_Materializer_Konclude(orarOntology,
			// intPort);
			// }
		}

		return materializer;

	}

	static private boolean argumentsAreValid(CommandLine commandLine) {
		if (commandLine.hasOption(Argument.HELP)) {
			return true;
		}
		if (commandLine.hasOption(Argument.ONTOLOGY)) {
			if (commandLine.hasOption(Argument.TBOX) || commandLine.hasOption(Argument.ABOX)) {
				System.err.print("More than one ways to read the input ontology. Please choose either -"
						+ Argument.ONTOLOGY + " or -" + Argument.TBOX + " -" + Argument.ABOX);
				return false;
			}
		}

		if (!commandLine.hasOption(Argument.ONTOLOGY)
				&& !(commandLine.hasOption(Argument.TBOX) && commandLine.hasOption(Argument.ABOX))) {
			// System.err.print("Input ontology arguments are missing. Please
			// choose either -" + Argument.ONTOLOGY
			// + " or -" + Argument.TBOX + " -" + Argument.ABOX);
			System.err.print("Input ontology arguments are missing");
			return false;

		}

		if (!Argument.reasonerList.contains(commandLine.getOptionValue(Argument.REASONER))) {
			System.err.print("Please choose correct name of the owlreasoner, choose among " + Argument.reasonerList);
			return false;
		}

		if (commandLine.getOptionValue(Argument.REASONER).equals(Argument.KONCLUDE)) {
			if (!commandLine.hasOption(Argument.KONCLUDEPATH) || !commandLine.hasOption(Argument.PORT)) {
				System.err.print("Konclude needs to has Path and Port");
				return false;
			}
		}

		if (commandLine.hasOption(Argument.SPLITTING)) {
			String typePerOntString = commandLine.getOptionValue(Argument.SPLITTING);
			try {
				Integer.parseInt(typePerOntString);
			} catch (NumberFormatException ex) {
				System.err.print("argument for -splitting is not an integer number");
				return false;
			}
		}

		if (commandLine.hasOption(Argument.OUTPUTABOX)) {
			String aboxFile = commandLine.getOptionValue(Argument.OUTPUTABOX);
			if (aboxFile.contains(" ")) {
				System.err.print("The output ABox file name contains space.");
				return false;
			}
		}

		return true;
	}

	static private void setConfigWithBooleanArguments(CommandLine commandLine) {
		if (commandLine.hasOption(Argument.LOADING_TIME)) {
			config.addLoginfoLevels(LogInfo.LOADING_TIME);
		}

		if (commandLine.hasOption(Argument.REASONING_TIME)) {
			config.addLoginfoLevels(LogInfo.REASONING_TIME);
		}

		if (commandLine.hasOption(Argument.STATISTIC)) {
			config.addLoginfoLevels(LogInfo.STATISTIC);
		}

		if (commandLine.hasOption(Argument.PRINT_RESULT)) {
			config.addLoginfoLevels(LogInfo.PRINT_RESULT);
		}

		if (commandLine.hasOption(Argument.PERFORMANCE)) {
			config.addLoginfoLevels(LogInfo.TIME_STAMP_FOR_EACH_STEP);
		}

		if (commandLine.hasOption(Argument.SPLITTING)) {
			String typePerOntString = commandLine.getOptionValue(Argument.SPLITTING);

			int typePerAbstractOnt = Integer.parseInt(typePerOntString);
			config.setNumberOfTypePerOntology(typePerAbstractOnt);

		}
	}

	static private OrarOntology2 getOrarOntology(CommandLine commandLine) {
		OrarOntology2 orarOntology;

		OntologyReader ontReader = new HornSHOIF_OntologyReader();

		if (commandLine.hasOption(Argument.ONTOLOGY)) {
			String owlFilePath = commandLine.getOptionValue(Argument.ONTOLOGY);
			// TODO: fix me. I just ignore this case by returning an empty
			// ontology.
			// orarOntology = ontReader.getNormalizedOrarOntology(owlFilePath);
			orarOntology = new MapbasedOrarOntology2();
		} else {
			String tboxFile = commandLine.getOptionValue(Argument.TBOX);
			String aboxList = commandLine.getOptionValue(Argument.ABOX);
			orarOntology = ontReader.getNormalizedOrarOntology(tboxFile, aboxList);
		}
		totalLoadingTime += ontReader.getLoadingTime();
		return orarOntology;
	}
}