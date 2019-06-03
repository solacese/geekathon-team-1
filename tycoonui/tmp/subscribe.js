var sessionProps = null;
var mySession = null;

// ---------------------------------------------------------------------
// connection inputs
//
var RouterURL = "http://192.168.56.104:88/solace" ;
var UserName = "default" ;
var UserPassword = "default" ;
var VpnName = "default" ;
var TopicStr = "topic1" ;

// define callbacks
var sessionEventCb = function(session, event) 
{
   //consoleLog("Session event received");
   consoleLog(event.toString());
}
var messageEventCb = function(session, message) 
{
   consoleLog("Message received");
   messageLog(message.dump());
}

// utility functions
// pad zero if < 10
function padZero(d) 
{
    if (d < 10) { d = "0" + d }
    return d;
}

// get timestamp now
function getTimeStamp() 
{
    var t = new Date()
    var str = padZero(t.getMonth()+1) + "/" + padZero(t.getDate()) + "/" + t.getFullYear() + " " + padZero(t.getHours()) + ":" + padZero(t.getMinutes()) + ":" + padZero(t.getSeconds());
    return str ;
}


function consoleLog(s) 
{
    var s1 = "[" + getTimeStamp() + "] " ;
    s1 += s + "\n";
    document.console_area.outtext.value += s1 ;
}

function ClearConsole() 
{
    var s1 = "[" + getTimeStamp() + "] " ;
    s1 += "console cleared" + "\n";
    document.console_area.outtext.value = s1 ;
}

function messageLog(s) 
{
    var s1 = "[" + getTimeStamp() + "] " ;
    s1 += s + "\n";
    document.message_area.outtext.value += s1 ;
}

// ------------------------------------------------------------------------
// create solace session and connect
// 
function CreateSession() 
{

    try {
    sessionProps = new solace.SessionProperties();
    sessionProps.connectTimeoutInMsecs = 25000;
    sessionProps.transportDowngradeTimeoutInMsecs = 5000;
    sessionProps.readTimeoutInMsecs = 30000;;
    sessionProps.keepAliveIntervalsLimit = 10;
    //sessionProps.userName = UserName ;
    //sessionProps.vpnName = VpnName ;
    //sessionProps.password = UserPassword ;
    //sessionProps.url = RouterURL ;
    sessionProps.url = document.getElementById("url").value ;
    sessionProps.vpnName = document.getElementById("vpnname").value ;
    sessionProps.userName = document.getElementById("username").value ;
    sessionProps.password = document.getElementById("password").value ;
    sessionProps.reapplySubscriptions = false ;
    sessionProps.keepAliveIntervalInMsecs = 3000;

    var o = " URL: "+sessionProps.url ;
    o += " User: "+sessionProps.userName ;
    o += " VPN: "+sessionProps.vpnName ;
    consoleLog ("Creating Session to " + o);

    mySession = solace.SolclientFactory.createSession(sessionProps,
	    new solace.MessageRxCBInfo(function(session, message) {
		    messageEventCb(session, message);
	    }, this),
	    new solace.SessionEventCBInfo(function(session, event) {
		sessionEventCb(session, event);
	    }, this)); 

       consoleLog ("Connecting to session") ;
       mySession.connect();
       consoleLog("done") ;

       // UI updates
       document.getElementById("connectBtn").disabled = true;
       document.getElementById("subscribeBtn").disabled = false;
       document.getElementById("closeBtn").disabled = false;
    } 
    catch (error) {
       consoleLog("Session creation/connect failed");
       consoleLog(error.toString());
    }
     
}

// ------------------------------------------------------------------------
// connect to solace session
// unused now
//
function ConnectSession() 
{

        try {
            if (connectedOnce) {
                mySession.connect();
            }
            else {
                mySession.connect();
                connectedOnce = true;
            }
        } catch (error) {
            consoleLog("Failed to connect session");
            consoleLog(error.toString());
        }
}

// ------------------------------------------------------------------------
// subscribe msg to session
//
function SubscribeMsg() 
{
	var msg = solace.SolclientFactory.createMessage();
	var topicStr = document.getElementById("topicname").value ;
	var timeoutSec = parseInt(document.getElementById("timeout").value) ;
	msg.setDestination(solace.SolclientFactory.createTopic(topicStr));
	consoleLog("Subscribeing to: " + topicStr);
	// UI update
        document.getElementById("subscribeBtn").disabled = true;
	try {
	   mySession.subscribe(solace.SolclientFactory.createTopic(topicStr),
                true, // confirmation on subscription 
                topicStr, // correlation-key
                timeoutSec // timeout in seconds
            );
	} catch (error) {
	   // failed to subsribe,
	   consoleLog("Failed to send message '" + msg.toString() + "'");
	   consoleLog(error.toString() + error.Message);
	}
}


// ------------------------------------------------------------------------
// Close session
//
function CloseSession() {
        consoleLog("Closing session");
        if (mySession !== null) {
            try {
                mySession.dispose();
		mySession = null;
		// UI updates
                document.getElementById("connectBtn").disabled = false ;
                document.getElementById("subscribeBtn").disabled = true;
                document.getElementById("closeBtn").disabled = true ;
            } catch (error) {
                consoleLog("Failed to close session");
                consoleLog(error.toString());
            }
        }
    };
