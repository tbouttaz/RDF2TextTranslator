package NLGService.test;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import NLGService.WYSIWYM.ontology.OntologyReader;
import NLGService.WYSIWYM.util.OntologyInputException;

import com.hp.hpl.jena.ontology.OntProperty;


public class OntologyReaderTest extends TestCase{

	public static OntologyReader ontologyReader;
	
	@Before
	public void setUp() {
		try {
			ontologyReader = new OntologyReader();
		} catch (OntologyInputException e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	public void testGetProperty() {
		OntProperty property = ontologyReader.getProperty("Title");
		property = ontologyReader.getProperty("title");
		assertEquals("failed to retrieve a property name", "title", property.getLocalName());
	}
	
	@After
	public void tearDown(){
		ontologyReader = null;
	}
}
