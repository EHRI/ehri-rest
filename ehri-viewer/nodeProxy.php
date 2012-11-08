<<?php 
    /** 
     * Only use for demonstration purposes 
     * eventually we need a nice RESTfull API exposing nodes
     */

    // make sure we have no injection here, only allow numbers
    if(!isset($_GET['id']) || is_numeric($_GET['id']) == FALSE) {
  		header('HTTP/1.1 404 Not Found');
  		return;
	}
	
	$id = $_GET['id'];

	// detect if relationships are requested
	$ext = "";
	if(isset($_GET['relationships'])) {
		$ext = "/relationships/all";
	}
	
	/** I love my daily sockets!
	 *
	 * if $id = 2 we want for example
	 * http://localhost:7474/db/data/node/2 
	 */
    $port = 7474;
    
    $fp = fsockopen('127.0.0.1', $port, $errno, $errstr, 30);
    if (!$fp) {
        echo "$errstr ($errno)<br />\n";
    } else {
        $out = "GET /db/data/node/" . $id . $ext . " HTTP/1.1\r\n";
        $out .= "Host: localhost:" . $port . "\r\n";
        $out .= "Accept: */*\r\n";
        $out .= "Connection: Close\r\n\r\n";
        fwrite($fp, $out);
        while (!feof($fp)) {
            $response .= fgets($fp, 128);
        }
        fclose($fp);
    
        list($header, $body) = explode("\r\n\r\n", $response, 2); 
        
        // Note that we could force the headers
        //header('Content-type: application/json');
        //header('Content-Encoding: UTF-8');
    
    	// copy all the headers
        $headerList = explode("\r\n",  $header);
        for($i = 0; $i < count($headerList); ++$i) {
    	    header($headerList[$i]);
        }
        
        echo $body;       
    }
?>
