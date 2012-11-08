/**
 * Javascript code to render neo4j nodes 
 * using the standard neo4j REST API
 * Needs a running neo4j server. 
 * Uses the jQuery javascript library   
 * 
 * Maybe make it to a jquery plugin and have it work on a given DOM element?
 * 
 * NOTE we now have an node.html, but it might be better to have a server page node.jsp  or node.php
 * for example that takes the node id as get parameter, 
 * The loadNode is then not AJAX, but only the inline related node properties loading is!
 * In that case we have a bookmarkable (referable) page.
 * 
 */


/**
 * neo4jTabularNodeViewer
 * 
 * Using the Self-Executing Anonymous Function pattern
 * see: http://enterprisejquery.com/2010/10/how-good-c-habits-can-encourage-bad-javascript-habits-part-1/
 * 
 * still needs major refactoring
 */
(function( neo4jTabularNodeViewer, $, undefined ) {

	//var nodeBaseUrl = "http://localhost:7474/db/data/node/";
	//	
	// proxy's below
	//var nodeBaseUrl = "http://ehri01.dans.knaw.nl/neo4j-viewer/nodeProxy.php";
	var nodeBaseUrl = "http://localhost/~paulboon/nodeProxy.php";

	function constructNodeUrl(id) {
		//return 	nodeBaseUrl + id;
		return 	nodeBaseUrl + "?id=" + id; // use params with proxy
	}
	function constructNodeRelationshipsUrl(id) {
		//return 	nodeBaseUrl + id + "/relationships/all";
		return 	nodeBaseUrl + "?id=" + id + "&relationships"; // use params with proxy
	}

	/**
	 * Note that it doesn't handle properties 
	 * that are lists or compound objects
	 */
	function renderProperties(obj) {
		// NOTE could use array 
		var propStrLines = [];
		propStrLines.push("<h2>Properties</h2>\n");
		propStrLines.push("<table class='properties'>\n");
		propStrLines.push("<tr><th>Name</th><th>Value</th></tr>\n");
		for ( var prop in obj) {
			propStrLines.push("<tr><td>" + prop + "</td><td>" + obj[prop] + "</td></tr>\n");
		}
		propStrLines.push("</table>\n");

		$("#result").append(propStrLines.join(''));
	}

	/**
	 * Maybe only use this one and not renderProperties??????
	 */
	function renderPropertiesInline(elem, obj) {
		// assume elem is jquery object wrapping a DOM element

		// NOTE could use array 
		var propStrLines = [];
		propStrLines.push("<h2>Properties</h2>\n");
		propStrLines.push("<table class='properties'>\n");
		propStrLines.push("<tr><th>Name</th><th>Value</th></tr>\n");
		for ( var prop in obj) {
			propStrLines.push("<tr><td>" + prop + "</td><td>" + obj[prop] + "</td></tr>\n");
		}
		propStrLines.push("</table>\n");

		elem.append(propStrLines.join(''));
	}
	
	function renderRelationshipsWithInlineExpanding(obj) {
		// it's assumed to be an array of relationship objects
		var relStrLines = [];
		relStrLines.push("<h2>Relationships (" + obj.length + ")</h2>\n");
		relStrLines.push("<table>\n");
		relStrLines.push("<tr><th>Start</th><th>Type</th><th>End</th><th></th></tr>\n");

		for (var i = 0; i < obj.length; i++) {
			var rel = obj[i];
			// make links
			// detect if the id is the id of the current node
			var nodeId = getNodeIdInControls();
			var startNodeId = extractIdFromNeo4jUrl(rel.start);
			if (nodeId == startNodeId) {
				var startLink = "thisNode";
			} else {
				var startLink = "<a class='nodeLink' href='" + rel.start + "'>" + extractIdFromNeo4jUrl(rel.start) + "</a>";
			}
			var endNodeId = extractIdFromNeo4jUrl(rel.end);
			if (nodeId == endNodeId) {
				var endLink = "thisNode";
			} else {
				var endLink = "<a class='nodeLink' href='" + rel.end + "'>" + extractIdFromNeo4jUrl(rel.end) + "</a>";
			}
			//console.log("node id: " + nodeId + " (" + startNodeId + "->" + endNodeId + ")");

			// make expanding link
			if (nodeId == startNodeId) {
				var expandingUrl = rel.end;
			} else {
				var expandingUrl = rel.start;
			}
			var exandingLink = "<a class='inlineExpandingNodeLink' href='" + 
			expandingUrl + 
			"'>&#43; expand</a><div class='inlineNode'></div>";

			relStrLines.push("<tr><td>" + startLink + "</td><td>" + rel.type + "</td><td>" + endLink+ "</td><td>" + exandingLink +"</td></tr>\n");
		}
		relStrLines.push("</table>\n");

		$("#result").append(relStrLines.join(''));

		// or attach the calls on all nodeLink class 
		$(".nodeLink").click(function(){
			//console.log("nodelink: " + this.href);
			var id = extractIdFromNeo4jUrl(this.href);

			updateView(id);//this.href);		
			return false;
		});

		// NOTE just get the properties (AJAX) and expand it in the existing node's view
		$(".inlineExpandingNodeLink").toggle(function() {
			var id = extractIdFromNeo4jUrl(this.href);
			
			loadNodeInline($(this),id);//this.href);
			$(this).text("- collapse");
		}, function() {
			// Note maybe not clean it, but hide instead?
			$(this).next().empty();
			$(this).text("+ expand");
		});
		
	}

	/**
	 * Uses just one REST call to get the main info of all the relationships of this node
	 */
	function loadRelationShips(url) {
		// assume valid node's all relationships url 

		var request = $.ajax({
			url : url,
			type : "GET",
			dataType : "json"
		});

		request.done(function(json) {
			console.log(json);
			renderRelationshipsWithInlineExpanding(json);
		});

		request.fail(function(jqXHR, textStatus) {
			alert("Request for " + url + " failed: " + textStatus);
		});
	}

	function loadNodeInline(elem, id) {
		// assume elem is jquery object wrapping a DOM element

		// get the next div (of class inlineNode)
		var container = elem.next();
		container.empty();
		container.append("<h2>Node " + id + "</h2>\n");

		var request = $.ajax({
			url : constructNodeUrl(id),
			type : "GET",
			dataType : "json"
		});

		request.done(function(json) {
			console.log(json);
			renderPropertiesInline(container, json.data);
		});

		request.fail(function(jqXHR, textStatus) {
			alert("Request failed: " + textStatus);
		});
	}

	function updateView(id) {
		$("#result").empty();
		neo4jTabularNodeViewer.loadNode(id);
	}

	function setNodeIdInControls(id) {
		if ($("#controls input").val() != id) // avoid loops
			$("#controls input").val(id);
	}

	function getNodeIdInControls() {
		// also trim the string
		var nodeId =  $.trim($("#controls input").val());
		
		// Should do nice validation, but do it quickly for now
		// prevent XSS
		if (!isInteger(nodeId))
			nodeId="";

		return nodeId;
	}
	
	var reInteger = /^\d+$/;
	function isInteger (s)
	{    
	    return reInteger.test(s)
	}

	/**
	 * Extract the node id from the neo4j REST url.  
	 * Example neo4j url: "http://localhost:7474/db/data/node/1700" 
	 * node id: 1700
	 * 
	 * @param url The url to extract the id from
	 * @returns The extracted id
	 */
	function extractIdFromNeo4jUrl(url) {
		var pos = url.lastIndexOf("/");
		return $.trim(url.substring(pos+1, url.length));
	} 
	
	neo4jTabularNodeViewer.initControls = function () {
		// quick and dirty...
		$("#controls").append("View node \n");
		$("#controls").append("<input type='text' value='node id' />\n");
		$("#controls").append("<button>Go</button>\n");

		$("#controls button").click(function () {

			var nodeId = getNodeIdInControls();
			
			console.log("input id: " + nodeId);
			updateView(nodeId);
		});
		
		$("#controls input:text").keypress(function(event){
			var keycode = (event.keyCode ? event.keyCode : event.which);
			if(keycode === 13) { // enter key
				var nodeId = getNodeIdInControls();
				
				console.log("input id: " + nodeId);
				updateView(nodeId);
			}
		});
	}

	neo4jTabularNodeViewer.loadNode = function (id) {
		//  check the id
		id = parseInt(id);
		if (isNaN(id)) 
			return;

		setNodeIdInControls(id);

		$("#result").append("<h2>Node " + id + "</h2>\n");

		var request = $.ajax({
			url : constructNodeUrl(id),
			type : "GET",
			dataType : "json"
		});

		request.done(function(json) {
			console.log(json);
			renderProperties(json.data);
			
			var id = extractIdFromNeo4jUrl(json.self);
			loadRelationShips(constructNodeRelationshipsUrl(id));
		});

		request.fail(function(jqXHR, textStatus) {
			alert("Request failed: " + textStatus);
		});
	}
	
}( window.neo4jTabularNodeViewer = window.neo4jTabularNodeViewer || {}, jQuery ));

