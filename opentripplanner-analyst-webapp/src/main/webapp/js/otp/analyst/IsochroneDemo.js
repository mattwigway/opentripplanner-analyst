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

    map :               null,
    locationField :     null,
    dateField :         null,   
    timeField :         null,   
    timeSlider :        null,
    usePurpleLineCB :     null,
    seriesNumberSlider :   null,
    seriesIntervalSlider:   null,
    currentLocation :   null,
    reachableLayer :    null,
    maxSeriesTime :     0,
    isoLayers :         null,
    

    initialize : function(config) {
        
        var thisMain = this;
        this.isoLayers = new Array();
           
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
        this.map.addControl(new OpenLayers.Control.LayerSwitcher());
        
        var initLocationProj = new OpenLayers.LonLat(-77.03197, 38.994172).transform(
                new OpenLayers.Projection("EPSG:4326"), this.map.getProjectionObject());
        // -122.681944, 45.52    
        console.log("map proj: "+this.map.getProjectionObject());    
        var markers = new OpenLayers.Layer.Vector(
            "Markers",
            {
                styleMap: new OpenLayers.StyleMap({
                    // Set the external graphic and background graphic images.
                    externalGraphic: "js/lib/openlayers/img/marker.png",
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

        var runSingleButton = new Ext.Button({
            text: "Run",
            width: 100,
            handler: function(btn, evt) {
                thisMain.clearIsoLayers();
                thisMain.maxSeriesTime = 0;
                thisMain.isoQuery(thisMain.timeSlider.getValue(), false);
            }   
        });
        
        this.seriesNumberSlider = new Ext.slider.SingleSlider({
            fieldLabel: 'Number',
            value: 5,
            minValue: 1,
            maxValue: 20,
            plugins: new GeoExt.SliderTip()
        });

        this.seriesIntervalSlider = new Ext.slider.SingleSlider({
            fieldLabel: 'Interval (min.)',
            value: 15,
            minValue: 0,
            maxValue: 60,
            plugins: new GeoExt.SliderTip()
        });
        
        var runSeriesButton = new Ext.Button({
            text: "Run",
            width: 100,
            handler: function(btn, evt) {
                thisMain.clearIsoLayers();
                thisMain.maxSeriesTime = thisMain.seriesNumberSlider.getValue() * thisMain.seriesIntervalSlider.getValue();
                thisMain.isoQuery(thisMain.seriesIntervalSlider.getValue(), true);
            }   
        });

        var singleIsoPanel =  new Ext.Panel({
            layout: 'form',
            title: 'Single Isochrone',
            padding: 10,
            items: [ this.timeSlider, runSingleButton ],
            style: {
                marginTop: '15px'
            }
        });
        
        var isoSeriesPanel =  new Ext.Panel({
            layout: 'form',
            title: 'Isochrone Series',
            padding: 10,
            items: [ this.seriesNumberSlider, this.seriesIntervalSlider, runSeriesButton ],
            style: {
                marginTop: '15px'
            }
        });
                            
        var controlsPanel = new Ext.Panel({
            layout: 'form',
            title: 'Controls',
            padding: 10,
            width: 300,
            region: 'west',
            items: [ this.locationField, this.dateField, this.timeField, this.usePurpleLineCB, singleIsoPanel, isoSeriesPanel ] //this.timeSlider, runButton ]
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

    isoQuery : function(maxTime, inSeries) {
    
        var thisMain = this;
        
        //console.log(currentLocation.lon + ", " + currentLocation.lat + ", " + timeSlider.getValue());
        var url = "/opentripplanner-api-webapp/ws/iso?fromLat=" + 
                   this.currentLocation.lat + "&fromLon=" + this.currentLocation.lon + "&maxTime=" + this.timeSlider.getValue();
        console.log("inseries="+inSeries);        
        console.log(url);
        
        Ext.Ajax.request({
            url : '/opentripplanner-api-webapp/ws/iso',
            method: 'GET',
            params : {
                'fromLat' : this.currentLocation.lat,
                'fromLon' : this.currentLocation.lon,
                'startDate' : '10/27/2011', //this.dateField.getValue(),
                'startTime' : this.timeField.getValue(),
                'usePurpleLine' : this.usePurpleLineCB.getValue(),
                'maxTime' : maxTime*60 //this.timeSlider.getValue()*60
            },
            success: function ( result, request ) {
                var geojson = new OpenLayers.Format.GeoJSON('Geometry');
                
                var ret = geojson.read(result.responseText);
                //console.log(result.responseText);
                
                //console.log("ret arr size = "+ret.length);
                var geom = ret[0];
                
                //console.log("geom="+geom.geometry);
                /*var geom2 = geom.geometry.transform(
                    new OpenLayers.Projection("EPSG:4326"), thisMain.map.getProjectionObject());*/
                //console.log("geom2="+geom2);
                
                //if(thisMain.reachableLayer != null) thisMain.map.removeLayer(thisMain.reachableLayer);
                
                var colorPct = inSeries ? maxTime / (thisMain.seriesNumberSlider.getValue() * thisMain.seriesIntervalSlider.getValue()) : 0;
                console.log("cPct="+colorPct);                
                var styleMap = new OpenLayers.StyleMap({
                    fillColor:   'rgb('+Math.floor(colorPct*30)+', '+Math.floor(colorPct*144)+', 205)', //'#1e90ff', 
                    fillOpacity: 0.5,
                    strokeWidth: 3,
                    strokeColor: 'rgb(0,'+Math.floor(colorPct*191)+', 205)'
                });
                var isoLayer = new OpenLayers.Layer.Vector("Isochrone-"+maxTime, {
                    styleMap: styleMap    
                });
                
                var features = [ new OpenLayers.Feature.Vector(geom.geometry) ];
                isoLayer.addFeatures(features);
                
                thisMain.map.addLayer(isoLayer);
                thisMain.map.setLayerIndex(isoLayer, -maxTime);
                thisMain.isoLayers.push(isoLayer);
                
                if(inSeries && maxTime + thisMain.seriesIntervalSlider.getValue() <= thisMain.maxSeriesTime) {
                    thisMain.isoQuery(maxTime + thisMain.seriesIntervalSlider.getValue(), true);
                }
            },  // end of success function
            failure: function ( result, request ) {
                alert("fail " + result.responseText);
            }
        });
    },

    locationUpdated : function() {
        this.locationField.setValue(this.currentLocation.lat + "," + this.currentLocation.lon);
    },
    
    clearIsoLayers : function () {
        for(var i = 0; i < this.isoLayers.length; i++) {
            var layer = this.isoLayers[i];  
            console.log("layer="+layer);
            this.map.removeLayer(layer)
        }
        this.isoLayers = new Array();
    },

    CLASS_NAME: "otp.analyst.IsochroneDemo"

};

otp.analyst.IsochroneDemo = new otp.Class(otp.analyst.IsochroneDemo);
