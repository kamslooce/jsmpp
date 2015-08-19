#!/bin/sh
mvn clean install && cd jsmpp-war && mvn clean install && cd .. && ls -al jsmpp/target/jsmpp-2.2.1.jar jsmpp-war/target/jsmpp-war.war
