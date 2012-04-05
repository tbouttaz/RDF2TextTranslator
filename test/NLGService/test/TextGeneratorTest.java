package NLGService.test;


import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import NLGService.WYSIWYM.ontology.OntologyReader;
import NLGService.WYSIWYM.ontology.SesameReader;
import NLGService.WYSIWYM.testclasses.TextTypesGenerator;
import NLGService.WYSIWYM.transformer.AutomaticGenerator;
import NLGService.WYSIWYM.transformer.SemanticGraphTransformer;
import NLGService.WYSIWYM.util.SesameException;
import NLGService.WYSIWYM.util.SurfaceRealisationException;
import NLGService.WYSIWYM.util.TextPlanningException;

import com.hp.hpl.jena.ontology.OntModel;

public class TextGeneratorTest extends TestCase{

 	private static OntologyReader reader;
 	private static SesameReader sesame;
 	private static TextTypesGenerator generator;
 	private static AutomaticGenerator ag;

 	public static final String RESOURCE_ID = "http://www.policygrid.org/project.owl#aadb9597-6392-42e0-9815-489cbe80cd19";
 	
	@Before
	public void setUp() throws Exception {
		reader = new OntologyReader();
		sesame = new SesameReader(false);
		ag = new AutomaticGenerator(reader, sesame);
	}

	@Test
	public void testSesameReader() {
		SemanticGraphTransformer sgt;
		
		OntModel jenaModel = sesame.getJenaModel(RESOURCE_ID);
		sgt = ag.buildGraphFromJenaModel(jenaModel, RESOURCE_ID);

		try {
			generator = new TextTypesGenerator(sgt, reader, sesame, "user");
		} catch (SesameException e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
		
		try {
			String generatedText = generator.getSurfaceText().toString();
			assertNotNull("The text generated is null", generatedText);
		} catch (SurfaceRealisationException e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		} catch (TextPlanningException e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}

	}
	
	@After
	public void tearDown() throws Exception {
	 	reader = null;
	 	sesame = null;
	 	generator = null;
	 	ag = null;
	}

}
