package com.slooce.smpp;

import static com.slooce.smpp.SlooceSMPPUtil.sanitizeCharacters;

import org.jsmpp.DefaultPDUReader;
import org.jsmpp.DefaultPDUSender;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.SynchronizedPDUSender;
import org.jsmpp.bean.AlertNotification;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.DataCodings;
import org.jsmpp.bean.DataSm;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.DeliveryReceipt;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GSMSpecificFeature;
import org.jsmpp.bean.MessageType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.OptionalParameters;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.extra.SessionState;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.session.Session;
import org.jsmpp.session.SessionStateListener;
import org.jsmpp.util.DefaultComposer;
import org.jsmpp.util.InvalidDeliveryReceiptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jsmpp.util.HexUtil.conventBytesToHexString;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * An SMPP Session with more familiar methods to connect, send MT messages, and to receive MO messages ({@link SlooceSMPPReceiver})).
 */
public class SlooceSMPPSession {
    private static final Logger logger = LoggerFactory.getLogger(SlooceSMPPSession.class);

    private SlooceSMPPProvider provider;
    private String serviceId;
    private String serviceType;
    private boolean useSSL;
    private String host;
    private int port;
    private String systemId;
    private String password;
    private String systemType;
    private boolean stripSystemType;
    private SlooceSMPPReceiver receiver;

    private SMPPSession smppSession = null;
    private boolean connected = false;

    public SlooceSMPPSession() {
        this.provider = SlooceSMPPProvider.OPEN_MARKET;
        this.serviceId = null;
        this.serviceType = SlooceSMPPConstants.SERVICE_TYPE_CELLULAR_MESSAGING;
        this.useSSL = false;
        this.host = null;
        this.port = 0;
        this.systemId = null;
        this.password = null;
        this.systemType = null;
        this.stripSystemType = false;
        this.receiver = null;
    }

    public SlooceSMPPSession(final SlooceSMPPProvider provider,
                             final String serviceId, final String serviceType,
                             final boolean useSSL, final String host, final int port,
                             final String systemId, final String password,
                             final String systemType, final boolean stripSystemType,
                             final SlooceSMPPReceiver receiver) {
        this.provider = provider;
        this.serviceId = serviceId;
        this.serviceType = serviceType;
        this.useSSL = useSSL;
        this.host = host;
        this.port = port;
        this.systemId = systemId;
        this.password = password;
        this.systemType = systemType;
        this.stripSystemType = stripSystemType;
        this.receiver = receiver;
    }

    public void reconfigure(final SlooceSMPPProvider provider,
                            final String serviceId, final String serviceType,
                            final boolean useSSL, final String host, final int port,
                            final String systemId, final String password,
                            final String systemType, final boolean stripSystemType,
                            final SlooceSMPPReceiver receiver) {
        this.provider = provider;
        this.serviceId = serviceId;
        this.serviceType = serviceType;
        this.useSSL = useSSL;
        this.host = host;
        this.port = port;
        this.systemId = systemId;
        this.password = password;
        this.systemType = systemType;
        this.stripSystemType = stripSystemType;
        this.receiver = receiver;
    }

    public boolean isConnected() {
        return connected;
    }

    public void close() {
        connected = false;
        if (smppSession != null) {
            smppSession.close();
            smppSession = null;
        }
    }

    public void connect(final int attempts, final int retryInterval) throws IOException {
        int attempt = 0;
        while (!connected && attempt < attempts) {
            try {
                ++attempt;
                logger.info("SlooceSMPPSession.connect - Connect attempt #{}... {}", attempt, this);
                connect();
            } catch (Exception e) {
                logger.error("SlooceSMPPSession.connect - Failed to connect - " + this, e);
                // wait before retrying
                try { Thread.sleep(retryInterval * attempt); } catch (Exception ignored) {
                    // ignore exceptions
                }
            }
        }
        if (!connected) {
            logger.error("SlooceSMPPSession.connect - Failed to initialize after {} attempts - {}", attempt, this);
        }
    }

