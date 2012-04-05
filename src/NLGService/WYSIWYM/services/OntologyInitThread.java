package NLGService.WYSIWYM.services;

import javax.servlet.ServletContext;

import NLGService.WYSIWYM.ontology.OntologyReader;
import NLGService.WYSIWYM.ontology.SesameReader;
import NLGService.WYSIWYM.util.OntologyInputException;
import NLGService.WYSIWYM.util.SesameException;

public class OntologyInitThread extends Thread{

	ServletContext context;
	
	public OntologyInitThread(ServletContext context) {
		this.context = context;
	}
	
	@Override
	public void run() {
		
		super.run();
		
		OntologyReader ontReader = null;

		System.out.println("*** Initialising NLG service ***");
		
		try {
			ontReader = new OntologyReader();
			context.setAttribute("OntologyReader", ontReader);
		} catch (OntologyInputException e) {
			e.printStackTrace();
		} 

		SesameReader sesameReader = null;

		try {
			sesameReader = new SesameReader(false);
			context.setAttribute("SesameReader", sesameReader);
		} catch (SesameException e) {
			e.printStackTrace();
		}
		System.out.println(" *** Ontology and Sesame Readers Initialised *** ");
	}
}
