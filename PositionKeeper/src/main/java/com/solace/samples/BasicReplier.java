/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.solace.samples;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.nio.charset.*;
import java.io.UnsupportedEncodingException;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
 
class OpenReply_OK {
    public final int request = 99;
    public final String status = "OK";
    public float credit;
}

class OpenReply_NG {
    public final int request = 99;
    public final String status = "NG";
    public float credit;
}

class Reply_NG {
    public int request;    
    public final String status = "NG";
    public String message;
}

class BuySellReply_OK {
    public int request; 
    public final String status = "OK";
    public float credit;
    public List<Positions> share_list;

    BuySellReply_OK(int req, float num, List<Positions> pos) {
        this.request = req;
        this.credit = num;
        this.share_list = pos;
    }
}

class Ranking {
    public String name;
    public float credit;

    Ranking(String acname, float accredit){
        this.name = acname;
        this.credit = accredit;
    }
}

class ClearingReply {
    public final int request = 88;
    public final String status = "OK";
    public List<Ranking> ranking_list;

    ClearingReply(List<Ranking> rank) {
        this.ranking_list = rank;
    }
}

class Positions {
    public String instrument;
    public int quantity;
 
    Positions(String name, int num){
        this.instrument = name;
        this.quantity = num;
    }
}

class CheckReply_OK {
    public final int request = 100;
    public final String status = "OK";
    public float credit;
    public List<Positions> share_list;

    CheckReply_OK(float num, List<Positions> pos) {
        this.credit = num;
        this.share_list = pos;
    }
}


public class BasicReplier {

    String[][] accountArr = new String[100][3];
    String[][] positionArr = new String[2000][3];
    String[][] dataArr = new String[20][2];
    int accountArrLastIndex = 0;
    int positionArrLastIndex = 0;
    int dataArrLastIndex = 0;
    final float openCredit = 20000;

