[![Build Status](https://travis-ci.org/SolaceSamples/solace-samples-java.svg?branch=master)](https://travis-ci.org/SolaceSamples/solace-samples-java)

## Prerequisites

This tutorial requires the Solace Java API library. Download the Java API library to your computer from [here](http://dev.solace.com/downloads/).

## Build the Samples

Just clone and build. For example:

  1. clone this GitHub repository
```
git clone https://github.com/neha-sin/CacheDemo 
cd CacheDemo
```
  2. `./gradlew assemble`

## Running the Samples

Stock Market Data Publisher:

1. MarketDataPublishFileToSolace: This will read the input file and generate random value for price, volume, lastTraded and delta.
Generate the value in infinite loop and publish it as comma-separated value to the topic “geekathon/stock/price/<Symbol>”

Command to execute:
./build/staged/bin/MarketDataPublishFileToSolace <msg-vpn> <vpn-name> <userid> <password> <inputfile>
 

# Publish Stock Price
