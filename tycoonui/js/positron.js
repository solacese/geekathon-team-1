var sessionProps = null;
var mySession = null;

// ---------------------------------------------------------------------
// connection inputs
//

// nram instance
// US Instance
//var RouterURL = "ws://mrrwtxvklujkj.messaging.solace.cloud:80"
//var RouterURL = "wss://mrrwtxvklujkj.messaging.solace.cloud:443"
//var UserName = "solace-cloud-client" ;
//var UserPassword = "ue5e437mnadlnimk9a0f2mmdc7" ;
//var VpnName = "msgvpn-rwtxvklujjp" ;
var ReqTopicPrefix = "team1/request" ;

var connection_info = { 
    //"reqtopic" : "team1/request" ,
    "num_connections" : 3,
    "connections" : [
      { "name" : "MDM_US",
        "host" : "wss://mrrwtxvklujkj.messaging.solace.cloud:443",
        "user" : "solace-cloud-client",
        "pass" : "ue5e437mnadlnimk9a0f2mmdc7",
        "vpn"  : "msgvpn-rwtxvklujjp" },

      { "name" : "MDM_ASIA",
        "host" : "wss://mrrwtxvkluj4f.messaging.solace.cloud:443",
        "user" : "solace-cloud-client",
        "pass" : "bv30csl37l1bsnm9iecnl1b199",
        "vpn"  : "msgvpn-rwtxvkluj3l" },

      { "name" : "MDM_LONDON",
        "host" : "wss://mrrwtxvkluiaz.messaging.solace.cloud:443",
        "user" : "solace-cloud-client",
        "pass" : "2h65j75fg2am70osog67epei62",
        "vpn"  : "msgvpn-rwtxvkluia5" },
    ],
} ;
//var ReqTopicPrefix = "basic/request" ;
//var RespTopicPrefix = "team1/response" ; // TODO - Req/Resp
var timeout = 5000 ;

// define callbacks
var sessionEventCb = function(session, event) 
{
   //consoleMsg("Session event received");
   consoleMsg(event.toString());
}
var messageEventCb = function(session, message) 
{
   consoleMsg("Message received");
   consoleMsg(message.dump());
   consoleMsg("<==== " + message.getDestination(), 1);
   consoleMsg(message.getXmlContent(), 1);
}


// Callback for replies
replyReceivedCb = function (session, message) {
        //console.log('Received reply dump: ' + message.dump());
        console.log('Received reply body: ' + message.getBinaryAttachment());
	const resp = JSON.parse(message.getBinaryAttachment());
           console.log ("status is : " + resp.status);
           console.log ("req code is : " + resp.request);
           //console.log ("req is : " + rmap(resp.request));
           console.log ("Writing to info box");
           document.form1.out_info.value = '' ;
           document.form1.out_info.value += '--- Got ' + resp.status + ' response for ' + resp.request + '\n';
	   if (typeof resp.message !== 'undefined') {
              document.form1.out_info.value += resp.message + '\n';
	   }
	   if (typeof resp.share_list !== 'undefined') {
	      for (i = 0; i < resp.share_list.length; i++) { 
	         r = resp.share_list[i]
                 document.form1.out_info.value += r.instrument + ' : ' + r.quantity + '\n';
	      }
	   }
	   if (typeof resp.ranking_list !== 'undefined') {
	      for (i = 0; i < resp.ranking_list.length; i++) { 
	         r = resp.ranking_list[i]
                 document.form1.out_info.value += r.name + ' : ' + r.credit + '\n';
	      }
	   }
	   //else {
           //document.form1.out_info.value += 'No share info returned' ;
	   //}
        //console.log('Received credit: ' + resp.credit) ;
	if (typeof resp.credit !== 'undefined') {
           document.form1.out_balance.value = resp.credit ;
	}
	//console.log (resp);
        //requestor.exit();

	if (resp.request == 99) {
	   console.log ('Disable Register and enable Actions');
           document.form1.in_name.disabled = true;
        document.form1.do_register.disabled = true;
        document.form1.do_buy.disabled = false;
        document.form1.do_sell.disabled = false;
        document.form1.do_check.disabled = false;
        document.form1.do_exit.disabled = false;
	}

	if (resp.request == 88) {
	   console.log ('Enaable Register and disable Actions');
           document.form1.in_name.disabled = false;
        document.form1.do_register.disabled = false;
        document.form1.do_buy.disabled = true;
        document.form1.do_sell.disabled = true;
        document.form1.do_check.disabled = true;
        document.form1.do_exit.disabled = true;
	}
};

