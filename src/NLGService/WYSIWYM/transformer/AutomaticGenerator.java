package NLGService.WYSIWYM.transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.naming.NameAlreadyBoundException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.GraphQueryResult;

import NLGService.WYSIWYM.NLGProperties;
import NLGService.WYSIWYM.libraries.Lexicon;
import NLGService.WYSIWYM.model.DatatypeNode;
import NLGService.WYSIWYM.model.Edge;
import NLGService.WYSIWYM.model.QueryEdge;
import NLGService.WYSIWYM.model.QueryResultGraph;
import NLGService.WYSIWYM.model.QueryValueNode;
import NLGService.WYSIWYM.model.SGAbstractNode;
import NLGService.WYSIWYM.model.SGAddressNode;
import NLGService.WYSIWYM.model.SGBooleanNode;
import NLGService.WYSIWYM.model.SGDateNode;
import NLGService.WYSIWYM.model.SGDoubleNode;
import NLGService.WYSIWYM.model.SGEdge;
import NLGService.WYSIWYM.model.SGIntNode;
import NLGService.WYSIWYM.model.SGNode;
import NLGService.WYSIWYM.model.SGStringNode;
import NLGService.WYSIWYM.model.SemanticGraph;
import NLGService.WYSIWYM.ontology.OntologyReader;
import NLGService.WYSIWYM.ontology.OntologyWriter;
import NLGService.WYSIWYM.ontology.QueryResult;
import NLGService.WYSIWYM.ontology.SesameReader;
import NLGService.WYSIWYM.util.SesameException;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**	AutomaticGenerator adds automatically generated information to the semantic graph,
 *	such as the date of deposit and the depositor details. It also reads information from
 *	the Sesame archive about the user or objects the user has requested information about
 *	through browsing options, and adds it to the Graph. Finally it also retrieves information
 *	about search results and put it in QueryResultGraphs.
 *
 *	@author Feikje Hielkema
 *	@version 1.1 27-02-2007
 *
 *	@version 1.2 27-02-2008
 */
public class AutomaticGenerator
{
	private OntologyReader reader;
	private SesameReader sesame;
	private SemanticGraphTransformer sgt;
	/** Map with every SGNode in the SGT */
	private Map<String, SGNode> idMap = new HashMap<String, SGNode>();
	private HashMap<String, Element> classLabelMap = new HashMap<String, Element>();
	
	/**	Constructor, takes an ontology model and Sesame archive
	 *	@param r Ontology
	 *	@param s Sesame
	 */
	public AutomaticGenerator(OntologyReader r, SesameReader s)
	{
		reader = r;
		sesame = s;
	}
	
	/**	Retrieves the current date and stores it in an SGDateNode
	 *
	 *	@return	SGDateNode containing the current date
	 */
	public static SGDateNode getCurrentDate()
	{
		Calendar cal = new GregorianCalendar();
    	int year = cal.get(Calendar.YEAR);             // 2002
    	int month = cal.get(Calendar.MONTH);           // 0=Jan, 1=Feb, ...
    	month++;	//months in calendar run from 0 to 11, while we have 1 to 12
    	int day = cal.get(Calendar.DAY_OF_MONTH);      // 1...
    	
    	return new SGDateNode(day, month, year);
	}
	
	/**	Returns a unique identifier (concatenation of namespace and unique ID)
	 *	for a new resource to be added to the database
	 *	@param c OntResource
	 *	@return String with a unique id
	 */
	public static String getUniqueID(OntResource c)
	{
		StringBuffer sb = new StringBuffer(c.getNameSpace());
		sb.append(UUID.randomUUID().toString());
		return sb.toString();
	}
	
	/**	Expands the SemanticGraph with date of deposit and depositor.
	 *	@param sgt SemanticGraphTransformer
	 *	@param user SGNode representing user
	 */
	public static void expandGraph(SemanticGraphTransformer sgt, SGNode user)
	{
		expandGraph(sgt, user, (SGNode) sgt.getGraph().getRoot());
	}
	
	/**	Expands the SemanticGraph with date of deposit and depositor.
	 *	@param sgt SemanticGraphTransformer
	 *	@param user SGNode representing user
	 *	@param source Root SGNode of Graph
	 */
	public static void expandGraph(SemanticGraphTransformer sgt, SGNode user, SGNode source)
	{
		try
		{
			SGNode date = getCurrentDate();
			SGEdge dateEdge = sgt.makeEdge("DateOfDeposit", source, date);
			dateEdge.setRemovable(false);
			sgt.addEdge(dateEdge);
		
			SGEdge nameEdge = sgt.makeEdge("DepositedBy", source, user);
			nameEdge.setRemovable(false);
			sgt.addEdge(nameEdge);
		}
		catch (NameAlreadyBoundException e)
		{
			System.out.println("TextFrame 85: NameAlreadyBoundException should not be possible here!");
			e.printStackTrace();
		}
	}
	
	/**	Expands the SemanticGraph with date of deposit, depositor and project.
	 *	@param sgt SemanticGraphTransformer
	 *	@param user SGNode representing user
	 *	@param projectID Project Sesame ID
	 */
	public static void expandGraph(SemanticGraphTransformer sgt, SGNode user, String projectID)
	{
		expandGraph(sgt, user, sgt.getGraph().getRoot(), projectID);
	}
	
	/**	Expands the SemanticGraph with date of deposit, depositor and project.
	 *	@param sgt SemanticGraphTransformer
	 *	@param user SGNode representing user
	 *	@param source Root SGNode of Graph
	 *	@param projectID Project Sesame ID
	 */
	public static void expandGraph(SemanticGraphTransformer sgt, SGNode user, SGNode source, String projectID)
	{
		expandGraph(sgt, user, source);
		if (projectID != null)
		{
			try
			{
				SGNode project = sgt.getGraph().getUniqueIDNode(projectID);
				if (project == null)
					return;
				//LATER WE WANT TO DISTINGUISH BETWEEN DIFFERENT SUBPROPERTIES OF PRODUCEDINPROJECT
				SGEdge projectEdge = sgt.makeEdge("ProducedInProject", source, project);
				sgt.addEdge(projectEdge);
			}
			catch (NameAlreadyBoundException e)
			{
				System.out.println("TextFrame 104: NameAlreadyBoundException should not be possible here!");
				e.printStackTrace();
			}
		}
	}
	
	/**	Retrieves all information about this object from the archive and puts it in the SemanticGraph. (old method, only use if don't apply policies)
	 *	@param user Username
	 *	@param resourceID Sesame ID of object
	 *	@return SemanticGraphTransformer
	 *	@throws SesameException
	 */
	public SemanticGraphTransformer buildGraphFromSesameRepo(String user, String resourceID) throws SesameException {
		prepareGraph(user, resourceID);
		expandGraph(idMap.get(resourceID), resourceID, false, false);	//add all information about the user from the database
		return sgt;
	}
	
	/**
	 * Prepare the Semantic Graph Transformer (SGT) by creating an empty root node that will contain the resource being described
	 * @param user
	 * @param resourceID Sesame ID of the resource being described
	 * @return the SGT
	 * @throws SesameException
	 */
	public SemanticGraphTransformer prepareGraph(String user, String resourceID) throws SesameException {
		idMap = new HashMap<String, SGNode>();
		try {	//create a new node for this object
			SGNode rootNode = new SGNode("x");	
			sgt = new SemanticGraphTransformer(new SemanticGraph(user), reader);		
			SGNode idTarget = new SGStringNode(resourceID);	//add the ID
			SGEdge idEdge = sgt.makeEdge("ID", rootNode, idTarget);
			sgt.addEdge(idEdge);
			
			//TODO URI: retrieve the nameSpace from the ID of the resource with a sparql query:
//			String getTypeSparqlQuery = OntologyWriter.getTypeQuery(id);
//			QueryResult qr = sesame.queryBinding(getTypeSparqlQuery, id);
//			List<String> types = qr.getBindingValues(id, "type");
//			if (!types.isEmpty()) {
//				String uri = types.get(0);
//				result.setNameSpace(uri.substring( 0, Util.splitNamespace(uri)) );
//			}

			sgt.getGraph().setRoot(rootNode);	//and add it to the graph
			idMap.put(resourceID, rootNode);
			return sgt;
		}
		catch (NameAlreadyBoundException e) {
			return null;
		}	//impossible
	}

	
	/**	Return semantic graph with all information about a user; but without
	 *	anchors!
	 *	@param user Username
	 *	@param  id Sesame ID of object
	 *	@return SemanticGraphTransformer
	 *	@throws SesameException
	 */
	public SemanticGraphTransformer getUserInfoNoAnchors(String user, String id) throws SesameException
	{
		idMap = new HashMap<String, SGNode>();
		
		try
		{	//create a new node for this object
			SGNode result = new SGNode("Person");	
			sgt = new SemanticGraphTransformer(new SemanticGraph(user), reader);		
			SGNode idTarget = new SGStringNode(id);	//add the ID
			SGEdge idEdge = sgt.makeEdge("ID", result, idTarget);
			sgt.addEdge(idEdge);
			sgt.getGraph().setRoot(result);	//and add it to the graph
			idMap.put(id, result);
			expandGraph(result, id, false);	//add all information about the user from the database

			for (Iterator it = sgt.getGraph().getNodes(); it.hasNext(); )
			{
				SGNode node = (SGNode) it.next();
				node.setRealise(SGNode.SHOW);		//but ensure all nodes are shown
				node.setAnchor(null);				//and there are no anchors
			}
			return sgt;
		}
		catch (NameAlreadyBoundException e)
		{
			return null;
		}	//impossible
	}
	
