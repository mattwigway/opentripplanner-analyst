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

otp.namespace("otp.analyst");

otp.analyst.IsochroneDemo = {

    map :                 null,
    locationField :       null,
    dateField :           null,   
    timeField :           null,   
    timeSlider :          null,
    usePurpleLineCB :     null,
    seriesNumberSlider :  null,
    seriesIntervalSlider: null,
    currentLocation :     null,
    reachableLayer :      null,
    maxSeriesTime :       0,
    isoLayer :            null,
    

    initialize : function(config) {
        
        var thisMain = this;
        this.isoLayer = null;
           
        this.map = new OpenLayers.Map();
        var arrayOSM = ["http://otile1.mqcdn.com/tiles/1.0.0/osm/${z}/${x}/${y}.png",
                        "http://otile2.mqcdn.com/tiles/1.0.0/osm/${z}/${x}/${y}.png",
                        "http://otile3.mqcdn.com/tiles/1.0.0/osm/${z}/${x}/${y}.png",
                        "http://otile4.mqcdn.com/tiles/1.0.0/osm/${z}/${x}/${y}.png"];
        var arrayAerial = ["http://oatile1.mqcdn.com/naip/${z}/${x}/${y}.png",
                           "http://oatile2.mqcdn.com/naip/${z}/${x}/${y}.png",
                           "http://oatile3.mqcdn.com/naip/${z}/${x}/${y}.png",
                           "http://oatile4.mqcdn.com/naip/${z}/${x}/${y}.png"];
            
        var baseOSM = new OpenLayers.Layer.OSM("MapQuest-OSM Tiles", arrayOSM);
        var baseAerial = new OpenLayers.Layer.OSM("MapQuest Open Aerial Tiles", arrayAerial);
        this.map.addLayer(baseOSM);
        this.map.addLayer(baseAerial);
        
        var url = "/opentripplanner-analyst-core/wms";
        var params = { // getmap query string
            layers : "test",
            //crs : this.map.getProjectionObject(),
            transparent : true,
            time : "2011-12-20T12:45:00Z"
        };
        var options = { // openlayers layer options
    		alwaysInRange : true,
            //'opacity': 0.8, 
    			visible : true,
            isBaseLayer : true,
            //numZoomLevels : 1
        };
        var isoLayer = new OpenLayers.Layer.WMS(
                'Isochrone',
                url,
                params,
                options );
        
        thisMain.map.addLayer(isoLayer);
        console.log(isoLayer);
        console.log(isoLayer.getFullRequestString());
        this.isoLayer = isoLayer;
      
        this.map.addControl(new OpenLayers.Control.LayerSwitcher());
        
        var initLocation = new OpenLayers.LonLat(-122.68, 45.50);
        var initLocationProj = initLocation.transform(
                new OpenLayers.Projection("EPSG:4326"), this.map.getProjectionObject());
        console.log("map proj: "+this.map.getProjectionObject());    
        var markers = new OpenLayers.Layer.Vector(
            "Markers",
            {
                styleMap: new OpenLayers.StyleMap({
                    // Set the external graphic and background graphic images.
                    externalGraphic: "js/lib/openlayers/img/marker-green.png",
                    graphicWidth: 21,
                    graphicHeight: 25,
                    graphicXOffset: -10.5,
                    graphicYOffset: -25
                }),
            }
        );
        
        var marker = new OpenLayers.Feature.Vector(
            new OpenLayers.Geometry.Point(initLocationProj.lon, initLocationProj.lat)
        );
            
        var features = [];
        features.push(marker);
        markers.addFeatures(features);
        this.map.addLayer(markers);

        var dragFeature = new OpenLayers.Control.DragFeature(markers);
        dragFeature.onComplete = function(evt) {
            thisMain.currentLocation = new OpenLayers.LonLat(marker.geometry.x, marker.geometry.y).transform(
                thisMain.map.getProjectionObject(), new OpenLayers.Projection("EPSG:4326"));       
            thisMain.locationUpdated();
        };

        this.map.addControl(dragFeature);
        dragFeature.activate();
        
        
        // set up the controls panel
        
        this.locationField = new Ext.form.TextField({
            fieldLabel: 'Current Location',
            anchor: '100%',
            readOnly: true
        });
        
        this.timeField = new Ext.form.TimeField({
            fieldLabel : 'Start Time',
            value :      new Date()
        });
        
        this.dateField = new Ext.form.DateField({
            fieldLabel : 'Start Date',
            value :      new Date()
        });
        
        this.usePurpleLineCB = new Ext.form.Checkbox({
            fieldLabel : 'Use Purple Line',
            value :      false
        });

        this.timeSlider = new Ext.slider.SingleSlider({
            fieldLabel: 'Max Time (min.)',
            value: 30,
            minValue: 0,
            maxValue: 120,
            plugins: new GeoExt.SliderTip()
        });

        this.runButton = new Ext.Button({
            text: "Run",
            width: 100,
            handler: function(btn, evt) {
                thisMain.isoQuery(thisMain.timeSlider.getValue(), false);
            }   
        });
        
//        var singleIsoPanel =  new Ext.Panel({
//            layout: 'form',
//            title: 'Single Isochrone',
//            padding: 10,
//            items: [ this.timeSlider, runButton ],
//            style: {
//                marginTop: '15px'
//            }
//        });
        
        var controlsPanel = new Ext.Panel({
            layout: 'form',
            title: 'Controls',
            padding: 10,
            width: 300,
            region: 'west',
            items: [ this.locationField, this.dateField, this.timeField, this.timeSlider, this.runButton ] //this.usePurpleLineCB, 
        });
        
        // set up the map panel
        var mapPanel = new GeoExt.MapPanel({
            map: this.map,
            title: 'Map',
            region: 'center'
        });
        
        // create the viewport
        new Ext.Viewport({
            layout: 'border',
            items: [ controlsPanel, mapPanel]
        });
                
        this.map.setCenter(initLocationProj, 10);
        this.currentLocation = initLocationProj.transform(
            this.map.getProjectionObject(), new OpenLayers.Projection("EPSG:4326")); 
        this.locationUpdated();
    },

//    isoQuery : function(maxTime, inSeries) {
//        var thisMain = this;
//        var url = "/opentripplanner-analyst-core/raster?fromLat=" + 
//                   this.currentLocation.lat + "&fromLon=" + 
//                   this.currentLocation.lon ;
//        console.log(url);
//        var options = {   
//        		'alwaysInRange' : true,
//                //'opacity': 0.8, 
//                'isBaseLayer': false,
//                numZoomLevels : 1 };
//        var newIsoLayer = new OpenLayers.Layer.Image(
//                'Isochrone',
//                url,
//                //this.map.getExtent(),
//                new OpenLayers.Bounds(-123.242146, 45.154576, -122.021497, 45.721398).transform(
//                		new OpenLayers.Projection("EPSG:4326"), this.map.getProjectionObject()),
//                new OpenLayers.Size(1904, 1260),
//                options);
//        
//        if (thisMain.isoLayer != null) 
//        	this.map.removeLayer(thisMain.isoLayer);
//        thisMain.map.addLayer(newIsoLayer);
//        //thisMain.map.setLayerIndex(isoLayer, -2);
//        thisMain.isoLayer = newIsoLayer;
//        console.log(this.map.getProjectionObject());
//        console.log(this.map.getExtent());
//        console.log(newIsoLayer);
//    },

    isoQuery : function(maxTime, inSeries) {
        var thisMain = this;
//        var url = "/opentripplanner-analyst-core/wms";
//        var params = { // getmap query string
//            layers : "test",
//            //crs : this.map.getProjectionObject(),
//            transparent : true,
//            time : 0,
//            DIM_ORIGINLON : this.currentLocation.lon,
//            DIM_ORIGINLAT : this.currentLocation.lat
//        };
//        var options = { // openlayers layer options
//    		alwaysInRange : true,
//            //'opacity': 0.8, 
//    		visible : true,
//            isBaseLayer : false,
//            //numZoomLevels : 1
//        };
//        var newIsoLayer = new OpenLayers.Layer.WMS(
//                'Isochrone',
//                url,
//                params,
//                options );
        
//        if (thisMain.isoLayer != null)
//        	this.map.removeLayer(thisMain.isoLayer);
//        thisMain.map.addLayer(newIsoLayer);
//        //thisMain.map.setLayerIndex(isoLayer, -2);
//        thisMain.isoLayer = newIsoLayer;
//        //console.log(this.map.getProjectionObject());
//        //console.log(this.map.getExtent());
//        console.log(newIsoLayer);
//        console.log(newIsoLayer.getFullRequestString());
        thisMain.isoLayer.mergeNewParams( {
        	DIM_ORIGINLON : this.currentLocation.lon,
        	DIM_ORIGINLAT : this.currentLocation.lat } );
        console.log(thisMain.isoLayer);
        console.log(thisMain.isoLayer.getFullRequestString());
    },

    locationUpdated : function() {
        this.locationField.setValue(this.currentLocation.lat + "," + this.currentLocation.lon);
        this.map.setCenter(this.currentLocation.transform(
                new OpenLayers.Projection("EPSG:4326"),
                this.map.getProjectionObject()), 10);
        this.isoQuery(0, false);
    },
    
    CLASS_NAME: "otp.analyst.IsochroneDemo"

};

otp.analyst.IsochroneDemo = new otp.Class(otp.analyst.IsochroneDemo);
