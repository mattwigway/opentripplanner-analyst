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
	minZoom : 10,
	maxZoom : 17
});

var osmUrl = 'http://otile3.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png',
osmAttrib = 'Map data &copy; 2011 OpenStreetMap contributors',
osmLayer = new L.TileLayer(osmUrl, {maxZoom: 18, attribution: osmAttrib});

var analyst = new L.TileLayer.WMS("http://localhost:8080/opentripplanner-analyst-core/wms", {
    layers: 'test',
    format: 'image/png',
    transparent: true,
    time: "2011-12-06T08:00:00Z",
    DIM_ORIGINLAT: 45.5191, 
    DIM_ORIGINLON: -122.6745,
    attribution: osmAttrib
});

var refresh = function (ll) {
	map.removeLayer(analyst);
    analyst.wmsParams.DIM_ORIGINLAT = ll.lat;
    analyst.wmsParams.DIM_ORIGINLON = ll.lng;
    map.addLayer(analyst);
}

map.on('click', function(e) {
	refresh(e.latlng);
});

map.setView(portland, 13)
map.addLayer(osmLayer);
map.addLayer(analyst);

refresh(portland);