	/**	Should retrieve all information about the currently logged in user from
	 *	the database. Not all this information needs to be presented in the text,
	 *	but it should be in the SemanticGraph to prevent the violation of cardinality
	 *	constraints etc.
	 *	@param username Username
	 *	@param userID User ID
	 *	@return SemanticGraphTransformer with user information
	 *	@throws SesameException
	 */
	public SemanticGraphTransformer getUserInfo(String username, String userID) throws SesameException
	{	
		idMap = new HashMap<String, SGNode>();
		
		try
		{	//create a new node for this user
			SGNode result = new SGNode("Person");	
			sgt = new SemanticGraphTransformer(new SemanticGraph(username), reader);	//result, username, reader);		
			SGNode idTarget = new SGStringNode(userID);	//add the ID
			SGEdge idEdge = sgt.makeEdge("ID", result, idTarget);
			sgt.addEdge(idEdge);
			sgt.getGraph().setRoot(result);	//and add it to the graph
			idMap.put(userID, result);

			if (expandGraph(result, userID, false))		//add all information about the user from the database
				result.setRealise(SGNode.HIDE);		//but don't put it in the text
	
			if (result.getOutgoingEdges("Name").size() == 0)
			{	//if the name isn't added yet (e.g. because this user is not yet in the database), do that now.
				SGNode nameNode = new SGStringNode(username);
				SGEdge edge = sgt.makeEdge("Name", result, nameNode);
				edge.setRemovable(false);
				sgt.addEdge(edge);
			}
		}
		catch (NameAlreadyBoundException e)
		{}	//impossible
		
		return sgt;
	}
	
	private boolean expandGraph(SGNode result, String id, boolean hasName)  throws SesameException {
		return expandGraph(result, id, hasName, true);
	}
	
	/**
	 * Expand the SGT from statements about the resource being described as subject, and statements about the type of resources related to the resource being described (to determine how to refer to them)
	 * @param rootNode root node of the SGT (corresponding to the resource being described)
	 * @param resourceID sesame ID of the resource being described
	 * @return true if some information has been added to the SGT 
	 */
	private boolean expandGraph(SGNode rootNode, String resourceID, boolean hasName, boolean hasType) throws SesameException {
		
		try {
			// Get every statements with ID as subject and statements about objects type:
			String query = OntologyWriter.getDescriptionAndTypesQuery(resourceID);
			GraphQueryResult graph = sesame.queryGraph(query);
			
			// Separating statements into 2 lists:
			Collection<Statement> resourceStatementList = new ArrayList<Statement>();
			Collection<Statement> relatedResourceStatementList = new ArrayList<Statement>();
			
			while (graph.hasNext()) {
				Statement statement = graph.next();
				// if the statement is about the resource being described:
				if (statement.getSubject().toString().contains(resourceID)) {
					resourceStatementList.add(statement);
					
				// if the statement is about the type of a related resource:
				} else if (statement.getObject() instanceof URI && OntologyWriter.TYPE_URI.equals(statement.getPredicate().toString()) ) {
					relatedResourceStatementList.add(statement);
				} else {
					System.out.println("the statement " + statement.toString() + "is neither about the resource that is being described or about a related resource. " +
							"This statement will be ignored.");
				}
			}
			
			// Process statements about the resource:
			boolean addedInfo = processResourceStatements(rootNode, hasType, resourceStatementList);

			// Process the statements about the related resources (to get their labels and class spec files)
			processRelatedResourceStatements(rootNode, relatedResourceStatementList);
						
			return addedInfo;
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new SesameException("Exception when retrieving data from the database about " + resourceID);
		}
	}
	
	/*TODO expandGraph with statements about resource as object of the statement (need to change SPARQL query) */
//	private boolean expandGraph(SGNode rootNode, String resourceID, boolean hasName, boolean hasType) throws SesameException {
//		
//		try {
//			// Get every statements with ID as subject and statements about objects type:
//			String query = OntologyWriter.getFullDescriptionAndTypesQuery(resourceID);
//			GraphQueryResult graph = sesame.queryGraph(query);
//			
//			//TODO: change query in OntologyWriter.getFullDescriptionAndTypesQuery() as it returns too many statements (or create another query just to get resource as object statements?)
//	
//			// Separating statements into 2 lists (query the repository once and then separate the statements into different lists is quicker than querying the repo several times):
//			Collection<Statement> resourceStatementList = new ArrayList<Statement>();
//			Collection<Statement> resourceObjectStatementList = new ArrayList<Statement>();
//			Collection<Statement> relatedResourceStatementList = new ArrayList<Statement>();
//			
//			while (graph.hasNext()) {
//				Statement statement = graph.next();
//				// if the statement is about the resource being described (i.e. the resource is the subject of the statement):
//				if (statement.getSubject().toString().contains(resourceID)) {
//					resourceStatementList.add(statement);
//					
//				} else if (statement.getObject().toString().contains(resourceID)) {
//					// if the statement is about the resource being described (i.e. the resource is the object of the statement): 
//					resourceObjectStatementList.add(statement);
//					
//				} else if (statement.getObject() instanceof URI && OntologyWriter.TYPE_URI.equals(statement.getPredicate().toString()) ) {
//					// if the statement is about the type of a related resource:
//					relatedResourceStatementList.add(statement);
//				} else {
//					System.out.println("the statement " + statement.toString() + "is neither about the resource that is being described or about a related resource. " +
//							"This statement will be ignored.");
//				}
//			}
//			
//			// Process statements about the resource:
//			boolean addedInfo = processResourceStatements(rootNode, hasType, resourceStatementList);
//
//			// Process statements about the resource (where the resource is the object of the statement):
//			processResourceObjectStatements(rootNode, resourceObjectStatementList);
//			
//			// Process the statements about the related resources (to get their labels and class spec files)
//			processRelatedResourceStatements(rootNode, relatedResourceStatementList);
//						
//			return addedInfo;
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new SesameException("Exception when retrieving data from the database about " + resourceID);
//		}
//	}

