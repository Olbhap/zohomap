<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <script type="text/javascript" src="http://www.google.com/jsapi?key=<%= request.getAttribute("gMapApiKey") %>"></script>
    <script type="text/javascript">
      google.load("maps", "2");
      google.load("search", "1");
      
      var zohoMarkers = <%= request.getAttribute("jsonMarkers") %>;

      var kmlMmapUrl = "http://www.panoramio.com/kml/"; // Throw in some pictures for good measure
      var map;
      // Call this function when the page has been loaded
      function initialize() {
        map = new google.maps.Map2(document.getElementById("map")); 
        map.setCenter( new google.maps.LatLng(59.9153337, 10.7285341), 4);
        map.setZoom(3);
        map.addControl(new google.maps.LargeMapControl());
        map.enableScrollWheelZoom();
        var countryOverlay = new google.maps.GeoXml("http://www.gelib.com/maps/_NL/world-borders.kml");
        var panoramioOverlay = new google.maps.GeoXml(kmlMmapUrl);
        //map.addOverlay(countryOverlay);
        //map.addOverlay(panoramioOverlay);
        addMarkers(zohoMarkers);
      }
      
      google.setOnLoadCallback(initialize);

	  function addMarkers(markers) {
	  	for (i=0; i<markers.length; i++) {
	  	  var p = markers[i]["Placemark"][0]["Point"]["coordinates"];
          var marker = new GMarker(new google.maps.LatLng(p[1], p[0]));
          map.addOverlay(marker);
	  	}
	  }
     
    </script>

  </head>
  <body>
    <h1>Leads worldwide</h1>
    <div id="map" style="width: 700px; height: 500px"></div>
    <div id="searchcontrol"></div>
  </body>

</html>