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
var ottawa       = new L.LatLng(45.41311, -75.63806);
var sanfrancisco = new L.LatLng(37.7805, -122.419);

var initLocation = sanfrancisco;

var map = new L.Map('map', {
	minZoom : 10,
	maxZoom : 16
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
var osmLayer = new L.TileLayer(arrayOSM[0], {maxZoom: 16, attribution: osmAttrib});

var aerAttrib = 'Map data &copy; 2011 OpenStreetMap contributors';
var aerLayer = new L.TileLayer(arrayAerial[0], {maxZoom: 16, attribution: aerAttrib});

var analyst = new L.TileLayer.WMS("/opentripplanner-analyst-core/wms", {
    layers: 'hagerstrand',
    styles: 'transparent',
    format: 'image/png',
    transparent: true,
    time:      "2012-03-01T08:00:00Z",
    DIM_TIMEB: "2012-03-01T10:00:00Z",
    attribution: osmAttrib,
    maxZoom: 16
});

var analystTile = new L.TileLayer(
	"/opentripplanner-analyst-core/tile/{z}/{x}/{y}.png?time=2011-12-06T08:00:00Z&lon=-122.6745&lat=45.5191", 
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

var baseMaps = {
    "OSM": osmLayer,
    "Aerial Photo": aerLayer
};
	        
var overlayMaps = {
    "Analyst WMS": analyst,
    "Analyst Tiles": analystTile
};

	        

var origMarker = new L.Marker(initLocation, {draggable: true});
var destMarker = new L.Marker(initLocation, {draggable: true});
//marker.bindPopup("I am marker.");
origMarker.on('dragend', refresh);
destMarker.on('dragend', refresh);

map.addLayer(aerLayer);
map.addLayer(analyst);
map.addLayer(origMarker);
map.addLayer(destMarker);
map.setView(initLocation, 13);

var layersControl = new L.Control.Layers(baseMaps, overlayMaps);
map.addControl(layersControl);

refresh();


// tools
var downloadTool = function () { 
    var params = {
        format: document.getElementById('downloadFormat').value,
        srs: document.getElementById('downloadProj').value,
        layers: document.getElementById('downloadLayer').value,
        resolution: document.getElementById('downloadResolution').value
    };

    // TODO: this bounding box needs to be reprojected!
    var bounds = map.getBounds();
    var bbox;

    // reproject
    var src = new Proj4js.Proj('EPSG:4326');
    // TODO: undefined srs?
    var dest = new Proj4js.Proj(params.srs);

    // wait until ready then execute
    var interval;
    interval = setInterval(function () {
        // if not ready, wait for next iteration
        if (!(src.readyToUse && dest.readyToUse))
            return;

        // clear the interval so this function is not called back.
        clearInterval(interval);

        var swll = bounds.getSouthWest();
        var nell = bounds.getNorthEast();
        
        var sw = new Proj4js.Point(swll.lng, swll.lat);
        var ne = new Proj4js.Point(nell.lng, nell.lat);

        Proj4js.transform(src, dest, sw);
        Proj4js.transform(src, dest, ne);

        // left, bot, right, top
        bbox = [sw.x, sw.y, ne.x, ne.y].join(',');


        var url = '/opentripplanner-analyst-core/wms?layers=' + params.layers +
            '&format=' + params.format + 
            '&srs=' + params.srs +
            '&resolution=' + params.resolution +
            '&bbox=' + bbox +
            '&DIM_ORIGINLAT=' + analyst.wmsParams.DIM_ORIGINLAT +
            '&DIM_ORIGINLON=' + analyst.wmsParams.DIM_ORIGINLON +
            '&time=' + analyst.wmsParams.time +
            '&DIM_ORIGINLATB=' + analyst.wmsParams.DIM_ORIGINLATB + 
            '&DIM_ORIGINLONB=' + analyst.wmsParams.DIM_ORIGINLONB +
            '&DIM_TIMEB=' + analyst.wmsParams.DIM_TIMEB;

        window.open(url);
    }, 1000); // this is the end of setInterval, run every 1s
};
