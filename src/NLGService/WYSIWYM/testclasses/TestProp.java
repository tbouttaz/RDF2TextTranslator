package NLGService.WYSIWYM.testclasses;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import NLGService.WYSIWYM.NLGProperties;

public class TestProp {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

//		NLGProperties.init();
//		NLGProperties.setBaseFolder(TestProp.class.get)
//		String path = NLGProperties.class.getProtectionDomain().getCodeSource().getLocation().getPath();
//		try {
//			String decodedPath = URLDecoder.decode(path, "UTF-8");
//			System.out.println("path:" + decodedPath);
//			NLGProperties.setBaseFolder(TestProp.class.getClassLoader().getResource("WYSIWYM/").getPath());
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.out.println("getresour: " + TestProp.class.getClassLoader().getResource("WYSIWYM/").getPath());
		System.out.println(NLGProperties.getString("lexiconPath"));
		
	}

}