    private void connect()
            throws IOException {
        if (connected) {
            return;
        }
        final SlooceSMPPSession smpp = this;
        smppSession = new SMPPSession(
                new SynchronizedPDUSender(new DefaultPDUSender(new DefaultComposer())),
                new DefaultPDUReader(),
                new SlooceSMPPSocketConnectionFactory(useSSL));
        smppSession.setEnquireLinkTimer(provider.getEnquireLinkTimer()); // Depends on the provider's inactivity timeout (actually, this is the SMPP Socket Read Timeout before enquiring to keep the link alive)
        smppSession.setTransactionTimer(provider.getTransactionTimer()); // Depends on the provider's response timeout (responses should be returned within a second)
        smppSession.setMessageReceiverListener(new MessageReceiverListener() {
            @Override
            public void onAcceptDeliverSm(final DeliverSm deliverSm) throws ProcessRequestException {
                final Alphabet alphabet = smpp.provider.getAlphabet(deliverSm, logger);
                if (MessageType.SMSC_DEL_RECEIPT.containedIn(deliverSm.getEsmClass())) {
                    // this message is a delivery receipt
                    try {
                        final DeliveryReceipt delReceipt = deliverSm.getShortMessageAsDeliveryReceipt();
                        String messageId = delReceipt.getId();
                        if (smpp.provider.isMessageIdDecimal()) {
                            // Provider sends the messageId in a delivery receipt as a decimal value string per the SMPP spec.
                            // Convert it to hex to match the messageId hex string returned when submitting the MT.
                            messageId = Long.toHexString(Long.valueOf(messageId));
                        }
                        final String operator = getOptionalParameterValueAsString(OptionalParameters.get(smpp.provider.getOperatorTag(), deliverSm.getOptionalParameters()));
                        final SlooceSMPPMessage mt = new SlooceSMPPMessage(messageId, deliverSm.getSourceAddr(), operator, deliverSm.getDestAddress());
                        String message;
                        if (alphabet == Alphabet.ALPHA_DEFAULT) {
                            message = SlooceSMPPUtil.fromGSMCharset(delReceipt.getText().getBytes());
                        } else {
                            message = delReceipt.getText();
                        }
                        mt.setMessage(message);
                        logger.info("Received delivery receipt - mt:{} dataCoding:{} alphabet:{} esmClass:0x{} {}{} - {}",
                                mt, deliverSm.getDataCoding(), alphabet, conventBytesToHexString(new byte[]{deliverSm.getEsmClass()}),
                                sanitizeCharacters(delReceipt.toString()), paramsToString(deliverSm.getOptionalParameters()), smpp.toShortString());
                        if (smpp.receiver != null) {
                            smpp.receiver.deliveryReceipt(mt, delReceipt.getFinalStatus(), delReceipt.getError(), smpp);
                        }
                    } catch (InvalidDeliveryReceiptException e) {
                        logger.error("Failed getting delivery receipt - " + smpp.toShortString(), e);
                    }
                } else {
                    // this message is an incoming MO
                    final String messageId = getOptionalParameterValueAsString(deliverSm.getOptionalParameter(OptionalParameter.Tag.RECEIPTED_MESSAGE_ID));
                    final String operator = getOptionalParameterValueAsString(OptionalParameters.get(smpp.provider.getOperatorTag(), deliverSm.getOptionalParameters()));
                    byte[] messageBytes = deliverSm.getShortMessage();
                    byte[] udhBytes = new byte[0];
                    final SlooceSMPPMessage mo = new SlooceSMPPMessage(messageId, deliverSm.getSourceAddr(), operator, deliverSm.getDestAddress());
                    final boolean hasUDHI = GSMSpecificFeature.UDHI.containedIn(deliverSm.getEsmClass());
                    if (hasUDHI) {
                        final int udhLength = messageBytes[0];
                        udhBytes = new byte[udhLength + 1];
                        System.arraycopy(messageBytes, 0, udhBytes, 0, udhLength + 1);
                        byte[] messageBytesCopy = new byte[messageBytes.length - udhLength - 1];
                        System.arraycopy(messageBytes, udhLength + 1, messageBytesCopy, 0, messageBytes.length - udhLength - 1);
                        messageBytes = messageBytesCopy;
                        if (udhBytes[1] == 0x00) { // Concatenated short messages, 8-bit reference number
                            mo.setCsmsReference(udhBytes[3] & 0xff);
                            mo.setCsmsTotalParts(udhBytes[4] & 0xff);
                            mo.setCsmsPartNumber(udhBytes[5] & 0xff);
                        } else if (udhBytes[1] == 0x08) { // Concatenated short messages, 16-bit reference number
                            mo.setCsmsReference(((udhBytes[3] & 0xff) << 8) | (udhBytes[4] & 0xff));
                            mo.setCsmsTotalParts(udhBytes[5] & 0xff);
                            mo.setCsmsPartNumber(udhBytes[6] & 0xff);
                        } else { // unsupported
                            logger.warn("Unsupported udh:{}", conventBytesToHexString(udhBytes));
                        }
                    }
                    String message;
                    if (alphabet == Alphabet.ALPHA_DEFAULT) {
                        message = SlooceSMPPUtil.fromGSMCharset(messageBytes);
                    } else if (alphabet == Alphabet.ALPHA_UCS2) {
                        try {
                            message = new String(messageBytes, "UTF-16");
                        } catch (UnsupportedEncodingException e) {
                            logger.warn(e.getMessage());
                            message = new String(messageBytes);
                        }
                    } else {
                        try {
                            message = new String(messageBytes, "ISO-8859-1");
                        } catch (UnsupportedEncodingException e) {
                            logger.warn(e.getMessage());
                            message = new String(messageBytes);
                        }
                    }
                    if (smpp.stripSystemType && smpp.systemType != null) {
                        message = Pattern.compile(smpp.systemType + "\\s*", Pattern.CASE_INSENSITIVE).matcher(message).replaceFirst("");
                    }
                    mo.setMessage(message);
                    logger.info("Received message - mo:{} dataCoding:{} alphabet:{} esmClass:0x{} udh:0x{}{} - {}",
                            mo, deliverSm.getDataCoding(), alphabet, conventBytesToHexString(new byte[]{deliverSm.getEsmClass()}), conventBytesToHexString(udhBytes),
                            paramsToString(deliverSm.getOptionalParameters()), smpp.toShortString());
                    if (smpp.receiver != null) {
                        smpp.receiver.mo(mo, smpp);
                    }
                }
            }

            private String getOptionalParameterValueAsString(final OptionalParameter optionalParameter) {
                if (optionalParameter == null) {
                    return null;
                }
                if (optionalParameter instanceof OptionalParameter.OctetString) {
                    return ((OptionalParameter.OctetString) optionalParameter).getValueAsString();
                } else {
                    throw new RuntimeException("OptionalParameter type is not yet supported: " + optionalParameter.getClass());
                }
            }

            @Override
            public void onAcceptAlertNotification(final AlertNotification alertNotification) {
            }

            @Override
            public DataSmResult onAcceptDataSm(final DataSm dataSm, final Session source) throws ProcessRequestException {
                return null;
            }
        });
        smppSession.addSessionStateListener(new SessionStateListener() {
            @Override
            public void onStateChange(final SessionState newState, final SessionState oldState, final Session source) {
                if (newState.equals(SessionState.CLOSED)) {
                    if (smpp.connected) {
                        logger.warn("Session closed - {}", smpp);
                        smpp.connected = false;
                        new Thread() {
                            @Override
                            public void run() {
                                smpp.receiver.onClose(smpp);
                            }
                        }.start();
                    } else {
                        logger.info("Session was already closed - {}", smpp);
                    }
                }
            }
        });

        smppSession.connectAndBind(host, port, new BindParameter(BindType.BIND_TRX, systemId, password, systemType, TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, null));
        connected = true;
        logger.info("Connected to {} using {}@{}:{} - {}", provider, systemId, host, port, smpp);
    }

