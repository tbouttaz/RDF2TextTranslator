package NLGService.WYSIWYM.testclasses;

public class TestTextGeneratorUtils {

 	public static String RESOURCE_ID = "http://www.policygrid.org/project.owl#aadb9597-6392-42e0-9815-489cbe80cd19";

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (args.length !=0 && args[0] != null) {
			RESOURCE_ID = args[0];
		} else {
			System.out.println("No resource URI provided. Generating Description of the PolicyGrid project...");
		}

		System.out.println(TextGeneratorUtils.getTextualDescription(RESOURCE_ID));
	}

}