	/**
	 * Extend the Semantic Graph from a list of statements about the resource being described
	 * @param rootNode Root node of the graph (i.e. the resource being described)
	 * @param hasType false if the graph doesn't already contains information about the type of the resource being described
	 * @param resourceStatementList List of statements about the resource being described
	 * @return True if some information was added to the graph
	 * @throws SesameException
	 * @throws NameAlreadyBoundException
	 */
	private boolean processResourceStatements(SGNode rootNode, boolean hasType, Collection<Statement> resourceStatementList)
			throws SesameException, NameAlreadyBoundException {
		
		boolean addedInfo = false;
		List<String> types = new ArrayList<String>();

		String resourceID = "";
		
		for (Statement s : resourceStatementList) {
			// Get info from statement:
			resourceID = s.getSubject().toString();
			Value object = s.getObject();
			String property = s.getPredicate().getLocalName();
			
			if (object instanceof URI) {
				String type = ((URI) object).toString();
				String namespace = s.getPredicate().getNamespace();
				if ( !namespace.contains("www.policygrid.org") && !namespace.contains("www.mooney.net/geo") && !namespace.contains("openprovenance.org")
						&& !hasType && (OntologyWriter.TYPE_URI.equals(s.getPredicate().toString()))) {
					types.add(type);
					continue;			//don't include properties from other namespaces,
				}						//as they won't be recognized by OntologyReader, and we already know the type							
			}
			
			// Creating new node corresponding to the Statement:
			SGNode target = null;
			
			// Determine the object's type (literal, blank or other type of resource (i.e. URI)):
			if (object instanceof URI){
				target = getObjectInfo((URI) object, rootNode, property);
			} else if (object instanceof Literal){
				target = getDatatypeNode((Literal) object, property, rootNode);
			} else if (object instanceof BNode){
				getSequence((BNode) object, s.getPredicate().getLocalName(), rootNode);
			} else {
				// if other than those, ignore this statement (should it ever happen?)
				continue;
			}
			
			if (target == null) {
				System.out.println("Couldn't determine the object's type (is it defined in the OntologyReader?). The statement " + s.toString() + " will be ignored.");
				continue;
			}
			
			try {
				SGEdge edge = sgt.makeEdge(property, rootNode, target);
				edge.setRealise(SGNode.HIDE);	//does not have to be realised in the text
				edge.setNamespace(s.getPredicate().getNamespace());
				sgt.addEdge(edge);
				
				addedInfo = true;

			} catch (NameAlreadyBoundException e) {
				// if the edge is already in the graph, process next statement:
				e.printStackTrace();
				continue;
			}
		}

		if (!hasType) {
			//TODO: rootNode.setLabel --> to set the label of the resource that is being described
			OntClass ontClass = reader.getMostSpecificClass(types);	//get the most specific class type, and create a new SGNode
			if (ontClass != null){
				rootNode.setLabel(ontClass.getLocalName());
				
				try {
					// find out which properties must be used to identify an instance and query the repository to retrieve the value of those properties
					String relatedResourceLabel = findResourceLabel(ontClass, resourceID);
					
					// Set the label as defined in the specification:
					if (relatedResourceLabel != null && !"".equals(relatedResourceLabel)) {
						rootNode.setSpecLabel(relatedResourceLabel);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
		return addedInfo;
	}

	
	/**
	 * Extent the Semantic Graph from a list of statements about the resource being described (statements where the resource being described is the object of the statements)
	 * @param rootNode Root node of the graph (i.e. the resource being described)
	 * @param hasType false if the graph doesn't already contains information about the type of the resource being described
	 * @param resourceStatementList List of statements about the resource being described
	 * @return True if some information was added to the graph
	 * @throws SesameException
	 * @throws NameAlreadyBoundException
	 */
	private boolean processResourceObjectStatements(SGNode rootNode, Collection<Statement> resourceStatementList)
			throws SesameException, NameAlreadyBoundException {
		
		boolean addedInfo = false;
		int i = 0;
		for (Statement s : resourceStatementList) {
			// Get info from statement:
			Value relatedResource = s.getSubject();
			String property = s.getPredicate().getLocalName();
			
			// Creating new node corresponding to the Statement:
			SGNode relatedResourceNode = null;
			
			// Determine the object's type (literal, blank or other type of resource (i.e. URI)):
			//	--> in this case, it will always be a URI ?
			if (relatedResource instanceof URI){
				relatedResourceNode = getObjectInfo((URI) relatedResource, rootNode, property);
//			} else if (relatedResource instanceof Literal){
//				relatedResourceNode = getDatatypeNode((Literal) relatedResource, property, rootNode);
//			} else if (relatedResource instanceof BNode){
//				getSequence((BNode) relatedResource, s.getPredicate().getLocalName(), rootNode);
			} else {
				// if other than those, ignore this statement (should it ever happen?)
				continue;
			}
			
			// Set edge from related resource to resource being described
			SGEdge edge = sgt.makeEdge(property, relatedResourceNode, rootNode);
			edge.setRealise(SGNode.HIDE);	//does not have to be realised in the text
			sgt.addEdge(edge);
			
			addedInfo = true;
			
			//TODO remove break (just for tests while SPARQL query is not working)
			i++;
			if (i > 5) {
				break;
			}
		}

		return addedInfo;
	}

	/**
	 * Process a list of statements related to the resource being described. Those statements must be about the type of resources related the resource being described.
	 * (e.g. If we describe a paper, this paper will have a property hasAuthor, relating the Paper to a Person. Therefore we have need a way to refer to that person (e.g. by using it's name))
	 * This method will look up the class language specification files to find out which properties must be used to identify an instance of that class
	 * @param rootNode Root node of the graph (i.e. the resource being described)
	 * @param relatedResourceStatementList List of statements about the resources related to the resource being described
	 * @throws UnsupportedEncodingException
	 * @throws JDOMException
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws SesameException
	 * @throws NameAlreadyBoundException
	 */
	private void processRelatedResourceStatements(SGNode rootNode, Collection<Statement> relatedResourceStatementList)
			throws UnsupportedEncodingException, JDOMException, IOException,
			FileNotFoundException, SesameException, NameAlreadyBoundException {

		for (Statement s : relatedResourceStatementList) {
			// Get info from statement:
			Value relatedResourceType = s.getObject();
			String relatedResourceId = s.getSubject().toString();

			// find out which properties must be used to identify an instance and query the repository to retrieve the value of those properties
			OntClass relatedResourceOntClass = reader.getClass(((URI)relatedResourceType).toString());
			String relatedResourceLabel = findResourceLabel(relatedResourceOntClass, relatedResourceId);

			// Set the label: (i.e. create an rdf:label edge for the related resource node)
			if (relatedResourceLabel != null && !"".equals(relatedResourceLabel)) {
				setLabelEdge(relatedResourceId, relatedResourceLabel, rootNode);
			} else {
				classLabelDefaultBehaviour(relatedResourceType.toString());
				continue;
			}
		}
	}

	/**
	 * Process a statement about a resource related to the original resource that is being described.
	 * In order to refer to this resource in the original description, we must use a label for this related resource.
	 * Information about class label are contained in the XML class specification files. 
	 * @param resourceTypeURI
	 * @param resourceID
	 * @return the label as defined in the corresponding class specification file, or null if no data has been retrieved from the repository
	 * @throws Exception
	 */
	private String findResourceLabel(OntClass resourceOntClass, String resourceID)
			throws UnsupportedEncodingException, JDOMException, IOException,
			FileNotFoundException, SesameException, NameAlreadyBoundException {

		if (resourceOntClass == null) {
			return null;
		}
		
		URI resourceTypeURI = new URIImpl(resourceOntClass.getURI());
		String resourceClassName = resourceTypeURI.getLocalName();
		StringBuffer resourceLabel = new StringBuffer();

		// Boolean to check that we retrieved metadata from the repository (otherwise the label will only consist of literal labels)
		//TODO: might need something more sophisticated: only add literal values if the previous property was retrieved successfully (to avoid addresses like " , Aberdeen" if only the #place was filled)
		boolean metadataRetrieved = false;
		
		/**
		 * Hardcoding some classes:
		 */
//		if (resourceTypeURI.toString().contains("Project")) {
//			relatedResourceLabel = "http://www.policygrid.org/project.owl#projectTitle";
//		} else {
//			// everything else here
//		}
		
		/**
		 * TODO: Find somewhere more central to store the Map (in the Lexicon?)
		 */
		Element labelElement = classLabelMap.get(resourceTypeURI.getNamespace() + resourceClassName);
		
		if (labelElement == null) {
			System.out.println("Looking for the file corresponding to the " + resourceTypeURI + " resource type.");
			
			File classSpecFile = new File(NLGProperties.getString("lexiconClassSpecPath") + URLEncoder.encode(resourceTypeURI.getNamespace(), "ISO-8859-1") + "/" + resourceClassName + ".xml");
			if (!classSpecFile.exists()) {
				System.out.println("No spec file found in " + classSpecFile.getAbsolutePath() + ". Looking for " + resourceClassName +"'s superClass spec file");
				
				// Check super classes spec files:
				List<OntClass> superClasses = reader.getSuperClasses(resourceOntClass);
				for (OntClass ontClass : superClasses) {
					String nameSpace = ontClass.getNameSpace();
					String localName = ontClass.getLocalName();
					if (nameSpace != null && !nameSpace.equals("")) {
						try {
							URL nameSpaceURL = new URL(nameSpace);	
							classSpecFile = new File(NLGProperties.getString("lexiconClassSpecPath") + URLEncoder.encode(nameSpaceURL.toString(), "ISO-8859-1") + "/" + localName + ".xml");
							if (classSpecFile.exists()) {
								break;
							}
						} catch (MalformedURLException e) {
							System.out.println("The namespace " + nameSpace + " is not a URL (class name: " + localName + ")");
						}

					}
				}

				if (!classSpecFile.exists()) {
					classLabelDefaultBehaviour(resourceClassName);
					return null;
				}
			}
			
			// Read Spec file:
			SAXBuilder saxbuild = new SAXBuilder();
		  	Document doc = saxbuild.build(new FileInputStream(classSpecFile));
			Element root = doc.getRootElement();
			
			// Find information about the label for this class (i.e. the "label" child):
			List<?> classInfoList = root.getChildren();
			
			if (classInfoList.isEmpty()) {
				System.out.println("No children in the XML file");
				classLabelDefaultBehaviour(resourceClassName);
				return null;
			}

			for (Object classInfo : classInfoList) {
				if (classInfo instanceof Element && Lexicon.CLASS_LABEL.equals(((Element) classInfo).getName())) {
					labelElement = (Element) classInfo;
					break;
				}
			}
			
			if (labelElement == null){
				System.out.println("No label element");
				classLabelDefaultBehaviour(resourceClassName);
				return null;
			}
			
			classLabelMap.put(resourceTypeURI.getNamespace() + resourceClassName, labelElement);
		}
		
		List<Element> labelPartElmtList = labelElement.getChildren(Lexicon.CLASS_LABEL_PART);
		
		for (Element labelPartElmt : labelPartElmtList) {
			// Literal:
			if (Lexicon.LITERAL_LABEL_PART_TYPE.equals(labelPartElmt.getAttributeValue(Lexicon.LABEL_PART_TYPE))) {
				resourceLabel.append(((org.jdom.Text)labelPartElmt.getContent().get(0)).getText());
				
			// Direct property:
			} else if (Lexicon.DIRECT_PROPERTY_LABEL_PART_TYPE.equals(labelPartElmt.getAttributeValue(Lexicon.LABEL_PART_TYPE))) {

				// Retrieve the property that must be used as a label for class of the related resource:
				String relatedResourcePropertyLabel = labelPartElmt.getValue();
				if (relatedResourcePropertyLabel == null || "".equals(relatedResourcePropertyLabel)) {
					System.out.println("The file " + resourceClassName + ".xml doesn't contains a specification about which property should be used as a label.");
					continue;
				}
				
				// SPARQL Query to get statement about this property
				String objectQuery = OntologyWriter.getObject(resourceID, relatedResourcePropertyLabel);
				QueryResult relatedResourceLabelResult = sesame.queryBinding(objectQuery);
				try {
					
					// Adding the label before the property value:
					String preLabel = labelPartElmt.getAttributeValue(Lexicon.PRE_LABEL);
					if (preLabel != null && relatedResourceLabelResult.getIDs().hasNext()) {
						resourceLabel.append(preLabel);
					}
					
					// if didn't find triple in the repository, search in the ontology files:
					if (relatedResourceLabelResult.size() == 0) {
						System.out.println("didn't find property " + relatedResourcePropertyLabel + "in the repository. Searching in the ontology files");
						OntModel ontology = reader.getOntology();
						resourceLabel.append(ontology.getProperty(resourceOntClass, ontology.getProperty(relatedResourcePropertyLabel)).getObject().toString());
					} else {
						resourceLabel.append((String) relatedResourceLabelResult.getIDs().next());
					}
					metadataRetrieved = true;
					
					// Adding the label after the property value:
					String postLabel = labelPartElmt.getAttributeValue(Lexicon.POST_LABEL);
					if (postLabel != null) {
						resourceLabel.append(postLabel);
					}
					
				} catch (Exception e) {
					System.out.println("The  resource " + resourceID + " doesn't have a property " + relatedResourcePropertyLabel);
					continue;
				}
				
			// Indirect property:
			} else if (Lexicon.INDIRECT_PROPERTY_LABEL_PART_TYPE.equals(labelPartElmt.getAttributeValue(Lexicon.LABEL_PART_TYPE))) {
				
				// Retrieve information from specification file:
				String classIsSubject = labelPartElmt.getAttributeValue(Lexicon.CLASS_SUBJECT);
				String propertyLink = labelPartElmt.getAttributeValue(Lexicon.PROPERTY_LINK);
				String propertylabel = labelPartElmt.getValue();
				
				if (classIsSubject == null ||  "".equals(classIsSubject) || propertyLink == null ||  "".equals(propertyLink) || propertylabel == null ||  "".equals(propertylabel)) {
					System.out.println("The file " + resourceClassName + ".xml doesn't contains all the information required to retrieve an indirect property.");
					continue;
				}
				
				String query = null;
				if (Boolean.parseBoolean(classIsSubject)) {
					query = OntologyWriter.getObject(resourceID, propertyLink);
				} else {
					query = OntologyWriter.getSubject(propertyLink, resourceID);
				}
				
				String relatedResource = null;
				QueryResult relatedResourceQueryResult = sesame.queryBinding(query);
				try {
					relatedResource = (String) relatedResourceQueryResult.getIDs().next();
				} catch (Exception e) {
					System.out.println("The  resource " + resourceID + " isn't linked with the property " + propertyLink );
					continue;
				}

				String relatedResourceLabelQuery = OntologyWriter.getObject(relatedResource, propertylabel);
				QueryResult relatedResourceLabelResult = sesame.queryBinding(relatedResourceLabelQuery);
				try {
					// Adding the label before the property value:
					String preLabel = labelPartElmt.getAttributeValue(Lexicon.PRE_LABEL);
					if (preLabel != null && relatedResourceLabelResult.getIDs().hasNext()) {
						resourceLabel.append(preLabel);
					}

					// if didn't find triple in the repository, search in the ontology files:
					if (relatedResourceLabelResult.size() == 0) {
						System.out.println("didn't find property " + propertylabel + "in the repository. Searching in the ontology files");
						OntModel ontology = reader.getOntology();
						resourceLabel.append(ontology.getProperty(resourceOntClass, ontology.getProperty(propertylabel)).getObject().toString());
					} else {
						resourceLabel.append((String) relatedResourceLabelResult.getIDs().next());
					}
					metadataRetrieved = true;
					
					// Adding the label after the property value:
					String postLabel = labelPartElmt.getAttributeValue(Lexicon.POST_LABEL);
					if (postLabel != null) {
						resourceLabel.append(postLabel);
					}

				} catch (Exception e) {
					System.out.println("The  resource " + relatedResource + " doesn't have a property " + propertylabel );
					continue;
				}
			}
		}
		if (metadataRetrieved) {
			return resourceLabel.toString();
		} else {
			return null;
		}
	}

	/**
	 * Default behavior if no label have been specified to identify a resource
	 * @param relatedResourceClassName Name of the related resource
	 */
	private void classLabelDefaultBehaviour(String relatedResourceClassName) {
		System.out.println("No valid spec file for the class " + relatedResourceClassName + ". No label will be used for that resource (e.g. the resource was deposited by a person)");
	}

	/**
	 * Creates a new edge containing the label of a resource
	 * @param resource
	 * @param label the label to be added
	 * @param node the node which will have that label
	 * @throws NameAlreadyBoundException
	 */
	private void setLabelEdge(String resource, String label, SGNode node)
			throws NameAlreadyBoundException {

		// Initialize "target" of the new edge:
		SGNode target = new SGStringNode(label);
		
		// Retrieve resource from map:
		SGNode resourceNode = idMap.get(resource);
		if (resourceNode == null) {
			System.out.println("The node corresponding to the resource " + resource + " hasn't been created yet. Can't add its label. \t" +
					"No spec file for the property linking the 2 resources? (e.g. holdsAccount.xml)");
			return;
		}
//		resourceNode.setLabel(relatedResourceLabel);
		
		// Create the Label edge:
		SGEdge edge = sgt.makeEdge(OntologyWriter.LABEL_URI, resourceNode, target);
		node.setRealise(SGNode.SHOW);	//have to be realized in the text
		sgt.addEdge(edge);
	}


	/**
	 * Reinitialise the SemanticGraphTransformer (sgt) and rebuild it so that it corresponds to the given Jena model
	 * @param jenaModel jena model that contains the graph required for the sgt
	 * @return whether informations were added to the root node of the sgt
	 */
	public SemanticGraphTransformer buildGraphFromJenaModel(OntModel jenaModel, String resourceID){
		
		try {
			prepareGraph("user", resourceID);
		} catch (SesameException e2) {
			e2.printStackTrace();
		}
		SGNode rootNode = idMap.get(resourceID);
		if (rootNode == null) {
			System.out.println("the Automatic Generator doesn't contain information about the resource: " + resourceID + ". This node will be created.");

			return null;
		// Otherwise reinitialize the Graph
//		} else {
//			// Reinitialize Root node (to remove previous edges and nodes):
//			rootNode.removeAllOutgoingEdge();
//			// Add the ID node to the root node
//			SGNode idTarget = new SGStringNode(resourceID);	//add the ID
//			try {
//				SGEdge idEdge = sgt.makeEdge("ID", rootNode, idTarget);
//				sgt.addEdge(idEdge);
//			} catch (NameAlreadyBoundException e1) {
//				e1.printStackTrace();
//				System.out.println("Problem when adding the ID node to the root node (i.e. to the resource being described)");
//			}
		}

		try {
			// Separating statements into 2 lists:
			Collection<Statement> resourceStatementList = new ArrayList<Statement>();
			Collection<Statement> relatedResourceStatementList = new ArrayList<Statement>();
			
			// Iterate statements:
			StmtIterator statementsIterator = jenaModel.listStatements();
			while (statementsIterator.hasNext()) {
				com.hp.hpl.jena.rdf.model.Statement statement = (com.hp.hpl.jena.rdf.model.Statement) statementsIterator.next();
				// Convert Jena statement to Sesame statement:
				Statement sesameStatement = SesameJenaUtilities.makeSesameStatement(statement.asTriple(), new ValueFactoryImpl());

				//TODO: ask Ed if there is another way to ignore all of the http://www.w3.org/ statements
				if (!sesameStatement.getSubject().toString().startsWith("http://www.w3.org/") && !sesameStatement.getObject().toString().startsWith("http://www.w3.org/")) {

					if (sesameStatement.getSubject().toString().contains(resourceID)) {
						resourceStatementList.add(sesameStatement);
						
					// and if the statement is about the type of a related resource:
					} else if (sesameStatement.getObject() instanceof URI && OntologyWriter.TYPE_URI.equals(sesameStatement.getPredicate().toString()) ) {
						relatedResourceStatementList.add(sesameStatement);
					} else {
						System.out.println("the statement" + sesameStatement.toString() + "is neither about the resource that is being described or about a related resource." +
								"This statement will be ignored.");
					}
				}
			}

				/* Old modifyGraph() */
//				// Only deal with statements about the resource:
//				Resource subject = statement.getSubject();
//				if (!subject.toString().contains(resourceID)) {
//					continue;
//				}
//				RDFNode object = statement.getObject();
//				Property predicate = statement.getPredicate();
//				
//				if (object.isURIResource()) {
//					String type = object.toString();
//					String namespace = predicate.getNameSpace();
//					if ((namespace.indexOf("www.policygrid.org") < 0) && (namespace.indexOf("www.mooney.net/geo") < 0))
//					{
//						if (predicate.toString().indexOf("www.w3.org/1999/02/22-rdf-syntax-ns#type") > 0 && subject.toString().contains(resourceID)){	//the type of the resource
//							types.add(type);
//						}
//						continue;	//don't include properties from other namespaces, 
//					}				//as they won't be recognised by OntologyReader, and we already know the type			
//				}
//				
//				SGNode target = null;
//				String property = predicate.getLocalName();
//
//				try {
//					if (object.isURIResource())
//						target = getObjectInfo(object.toString(), rootNode, property);
//					else if (object.isLiteral()) {
//						//problem: when the object is a URI, it still thinks that it's a literal (check with ed)
//						//TODO need to change the param of new URIImpl( dataType? (see expandNode()))
//						target = getDatatypeNode(new JenaLiteral(object.toString(), new URIImpl(predicate.getURI())), property, rootNode);
//					
//					//TODO: check if BNode same as Anon?
//					} else if (object.isAnon())
//						getSequence(object.toString(), predicate.getLocalName(), rootNode);
//				
//					if (target != null){
//						SGEdge edge = sgt.makeEdge(property, rootNode, target);
//						edge.setRealise(SGNode.HIDE);	//does not have to be realised in the text
//						sgt.addEdge(edge);
//						graphChanged = true;
//					}	
//				} catch (Exception e) {
//					e.printStackTrace();
//				}

			// Process statements about the resource:
			processResourceStatements(rootNode, false, resourceStatementList);

			// Process the statements about the related resources (to get their labels and class spec files)
			processRelatedResourceStatements(rootNode, relatedResourceStatementList);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error when trying to update the graph with the Jena model");
		}

		return sgt;
	}
	/**	Retrieves the components of a sequence and adds them to source
	 */
	private void getSequence(Value sequence, String property, SGNode source) throws SesameException
	{
		String id = sequence.toString();
		getSequence(id, property, source);
	}
	
	/**	Retrieves the components of a sequence and adds them to source
	 */
	private void getSequence(String id, String property, SGNode source) throws SesameException
	{
		try
		{
			String query = OntologyWriter.getDescriptionQuery(id);
		//	SesameReader sesame = new SesameReader();
			GraphQueryResult graph = sesame.queryGraph(query);

			while (graph.hasNext())
			{
				Statement s = graph.next();
				String ordering = s.getPredicate().getLocalName();
				int idx = -1;
				try	//try to get the rank order of the sequence
				{
					idx = Integer.parseInt(ordering.substring(1));	//e.g. property is '_1'
				}
				catch (NumberFormatException e)
				{	//must be some other kind of edge (e.g. type), which can be skipped
					continue;
				}
				
				try
				{
					Value object = s.getObject();	
					SGNode target = null;
					if (object instanceof URI)
						target = getObjectInfo((URI) object, source, property);
					else if (object instanceof Literal)
						target = getDatatypeNode((Literal) object, property, source);
						
					if (target != null)
					{
						SGEdge edge = sgt.makeEdge(property, source, target);
						sgt.addEdge(edge);				
						edge.setRealise(SGNode.HIDE);	//does not have to be realised in the text
						edge.setRealiseNr(idx);
					}
				}
				catch (NameAlreadyBoundException e)
				{	//very unlikely
					e.printStackTrace();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw(new SesameException(e.getMessage()));
		}
	}
	
	/**	Creates a DatatypeNode with the literal's value
	 */
	private SGNode getDatatypeNode(Literal lit, String property, SGNode source) throws SesameException
	{
		try
		{
			SGNode result;
			String value = lit.getLabel();
			
			//TODO: hardcoding special types of date that must be adapted (e.g. date and time)
			if (lit.getDatatype() != null && lit.getDatatype().toString().equalsIgnoreCase("http://www.w3.org/2001/XMLSchema#dateTime")) {
				value = value.substring(0, value.indexOf('T')) + " at " + value.substring(value.indexOf('T') + 1, value.length()); 
			}
			

			if (lit.getDatatype() == null)
				result = new SGStringNode(value);
			else
			{
				String datatype = lit.getDatatype().getLocalName();	
				if (datatype == null)
					result = new SGStringNode(value);
				else if (datatype.equals("int"))
					result = new SGIntNode(new Integer(value));
				else if (datatype.equals("double") || datatype.equals("float"))
					result = new SGDoubleNode(new Double(value));
				else if (datatype.equals("boolean"))
					result = new SGBooleanNode(new Boolean(value));
				else if (property.equals("HasAbstract"))
					result = new SGAbstractNode(value);
				else
					result = new SGStringNode(value);
			}
			
			List<Edge> outgoing = source.getOutgoingEdges(property);
			for (int i = 0; i < outgoing.size(); i++)
			{	//if the graph already has a node with this property, this source and this value, don't add another edge!	
				SGNode target = (SGNode) outgoing.get(i).getTarget();
				if (target.getNLLabel(reader).equals(result.getNLLabel(reader)))
					return null;
//					return result;
			}
			return result;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new SesameException("Error when creating semantic graph from data from the database.");
		}
	}
	
	/**	Retrieves the information about the object with the given id needed to present it in
	 *	the text - so not all information! Merely its type, NLName property, its URI and access conditions
	 *	(if it has them).
	 */
	private SGNode getObjectInfo(URI object, SGNode source, String property) throws SesameException
	{
		String id = object.toString();
		return getObjectInfo(id, source, property);
	}
	
	/**	Retrieves nl-information about this object from the archive (e.g. type, name, title, gender...) 
	 *	and adds it to the Graph. First though it checks whether the object is already in the Graph;
	 *	if so, return null, and if there is not yet an edge of this property, its inverse, or a super- 
	 *	or sub-property that connects them, an edge is added.
	 *
	 *	@param id Sesame ID
	 *	@param source Parent SGNode, source of edge
	 *	@param property Property name
	 *	@return new SGNode, or null if no new node was created
	 *	@throws SesameException
	 */
	public SGNode getObjectInfo(String id, SGNode source, String property) throws SesameException
	{
		OntProperty prop = reader.getProperty(property);
		try
		{	//Get all information needed to present this object in NL from the database
			if (idMap.containsKey(id))		//if there is already a node with this id in the graph
			{
				List<String> subProps = reader.getSubProperties(property);	
				List<Edge> outgoing = source.getOutgoingEdges(property);
				for (int i = 0; i < subProps.size(); i++)
					outgoing.addAll(source.getOutgoingEdges(subProps.get(i)));
			
				for (int i = 0; i < outgoing.size(); i++)	//if this node is already in the graph with this property or a sub-property, 	
					if (id.equals(((SGNode)outgoing.get(i).getTarget()).getUniqueID()))	//don't add another edge!
						return null;				//check if there has been an edge added between this source and target with a super property;
				
				List<String> superProps = reader.getSuperProperties(property);
				outgoing = new ArrayList<Edge>();
				for (int i = 0; i < superProps.size(); i++)
					outgoing.addAll(source.getOutgoingEdges(superProps.get(i)));

				for (int i = 0; i < outgoing.size(); i++)	//if this node is already in the graph with this property or a sub-property, 	
				{
					SGEdge edge = (SGEdge) outgoing.get(i);
					if (id.equals(edge.getTarget().getUniqueID()))	//don't add another edge!	
					{	//set the label to this more specific property, then return
						edge.setLabel(property);
						return null;
					}
				}

				String inverse = reader.getInverse(property);
				if (inverse != null)
				{	 //the same goes for the inverse property
					List<Edge> incoming = source.getIncomingEdges(inverse);
					subProps = reader.getSubProperties(inverse);	
					
					for (int i = 0; i < subProps.size(); i++)
						incoming.addAll(source.getIncomingEdges(subProps.get(i)));

					for (int i = 0; i < incoming.size(); i++)
						if (id.equals(((SGNode) incoming.get(i).getSource()).getUniqueID()))
							return null;		//check if the super property has already been created
					
					List<String> superInverse = reader.getSuperProperties(inverse);
					incoming = new ArrayList<Edge>();
					for (int i = 0; i < superInverse.size(); i++)
						incoming.addAll(source.getIncomingEdges(superInverse.get(i)));
					
					for (int i = 0; i < incoming.size(); i++)	//if this node is already in the graph with this property or a sub-property, 	
					{
						SGEdge edge = (SGEdge) incoming.get(i);
						if (id.equals(edge.getTarget().getUniqueID()))	//don't add another edge!	
						{	//set the label to this more specific property, then return
							edge.setLabel(inverse);
							return null ;
						}
					}
				}
				else if ((prop != null) && prop.isSymmetricProperty())
				{
					List<Edge> incoming = source.getIncomingEdges(property);
					for (int i = 0; i < incoming.size(); i++)
						if (id.equals(((SGNode) incoming.get(i).getSource()).getUniqueID()))
							return null;	//if property is symmetric, and a link already exists the other way, don't bother adding this one.
				}
				else
				{
					System.out.print(property);
					if (prop == null)
						System.out.println(" DOES NOT EXIST !!!!!!!!!!!!");
					else if (inverse == null)
						System.out.println("Does not have an inverse!!");
				}
							
				return idMap.get(id);		//if none of these edges exist, return the node so a new edge can be created
			}
			
			//TODO: URI classMap: might need to change the sparql query (to retrive other properties as well)
			String sparql = OntologyWriter.getNLQuery(id);	//i.e. name, title, gender and type
			QueryResult result = sesame.queryBinding(sparql, id);
			Map<String, List<String>> map = result.getBindings(id);

			if ((map == null) || (map.size() == 0))
			{
				try
				{
					getSequence(id, property, source);	//it may be because this is a sequence, so try that
				}
				catch (SesameException e)
				{}
				return null;
			}
			
			List<String> types = new ArrayList<String>();
			List<SGEdge> edges = new ArrayList<SGEdge>();
			
			for (Iterator it = map.keySet().iterator(); it.hasNext(); )
			{	//create an edge for each nl property, and store the types in a list
				String key = (String) it.next();
				if (reader.useAsProperName(key) || key.equalsIgnoreCase("HasURI") || key.equalsIgnoreCase("AccessConditions"))
				{
					SGEdge e = sgt.makeEdge(key);
					SGNode target = new SGStringNode(map.get(key).get(0));	//NL properties can only have 1 value
					e.setTarget(target);
					e.setRealise(SGNode.HIDE);		//does not have to be realised in the text
					edges.add(e);
				}
				else
				{
					for (int i = 0; i < map.get(key).size(); i++)
					{	//must be rdf:type property; add all values to the types list
//						String value = map.get(key).get(i);
//						//adapted to foaf ontology (URIs use / instead of #)
//						int indexOfType = value.indexOf("#");
//						if (indexOfType < 0) {
//							indexOfType = value.lastIndexOf("/");
//						}
//						types.add(value.substring(indexOfType + 1));
//						if (map.get(key).get(i).equals("OurSpacesAccount")) {
//							types.add("http://www.policygrid.org/ourspacesVRE.owl#OurSpacesAccount");
//						} else if (map.get(key).get(i).equals("Project")) {
//							types.add("http://www.policygrid.org/project.owl#Project");
//						} else {
							types.add(map.get(key).get(i));
//						}
						
					}
				}
			}
			OntClass c = reader.getMostSpecificClass(types);	//get the most specific class type, and create a new SGNode
			if (c == null)
			{
				if ((types.size() > 0) && types.get(0).equals("Seq"))
					getSequence(id, property, source);
				return null;
			}
			
			String type = c.getLocalName();
			
			/*
			 * TODO: URI: change the name of the SGNode to full URI (might break everything else...)
			 * If change the name of the node to the URI, the URI will be used to identify the resource when NLG 
			 */
//			SGNode node = new SGNode(c.getNameSpace() + c.getLocalName());
			SGNode node = new SGNode(type);
			node.setNameSpace(c.getNameSpace());
			
			if (type.equals(reader.DATEPOINT) || type.equals(reader.DATEPERIOD))
				node = getDate(id);
			else if (type.equals("Address"))
				node = new SGAddressNode(type);
			
			SGNode idTarget = new SGStringNode(id);	//add the ID
			SGEdge idEdge = sgt.makeEdge("ID", node, idTarget);
			for (int i = 0; i < edges.size(); i++)	//add all the NL edges
			{
				sgt.addEdge(edges.get(i));
				edges.get(i).setSource(node);
			}
			sgt.addEdge(idEdge);
			
			idMap.put(id, node);	//add node to the 'existing id's' map
			node.setRealise(SGNode.INCOMPLETE);	//not all information about this object has been retrieved yet!
			return node;		
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new SesameException("Error when creating semantic graph from data from the database.");
		}
	}
	
	/**	Reads and builds an SGDateNode from the database.
	 */
	private SGDateNode getDate(String id) throws SesameException
	{
		SGDateNode result = new SGDateNode();
		try
		{
			String query = OntologyWriter.getDescriptionQuery(id);
			GraphQueryResult graph = sesame.queryGraph(query);
	
			while (graph.hasNext())
			{
				Statement s = graph.next();
				String namespace = s.getPredicate().getNamespace();
				if ((namespace.indexOf("www.policygrid.org") < 0) && (namespace.indexOf("www.mooney.net/geo") < 0))
					continue;
							
				String property = s.getPredicate().getLocalName();	
				Value object = s.getObject();
				if (!(object instanceof Literal))	//ignore any other statements; we're only after the literals
					continue;
				
				Literal lit = (Literal) object;
				String value = lit.getLabel();
				try
				{
					result.setValue(property, Integer.parseInt(value));
				}
				catch(NumberFormatException e)
				{
					System.out.println("Exception when trying to create an Integer from " + value);
				}
			}
			return result;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new SesameException("Exception when retrieving data from the database about " + id);
		}	
	}
	
	/**	Retrieves nl-information about the given resource ID and adds it to the graph
	 *	@param sgt SemanticGraphTransformer
	 *	@param id Sesame ID
	 *	@return SGNode
	 *	@throws SesameException
	 */
	public SGNode getNLInformation(SemanticGraphTransformer sgt, String id) throws SesameException
	{
		try
		{
			String sparql = OntologyWriter.getNLQuery(id);	//i.e. name, title, gender and type
			QueryResult result = sesame.queryBinding(sparql, id);
			Map<String, List<String>> map = result.getBindings(id);
			if ((map == null) || (map.size() == 0))
				return null;
			
			List<String> types = new ArrayList<String>();
			List<SGEdge> edges = new ArrayList<SGEdge>();	
			for (Iterator it = map.keySet().iterator(); it.hasNext(); )
			{	//create an edge for each nl property, and store the types in a list
				String key = (String) it.next();
				if (reader.useAsProperName(key) || key.equalsIgnoreCase("HasURI") || key.equalsIgnoreCase("AccessConditions"))
				{
					SGEdge e = sgt.makeEdge(key);
					SGNode target = new SGStringNode(map.get(key).get(0));	//NL properties can only have 1 value
					e.setTarget(target);
					e.setRealise(SGNode.HIDE);		//does not have to be realised in the text
					edges.add(e);
				}
				else
				{
					for (int i = 0; i < map.get(key).size(); i++)
					{	//must be rdf:type property; add all values to the types list
						String value = map.get(key).get(i);
						types.add(value.substring(value.indexOf("#") + 1));
					}
				}
			}
			
			OntClass c = reader.getMostSpecificClass(types);	//get the most specific class type, and create a new SGNode
			if (c == null)
				return null;
			
			String type = c.getLocalName();
			SGNode node = new SGNode(type);
			if (type.equals(reader.DATEPOINT) || type.equals(reader.DATEPERIOD))
				node = getDate(id);
			else if (type.equals("Address"))
				node = new SGAddressNode(type);
			
			SGNode idTarget = new SGStringNode(id);	//add the ID
			SGEdge idEdge = sgt.makeEdge("ID", node, idTarget);
			for (int i = 0; i < edges.size(); i++)	//add all the NL edges
			{
				sgt.addEdge(edges.get(i));
				edges.get(i).setSource(node);
			}
			sgt.addEdge(idEdge);
			
			idMap.put(id, node);	//add node to the 'existing id's' map
			node.setRealise(SGNode.INCOMPLETE);	//not all information about this object has been retrieved yet!
			return node;		
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new SesameException("Error when creating semantic graph from data from the database.");
		}
	}
	
	/**	Retrieves all information about the resource corresponding to the given node,
	 *	that has been added to the database at some earlier point, and adds it to
	 *	the semantic graph
	 *
	 *	@param	node The placeholder SGNode about which all information should now be retrieved
	 *	@param	sgt SemanticGraphTransformer holding the semantic graph to which the retrieved information will be added
	 *	@throws SesameException
	 */
	public void getInformation(SGNode node, SemanticGraphTransformer sgt) throws SesameException
	{
		getInformation(node, sgt, true, true);
	}
	
	/**	Retrieves all information about the resource corresponding to the given node,
	 *	that has been added to the database at some earlier point, and adds it to
	 *	the semantic graph
	 *
	 *	@param	node The placeholder SGNode about which all information should now be retrieved
	 *	@param	sgt SemanticGraphTransformer holding the semantic graph to which the retrieved information will be added
	 *	@param	hasNL True if the Graph already contains nl-information about this node
	 *	@param 	hasType True if the Graph already contains this node's class type.
	 *	@throws SesameException
	 */
	public void getInformation(SGNode node, SemanticGraphTransformer sgt, boolean hasNL, boolean hasType) throws SesameException
	{
		this.sgt = sgt;
		idMap = new HashMap<String, SGNode>();
		for (Iterator it = sgt.getGraph().getNodes(); it.hasNext(); )
		{	//add all nodes with a unique id in the semantic graph to the id map
			SGNode n = (SGNode) it.next();
			String id = n.getUniqueID();
			if (id != null)
				idMap.put(id, n);
		}
		
		try
		{
			if (expandGraph(node, node.getUniqueID(), hasNL, hasType))	//if any information was added
				node.setRealise(SGNode.SHOW);	//show it in the text
			else
				node.setRealise(SGNode.NOINFO);
			System.out.println("Node must be realised: " + node.mustRealise());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**	Retrievs the information about the object with the given ID that is asked for in the given QueryGraph,
	 *	and adds it to it.
	 *	Preferred methods are getQueryResultDescription(QueryResult, SemanticGraph) or getQueryGraph(String, SemanticGraph)!
	 *	@param 	id Sesame ID
	 *	@param	parent Parent SGNode, source of property
	 *	@param  property Property name
	 *	@param	sgt QueryBuilder holding the QueryGraph
	 *	@throws SesameException
	 */
	public SGNode getQueryInformation(String id, SGNode parent, String property, QueryBuilder sgt) throws SesameException
	{
		this.sgt = sgt;
		idMap = new HashMap<String, SGNode>();
		for (Iterator it = sgt.getGraph().getNodes(); it.hasNext(); )
		{	//add all nodes with a unique id in the semantic graph to the id map
			SGNode n = (SGNode) it.next();
			String i = n.getUniqueID();
			if (i != null)
				idMap.put(i, n);
		}
		
		try
		{
			return getObjectInfo(id, parent, property);
		}
		catch (Exception e)
		{
			throw new SesameException(e.getMessage());
		}
	}
	
	/**	Returns a graph containing the requested information about all matches of
	 *	the query.
	 *	@param matches Search result
	 *	@param queryGraph QueryGraph
	 *	@throws SesameException
	 */
	public QueryResultGenerator getQueryResultDescription(QueryResult matches, SemanticGraph queryGraph) throws SesameException
	{
		QueryResultGraph graph = new QueryResultGraph(queryGraph.getUser());
		sgt = new QueryResultGenerator(graph, reader, sesame);
		idMap = new HashMap<String, SGNode>();
		List<SGNode> list = new ArrayList<SGNode>();
		list.add(queryGraph.getRoot());
		
		for (Iterator it = matches.getIDs(); it.hasNext(); )
			graph.addRoot(expandQueryResult((String) it.next(), null, list, null));
		FeedbackTextGenerator.printGraph("QueryResult", graph);
		return (QueryResultGenerator) sgt;
	}
	
	/**	Retrieves the information about this id that is relevant to the user's query,
	 *	and puts it in a semantic graph.
	 *
	 *	@param id Sesame ID
	 *	@param queryGraph QueryGraph
	 *	@throws SesameException
	 */
	public SemanticGraphTransformer getQueryInformation(String id, SemanticGraph queryGraph) throws SesameException
	{
		sgt = new SemanticGraphTransformer(new SemanticGraph(queryGraph.getUser()), reader);
		idMap = new HashMap<String, SGNode>();
		List<SGNode> list = new ArrayList<SGNode>();
		list.add(queryGraph.getRoot());
		SGNode root = expandQueryResult(id, null, list, null);
		sgt.getGraph().setRoot(root);
		FeedbackTextGenerator.printGraph("QueryResult", sgt.getGraph());
		return sgt;
	}	
		
	/**	Retrieves the information about this node that is relevant to the user's query.
	 */
	private SGNode expandQueryResult(String id, SGNode source, List<SGNode> graphSource, String superProp) throws SesameException
	{
		if (idMap.containsKey(id))
			return idMap.get(id);
			
		try
		{
			SGNode node = new SGNode("x");	//don't know the type yet, x is a placeholder
			node.setUniqueID(id);
			idMap.put(id, node);
			
			String query = OntologyWriter.getDescriptionQuery(id);
			GraphQueryResult graph = sesame.queryGraph(query);
			List<String> types = new ArrayList<String>();
	
			while (graph.hasNext())
			{
				Statement s = graph.next();
				String property = s.getPredicate().getLocalName();
				
				if (s.getObject() instanceof URI)
				{
					String type = ((URI) s.getObject()).getLocalName();
					if ((source != null) && type.equals(source.getUniqueID()) && reader.getInverse(superProp).equals(property))
						continue;	//skip this property, as its already in the graph		
					if (s.getPredicate().toString().indexOf("www.w3.org/1999/02/22-rdf-syntax-ns#type") > 0)	//the type
					{	//if this node is a sequence, map the nodes in the sequence with the given property
						if (type.equals("Seq") && (superProp != null))
						{
							getSequence(id, superProp, source, graphSource);
							return null;
						}
						else
							types.add(type);
						continue;
					}
				}
				String namespace = s.getPredicate().getNamespace();
				if ((namespace.indexOf("www.policygrid.org") < 0) && (namespace.indexOf("www.mooney.net/geo") < 0))
					continue;	//skip all other properties outside the policygrid namespace
				
				Value object = s.getObject();
				List<Edge> outgoing = new ArrayList<Edge>();
				for (int i = 0; i < graphSource.size(); i++)
					outgoing.addAll(getMatchingEdges(graphSource.get(i), property, object));
				if ((!reader.useAsProperName(property)) && outgoing.size() == 0) 
					continue;		//only add information that was asked for, about the depositor, or is the name
					
				SGNode target = null;
				if (object instanceof Literal)
					target = getDatatypeNode((Literal) object, property, node);
				else if (object instanceof URI)
				{
					List<SGNode> recurse = new ArrayList<SGNode>();
					for (int i = 0; i < outgoing.size(); i++)
					{
						SGNode n = (SGNode) outgoing.get(i).getTarget();
						if ((n.getOutgoingEdgesWithoutNLNumber(reader) > 0) && (!recurse.contains(n)))
							recurse.add(n);
					}
					
					if (recurse.size() > 0)
						expandQueryResult(object.toString(), node, recurse, property);
					else	//else just get enough information to present the node in NL and its uri
						target = getObjectInfo((URI) object, node, property);
				}
				
				if ((target != null) && (!hasEdge(node, target, property)))
				{	//check that the edge or its inverse is not already there
					SGEdge edge = sgt.makeEdge(property, node, target);	//if not, add the edge
					edge.setRealise(SGNode.SHOW);
					sgt.addEdge(edge);
				}
			}
			
			OntClass c = reader.getMostSpecificClass(types);	//get the most specific class type, and create a new SGNode
			if (c != null)
			{
				node.setLabel(c.getLocalName());
				if (c.getLocalName().equals("Address"))
					node = new SGAddressNode(node, sgt);
			}
			
			if ((superProp != null) && (!hasEdge(source, node, superProp)))
			{
				SGEdge edge = sgt.makeEdge(superProp, source, node);
				edge.setRealise(SGNode.SHOW);
				sgt.addEdge(edge);
			}
			else
				sgt.addNode(node);		//node may not have been added to the graph yet!
			node.setRealise(SGNode.INCOMPLETE);
			return node;
		}
		catch (Exception e)
		{
			e.printStackTrace(); 	
			throw new SesameException("Exception when retrieving data from the database about " + id);
		}	
	}
	
	private boolean hasEdge(SGNode source, SGNode target, String property)
	{
		List<Edge> edges = source.getOutgoingEdges(property);
		for (int i = 0; i < edges.size(); i++)
		{
			SGNode node = (SGNode) edges.get(i).getTarget();
			String id = node.getUniqueID();
			if ((id != null) && id.equals(target.getUniqueID()))
				return true;
		}		
		
		String inverse = reader.getInverse(property);
		if (inverse == null)
			return false;
	
		edges = target.getOutgoingEdges(inverse);
		for (int i = 0; i < edges.size(); i++)
		{
			SGNode node = (SGNode) edges.get(i).getTarget();
			if (node.getUniqueID().equals(source.getUniqueID()))
				return true;
		}
		
		return false;			
	}
	
	/**	Adds all edges outgoing from the given node in the query that have the label
	 *	'property'. It also adds edges that have the 'anything' label and whose
	 *	target node is of the same or super type as object
	 */
	private List<Edge> getMatchingEdges(SGNode source, String property, Value object) throws SesameException
	{
		List<Edge> result = source.getOutgoingEdges(property);
		List<Edge> any = source.getOutgoingEdges(QueryEdge.ANYTHING);
		for (int i = 0; i < any.size(); i++)
		{
			SGNode target = (SGNode) any.get(i).getTarget();
			if (object instanceof Literal)
			{
				if (!(target instanceof DatatypeNode))
					continue;
				//both are datatypes
				DatatypeNode node = (DatatypeNode) target;
				URI type = ((Literal)object).getDatatype();
				if (type == null) 
				{	//if they are both strings, add the edge to result
					if (node.getDatatype() == DatatypeNode.STRING)
						result.add(any.get(i));
				}
				else
				{	//if they are of the same datatype, add the edge to result
					String datatype = type.getLocalName();
					switch(node.getDatatype())
					{
						case 0: if (datatype.equalsIgnoreCase("string")) result.add(any.get(i)); break;
						case 1: if (datatype.equalsIgnoreCase("int")) result.add(any.get(i)); break;
						case 2: if (datatype.equalsIgnoreCase("double")) result.add(any.get(i)); break;
						case 3: if (datatype.equalsIgnoreCase("boolean")) result.add(any.get(i)); break;
					}
				}
			}
			else
			{	
				String typeName = target.getLabel();
				if (target instanceof DatatypeNode)
				{	//if target is a datatype, they don't match
					int type = ((DatatypeNode)target).getDatatype();
					if (type < 0)
						typeName = ((QueryValueNode)target).getClassType();
					else
						continue;
				}
				
				//both are individuals
				String id = object.toString();
				String sparql = OntologyWriter.getTypeQuery(id);
				QueryResult qr = sesame.queryBinding(sparql, id);
				List<String> types = qr.getBindingValues(id, "type");
				if (types == null)
					continue;
			
				for (int j = 0; j < types.size(); j++)
				{	//if object is the same or a superclass of target, add the edge to result
					if (types.get(j).indexOf(typeName) >= 0)
					{
						result.add(any.get(i));
						continue;
					}
				}
			}
		}
		
		return result;
	}
			
	/**	First parameter could be a BNode (a blank node) or a URI that is an RDF sequence.
	 *	This method extracts the components of the sequence.
	 *
	 */
	private void getSequence(String sequence, String property, SGNode source, List<SGNode> queryNodes) throws SesameException
	{
		try
		{
			String query = OntologyWriter.getDescriptionQuery(sequence);
			GraphQueryResult graph = sesame.queryGraph(query);

			while (graph.hasNext())
			{
				Statement s = graph.next();
				String ordering = s.getPredicate().getLocalName();
				int idx = -1;
				try	//try to get the rank order of the sequence
				{
					idx = Integer.parseInt(ordering.substring(1));	//e.g. property is '_1'
				}
				catch (NumberFormatException e)	//must be some other kind of edge (e.g. type), which can be skipped
				{
					continue;
				}
				
				try
				{
					Value object = s.getObject();	
					SGEdge edge = null;
					if (object instanceof URI)
					{
						SGNode target = expandQueryResult(object.toString(), source, queryNodes, property);
						List<Edge> list = target.getIncomingEdges(property);
						for (int i = 0; i < list.size(); i++)
						{
							edge = (SGEdge) list.get(i);
							edge.setRealiseNr(idx);
							String parentID = edge.getSource().getUniqueID();
							if ((parentID != null) && parentID.equals(source.getUniqueID()))
								break;
						}
					}
					else if (object instanceof Literal)
					{
						SGNode target = getDatatypeNode((Literal) object, property, source);
						if (target != null)
						{
							edge = sgt.makeEdge(property, source, target);
							edge.setRealise(SGNode.SHOW);
							edge.setRealiseNr(idx);
							sgt.addEdge(edge);	
						}
					}
				}
				catch (NameAlreadyBoundException e)
				{	//very unlikely
					e.printStackTrace();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw(new SesameException(e.getMessage()));
		}
	}
}