    public String openAccount(String name) {
        boolean exist = false;
        String reply = "";
        float credit = 0f;

        System.out.printf("Account Open Request received for %s %n", name);

        for (int i = 0; i < accountArrLastIndex; i++) {
            if (accountArr[i][0].equals(name)) {
                exist = true;
                credit = Float.parseFloat(accountArr[i][1]);
                break;
            }
        }
        if (exist) {
            OpenReply_OK openreply = new OpenReply_OK();
            openreply.credit = credit;
            ObjectMapper mapper = new ObjectMapper();
            try {
                reply = mapper.writeValueAsString(openreply);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            accountArr[accountArrLastIndex][0] = name;
            accountArr[accountArrLastIndex][1] = Float.toString(openCredit);
            accountArrLastIndex = accountArrLastIndex + 1;
            OpenReply_OK openreply = new OpenReply_OK();
            openreply.credit = openCredit;
            ObjectMapper mapper = new ObjectMapper();
            try {
                reply = mapper.writeValueAsString(openreply);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(reply);
        return reply;
    }
    public String buy(String name, String symbol, int number) {
        boolean exist = false;
        boolean posExist = false;
        float acCredit = 0;
        int posNumber = 0;
        int index = 0;
        int posIndex = 0;
        String reply = "";

        System.out.printf("Buy Request received for %s %s %d %n", name, symbol, number);

        // Account existence check and get current credit
        for (int i = 0; i < accountArrLastIndex; i++) {
            if (accountArr[i][0].equals(name)) {
                exist = true;
                index = i;
                acCredit = Float.parseFloat(accountArr[i][1]);
                break;
            }
        }
        if (!exist) {
            Reply_NG buyreply = new Reply_NG();
            buyreply.request = 101;
            buyreply.message = "No account exist for " + name;
            ObjectMapper mapper = new ObjectMapper();
            try {
                reply = mapper.writeValueAsString(buyreply);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Account credit check
            float price = 0f;
            int dataArrIndex = 0;
            exist = false;
            for (int i = 0; i < dataArrLastIndex; i++) {
                if (dataArr[i][0].equals(symbol)) {
                    dataArrIndex = i;
                    exist = true;
                    break;
                }
            }
            if (exist) {
                price = Float.parseFloat(dataArr[dataArrIndex][1]);
                float amount = price * number;
                if (amount > acCredit) {
                    Reply_NG buyreply = new Reply_NG();
                    buyreply.request = 101;
                    buyreply.message = "Lack of credit. Your current credit is " + Float.toString(acCredit);
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        reply = mapper.writeValueAsString(buyreply);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Buy, update credit.
                    acCredit = acCredit - amount;
                    accountArr[index][1] = Float.toString(acCredit);
                    // Position existence check
                    for (int i = 0; i < positionArrLastIndex; i++) {
                       if (positionArr[i][0].equals(name) && positionArr[i][1].equals(symbol)) {
                            posExist = true;
                            posIndex = i;
                            posNumber = Integer.parseInt(positionArr[i][2]);
                            break;
                        }
                    }
                    if (!posExist) {
                        // Add new position record.
                        positionArr[positionArrLastIndex][0] = name;
                        positionArr[positionArrLastIndex][1] = symbol;
                        positionArr[positionArrLastIndex][2] = Integer.toString(number);
                        positionArrLastIndex = positionArrLastIndex + 1;
                    } else {
                        // Update position record.
                        positionArr[posIndex][2] = Integer.toString(posNumber + number);
                    }
                    // Create OK reply.
                    List<Positions> p = new ArrayList<Positions>();
                    Positions p1 = new Positions(symbol, number);
                    p.add(p1);

                    BuySellReply_OK buysellreply = new BuySellReply_OK(101, acCredit, p);
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        reply = mapper.writeValueAsString(buysellreply);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Reply_NG buyreply = new Reply_NG();
                buyreply.request = 101;
                buyreply.message = "No Market data exist for " + symbol;
                ObjectMapper mapper = new ObjectMapper();
                try {
                    reply = mapper.writeValueAsString(buyreply);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println(reply);
        return reply;
    }

    public String sell(String name, String symbol, int number) {
        System.out.printf("Sell Request received for %s %s %d %n", name, symbol, number);
        boolean exist = false;
        boolean posExist = false;
        float acCredit = 0;
        int posNumber = 0;
        int index = 0;
        int posIndex = 0;
        String reply = "";

        // Account existence check and get current credit
        for (int i = 0; i < accountArrLastIndex; i++) {
            if (accountArr[i][0].equals(name)) {
                exist = true;
                index = i;
                acCredit = Float.parseFloat(accountArr[i][1]);
                break;
            }
        }
        if (!exist) {
            Reply_NG sellreply = new Reply_NG();
            sellreply.request = 102;
            sellreply.message = "No account exist for " + name;
            ObjectMapper mapper = new ObjectMapper();
            try {
                reply = mapper.writeValueAsString(sellreply);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Position existence check
            for (int i = 0; i < positionArrLastIndex; i++) {
                if (positionArr[i][0].equals(name) && positionArr[i][1].equals(symbol)) {
                    posExist = true;
                    posIndex = i;
                    posNumber = Integer.parseInt(positionArr[i][2]);
                    break;
                }
            }
            if (!posExist) {
                // No position for that stock.
                Reply_NG sellreply = new Reply_NG();
                sellreply.request = 102;
                sellreply.message = "No position exist for " + symbol;
                ObjectMapper mapper = new ObjectMapper();
                try {
                    reply = mapper.writeValueAsString(sellreply);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (number > posNumber) {
                    Reply_NG sellreply = new Reply_NG();
                    sellreply.request = 102;
                    sellreply.message = "Lack of Quantity. Your current Quantity is " + Integer.toString(posNumber);
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        reply = mapper.writeValueAsString(sellreply);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    float price = 0f;
                    int dataArrIndex = 0;
                    exist = false;
                    for (int i = 0; i < dataArrLastIndex; i++) {
                        if (dataArr[i][0].equals(symbol)) {
                            dataArrIndex = i;
                            exist = true;
                            break;
                        }
                    }
                    if (exist) {
                        price = Float.parseFloat(dataArr[dataArrIndex][1]);
                        float amount = price * number;
                        // Sell, update credit.
                        acCredit = acCredit + amount;
                        accountArr[index][1] = Float.toString(acCredit);
                        // Sell, update position.
                        posNumber = posNumber - number;
                        positionArr[posIndex][2] = Integer.toString(posNumber);
                        // Create OK reply.
                        List<Positions> p = new ArrayList<Positions>();
                        Positions p1 = new Positions(symbol, number);
                        p.add(p1);

                        BuySellReply_OK sellreply = new BuySellReply_OK(102, acCredit, p);
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            reply = mapper.writeValueAsString(sellreply);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Reply_NG sellreply = new Reply_NG();
                        sellreply.request = 102;
                        sellreply.message = "No Market data exist for " + symbol;
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            reply = mapper.writeValueAsString(sellreply);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        System.out.println(reply);
        return reply;
    }

    public String check(String name) {
        boolean exist = false;
        boolean posExist = false;
        float acCredit = 0;
        int posNumber = 0;
        int index = 0;
        int posIndex = 0;
        String reply = "";

        System.out.printf("Check Request received for %s %n", name);

        // Account existence check and get current credit
        for (int i = 0; i < accountArrLastIndex; i++) {
            if (accountArr[i][0].equals(name)) {
                exist = true;
                index = i;
                acCredit = Float.parseFloat(accountArr[i][1]);
                break;
            }
        }
        if (!exist) {
            Reply_NG checkreply = new Reply_NG();
            checkreply.request = 100;
            checkreply.message = "No account exist for " + name;
            ObjectMapper mapper = new ObjectMapper();
            try {
                reply = mapper.writeValueAsString(checkreply);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Set credit
            // Position existence check
            List<Positions> p = new ArrayList<Positions>();
            posIndex = 0;
            for (int i = 0; i < positionArrLastIndex; i++) {
                if (positionArr[i][0].equals(name)) {
                        Positions p1 = new Positions(positionArr[i][1], Integer.parseInt(positionArr[i][2]));
                        p.add(p1);
                        posIndex = posIndex + 1;
                }
            }

            CheckReply_OK checkreply = new CheckReply_OK(acCredit, p);

            // Create OK reply.
            ObjectMapper mapper = new ObjectMapper();
            try {
                reply = mapper.writeValueAsString(checkreply);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(reply);
        return reply;
    }

    public String clearing() {
        String trname;
        Float totalnumber, val;
        String symbol;
        int num;
        String reply = "";

        System.out.printf("Clearing Request received %n");

        for (int i = 0; i < accountArrLastIndex; i++) {
            totalnumber = 0f;
            trname = accountArr[i][0];
            for (int j = 0; j < positionArrLastIndex; j++) {
                if (positionArr[j][0].equals(trname)) {
                    symbol = positionArr[j][1];
                    num = Integer.parseInt(positionArr[j][2]);
                    for (int k = 0; k < dataArrLastIndex; k++) {
                        if (dataArr[k][0].equals(symbol)) {
                            val = Float.parseFloat(dataArr[k][1]);
                            totalnumber = totalnumber + (val * num);
                            break;
                        }
                    }
                }
            }
            accountArr[i][2] = Float.toString(totalnumber + Float.parseFloat(accountArr[i][1]));
        }

        // Ranking
        int[] rank = new int[10];
        Float maxCredit;
        int acIndex;
        Float prevMax = 1000000000f;
        int acNum;

        if (accountArrLastIndex < 10) {
            acNum = accountArrLastIndex;
        } else {
            acNum = 10;
        }
        for (int h = 0; h < acNum; h++) {
            maxCredit = 0f;
            acIndex = 0;
            for (int i = 0; i < accountArrLastIndex; i++) {
                if (Float.parseFloat(accountArr[i][2]) > maxCredit) {
                    if (Float.parseFloat(accountArr[i][2]) < prevMax) {
                        maxCredit = Float.parseFloat(accountArr[i][2]);
                        acIndex = i;
                     }
                }
            }
            rank[h] = acIndex;
            prevMax = maxCredit;
        }

        List<Ranking> p = new ArrayList<Ranking>();
        for (int i = 0; i < acNum; i++) {
            Ranking p1 = new Ranking(accountArr[rank[i]][0], Float.parseFloat(accountArr[rank[i]][2]));
            p.add(p1);
        }

        ClearingReply clearingreply = new ClearingReply(p);
            
        ObjectMapper mapper = new ObjectMapper();
        try {
            reply = mapper.writeValueAsString(clearingreply);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(reply);
        return reply;
    }


    public void run(String... args) throws JCSMPException {
        System.out.println("BasicReplier initializing...");
        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, args[0]);     // host:port
        properties.setProperty(JCSMPProperties.USERNAME, args[1].split("@")[0]); // client-username
        properties.setProperty(JCSMPProperties.VPN_NAME,  args[1].split("@")[1]); // message-vpn
        if (args.length > 2) {
            properties.setProperty(JCSMPProperties.PASSWORD, args[2]); // client-password
        }
        final JCSMPSession session = JCSMPFactory.onlyInstance().createSession(properties);
        session.connect();

        final Topic topic = JCSMPFactory.onlyInstance().createTopic("team1/request");

        /** Anonymous inner-class for handling publishing events */
        final XMLMessageProducer producer = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
            @Override
            public void responseReceived(String messageID) {
                System.out.println("Producer received response for msg: " + messageID);
            }

            @Override
            public void handleError(String messageID, JCSMPException e, long timestamp) {
                System.out.printf("Producer received error for msg: %s@%s - %s%n", messageID, timestamp, e);
            }
        });

        /** Anonymous inner-class for request handling **/
        final XMLMessageConsumer cons = session.getMessageConsumer(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage request) {
                String requestString = "";
                byte[] messageBinaryPayload;
                String replyString = "";
                if (request.getReplyTo() != null) {
                    //System.out.println("Received request, generating response");

                    //TextMessage reply = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
                    BytesXMLMessage reply = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);

                    if (request instanceof TextMessage) {
                        requestString = ((TextMessage)request).getText();
                        System.out.printf("TextMessage received: '%s'%n", requestString);
                    } else if (request instanceof BytesMessage) {
                        messageBinaryPayload = request.getAttachmentByteBuffer().array();
                        try {
                            requestString = new String(messageBinaryPayload, "UTF-8");
                            System.out.printf("ByteMessage received: '%s'%n", requestString);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    } else {
                       System.out.println("Message received but message type is not Text or Binary");
                    }

                    String traderName = "";
                    String tradeSymbol = "";
                    int opCode = 0;
                    String[] array;
                    int tradeNumber = 0;

                    if (requestString.contains(",")) {
                        array = requestString.split(","); 
                        opCode = Integer.parseInt(array[0].trim());
                        traderName = array[1].trim();
                        tradeNumber = 0;
                        if (array.length > 2) {
                            tradeSymbol = array[2].trim();
                            tradeNumber = Integer.parseInt(array[3].trim());
                        } 
                    } else {
                        opCode = Integer.parseInt(requestString);
                    }

                    if (opCode == 99) {
                        replyString = openAccount(traderName);
                    } else if (opCode == 101) {
                        replyString = buy(traderName, tradeSymbol, tradeNumber);
                    } else if (opCode == 102) {
                        replyString = sell(traderName, tradeSymbol, tradeNumber);                        
                    } else if (opCode == 100) {
                        replyString = check(traderName);
                    } else if (opCode == 88) {
                        replyString = clearing();
                    } else {
                        System.out.println("Received but wrong operation code:" + Integer.toString(opCode));
                    }

                    //reply.setText(replyString);
                    reply.writeAttachment(replyString.getBytes(Charset.forName("UTF-8")));

                    try {
                        producer.sendReply(request, reply);
                    } catch (JCSMPException e) {
                        System.out.println("Error sending reply.");
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Received message without reply-to field");
                }

            }

            public void onException(JCSMPException e) {
                System.out.printf("Consumer received exception: %s%n", e);
            }
        });

        session.addSubscription(topic);
        cons.start();
        // Consume-only session is now hooked up and running!
        System.out.println("Listening for request messages on topic " + topic);

/*
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Close consumer
        cons.close();
        System.out.println("Exiting.");
        session.closeSession();
*/

        final Topic topic2 = JCSMPFactory.onlyInstance().createTopic("geekathon/stock/price/>");
        final JCSMPSession session2 = JCSMPFactory.onlyInstance().createSession(properties);

        session2.connect();

        /** Anonymous inner-class for MessageListener
         *  This demonstrates the async threaded message callback */
        final XMLMessageConsumer cons2 = session2.getMessageConsumer(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
                if (msg instanceof TextMessage) {
                    String msgString = ((TextMessage)msg).getText();
                    String[] msgElement = msgString.split(",",0);
                    String symbol = msgElement[1];
                    String price = msgElement[3];
                    //System.out.printf("Market data received: '%s' '%s' %n", symbol, price);

                    int dataArrIndex = 0;
                    boolean exist = false;
                    for (int i = 0; i < dataArrLastIndex; i++) {
                        if (dataArr[i][0].equals(symbol)) {
                            dataArrIndex = i;
                            exist = true;
                            break;
                        }
                    }
                    if (exist) {
                        dataArr[dataArrIndex][1] = price;
                    } else {
                        dataArr[dataArrLastIndex][0] = symbol;
                        dataArr[dataArrLastIndex][1] = price;
                        dataArrLastIndex = dataArrLastIndex + 1;
                   }
                } else {
                    System.out.println("Message received.");
                }
            }

            @Override
            public void onException(JCSMPException e) {
                System.out.printf("Consumer received exception: %s%n",e);
            }
        });

        session2.addSubscription(topic2);
        System.out.println("Connected. Awaiting Market Data message...");
        cons2.start();
        // Consume-only session is now hooked up and running!


        while (true) {}

    }

    public static void main(String... args) throws JCSMPException {

        // Check command line arguments
        if (args.length < 2 || args[1].split("@").length != 2) {
            System.out.println("Usage: BasicReplier <host:port> <client-username@message-vpn> [client-password]");
            System.out.println();
            System.exit(-1);
        }
        if (args[1].split("@")[0].isEmpty()) {
            System.out.println("No client-username entered");
            System.out.println();
            System.exit(-1);
        }
        if (args[1].split("@")[1].isEmpty()) {
            System.out.println("No message-vpn entered");
            System.out.println();
            System.exit(-1);
        }

        BasicReplier replier = new BasicReplier();
        replier.run(args);
    }
}
