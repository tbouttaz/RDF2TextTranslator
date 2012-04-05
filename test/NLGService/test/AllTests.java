package NLGService.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		//$JUnit-BEGIN$
		suite.addTestSuite(SesameReaderTest.class);
		suite.addTestSuite(OntologyReaderTest.class);
		suite.addTestSuite(TextGeneratorTest.class);
		//$JUnit-END$
		return suite;
	}

}
