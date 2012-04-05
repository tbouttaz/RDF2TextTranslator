package NLGService.WYSIWYM.testclasses;

import NLGService.WYSIWYM.ontology.OntologyReader;
import NLGService.WYSIWYM.ontology.SesameReader;
import NLGService.WYSIWYM.transformer.AutomaticGenerator;
import NLGService.WYSIWYM.transformer.SemanticGraphTransformer;
import NLGService.liber.AnchorInfo;

import com.hp.hpl.jena.ontology.OntModel;

/**
 * Class to create Sesame and Ontology readers and a TextTypesGenerator that is gonna generate the text.
 * Contains differant methods to retrieve the generated feedback text from a sesame resource
 * @author thomas
 *
 */
public class TextGeneratorUtils {

 	private static OntologyReader reader;
 	private static SesameReader sesame;
 	private static TextTypesGenerator generator;
 	private static AutomaticGenerator ag;
 	
 	public TextGeneratorUtils() {
		try {
			reader = new OntologyReader();
			sesame = new SesameReader(false);
			ag = new AutomaticGenerator(reader, sesame);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

 	/*
 	 * TODO: method to return the expandable text (AnchorInfo[] returned form generator.getSurfaceText()  
 	 */
 	
 	public static AnchorInfo[] getAnchorFeedbackText(OntologyReader ontReader, SesameReader sesameReader, OntModel jenaModel, String resourceID) {
 		AutomaticGenerator autoGenerator = new AutomaticGenerator(ontReader, sesameReader);
 		try {
			SemanticGraphTransformer sgt = autoGenerator.buildGraphFromSesameRepo("user",resourceID);
	 		autoGenerator.buildGraphFromJenaModel(jenaModel, resourceID);
	 		
	 		TextTypesGenerator generator = new TextTypesGenerator(sgt, ontReader, sesameReader, "user");
//	 		generator.getSurfaceText().toString();
	 		return generator.getSurfaceText();
 		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
 	
 	/**
 	 * Generate the feedback text from the Jena Model
 	 * @param jenaModel
 	 * @param resourceID
 	 * @return the feedback etxt representing this resource
 	 */
 	public String getTextualDescription(OntModel jenaModel, String resourceID){
 		try {
			SemanticGraphTransformer sgt = ag.buildGraphFromSesameRepo("user",resourceID);
			ag.buildGraphFromJenaModel(jenaModel, resourceID);
			generator = new TextTypesGenerator(sgt, reader, sesame, "user");
			generator.getSurfaceText().toString();
			return generator.getText(3);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error when trying to generate the feedback text from the Jena Model");
		}
 		return null;
 	}
 	
 	/**
 	 * return the Feedback text from the ID of a sesame resource
 	 * @param resourceID sesame resource ID
 	 * @return the feedback text describing that resource
 	 */
 	public static String getTextualDescription(String resourceID){
		SemanticGraphTransformer sgt;
		try {
			reader = new OntologyReader();
			sesame = new SesameReader(false);
			ag = new AutomaticGenerator(reader, sesame);

			sgt = ag.buildGraphFromSesameRepo("user",resourceID);
			generator = new TextTypesGenerator(sgt, reader, sesame, "user");
			generator.getSurfaceText().toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "Error when generating the textual description";
		}

		return generator.getText(3);
 	}
 	
	/**
	 * Generate a Jema OntModel corresponding to the given sesame resource ID
	 * @param resourceID the sesame resource ID
	 * @return Jena model of that resource
	 */
 	public OntModel getJenaModel(String resourceID) {
 		return sesame.getJenaModel(resourceID);
	}
 	
	/**
	 * Reinitialise the SemanticGraphTransform (sgt) and rebuild it so that it corresponds to the given Jena model
	 * @param jenaModel jena model that contains the graph required for the sgt
	 * @return whether informations were added to the root node of the sgt
	 */
//	public boolean modifyGraph(OntModel jenaModel, String resourceID){
//		return ag.buildGraphFromJenaModel(jenaModel, resourceID);
//	}
}
