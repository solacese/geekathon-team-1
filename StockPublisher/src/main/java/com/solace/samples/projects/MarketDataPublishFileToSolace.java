/**
 *  Copyright 2012-2018 Solace Corporation. All rights reserved.
 *
 *  http://www.solace.com
 *
 *  This source is distributed under the terms and conditions
 *  of any contract or contracts between Solace and you or
 *  your company. If there are no contracts in place use of
 *  this source is not authorized. No support is provided and
 *  no distribution, sharing with others or re-use of this
 *  source is authorized unless specifically stated in the
 *  contracts referred to above.
 *
 *  HelloWorldPub
 *
 *  This sample shows the basics of creating session, connecting a session,
 *  and publishing a direct message to a topic. This is meant to be a very
 *  basic example for demonstration purposes.
 */

package com.solace.samples.projects;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Time;
import java.util.List;
import java.util.Random;

import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageProducer;

public class MarketDataPublishFileToSolace {
	
    static String textFileName = "";
    
    final static String[] headings = new String[] {"instrumentName","instrumentSymbol","currency","price","volume","lastTraded","delta"};
    
    /**
     * @param args
     * @throws JCSMPException
     * @throws InterruptedException
     * @throws IOException
     */
    public static void main(String... args) throws JCSMPException, InterruptedException, IOException {
    	// Check command line arguments
        if (args.length < 4) {
            System.out.println("Usage: MarketDataPublishFileToSolace <msg_backbone_ip:port> <vpn> <client-username> <client-password> <Inputfile>");
            System.exit(-1);
        }
        System.out.println("MarketDataPublishFileToSolace initializing...");

    	// Create a JCSMP Session
        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, args[0]);      // msg-backbone ip:port
        properties.setProperty(JCSMPProperties.VPN_NAME, args[1]);  // message-vpn
        properties.setProperty(JCSMPProperties.USERNAME, args[2]);  // client-username
        properties.setProperty(JCSMPProperties.PASSWORD, args[3]);  // client-password
        final JCSMPSession session =  JCSMPFactory.onlyInstance().createSession(properties);
                
        
      //  final Topic topic = JCSMPFactory.onlyInstance().createTopic("geekathon/stock/price");
        
        session.connect();
        /** Anonymous inner-class for handling publishing events */
        XMLMessageProducer prod = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
            public void responseReceived(String messageID) {
                System.out.println("Producer received response for msg: " + messageID);
            }
            public void handleError(String messageID, JCSMPException e, long timestamp) {
                System.out.printf("Producer received error for msg: %s@%s - %s%n",
                        messageID,timestamp,e);
            }
        });

        /* File Format
         *  Item 1: instrumentName
         *  Item 2: instrumentSymbol
         *  Item 3: currency
         *  Item 4: price
         *  Item 5: volume
         *  Item 6: lastTraded
         *  Item 7: delta
         */
        
        String filename = args[4];
        
        TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
        
        File file = new File(filename);
        int i = 10;
        while (i==10) {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8); 
        for (String line : lines) { 
           String[] array = line.split(","); 
           String instrumentName = array[0];
           String instrumentSymbol = array[1];
           String currency = array[2];
           
           final Random randomGenerator = new Random();
           final int millisInDay = 24*60*60*1000;
           
           String topicName = "geekathon/stock/price/" + array[1];
           StringBuilder jsonPayload = new StringBuilder("{");
           jsonPayload.append('"').append(headings[0]).append("\":\"").append(instrumentName).append("\",")
           			  .append('"').append(headings[1]).append("\":\"").append(instrumentSymbol).append("\",")
           			  .append('"').append(headings[2]).append("\":\"").append(currency).append("\",")
           			  .append('"').append(headings[3]).append("\":\"").append(randomGenerator.nextInt(1000)).append("\",")
           			  .append('"').append(headings[4]).append("\":\"").append(randomGenerator.nextInt(10000)).append("\",")
           			  .append('"').append(headings[5]).append("\":\"").append(new Time((long)randomGenerator.nextInt(millisInDay))).append("\",")
           			  .append('"').append(headings[6]).append("\":\"").append(randomGenerator.nextInt(15) * (randomGenerator.nextBoolean() ? -1 : 1)).append('"').append("}");
           //String message=instrumentName + "," + instrumentSymbol + "," + currency + "," + randomGenerator.nextInt(1000) + "," + randomGenerator.nextInt(10000) + "," + new Time((long)randomGenerator.nextInt(millisInDay)) + "," + randomGenerator.nextInt(15) * (randomGenerator.nextBoolean() ? -1 : 1);
           
           
           Topic topic = JCSMPFactory.onlyInstance().createTopic(topicName);
           msg.setText(jsonPayload.toString());
           prod.send(msg,topic);
        } 
    } 
                
        System.out.println("text file done!");
        System.out.println("Text File Messages sent. Exiting.");
        session.closeSession();
    }
}
