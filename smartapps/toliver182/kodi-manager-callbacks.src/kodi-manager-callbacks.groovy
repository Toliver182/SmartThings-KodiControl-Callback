/**
 *  KODI Manager
 *
 *  forked from a plex version: https://github.com/iBeech/SmartThings/tree/master/PlexManager
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "KODI Manager - Callbacks",
    namespace: "toliver182",
    author: "toliver182",
    description: "Add kodi endpoints",
    category: "Safety & Security",
    iconUrl: "https://raw.githubusercontent.com/xbmc/xbmc/master/media/icon48x48.png",
    iconX2Url: "https://raw.githubusercontent.com/xbmc/xbmc/master/media/icon120x120.png",
    iconX3Url: "https://raw.githubusercontent.com/xbmc/xbmc/master/media/icon256x256.png",
    oauth: true)


preferences {
	page(name: "pgSettings")
    page(name: "pgURL") 
   }
   
   //PAGES
///////////////////////////////
def pgSettings() {
    dynamicPage(name: "pgSettings", title: "Settings") {
      section("Kodi Client"){
      	input "clientName", "text", "title": "Client Name", multiple: false, required: true
  		input "kodiIp", "text", "title": "Kodi IP", multiple: false, required: true
        input "kodiPort", "text", "title": "Kodi port", multiple: false, required: true
    	input "kodiUsername", "text", "title": "Kodi Username", multiple: false, required: false
    	input "kodiPassword", "password", "title": "Kodi Password", multiple: false, required: false
    	input "theHub", "hub", title: "On which hub?", multiple: false, required: true
  }
        section("View URLs"){
        	href( "pgURL", description: "Click here to view URLs", title: "")
        }
    }
}

def pgURL(){
    dynamicPage(name: "pgURL", title: "URLs" , uninstall: false, install: true) {
    	if (!state.accessToken) {
        	createAccessToken() 
    	}
    	def url = apiServerUrl("/api/token/${state.accessToken}/smartapps/installations/${app.id}/")
    	section("Instructions") {
            paragraph "This app is designed to work with the xbmc.callbacks2 plugin for Kodi. Please download and install callbacks2 and in its settings assign the following URLs for corresponding events:"
            input "playvalue", "text", title:"Web address to copy for play command:", required: false, defaultValue:"${url}play"
            input "stopvalue", "text", title:"Web address to copy for stop command:", required: false, defaultValue:"${url}stop"
            input "pausevalue", "text", title:"Web address to copy for pause command:", required: false, defaultValue:"${url}pause"
            input "resumevalue", "text", title:"Web address to copy for resume command:", required: false, defaultValue:"${url}resume"
            paragraph "If you have more than one Kodi install, you may install an additional copy of this app for unique addresses specific to each room."
        }
    }
}
//END PAGES
/////////////////////////





def installed() {
	
	log.debug "Installed with settings: ${settings}"
	initialize()
    
}

def initialize() {
checkKodi();
subscribe(location, null, response, [filterEvents:false])  
}

def updated() {
unsubscribe();
initialize()

}

//Incoming state changes from kodi

mappings {

	path("/play") {
		action: [
			GET: "stateIsPlay"
		]
	}
	path("/stop") {
		action: [
			GET: "stateIsStop"
		]
	}
	path("/pause") {
		action: [
			GET: "stateIsPause"
		]
	}
    path("/stateIsPause") {
        action: [
            GET: "resume"
        ]
	}  
}
void stateIsPlay() {
	//Code to execute when playback started in KODI
    log.debug "Play command started"
	//Find client
    def children = getChildDevices()
    def KodiClient = children.find{ d -> d.deviceNetworkId.contains(NetworkDeviceId()) }  
    //Set State
    KodiClient.setPlaybackState("playing")
    getPlayingtitle()
}
void stateIsStop() {
	//Code to execute when playback stopped in KODI
    log.debug "Stop command started"
   	//Find client
    def children = getChildDevices()
    def KodiClient = children.find{ d -> d.deviceNetworkId.contains(NetworkDeviceId()) }  
    //Set State
    KodiClient.setPlaybackState("stopped")
 }
void stateIsPause() {
	//Code to execute when playback paused in KODI
    log.debug "Pause command started"
   	//Find client
    def children = getChildDevices()
    def KodiClient = children.find{ d -> d.deviceNetworkId.contains(NetworkDeviceId()) }  
    //Set State
    KodiClient.setPlaybackState("paused")
    getPlayingtitle()
}
void stateIsResume() {
	//Code to execute when playback resumed in KODI
    log.debug "Resume command started"
	//Find client
    def children = getChildDevices()
    def KodiClient = children.find{ d -> d.deviceNetworkId.contains(NetworkDeviceId()) }  
    //Set State
    KodiClient.setPlaybackState("playing")
    getPlayingtitle()
}



def response(evt) {	 
    def msg = parseLanMessage(evt.description);
}


//Incoming command handler
def switchChange(evt) {

    // We are only interested in event data which contains 
    if(evt.value == "on" || evt.value == "off") return;   
    
	//log.debug "Kodi event received: " + evt.value;

    def kodiIP = getKodiAddress(evt.value);
    
    // Parse out the new switch state from the event data
    def command = getKodiCommand(evt.value);
   
    //log.debug "state: " + state
    
    switch(command) {
    	case "next":
        	log.debug "Sending command 'next' to " + kodiIP
            next(kodiIP);
        break;
        
        case "previous":
        	log.debug "Sending command 'previous' to " + kodiIP
            previous(kodiIP);
        break;
        
        case "play":
        case "pause":
        	playpause(kodiIP);
        break;
        case "stop":
    		stop(kodiIP);
        break;
        case "scanNewClients":
        	getClients();
            
        case "setVolume":
        	def vol = getKodiVolume(evt.value);
            log.debug "Vol is: " + vol
        	setVolume(kodiIP, vol);
        break;
    }
    
    return;
}



//Child device setup
def checkKodi() {

		log.debug "Checking to see if the client has been added"
    
    	def children = getChildDevices()  ;
  		def childrenEmpty = children.isEmpty();  
      
        
     	def KodiClient = children.find{ d -> d.deviceNetworkId.contains(NetworkDeviceId()) }  
     
        if(!KodiClient){
        log.debug "No Devices found, adding device"
		KodiClient = addChildDevice("toliver182", "Kodi Client", NetworkDeviceId() , theHub.id, [label:"$settings.clientName", name:"$settings.clientName"])
        log.debug "Added Device"
        }
        else
        {
        log.debug "Device Already Added"
        }
        subscribe(KodiClient, "switch", switchChange)
}



//Commands to kodi
def playpause(kodiIP) {
	log.debug "playpausehere"
	def command = "{\"jsonrpc\": \"2.0\", \"method\": \"Player.PlayPause\", \"params\": { \"playerid\": 1 }, \"id\": 1}"
	executeRequest("/jsonrpc", "POST",command);
}

def next(kodiIP) {
	log.debug "Executing 'next'"
	def command = "{\"jsonrpc\": \"2.0\", \"method\": \"Player.GoTo\", \"params\": { \"playerid\": 1, \"to\": \"next\" }, \"id\": 1}"
    executeRequest("/jsonrpc", "POST",command)
}

def stop(kodiIP){
	def command = "{ \"id\": 1, \"jsonrpc\": \"2.0\", \"method\": \"Player.Stop\", \"params\": { \"playerid\": 1 } }"
    executeRequest("/jsonrpc", "POST",command)
}

def previous(kodiIP) {
	log.debug "Executing 'next'"
	def command = "{\"jsonrpc\": \"2.0\", \"method\": \"Player.GoTo\", \"params\": { \"playerid\": 1, \"to\": \"previous\" }, \"id\": 1}"
    executeRequest("/jsonrpc", "POST",command)
}

def setVolume(kodiIP, level) {
//TODO
	def command = "{\"jsonrpc\": \"2.0\", \"method\": \"Application.SetVolume\", \"params\": { \"volume\": "+ level + "}, \"id\": 1}"
    executeRequest("/jsonrpc", "POST",command)
}
def getPlayingtitle(){
def command = "{\"jsonrpc\": \"2.0\", \"method\": \"Player.GetItem\", \"params\": { \"properties\": [\"title\", \"album\", \"artist\", \"season\", \"episode\", \"duration\", \"showtitle\", \"tvshowid\", \"thumbnail\", \"file\", \"fanart\", \"streamdetails\"], \"playerid\": 1 }, \"id\": \"VideoGetItem\"}"
	executeRequest("/jsonrpc", "POST",command);

}

//main command handler
def executeRequest(Path, method, command) {
    log.debug "Sending command to $settings.kodiIp"
	def headers = [:] 
    
	headers.put("HOST", "$settings.kodiIp:$settings.kodiPort")
    if("$settings.kodiUsername" !="" ){
    def basicAuth = basicAuthBase64();
    headers.put("Authorization", "Basic " + basicAuth )
    }else{
    log.debug "No Auth needed"
    }
    headers.put("Content-Type", "application/json")
	try {    
		def actualAction = new physicalgraph.device.HubAction(
		    method: method,
		    path: Path,
            body: command,
		    headers: headers)
			
		sendHubCommand(actualAction)        
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}




// Helpers
private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    //log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    //log.debug hexport
    return hexport
}

def String getKodiCommand(deviceNetworkId) {
	def parts = deviceNetworkId.tokenize('.');
	return parts[1];
}
def String getKodiVolume(evt) {
	def parts = evt.tokenize('.');
	return parts[2];
}
private String NetworkDeviceId(){
    def iphex = convertIPtoHex(settings.kodiIp).toUpperCase()
    def porthex = convertPortToHex(settings.kodiPort).toUpperCase()
    return "$iphex:$porthex" 
}

//Method for encoding username and password in base64
def basicAuthBase64() {
def s ="$settings.kodiUsername:$settings.kodiPassword"
def encoded = s.bytes.encodeBase64();
return encoded
}

def String getKodiAddress(deviceNetworkId) {
def ip = deviceNetworkId.replace("KodiClient:", "");
	def parts = ip.tokenize('.');
	return parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
}