package NLGService.WYSIWYM.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import NLGService.WYSIWYM.model.Anchor;
import NLGService.WYSIWYM.model.SGNode;
import NLGService.WYSIWYM.model.SummationAnchor;
import NLGService.WYSIWYM.testclasses.TextTypesGenerator;
import NLGService.liber.AnchorInfo;

public class HTMLInterfaceUtils {

	public static String prepareHTMLResponse(
		String resourceID, TextTypesGenerator generator,
		AnchorInfo[] anchorFeedbackText, boolean nlgProvenance, String servletLocation)
		throws IOException, UnsupportedEncodingException {

		// Textual description wrapped in HTML code:
		StringBuffer htmlFeedbackText = new StringBuffer();
		htmlFeedbackText.append("<div id=\"" + resourceID +"\" text-decoration:none> <p>");
	
		// Counter for related resources
		int relatedResourcesNb = 0;
		
		//Create a string containing a list of <div> where the text will be added when a user expand the text about a resource:
		StringBuffer expandableTextDivs = new StringBuffer();
		
		// Iterate all parts of the feedback text:
		for (AnchorInfo anchorInfo : anchorFeedbackText) {
			String anchorInfoWords = anchorInfo.getWords();
			
			// If this anchor contains informations:
			if (anchorInfoWords != null && !"".equals(anchorInfoWords) && !"The x:".equals(anchorInfoWords)) {
				
				// If this part of the text refers to another resource, create a link to that resource:
				if (anchorInfo.isAnchor()) {
					
					Anchor anchor = generator.getText().getAnchor(anchorInfo.getID());
	
					//TODO: problem with ourSpacesVRE.owl#hasContact property: anchor.getNode() is null so can't create links to those other resources
					if (anchorInfo.getID() == null || "".equals(anchorInfo.getID()) || anchor.getNode() == null) {
						System.out.println("No ID for the node " + anchor.getID());
	
						if (anchor instanceof SummationAnchor && ((SummationAnchor) anchor).getAggregatedProperty() != null && ((SummationAnchor) anchor).getAggregatedProperty() != "") {
							// Get the property summarized by this anchor :
							String aggregatedProperty = ((SummationAnchor) anchor).getAggregatedProperty();
							String aggregatedPropertyEncoded = URLEncoder.encode(aggregatedProperty, "ISO-8859-1");
							
							String anchoredResourceURI = URLEncoder.encode(resourceID, "ISO-8859-1");
	//						String anchoredResourceID = System.currentTimeMillis() + anchoredResourceURI.substring(anchoredResourceURI.indexOf("%23") + 3);
							String anchoredResourceID = anchoredResourceURI.substring(anchoredResourceURI.indexOf("%23") + 3) + aggregatedPropertyEncoded.substring(aggregatedPropertyEncoded.indexOf("%23") + 3) + System.currentTimeMillis() ;
	
							
							// onclick to log when a user request a description of another resource:
	//						onclickLogger = buildOnclickString(resourceID, userDbID, aggregatedProperty);
							if (nlgProvenance) {
								// if the user is exploring the provenance graph, the link to un-aggregate the property will display all the provenance, as we can't specify only one property (because the of the way provenance is represented in the repository)
								htmlFeedbackText.append("<a style=\"text-decoration:underline;\" href=\"" + servletLocation + "?resourceID=" 
										+ anchoredResourceURI + "&Property=provenance&AggregationThreshold=50\" rel=\"" + anchoredResourceID + "\" class=\"liber2\">" + anchorInfoWords + "</a>");
							} else {
								htmlFeedbackText.append("<a style=\"text-decoration:underline;\" href=\"" + servletLocation + "?resourceID=" 
										+ anchoredResourceURI + "&Property=" + aggregatedPropertyEncoded + "&AggregationThreshold=50\" rel=\"" + anchoredResourceID + "\" class=\"liber2\">" + anchorInfoWords + "</a>");
							}
	
							expandableTextDivs.append("<div id=\"" + anchoredResourceID + "\"></div>");
							
						} else {
							// Otherwise simply add the text (no anchor)
							htmlFeedbackText.append(anchorInfoWords);
						}
	
						continue;
					}
	
					// Retrieve the URI of this resource:
					String anchoredResourceURI = ((SGNode) generator.getGraph().getNode(anchor.getNode().getID())).getUniqueID();
					
					// If that resource is the resource we're already describing, don't create an anchor:
					if (anchoredResourceURI.contains(resourceID)) {
						//TODO: replace nlgLinkTable by the SQL table logging when a user follows a link after a NLG
						// and add the different values to insert in that table as the 2nd argument of log()
						htmlFeedbackText.append("<strong>" + anchorInfoWords + "</strong>");
	//							" <a href=\"" + Utility.getDetailPage(resourceType, resourceID) + "\"> resource's page</a>");
						
					// Otherwise, create an anchor to that other resource:
					} else {
						// Convert # to its ASCII code (%23) so it can be sent as a parameter with a GET (use URLConverter instead?)
						anchoredResourceURI = URLEncoder.encode(anchoredResourceURI, "ISO-8859-1");
						
						// Create an ID for the related resource: System current time + related resource ID (see BUG: can't expend twice on the same resource):
						String anchoredResourceID = /*resourceID.substring(resourceID.indexOf("#") + 1)*/ System.currentTimeMillis() + anchoredResourceURI.substring(anchoredResourceURI.indexOf("%23") + 3);
	//					htmlFeedbackText += "<a style=\"text-decoration:underline;\" href=\"" + anchoredResourceURI + "\" rel=\"" + anchoredResourceID + "\" class=\"liber\">" + anchorInfoWords + "</a>";
						htmlFeedbackText.append("<a style=\"text-decoration:underline;\" href=\"" + servletLocation + "?resourceID=" + anchoredResourceURI);
						
						// Explore the provenance graph of related resources:
						if (nlgProvenance) {
							htmlFeedbackText.append("&Property=provenance");
						}
						
						htmlFeedbackText.append("\" rel=\"" + anchoredResourceID + "\" class=\"liber2\">" + anchorInfoWords + "</a>");
						
						// add a div where the text about this linked resource will be added
						expandableTextDivs.append("<div id=\"" + anchoredResourceID + "\"></div>");
						
						relatedResourcesNb++;
					}
					
				} else {
					//Otherwise, just add the text without anchor:
					htmlFeedbackText.append(anchorInfoWords);
				}
			}
		}
		
		// add the expandable divs at the end of the text to allow user to explore related resoruces:
		htmlFeedbackText.append("</p>" + expandableTextDivs.toString() + "</div>");
		
		// if no info about the resource, display a message instead of just the name of the resource
		if (anchorFeedbackText.length <= 7 ) {
			htmlFeedbackText.append("no information available.");
		} 		
	
		// Write performances file:
	
		return htmlFeedbackText.toString();
	}
}
