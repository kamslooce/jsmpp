<%@ page import="com.slooce.smpp.SlooceSMPPProvider" %>
<%@ page import="com.slooce.smpp.SlooceSMPPReceiver" %>
<%@ page import="com.slooce.smpp.SlooceSMPPSession" %>
<%@ page import="org.jsmpp.util.DeliveryReceiptState" %>
<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Date" %>
<%!
    private SlooceSMPPSession slooceSMPPSession1 = null;
    private SlooceSMPPSession slooceSMPPSession2 = null;

    private static final Logger logger = LoggerFactory.getLogger(SlooceSMPPSession.class);
    private static ArrayList<String> history = new ArrayList();
    public void log(final String log) {
        final String dateTimePrefix = String.format("%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tL ", new Date());
        history.add(dateTimePrefix + log);
        logger.info(log);
    }
%>
<%
    final String command = request.getParameter("command");
    if (command != null) {
        log("Command: " + command);
    }

    // Reconnect
    boolean reconnect1 = false;
    boolean reconnect2 = false;
    if ("Reconnect 1".equals(command)) {
        reconnect1 = true;
    }
    if ("Reconnect 2".equals(command)) {
        reconnect2 = true;
    }
    if ("Reconnect Both".equals(command)) {
        reconnect1 = true;
        reconnect2 = true;
    }
    if (reconnect1 && slooceSMPPSession1 != null) {
        slooceSMPPSession1.close();
    }
    if (reconnect2 && slooceSMPPSession2 != null) {
        slooceSMPPSession2.close();
    }

    // Connect and Receive MOs
    boolean connect1 = false;
    boolean connect2 = false;
    if ("Connect 1".equals(command)) {
        connect1 = true;
    }
    if ("Connect 2".equals(command)) {
        connect2 = true;
    }
    if ("Connect Both".equals(command)) {
        connect1 = true;
        connect2 = true;
    }
    if (connect1 || connect2) {
        final String provider = request.getParameter("provider");
        final String serviceType = request.getParameter("serviceType").equals("null") ? null : request.getParameter("serviceType");
        final String serviceId = request.getParameter("serviceId").equals("null") ? null : request.getParameter("serviceId");
        final boolean useSSL = Boolean.parseBoolean(request.getParameter("useSSL"));
        final String host = request.getParameter("host");
        final int port = Integer.parseInt(request.getParameter("port"));
        final String systemId = request.getParameter("systemId");
        final String password = request.getParameter("password");
        final String systemType = request.getParameter("systemType").equals("null") ? null : request.getParameter("systemType");
        final boolean stripSystemType = Boolean.parseBoolean(request.getParameter("stripSystemType"));

        if (connect1 && slooceSMPPSession1 == null) {
            slooceSMPPSession1 = new SlooceSMPPSession(SlooceSMPPProvider.valueOf(provider), serviceId, serviceType, useSSL, host, port, systemId, password, systemType, stripSystemType, new SlooceSMPPReceiver() {
                public void mo(final String messageId, final String message, final String subscriber, final String operator,
                               final String shortcode, final SlooceSMPPSession smpp) {
                    log("MO Received by 1: messageId="+messageId+",subscriber="+subscriber+",operator="+operator+",shortcode="+shortcode+",message="+message+" - "+smpp);
                }
                public void deliveryReceipt(String messageId, String message, String subscriber, String operator, String shortcode, DeliveryReceiptState status, String errorCode, SlooceSMPPSession smpp) {
                    log("Delivery Receipt Received by 1: messageId="+messageId+",status="+status+",errorCode="+errorCode+",subscriber="+subscriber+",operator="+operator+",shortcode="+shortcode+",message="+message+" - "+smpp);
                }
                public void onClose(final SlooceSMPPSession smpp) {
                    log("Session closed by 1 - "+smpp);
                }
            });
        }
        if (connect2 && slooceSMPPSession2 == null) {
            slooceSMPPSession2 = new SlooceSMPPSession(SlooceSMPPProvider.valueOf(provider), serviceId, serviceType, useSSL, host, port, systemId, password, systemType, stripSystemType, new SlooceSMPPReceiver() {
                public void mo(final String messageId, final String message, final String subscriber, final String operator,
                               final String shortcode, final SlooceSMPPSession smpp) {
                    log("MO Received by 2: messageId="+messageId+",subscriber="+subscriber+",operator="+operator+",shortcode="+shortcode+",message="+message+" - "+smpp);
                }
                public void deliveryReceipt(String messageId, String message, String subscriber, String operator, String shortcode, DeliveryReceiptState status, String errorCode, SlooceSMPPSession smpp) {
                    log("Delivery Receipt Received by 2: messageId="+messageId+",status="+status+",errorCode="+errorCode+",subscriber="+subscriber+",operator="+operator+",shortcode="+shortcode+",message="+message+" - "+smpp);
                }
                public void onClose(final SlooceSMPPSession smpp) {
                    log("Session closed by 2 - "+smpp);
                }
            });
        }
    }

    if (connect1 || connect2 || reconnect1 || reconnect2) {
        final int attempts = Integer.parseInt(request.getParameter("attempts"));
        final int retryInterval = Integer.parseInt(request.getParameter("retryInterval"));
        if ((connect1 || reconnect1) && slooceSMPPSession1 != null && !slooceSMPPSession1.isConnected()) {
            slooceSMPPSession1.connect(attempts, retryInterval);
        }
        if ((connect2 || reconnect2) && slooceSMPPSession2 != null && !slooceSMPPSession2.isConnected()) {
            slooceSMPPSession2.connect(attempts, retryInterval);
        }
    }

    // Send MT
    boolean sendMt1 = false;
    boolean sendMt2 = false;
    if ("Send MT via 1".equals(command)) {
        sendMt1 = true;
    }
    if ("Send MT via 2".equals(command)) {
        sendMt2 = true;
    }
    if ("Send MT via Both".equals(command)) {
        sendMt1 = true;
        sendMt2 = true;
    }
    if (sendMt1 || sendMt2) {
        final String message = request.getParameter("message");
        final String shortOrLongCode = request.getParameter("shortOrLongCode");
        final String subscriber = request.getParameter("subscriber");
        String operator = request.getParameter("operator");
        if (operator.equals("null")) {
            operator = null;
        }
        try {
            if (sendMt1 && slooceSMPPSession1 != null) {
                slooceSMPPSession1.mt(message + " via 1", subscriber, shortOrLongCode, operator);
            }
            if (sendMt2 && slooceSMPPSession2 != null) {
                slooceSMPPSession2.mt(message + " via 2", subscriber, shortOrLongCode, operator);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Close Connection
    boolean close1 = false;
    boolean close2 = false;
    if ("Close Connection 1".equals(command)) {
        close1 = true;
    }
    if ("Close Connection 2".equals(command)) {
        close2 = true;
    }
    if ("Close Connection Both".equals(command)) {
        close1 = true;
        close2 = true;
    }
    if (close1 && slooceSMPPSession1 != null) {
        slooceSMPPSession1.close();
        slooceSMPPSession1 = null;
    }
    if (close2 && slooceSMPPSession2 != null) {
        slooceSMPPSession2.close();
        slooceSMPPSession2 = null;
    }
%>
<html>
<% if (request.getMethod().equals("POST")) { %>
<head><meta http-equiv="refresh" content="0;URL=<%=request.getRequestURL()%>"></head>
<% } %>
<body>
Status 1: <%=slooceSMPPSession1 == null ? "Not connected" : slooceSMPPSession1%><br>
Status 2: <%=slooceSMPPSession2 == null ? "Not connected" : slooceSMPPSession2%><br>
<hr>
<form name="connect" action="index.jsp" method="POST">
    <label for="provider">Provider:</label>
    <select id="provider" name="provider">
        <option value="MBLOX" selected>MBlox</option>
        <option value="OPEN_MARKET">OpenMarket</option>
    </select><br>
    <label for="serviceId">Service Id:</label>
    <input type="text" id="serviceId" name="serviceId" value="64217">
    (used in MT messages to indicate the service or campaign associated with this message, e.g., required to send messages to Verizon and T-Mobile, default is null)<br>
    <label for="serviceType">Service Type:</label>
    <input type="text" id="serviceType" name="serviceType" value="35076">
    (used in MT messages to indicate the SMS Application service associated with the message, e.g., CMT for Cellular Messaging, default is null)<br>
    <label for="useSSL">Use SSL:</label>
    <select id="useSSL" name="useSSL">
        <option value="true">true</option>
        <option value="false" selected>false</option>
    </select>
    (true to use an SSL SMPP connection)<br>
    <label for="host">Host:</label>
    <input type="text" id="host" name="host" value="smpp.psms.us.mblox.com"><br>
    <label for="port">Port:</label>
    <input type="text" id="port" name="port" value="3204"><br>
    <label for="systemId">System ID:</label>
    <input type="text" id="systemId" name="systemId" value="SlooceTechUS"><br>
    <label for="password">Password:</label>
    <input type="text" id="password" name="password" value="Qn3BvXcZ"><br>
    <label for="systemType">System Type:</label>
    <input type="text" id="systemType" name="systemType" value="SlooceTech">
    (categorizes the type of ESME that is binding, e.g., VMS for Voice Mail System, max 13 characters, default is null - this is the message prefix when testing Mblox MOs)<br>
    <label for="stripSystemType">Strip System Type:</label>
    <select id="stripSystemType" name="stripSystemType">
        <option value="true" selected>true</option>
        <option value="false">false</option>
    </select>
    (true to strip the systemType from the MO message)<br>
    <label for="attempts">Connect attempts:</label>
    <input type="text" id="attempts" name="attempts" value="3"><br>
    <label for="retryInterval">Connect retry interval (ms):</label>
    <input type="text" id="retryInterval" name="retryInterval" value="1000"><br>
    <input type="submit" name="command" value="Connect 1"/>
    <input type="submit" name="command" value="Connect 2"/>
    <input type="submit" name="command" value="Connect Both"/><br>
</form>
<hr>
<form name="reconnect" action="index.jsp" method="POST">
    <label for="attempts">Connect attempts:</label>
    <input type="text" id="attempts" name="attempts" value="3"><br>
    <label for="retryInterval">Connect retry interval (ms):</label>
    <input type="text" id="retryInterval" name="retryInterval" value="1000"><br>
    <input type="submit" name="command" value="Reconnect 1"/>
    <input type="submit" name="command" value="Reconnect 2"/>
    <input type="submit" name="command" value="Reconnect Both"/><br>
</form>
<hr>
<form name="send" action="index.jsp" method="POST">
    <label for="shortOrLongCode">From Short Or Long Code:</label>
    <input type="text" id="shortOrLongCode" name="shortOrLongCode" value="28444">
    (A Short Code is a 4 to 6 digit number for A2P messages. A Long Code is a standard phone number like 13108784998 that is usually used for P2P messages.)<br>
    <label for="subscriber">To Subscriber:</label>
    <input type="text" id="subscriber" name="subscriber" value="14085551212"><br>
    <label for="operator">To Operator:</label>
    <input type="text" id="operator" name="operator" value="null">
    (e.g., default is null. Required for Mblox: use 31002 for AT&amp;T; 31003, Verizon; 31004, T-Mobile; 31005, Sprint)<br>
    <label for="message">Message:</label>
    <textarea id="message" name="message" cols="20" rows="4">From Slooce using SMPP</textarea><br>
    <input type="submit" name="command" value="Send MT via 1"/>
    <input type="submit" name="command" value="Send MT via 2"/>
    <input type="submit" name="command" value="Send MT via Both"/><br>
</form>
<hr>
<form name="close" action="index.jsp" method="POST">
    <input type="submit" name="command" value="Close Connection 1"/>
    <input type="submit" name="command" value="Close Connection 2"/>
    <input type="submit" name="command" value="Close Connection Both"/><br>
</form>
<hr>
<% for (int i = history.size()-1; i >= 0; i--) { %>
<%=history.get(i)%><br>
<% } %>
</body>
</html>