    public String mt(final String message, final String subscriber, final String shortOrLongCode, final String operator)
            throws InvalidResponseException, PDUException, IOException, NegativeResponseException,
            ResponseTimeoutException {
        if (provider == SlooceSMPPProvider.MBLOX) {
            final List<OptionalParameter> optionalParameters = new ArrayList<OptionalParameter>();
            // Mblox requires Operator and Tariff
            optionalParameters.add(new OptionalParameter.OctetString(SlooceSMPPConstants.TAG_MBLOX_OPERATOR, operator));
            optionalParameters.add(new OptionalParameter.OctetString(SlooceSMPPConstants.TAG_MBLOX_TARIFF, "0"));
            // ServiceId is required for TMobile and Verizon
            if (SlooceSMPPConstants.OPERATOR_MBLOX_T_MOBILE.equals(operator)
             || SlooceSMPPConstants.OPERATOR_MBLOX_VERIZON.equals(operator)) {
                optionalParameters.add(new OptionalParameter.OctetString(SlooceSMPPConstants.TAG_MBLOX_SERVICEID, serviceId));
            }
            return sendMT(message,
                    shortOrLongCode.length() < 7 ? TypeOfNumber.NETWORK_SPECIFIC : TypeOfNumber.INTERNATIONAL, shortOrLongCode,
                    TypeOfNumber.INTERNATIONAL, subscriber,
                    optionalParameters.toArray(new OptionalParameter[optionalParameters.size()]));
        } else {
            return sendMT(message,
                    shortOrLongCode.length() < 7 ? TypeOfNumber.NETWORK_SPECIFIC : TypeOfNumber.INTERNATIONAL, shortOrLongCode,
                    TypeOfNumber.INTERNATIONAL, subscriber);
        }
    }

