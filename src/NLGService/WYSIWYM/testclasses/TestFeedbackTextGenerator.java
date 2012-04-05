package NLGService.WYSIWYM.testclasses;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import NLGService.WYSIWYM.model.Anchor;
import NLGService.WYSIWYM.model.SGEdge;
import NLGService.WYSIWYM.model.SGNode;
import NLGService.WYSIWYM.model.SummationAnchor;
import NLGService.WYSIWYM.ontology.OntologyReader;
import NLGService.WYSIWYM.ontology.SesameReader;
import NLGService.WYSIWYM.transformer.AutomaticGenerator;
import NLGService.WYSIWYM.transformer.SemanticGraphTransformer;
import NLGService.WYSIWYM.util.SesameException;
import NLGService.WYSIWYM.util.SurfaceRealisationException;
import NLGService.WYSIWYM.util.TextPlanningException;
import NLGService.liber.AnchorInfo;
import NLGService.simplenlg.realiser.AnchorString;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Statement;

public class TestFeedbackTextGenerator {

 	private static OntologyReader reader;
 	private static SesameReader sesame;
 	private static TextTypesGenerator generator;

 	// RDF file containing the policy rule to apply to the model
    private static final String POLICY_ONTOLOGY = "/Users/thomas/Documents/workspace/OurSpaces/trunk/web/NLGTest/SpinRules/ConstraintViolTranscriptedBy.rdf";

// 	public static final String RESOURCE_ID = "http://xmlns.com/foaf/0.1/#7771aed1-c59c-488f-b9b7-324f72be6642";
// 	public static final String RESOURCE_ID = "http://openprovenance.org/ontology#e535f7fb-7322-446b-8106-9f211a8cff3c";
 	// the Transcript "Interview Transcript Subject 1"
// 	public static final String RESOURCE_ID = "http://openprovenance.org/ontology#539166d0-6dba-4224-9cc4-61a0581b0bf2";
// 	public static final String RESOURCE_ID = "http://xmlns.com/foaf/0.1/#0c8d01df-1d7a-4a2c-8298-b5e6fbb1aa9c";
 	// the Person Thomas Bouttaz
 	public static final String RESOURCE_ID = "http://xmlns.com/foaf/0.1/#b2d48d88-d78f-418d-aa46-6b7419882ccd";
    // the account Thomas Bouttaz
//    public static final String RESOURCE_ID = "http://www.policygrid.org/ourspacesVRE.owl#24bc8402-6639-4f60-b766-f51af003ea5c";
// 	public static final String RESOURCE_ID = "http://openprovenance.org/ontology#e535f7fb-7322-446b-8106-9f211a8cff3c";
// 	public static final String RESOURCE_ID = "http://openprovenance.org/ontology#93a1ec5a-65d0-4be5-a377-80ecc630cffb";
// 	public static final String RESOURCE_ID = "http://xmlns.com/foaf/0.1/#ec272513-360a-467a-8f43-0e951d1243db";
 	// Project: "SASWeb paper" 
// 	public static final String RESOURCE_ID = "http://www.policygrid.org/project.owl#0b84229a-11f3-4d21-a436-afa8d98a0a12";
 	// Paper: "SASWeb paper (1st draft)"
// 	public static final String RESOURCE_ID = "http://openprovenance.org/ontology#5b217b52-93ea-4fe2-91b0-fb704b957b67";
    // Project: policyGrid II
// 	public static final String RESOURCE_ID = "http://www.policygrid.org/project.owl#aadb9597-6392-42e0-9815-489cbe80cd19";
    // Communication: NLG workshop
// 	public static final String RESOURCE_ID = "http://openprovenance.org/ontology#8100bfa8-141b-4390-bfc6-8acfd5d6a035";
    // Interview
// 	public static final String RESOURCE_ID = "http://openprovenance.org/ontology#539166d0-6dba-4224-9cc4-61a058Inter";
 	// Feature MacRobert building
// 	public static final String RESOURCE_ID = "http://www.geonames.org/ontology#0224b681-9e2f-44ba-b2e3-4789eb5f2232";
 	// the post ENLG worshop
// 	public static final String RESOURCE_ID = "http://rdfs.org/sioc/ns#af1454ae-5e92-4779-bb74-504c36ff2f0c";
    // the post "More aabout D2R":
// 	public static final String RESOURCE_ID = "http://rdfs.org/sioc/ns#433643d4-290b-473d-abbb-fea35d89f088";

// 	public static final String RESOURCE_ID = "http://openprovenance.org/ontology#74d09d40-107c-46f9-ac43-25b646e20061";
  
 	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			reader = new OntologyReader();
			sesame = new SesameReader(false);
		} catch (Exception e) {
			e.printStackTrace();
		}

 		AutomaticGenerator automaticGenerator = new AutomaticGenerator(reader, sesame);
		SemanticGraphTransformer sgt;
		try {
			
			//Test Jena Model
			OntModel jenaModel = sesame.getJenaModel(RESOURCE_ID);			

		    // Reading Policy ontology and add it to the jena model
			FileInputStream in = null;
			try {
				in = new FileInputStream(POLICY_ONTOLOGY);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
	        jenaModel.read(in, "");
	        
			System.out.println("*** Jena Model before Spin rule ***");
			jenaModel.write(System.out);
	        
			// add a constraint to Jena model liking the Transcript to the Construct
	        //TODO: needs to be changed --> how do we the Construct that is related to the transcript? it's the construct that has a Where --> Artifact a Transcript (try to add ?q sp:where)
//		    Query addConstraintQuery = QueryFactory.create("PREFIX pggen:<http://www.policygrid.org/provenance-generic.owl#>" +
//    		"CONSTRUCT { pggen:Transcript <http://spinrdf.org/spin#constraint> ?q . } WHERE { ?q a <http://spinrdf.org/sp#Construct> .  }") ;
//
//			System.out.println("*** SPIN Rule Query ***");
//	        System.out.println(addConstraintQuery.toString());
//	        
//	        // Execute query adding SPIN rule to the model:
//	        QueryExecution cqexec = QueryExecutionFactory.create(addConstraintQuery, jenaModel) ;
//			Model addConstraintModel = cqexec.execConstruct();
//			cqexec.close() ;
//
//			System.out.println("*********** SPIN Rule Model ***********");
//			addConstraintModel.write(System.out, "N-TRIPLE");
//
//			// add constraints to the Jena model:
//			jenaModel.add(addConstraintModel, true);
			//TODO OR jenaModel.addSubModel(spinRuleModel);
			
			
			/* Workaround for removing violation paths */
			// For each ConstraintViolation in the jenaModel, remove the statement corresponding to the violationPath:
			// Copy jenaModel to be able to iterate the model and change it at the same time:
//			OntModel jenaModelCopy = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
//			jenaModelCopy.add(jenaModel);
//			StmtIterator it = jenaModelCopy.listStatements(null, RDF.type, SPIN.ConstraintViolation);
//			while (it.hasNext()) {
//				Statement violStmnt = (Statement) it.next();
//				Resource subject = violStmnt.getSubject();
//				
//				Statement violRoot = subject.getProperty(SPIN.violationRoot);
//				System.out.println("violRoot: " + violRoot.toString());
//
//				// getProperty() only returns one of the violationPath (not all of them) --> use listStatements()
////				Statement violPath = subject.getProperty(SPIN.violationPath);
//				Collection<Property> violPathPropList = new ArrayList<Property>();
//				StmtIterator violPathStmtIter = jenaModel.listStatements(subject, SPIN.violationPath, (Property) null);
//				while (violPathStmtIter.hasNext()) {
//					Statement violPathStmt = (Statement) violPathStmtIter.next();
//					System.out.println("violPath: " + violPathStmt);
//					violPathPropList.add(jenaModel.getProperty(violPathStmt.getObject().toString()));
//				}
//				
//				// Remove every violation path from model:
//				for (Property property : violPathPropList) {
//					jenaModel.removeAll((Resource) violRoot.getObject(), property, (RDFNode) null);
//				}
//			}
			
			
			/* Inferences */
			// Create and add Model for inferred triples
//			Model newTriples = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
//			jenaModel.addSubModel(newTriples);
//
//			// Register locally defined functions:
//			SPINModuleRegistry.get().registerAll(jenaModel);
//			
//			// Run all inferences:
//			SPINInferences.run(jenaModel, newTriples, null, null, false, null);
//			
//			System.out.println("Inferred triples: " + newTriples.size());
//			newTriples.write(System.out,"N-TRIPLE");

			
			// Run constraints:
//			List<ConstraintViolation> constraintViolationList = SPINConstraints.check(jenaModel, null);
//			
//			// Process each Constraint Violation:
//			System.out.println("*********** Constraint Violation Detected ***********");
//			int k = 1;
//			for (ConstraintViolation constraintViolation : constraintViolationList) {
//				System.out.println("constraint " + k + " : " + constraintViolation.getMessage());
//
//				// Retrieve information from the constraint:
//				Resource violationRoot = constraintViolation.getRoot();
//				Collection<SimplePropertyPath> violationPathList = constraintViolation.getPaths();
//				
//				// For each property violated, remove that information from the model:
//				for (SimplePropertyPath violationPath : violationPathList) {
//					System.out.println("Path: " + violationPath.toString());
//					jenaModel.removeAll(violationRoot, violationPath.getPredicate(), null);
//				}
//				k++;
//			}
//			
//			System.out.println("*** Jena Model after applying constraints ***");
//			jenaModel.write(System.out);
//			
//			System.out.println("*********** Jena Statements ***********");
//			StmtIterator jenaModelListStatements = jenaModel.listStatements();
//			int i = 1;
//			while (jenaModelListStatements.hasNext()) {
//				Statement statement = (Statement) jenaModelListStatements.next();
//				System.out.println("Statement " + i + " : " + statement.toString());
//				i++;
//			}
			
			
//			ontModel.write(System.out);
//			sgt = automaticGenerator.getObjectInformation("user",RESOURCE_ID);
//			sgt = automaticGenerator.buildGraphFromSesameRepo("user",RESOURCE_ID);

			Statement resourceType = jenaModel.getResource(RESOURCE_ID).getProperty(jenaModel.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
//			String resourcePageURL = Utility.getDetailPage(resourceType.toString(), RESOURCE_ID);
			String resClass = jenaModel.getOntClass(RESOURCE_ID).toString();

			// After reasoning on the JenaModel, we re-generate the liber model to NLG:
			sgt = automaticGenerator.buildGraphFromJenaModel(jenaModel, RESOURCE_ID);

			generator = new TextTypesGenerator(sgt, reader, sesame, "user");
			generator.setAggregationThreshold(20);
			
			sgt.printGraph();

		} catch (SesameException e) {
			e.printStackTrace();
		}
		
		// Display the generated feedback text: (need to call getSurfaceText() first)
		try {
//			AnchorInfo[] anchorFeedbackText = new AnchorInfo[1];  
			AnchorInfo[] anchorFeedbackText = generator.getSurfaceText();
			
			String htmlFeedbackText = "<div id=\"" + RESOURCE_ID +"\" text-decoration:none> <p>";
			String expandableTextDivs = "";
			// Counter for related resources
			int relatedResourcesNb = 0;

			// Iterate all parts of the feedback text:
			for (AnchorInfo anchorInfo : anchorFeedbackText) {
				String anchorInfoWords = anchorInfo.getWords();
				if (anchorInfo.isSummation()) {
					generator.showSummation(anchorInfo.getID());
//					try {
//						generator.changeTextContent(anchorInfo.getID(), true);
//					} catch (SesameException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					Anchor anchor = generator.getText().getAnchor(anchorInfo.getID());
//					anchor.getNode().getOutgoingEdgeNr();
					/* create an href on anchorInfoWords that will show/hide the edges of that anchor in a <div>*/
					
					/* for each edges, get the associated anchorString and add them to the <div> with hrefs to other resources*/
					if (anchor instanceof SummationAnchor) {
						/*TODO: Try to get every AnchorStrings associated with that SummationAnchor?*/
						List<SGEdge> edges = ((SummationAnchor) anchor).getEdges();
						for (SGEdge sgEdge : edges) {
							SGNode target = sgEdge.getTarget();
							target.getAnchor();
							target.getUniqueID();
							//Get anchorInfo from anchor: look in the list of anchorString for the one that have that anchor
							AnchorString anchorStringFromNode = generator.getText().getAnchorStringsFromNode(target.getID());
//							for (AnchorString anchorString : anchorStringsFromNode) {
								System.out.println("anchorString: " + anchorStringFromNode);
//							}
							System.out.println("agragated node ID: " + ((SGNode) generator.getGraph().getNode(target.getID())).getUniqueID());
						}
					}
//					generator.setGraphRootNode(anchorInfo.getID());
//					generator.getSurfaceText();
				}
				
				// If this anchor contains informations:
				if (anchorInfoWords != null && !"".equals(anchorInfoWords) && !"The x:".equals(anchorInfoWords)) {
					
					// If this part of the text refers to another resource, create a link to that resource:
					if (anchorInfo.isAnchor()) {
						
						Anchor anchor = generator.getText().getAnchor(anchorInfo.getID());

						//TODO: problem with ourSpacesVRE.owl#hasContact property: anchor.getNode() is null so can't create links to those other resources
						if (anchorInfo.getID() == null || "".equals(anchorInfo.getID()) || anchor.getNode() == null) {
							System.out.println("No ID for the node " + anchor.getID());
							htmlFeedbackText += anchorInfoWords;
							continue;
						}

						// Retrieve the URI of this resource:
						String anchoredResourceURI = ((SGNode) generator.getGraph().getNode(anchor.getNode().getID())).getUniqueID();
						
						// If that resource is the resource we're already describing, don't create an anchor:
						if (anchoredResourceURI.contains(RESOURCE_ID)) {
							htmlFeedbackText += "<strong>" + anchorInfoWords + "</strong>";
							
						// Otherwise, create an anchor to that other resource:
						} else {
							// Convert # to its ASCII code (%23) so it can be sent as a parameter with a GET (use URLConverter instead?)
							try {
								anchoredResourceURI = URLEncoder.encode(anchoredResourceURI, "ISO-8859-1");
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							// Create an ID for the related resource: System current time + related resource ID (see BUG: can't expend twice on the same resource):
							String anchoredResourceID = /*resourceID.substring(resourceID.indexOf("#") + 1)*/ System.currentTimeMillis() + anchoredResourceURI.substring(anchoredResourceURI.indexOf("%23") + 3);
							htmlFeedbackText += "<a style=\"text-decoration:underline;\" href=\"" + anchoredResourceURI + "\" rel=\"" + anchoredResourceID + "\" class=\"liber\">" + anchorInfoWords + "</a>";
							
							// add a div where the text about this linked resource will be added
							expandableTextDivs += "<div id=\"" + anchoredResourceID + "\"></div>";
							
							relatedResourcesNb++;
						}
						
					} else {
						//Otherwise, just add the text without anchor:
						htmlFeedbackText += anchorInfoWords;
					}
				}
			}

		System.out.println("htmlFeedbackText: " + htmlFeedbackText);

			
			
		} catch (SurfaceRealisationException e) {
			e.printStackTrace();
		} catch (TextPlanningException e) {
			e.printStackTrace();
		}
//		System.out.println("Condition 1: " + generator.getText(1));
//		System.out.println("Condition 2: " + generator.getText(2));
		System.out.println("Condition 3: " + generator.getText(3));
		
		
		FileWriter timeStatFile;
		try {
			timeStatFile = new FileWriter("./test123.txt");
			timeStatFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
