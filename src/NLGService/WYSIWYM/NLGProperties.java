package NLGService.WYSIWYM;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

public class NLGProperties {

private static String file = "nlgService.properties";
private static String baseFolder = "./"; //NLGProperties.class.getClassLoader().getResource("WYSIWYM/").getPath();
private static Configuration values = null;

    public static synchronized void init() {    	
			try {
				values = new PropertiesConfiguration(baseFolder+file);
//				System.out.println(Thread.currentThread().getContextClassLoader().getResourceAsStream("filename.properties"));
			} catch (Exception e) {
				e.printStackTrace();
			} 
    }
    
    public static synchronized Configuration getValues() {
    	
    	if(values == null) {
    		init();
        }
    	return values;
    }
    
    public static String getString(String propertyName){
    	if(values == null) {
    		init();
        }
    	return values.getString(propertyName);
    }

	public static String getFile() {
		return file;
	}

	public static void setFile(String file) {
		NLGProperties.file = file;
	}

	public static String getBaseFolder() {
		return baseFolder;
	}

	public static void setBaseFolder(String baseFolder) {
		NLGProperties.baseFolder = baseFolder;
	}
    
}