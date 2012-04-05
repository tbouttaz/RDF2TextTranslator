package NLGService.WYSIWYM.testclasses;

import com.hp.hpl.jena.ontology.OntModel;

import NLGService.WYSIWYM.ontology.OntologyReader;
import NLGService.WYSIWYM.ontology.SesameReader;
import NLGService.WYSIWYM.transformer.AutomaticGenerator;
import NLGService.WYSIWYM.transformer.SemanticGraphTransformer;

public class TestTextGeneratorUtils {
 	private static OntologyReader reader;
 	private static SesameReader sesame;
 	private static TextTypesGenerator generator;
 	private static AutomaticGenerator ag;

 	public static final String RESOURCE_ID = "http://www.policygrid.org/project.owl#aadb9597-6392-42e0-9815-489cbe80cd19";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		TextGeneratorUtils textGeneratorUtils = new TextGeneratorUtils();
//		System.out.println(TextGeneratorUtils.getTextualDescription("http://openprovenance.org/ontology#539166d0-6dba-4224-9cc4-61a058Inter"));
		
		SemanticGraphTransformer sgt;
		try {
			reader = new OntologyReader();
			sesame = new SesameReader(false);
			ag = new AutomaticGenerator(reader, sesame);

			
			OntModel jenaModel = sesame.getJenaModel(RESOURCE_ID);
			sgt = ag.buildGraphFromJenaModel(jenaModel, RESOURCE_ID);

			generator = new TextTypesGenerator(sgt, reader, sesame, "user");
			generator.getSurfaceText().toString();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error when generating the textual description");
		}

		System.out.println(generator.getText(3));

	}

}