// Callback for request failures
requestFailedCb = function (session, event) {
        console.log('Request failure: ' + event.toString());
        //requestor.exit();
};

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


function consoleMsg(s,t=0) 
{
    var s1 = "[" + getTimeStamp() + "] " ;
    s1 += s + "\n"; 
    console.log(s1);
    //if (t>0) {
    //document.form1.log.value += s1 ;
    //}
}

function ClearConsole() 
{
    var s1 = "[" + getTimeStamp() + "] " ;
    s1 += "console cleared" + "\n";
    //document.outform.outtext.value = s1 ;
    document.logTextArea.outtext.value = s1 ;
}

// ------------------------------------------------------------------------
// create solace session and connect
// 
function CreateSession() 
{
       console.log ("CreateSession") ;

    try {
    sessionProps = new solace.SessionProperties();
    sessionProps.connectTimeoutInMsecs = 25000;
    sessionProps.transportDowngradeTimeoutInMsecs = 5000;
    sessionProps.readTimeoutInMsecs = 30000;;
    sessionProps.keepAliveIntervalsLimit = 10;

    var i = Math.round(Math.random()*(connection_info.num_connections-1));
    var r = connection_info.connections[i] ;
    console.log ('index: ' + i + ' , r.name: ' + r.name);
    var ConnectioName = r.name ;
    var RouterURL = r.host ;
    var VpnName  = r.vpn ;
    var UserName = r.user ;
    var UserPassword = r.pass ;
    var ConnectionName = r.name ;

    sessionProps.userName = UserName ;
    sessionProps.vpnName = VpnName ;
    sessionProps.password = UserPassword ;
    sessionProps.url = RouterURL ;
    //sessionProps.url = document.getElementById("url").value ;
    //sessionProps.vpnName = document.getElementById("vpnname").value ;
    //sessionProps.userName = document.getElementById("username").value ;
    //sessionProps.password = document.getElementById("password").value ;
    sessionProps.reapplySubscriptions = false ;
    sessionProps.keepAliveIntervalInMsecs = 3000;

    console.log ("CreateSession --") ;
    var o = ConnectionName ;
    o += " URL: "+sessionProps.url ;
    o += " User: "+sessionProps.userName ;
    o += " VPN: "+sessionProps.vpnName ;
    console.log ("Creating Session to " + o);

    document.form1.out_info.value = '### Connecting to ' + ConnectionName ;

    mySession = solace.SolclientFactory.createSession(sessionProps,
	    new solace.MessageRxCBInfo(function(session, message) {
		    messageEventCb(session, message);
	    }, this),
	    new solace.SessionEventCBInfo(function(session, event) {
		sessionEventCb(session, event);
	    }, this)); 

       console.log ("Connecting to session") ;
       mySession.connect();
       console.log("connect call complete") ;

       // UI updates
       //document.getElementById("connectBtn").disabled = true;
       //document.getElementById("publishBtn").disabled = false;
       //document.getElementById("closeBtn").disabled = false;
    } 
    catch (error) {
       //consoleMsg("### Session creation/connect failed ###");
       console.log("### Session creation/connect failed ###");
       console.log(error.toString());
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
            consoleMsg("Failed to connect session");
            consoleMsg(error.toString());
        }
}

// ------------------------------------------------------------------------
// publish msg to session
//
function PublishMsg_UNUSED() 
{
	var msg = solace.SolclientFactory.createMessage();
	var topicStr = document.getElementById("topicname").value ;
	var messageStr = document.getElementById("message").value ;
	msg.setDestination(solace.SolclientFactory.createTopic(topicStr));
	msg.setBinaryAttachment(messageStr);
	msg.setDeliveryMode(solace.MessageDeliveryModeType.DIRECT);
	consoleMsg("Publishing req to: " + topicStr);
	consoleMsg("===> " + topicStr, 1);
	consoleMsg(messageStr, 1);
        try {
            mySession.sendRequest(
                msg,
                5000, // 5 seconds timeout for this operation
                function (session, message) {
                    replyReceivedCb(session, message);
                },
                function (session, event) {
                    requestFailedCb(session, event);
                },
                null // not providing correlation object
            );
        } catch (error) {
            console.log(error.toString());
        }

	//try {
	//   mySession.send(msg);
	//} catch (error) {
	//   // failed to send,
	//   consoleMsg("Failed to send message '" + msg.toString() + "'");
	//   consoleMsg(error.toString() + error.Message);
	//}
}

