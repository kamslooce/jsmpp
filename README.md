
Introduction
------------

jSMPP is a java implementation (SMPP API) of SMPP protocol (currently support
SMPP v3.4). It provides interfaces to communicate with Message Center or
ESME (External Short Message Entity) and able to handle
traffic 3000-5000 messages per second. 

jSMPP is not a high-level library.  Many people looking for a quick way to
get started with SMPP may be better of using an abstraction layer such
as the Apache Camel SMPP component:
  http://camel.apache.org/smpp.html

Travis-CI status:
-----------------

[![Build Status](https://travis-ci.org/opentelecoms-org/jsmpp.svg?branch=master)](https://travis-ci.org/opentelecoms-org/jsmpp)

History
-------

The project started on Google Code:  http://code.google.com/p/jsmpp/

It was maintained by uudashr on Github until 2013

It is now a community project maintained at http://jsmpp.org

Release procedure
-----------------

  mvn deploy -DperformRelease=true -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging

  * log in here: https://oss.sonatype.org
  * click the `Staging Repositories' link
  * select the repository and click close
  * select the repository and click release

License
-------

Copyright (C) 2007-2013, Nuruddin Ashr <uudashr@gmail.com>
Copyright (C) 2012-2013, Denis Kostousov <denis.kostousov@gmail.com>
Copyright (C) 2014, Daniel Pocock http://danielpocock.com

This project is licensed under the Apache Software License 2.0.

