package NLGService.WYSIWYM.transformer;

import NLGService.WYSIWYM.model.Anchor;
import NLGService.WYSIWYM.model.ContentPlan;
import NLGService.WYSIWYM.model.FeedbackText;
import NLGService.WYSIWYM.model.SGNode;
import NLGService.WYSIWYM.model.SemanticGraph;
import NLGService.WYSIWYM.ontology.OntologyReader;
import NLGService.WYSIWYM.ontology.SesameReader;
import NLGService.WYSIWYM.util.BadAnchorException;
import NLGService.WYSIWYM.util.SesameException;
import NLGService.WYSIWYM.util.SurfaceRealisationException;
import NLGService.WYSIWYM.util.TextPlanningException;
import NLGService.liber.AnchorInfo;

import com.hp.hpl.jena.ontology.OntClass;

/**	BrowsingGenerator generates the feedback text of an object in the database,
 *	adding both options for adding information and for viewing hidden information.
 *
 *	@author Feikje Hielkema
 *	@version 1.2 24-03-2008
 */
public class BrowsingGenerator extends FeedbackTextGenerator
{
	private String key;
	private boolean editable = false;	//by default, you can't add info in the browsing tool
	
	/**	Constructs a BrowsingGenerator for the given SemanticGraph. The generator will first
	 *	extract all information about the graph root from the archive, then generate the feedback
	 *	text.
	 *	
	 *	@param sgt SemanticGraphTransformer
	 *	@param reader Ontology
	 *	@param s Sesame
	 *	@param userID User ID, store so we can check access rights to any downloadable resources
	 *	@param edit if true, the text should contain editing options where the access rights permit
	 *	@throws SesameException if there was an error retrieving information from the archive.
	 */
	public BrowsingGenerator(SemanticGraphTransformer sgt, OntologyReader reader, SesameReader s, String userID, boolean edit) throws SesameException
	{
		super(sgt, reader, s, userID);
		this.editable = edit;
		init();
	}
	
	/**	Constructs a BrowsingGenerator for the given SemanticGraph. The generator will first
	 *	extract all information about the graph root from the archive, then generate the feedback
	 *	text.
	 *	
	 *	@param sgt SemanticGraphTransformer
	 *	@param reader Ontology
	 *	@param noAnchors If true, do not include any anchors, the text will not be browsable
	 *	@param s Sesame
	 *	@param userID User ID, store so we can check access rights to any downloadable resources
	 *	@throws SesameException if there was an error retrieving information from the archive.
	 */
	public BrowsingGenerator(SemanticGraphTransformer sgt, OntologyReader reader, boolean noAnchors, SesameReader s, String userID) throws SesameException
	{
		super(sgt, reader, s, userID);
		if (noAnchors)
		{
			printGraph("BrowsingText", getGraph());
			getGraph().stopFlashing();
		}
		else
			init();
	}
	
	/**	Constructs a BrowsingGenerator for the given SemanticGraph. There is no
	 *	initialisation, the generator assumes the given graph is complete.
	 *	
	 *	@param graph SemanticGraph
	 *	@param r Ontology
	 *	@param s Sesame
	 */
	public BrowsingGenerator(SemanticGraph graph, OntologyReader r, SesameReader s)
	{
		super(graph, r, s);
	}
	
	/**	Sets the unique identifier of this browsing session.
	 *	@param k String key
	 */
	public void setKey(String k)
	{
		key = k;
	}
	
	/**	Adds all information about the root node in the database to the semantic
	 *	graph
	 */
	private void init() throws SesameException
	{
		AutomaticGenerator gen = new AutomaticGenerator(reader, sesame);
		// commented the getInformation() so that we don't rebuild the graph after it's been build from the Jena Model
//		gen.getInformation(getGraph().getRoot(), this);
		printGraph("BrowsingText", getGraph());
		addAnchors();
		getGraph().stopFlashing();
	}
	
	/**	Overload: Updates the feedbacktext when the semantic graph has been changed
	 *	@throws	SurfaceRealisationException if an error occurs during surface realisation
	 *	@throws	TextPlanningException if an error occurs during text planning
	 */
	public void updateText() throws SurfaceRealisationException, TextPlanningException
	{	//plan text
		ContentPlan plan = new ContentPlanner(reader, getGraph()).plan();
		SurfaceRealiser sr = new SurfaceRealiser();
		setText(new FeedbackText(sr.realise(plan)));
	}
	
	/**
	 * TOFIX: doesn't set the root node
	 */
	public void setGraphRootNode(String nodeID) {
		getGraph().setRoot(getGraph().getNodeMap().get(nodeID));
	}
	
	/**	Overload: returns serialisable version of feedback text where the last
	 *	element is the key with which this generator is stored in the map.
	 *	@return AnchorInfo[] with feedback text
	 *	@throws	SurfaceRealisationException if an error occurs during surface realisation
	 *	@throws	TextPlanningException if an error occurs during text planning
	 */
	public AnchorInfo[] getSurfaceText() throws SurfaceRealisationException, TextPlanningException
	{
		updateText();
		AnchorInfo[] result = new AnchorInfo[getText().size() + 1];
		for (int i = 0; i < getText().size(); i++)
			result[i] = getText().next().toAnchorInfo();				
		result[result.length - 1] = new AnchorInfo();
		result[result.length - 1].setWords(key);
		return result;
	}
	
	/**	Overload: updates the anchor for the given node. If no anchor is needed,
	 *	it is removed. If this browsing session is editable, the parent method is used;
	 *	if not, a simpler anchor is created, that only contains browsing options.
	 *	@see FeedbackTextGenerator#updateAnchor(SGNode)
	 */
	public void updateAnchor(SGNode node)
	{
		if (editable)
		{	//if this browsing mode is editable, the anchor behaves the same as in feedbacktextgenerator
			super.updateAnchor(node);
			return;
		}
		
		try	//else make a curtailed version
		{
			OntClass c = reader.getClass(node);
			if (c != null)
				new Anchor(reader, c, node, userID);
		}
		catch (BadAnchorException e)
		{
			System.out.println(e.getMessage());
			node.removeAnchor();
		}	
	}
	
}