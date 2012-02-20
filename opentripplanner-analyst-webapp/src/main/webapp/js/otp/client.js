/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

var portland     = new L.LatLng(45.5191, -122.6745); 
var portlandEast = new L.LatLng(45.5191, -122.6600); 

var map = new L.Map('map', {
	minZoom : 09,
	maxZoom : 18
});

var arrayOSM = ["http://otile1.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png",
                "http://otile2.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png",
                "http://otile3.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png",
                "http://otile4.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png"];
var arrayAerial = ["http://oatile1.mqcdn.com/naip/{z}/{x}/{y}.png",
                   "http://oatile2.mqcdn.com/naip/{z}/{x}/{y}.png",
                   "http://oatile3.mqcdn.com/naip/{z}/{x}/{y}.png",
                   "http://oatile4.mqcdn.com/naip/{z}/{x}/{y}.png"];

var osmAttrib = 'Map data &copy; 2011 OpenStreetMap contributors';
var osmLayer = new L.TileLayer(arrayOSM[0], {maxZoom: 18, attribution: osmAttrib});

var aerAttrib = 'Map data &copy; 2011 OpenStreetMap contributors';
var aerLayer = new L.TileLayer(arrayAerial[0], {maxZoom: 18, attribution: aerAttrib});

var analyst = new L.TileLayer.WMS("http://localhost:8080/opentripplanner-analyst-core/wms", {
    layers: 'hagerstrand',
    styles: 'transparent',
    format: 'image/png',
    transparent: true,
    time:      "2011-12-06T08:00:00Z",
    DIM_TIMEB: "2011-12-06T09:30:00Z",
    attribution: osmAttrib
});

var analystTile = new L.TileLayer(
	"http://localhost:8080/opentripplanner-analyst-core/tile/{z}/{x}/{y}.png?time=2011-12-06T08:00:00Z&lon=-122.6745&lat=45.5191", 
	{ attribution: osmAttrib }
);

var refresh = function () {
	var o = origMarker.getLatLng();
	var d = destMarker.getLatLng();
	map.removeLayer(analyst);
    analyst.wmsParams.DIM_ORIGINLAT  = o.lat;
    analyst.wmsParams.DIM_ORIGINLON  = o.lng;
    analyst.wmsParams.DIM_ORIGINLATB = d.lat;
    analyst.wmsParams.DIM_ORIGINLONB = d.lng;
    map.addLayer(analyst);
};

map.on('click', function(e) {
	refresh(e.latlng, e.latlng);
});

var baseMaps = {
    "OSM": osmLayer,
    "Aerial Photo": aerLayer
};
	        
var overlayMaps = {
    "Analyst WMS": analyst,
    "Analyst Tiles": analystTile
};

	        

var origMarker = new L.Marker(portland, {draggable: true});
var destMarker = new L.Marker(portlandEast, {draggable: true});
//marker.bindPopup("I am marker.");
origMarker.on('dragend', refresh);
destMarker.on('dragend', refresh);

map.addLayer(aerLayer);
map.addLayer(analyst);
map.addLayer(origMarker);
map.addLayer(destMarker);
map.setView(portland, 13);

var layersControl = new L.Control.Layers(baseMaps, overlayMaps);
map.addControl(layersControl);

refresh();