function DoRegister() 
{
	var msg = solace.SolclientFactory.createMessage();
	var in_name = document.getElementById("in_name").value ;
	var in_inst = document.getElementById("in_inst").value ;
	var in_num = document.getElementById("in_num").value ;

	var topicStr = ReqTopicPrefix ;
	//var messageStr = in_name + ",99" ;
	var messageStr = '99,' + in_name  ;
	msg.setDestination(solace.SolclientFactory.createTopic(topicStr));
	msg.setBinaryAttachment(messageStr);
	msg.setDeliveryMode(solace.MessageDeliveryModeType.DIRECT);
	console.log("Publishing Register request to: <" + topicStr + "> message: [" + messageStr + "] Timeout: "+ timeout);
	//consoleMsg("====> " + topicStr, 1);
	//consoleMsg(messageStr, 1);
        try {
            mySession.sendRequest(
                msg,
                timeout,
                function (session, message) {
                    replyReceivedCb(session, message);
                },
                function (session, event) {
                    requestFailedCb(session, event);
                },
                null // not providing correlation object
            );
        } catch (error) {
            console.log(error.toString());
        }
	//try {
	//   mySession.send(msg);
           //SubscribeResponseTopic();
	//} catch (error) {
	   // failed to send,
	//   console.log("Failed to send message '" + msg.toString() + "'");
	//   console.log(error.toString() + error.Message);
	//}
}

function DoBuy() 
{
	var msg = solace.SolclientFactory.createMessage();
	var in_name = document.getElementById("in_name").value ;
	var in_inst = document.getElementById("in_inst").value ;
	var in_num = document.getElementById("in_num").value ;
	var topicStr = ReqTopicPrefix ;
	//var messageStr = in_name + ",101" ;
	var messageStr = '101,' + in_name +"," + in_inst + "," + in_num ;
	msg.setDestination(solace.SolclientFactory.createTopic(topicStr));
	msg.setBinaryAttachment(messageStr);
	msg.setDeliveryMode(solace.MessageDeliveryModeType.DIRECT);
	console.log("Publishing Buy request to: <" + topicStr + "> message: [" + messageStr + "] Timeout: "+ timeout);
	//consoleMsg("====> " + topicStr, 1);
	//consoleMsg(messageStr, 1);
        try {
            mySession.sendRequest(
                msg,
                timeout,
                function (session, message) {
                    replyReceivedCb(session, message);
                },
                function (session, event) {
                    requestFailedCb(session, event);
                },
                null // not providing correlation object
            );
        } catch (error) {
            console.log(error.toString());
        }
	//try {
	//   mySession.send(msg);
           //SubscribeResponseTopic();
	//} catch (error) {
	   // failed to send,
	//   console.log("Failed to send message '" + msg.toString() + "'");
	//   console.log(error.toString() + error.Message);
	//}
}

function DoSell() 
{
	var msg = solace.SolclientFactory.createMessage();
	var in_name = document.getElementById("in_name").value ;
	var in_inst = document.getElementById("in_inst").value ;
	var in_num = document.getElementById("in_num").value ;
	var topicStr = ReqTopicPrefix ;
	//var messageStr = in_name + ",102" ;
	var messageStr = '102,' + in_name +"," + in_inst + "," + in_num ;
	msg.setDestination(solace.SolclientFactory.createTopic(topicStr));
	msg.setBinaryAttachment(messageStr);
	msg.setDeliveryMode(solace.MessageDeliveryModeType.DIRECT);
	console.log("Publishing Sell request to: <" + topicStr + "> message: [" + messageStr + "] Timeout: "+ timeout);
	//consoleMsg("====> " + topicStr, 1);
	//consoleMsg(messageStr, 1);
        try {
            mySession.sendRequest(
                msg,
                timeout,
                function (session, message) {
                    replyReceivedCb(session, message);
                },
                function (session, event) {
                    requestFailedCb(session, event);
                },
                null // not providing correlation object
            );
        } catch (error) {
            console.log(error.toString());
        }
}