    private String sendMT(final String message,
                        final TypeOfNumber sourceTon, final String source,
                        final TypeOfNumber destinationTon, final String destination,
                        OptionalParameter... optionalParameters)
            throws InvalidResponseException, PDUException, IOException, NegativeResponseException,
            ResponseTimeoutException {
        try {
            final String messageId = smppSession.submitShortMessage(serviceType, sourceTon, NumberingPlanIndicator.UNKNOWN, source, destinationTon, NumberingPlanIndicator.UNKNOWN, destination,
                    new ESMClass(), (byte) 0, (byte) 1, null, null, new RegisteredDelivery(SMSCDeliveryReceipt.SUCCESS_FAILURE), (byte) 0, DataCodings.ZERO, (byte) 0, message.getBytes("ISO-8859-1"),
                    optionalParameters);
            logger.info("MT sent - messageId:{} to:{} from:{} text:{}{} - {}", messageId, destination, source, message, paramsToString(optionalParameters), this.toShortString());
            return messageId;
        } catch (final PDUException e) {
            logger.error("Failed to send MT - to:" + destination + " from:" + source + " text:" + message + paramsToString(optionalParameters) + " - " + this.toShortString(), e);
            throw e;
        } catch (final ResponseTimeoutException e) {
            logger.error("Failed to send MT - to:" + destination + " from:" + source + " text:" + message + paramsToString(optionalParameters) + " - " + this.toShortString(), e);
            throw e;
        } catch (final InvalidResponseException e) {
            logger.error("Failed to send MT - to:" + destination + " from:" + source + " text:" + message + paramsToString(optionalParameters) + " - " + this.toShortString(), e);
            throw e;
        } catch (final NegativeResponseException e) {
            logger.error("Failed to send MT - to:" + destination + " from:" + source + " text:" + message + paramsToString(optionalParameters) + " - " + this.toShortString(), e);
            throw e;
        } catch (final IOException e) {
            logger.error("Failed to send MT - to:" + destination + " from:" + source + " text:" + message + paramsToString(optionalParameters) + " - " + this.toShortString(), e);
            throw e;
        } catch (final Throwable e) {
            logger.error("Failed to send MT - to:" + destination + " from:" + source + " text:" + message + paramsToString(optionalParameters) + " - " + this.toShortString(), e);
            throw new RuntimeException(e);
        }
    }

    protected static String paramsToString(final OptionalParameter[] optionalParameters) {
        final StringBuilder params = new StringBuilder();
        try {
            if (optionalParameters != null) {
                for (final OptionalParameter op : optionalParameters) {
                    params.append(" tag:0x").append(Integer.toHexString(op.tag));
                    params.append(" serialized:0x").append(conventBytesToHexString(op.serialize()));
                    if (op instanceof OptionalParameter.OctetString) {
                        params.append(" stringValue:").append(sanitizeCharacters(((OptionalParameter.OctetString) op).getValueAsString()));
                    } else {
                        params.append(" stringValue:").append(sanitizeCharacters(op.toString()));
                    }
                }
            }
        } catch (final Throwable t) {
            logger.warn("Issue in OptionalParameters paramsToString", t);
        }
        return params.toString();
    }

    @Override
    public String toString() {
        return "SlooceSMPPSession{" +
                "connected=" + connected +
                ", provider=" + provider +
                ", serviceId='" + serviceId + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", systemId='" + systemId + '\'' +
                ", systemType='" + systemType + '\'' +
                ", stripSystemType=" + stripSystemType +
                ", receiver=" + receiver +
                ", smppSession=" + smppSession +
                '}';
    }

    public String toShortString() {
        return "SlooceSMPPSession{" +
                "connected=" + connected +
                ", host='" + host + '\'' +
                ", systemId='" + systemId + '\'' +
                ", smppSession=" + smppSession +
                '}';
    }

    public SlooceSMPPProvider getProvider() {
        return provider;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getSystemId() {
        return systemId;
    }

    public String getPassword() {
        return password;
    }

    public String getSystemType() {
        return systemType;
    }

    public boolean getStripSystemType() {
        return stripSystemType;
    }

    public SMPPSession getSmppSession() {
        return smppSession;
    }
}
