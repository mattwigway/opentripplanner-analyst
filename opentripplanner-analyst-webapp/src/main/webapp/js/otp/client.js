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

var portland = new L.LatLng(45.5191, -122.6745); 
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
    layers: 'test',
    format: 'image/png',
    transparent: true,
    time: "2011-12-06T08:00:00Z",
    attribution: osmAttrib
});

var refresh = function (ll) {
	map.removeLayer(analyst);
    analyst.wmsParams.DIM_ORIGINLAT = ll.lat;
    analyst.wmsParams.DIM_ORIGINLON = ll.lng;
    map.addLayer(analyst);
};

map.on('click', function(e) {
	refresh(e.latlng);
});

map.setView(portland, 13);
map.addLayer(aerLayer);
map.addLayer(analyst);

refresh(portland);