function DoCheck() 
{
	var msg = solace.SolclientFactory.createMessage();
	var in_name = document.getElementById("in_name").value ;
	var in_inst = document.getElementById("in_inst").value ;
	var in_num = document.getElementById("in_num").value ;
	var topicStr = ReqTopicPrefix ;
	//var messageStr = in_name + ",101" ;
	var messageStr = '100,' + in_name ;
	msg.setDestination(solace.SolclientFactory.createTopic(topicStr));
	msg.setBinaryAttachment(messageStr);
	msg.setDeliveryMode(solace.MessageDeliveryModeType.DIRECT);
	console.log("Publishing check request to: <" + topicStr + "> message: [" + messageStr + "] Timeout: "+ timeout);
	//consoleMsg("====> " + topicStr, 1);
	//consoleMsg(messageStr, 1);
        try {
            mySession.sendRequest(
                msg,
                timeout,
                function (session, message) {
                    replyReceivedCb(session, message);
                },
                function (session, event) {
                    requestFailedCb(session, event);
                },
                null // not providing correlation object
            );
        } catch (error) {
            console.log(error.toString());
        }
}

function DoScore() 
{
	var msg = solace.SolclientFactory.createMessage();
	var in_name = document.getElementById("in_name").value ;
	var in_inst = document.getElementById("in_inst").value ;
	var in_num = document.getElementById("in_num").value ;
	var topicStr = ReqTopicPrefix ;

	//var messageStr = in_name + ",101" ;
	var messageStr = '88,' + in_name ;
	msg.setDestination(solace.SolclientFactory.createTopic(topicStr));
	msg.setBinaryAttachment(messageStr);
	msg.setDeliveryMode(solace.MessageDeliveryModeType.DIRECT);
	console.log("Publishing Score request to: <" + topicStr + "> message: [" + messageStr + "] Timeout: "+ timeout);
	//consoleMsg("====> " + topicStr, 1);
	//consoleMsg(messageStr, 1);
        try {
            mySession.sendRequest(
                msg,
                timeout,
                function (session, message) {
                    replyReceivedCb(session, message);
                },
                function (session, event) {
                    requestFailedCb(session, event);
                },
                null // not providing correlation object
            );
        } catch (error) {
            console.log(error.toString());
        }
}

// ------------------------------------------------------------------------
// subscribe msg to session
//
function SubscribeResponseTopic()
{
        var msg = solace.SolclientFactory.createMessage();
	var topicStr = RespTopicPrefix ;
        var timeoutSec = 60; //parseInt(document.getElementById("timeout").value) ;
        msg.setDestination(solace.SolclientFactory.createTopic(topicStr));
        consoleMsg("Subscribeing to: " + topicStr);
        // UI update
        //document.getElementById("subscribeBtn").disabled = true;
        try {
           mySession.subscribe(solace.SolclientFactory.createTopic(topicStr),
                true, // confirmation on subscription
                topicStr, // correlation-key
                timeoutSec // timeout in seconds
            );
        } catch (error) {
           // failed to subsribe,
           consoleMsg("Failed to send message '" + msg.toString() + "'");
           consoleMsg(error.toString() + error.Message);
        }
}

function SubscribeTGTopic()
{
        var msg = solace.SolclientFactory.createMessage();
	var topicStr = 'sixcoin/*/' + document.getElementById("cc").value ;
        var timeoutSec = 60; //parseInt(document.getElementById("timeout").value) ;
        msg.setDestination(solace.SolclientFactory.createTopic(topicStr));
        consoleMsg("Subscribeing to: " + topicStr);
        // UI update
        //document.getElementById("subscribeBtn").disabled = true;
        try {
           mySession.subscribe(solace.SolclientFactory.createTopic(topicStr),
                true, // confirmation on subscription
                topicStr, // correlation-key
                timeoutSec // timeout in seconds
            );
        } catch (error) {
           // failed to subsribe,
           consoleMsg("Failed to send message '" + msg.toString() + "'");
           consoleMsg(error.toString() + error.Message);
        }
}

// ------------------------------------------------------------------------
// Close session
//
function CloseSession() {
        consoleMsg("Closing session");
        if (mySession !== null) {
            try {
                mySession.dispose();
		mySession = null;
		// UI updates
                document.getElementById("connectBtn").disabled = false ;
                document.getElementById("publishBtn").disabled = true;
                document.getElementById("closeBtn").disabled = true ;
            } catch (error) {
                consoleMsg("Failed to close session");
                consoleMsg(error.toString());
            }
        }
    };
