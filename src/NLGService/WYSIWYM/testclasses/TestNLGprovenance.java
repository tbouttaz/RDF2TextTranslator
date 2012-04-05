package NLGService.WYSIWYM.testclasses;

import NLGService.WYSIWYM.ontology.OntologyReader;
import NLGService.WYSIWYM.ontology.OntologyWriter;
import NLGService.WYSIWYM.ontology.SesameReader;
import NLGService.WYSIWYM.transformer.AutomaticGenerator;
import NLGService.WYSIWYM.transformer.SemanticGraphTransformer;

import com.hp.hpl.jena.ontology.OntModel;

public class TestNLGprovenance {

	//the artifact "Grid Diagram"
// 	public static final String RESOURCE_ID = "http://openprovenance.org/ontology#dbd1f00b-0772-42af-8d68-5694abbe225a";	
 	
 	//the paper "Paper for ESWC 2012"
 	public static final String RESOURCE_ID = "http://openprovenance.org/ontology#11ce67d6-182b-4b62-8c29-fd952ab43dfb";

 	// the Person Thomas Bouttaz
// 	public static final String RESOURCE_ID = "http://xmlns.com/foaf/0.1/#b2d48d88-d78f-418d-aa46-6b7419882ccd";

 	//The edit process "Editing of a resource"
// 	public static final String RESOURCE_ID = "http://www.policygrid.org/ourspacesVRE.owl#44dec455-620c-4f77-a5c1-69cf0838997c";	 	
 	
 	private static OntologyReader ontoReader;
 	private static SesameReader sesameReader;
 	private static TextTypesGenerator generator;

	public static void main(String[] args) {
		try {
			ontoReader = new OntologyReader();
			sesameReader = new SesameReader(false);
		} catch (Exception e) {
			e.printStackTrace();
		}

 		AutomaticGenerator automaticGenerator = new AutomaticGenerator(ontoReader, sesameReader);
		SemanticGraphTransformer sgt;

//		OntModel jenaModel = sesameReader.getJenaModel(RESOURCE_ID);			
//		
//		sgt = automaticGenerator.buildGraphFromJenaModel(jenaModel, RESOURCE_ID);
		OntModel jenaModel = sesameReader.getJenaModelFromSparqlQuery(OntologyWriter.getInferredProvenanceQuery(RESOURCE_ID));

		AutomaticGenerator autoGenerator = new AutomaticGenerator(ontoReader, sesameReader);
		sgt = autoGenerator.buildGraphFromJenaModel(jenaModel, RESOURCE_ID);

		try {
			generator = new TextTypesGenerator(sgt, ontoReader, sesameReader, "x");
			generator.getSurfaceText();
			System.out.println("Condition 3: " + generator.getText(3));
		} catch (Exception e) {
			e.printStackTrace();
		}


	}
}
