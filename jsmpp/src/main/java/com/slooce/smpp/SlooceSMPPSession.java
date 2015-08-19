package com.slooce.smpp;

import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.AlertNotification;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.DataCodings;
import org.jsmpp.bean.DataSm;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.DeliveryReceipt;
import org.jsmpp.bean.ESMClass;
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
import org.jsmpp.util.HexUtil;
import org.jsmpp.util.InvalidDeliveryReceiptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * An SMPP Session with more familiar methods to connect, send MT messages, and to receive MO messages ({@link SlooceSMPPReceiver})).
 */
public class SlooceSMPPSession {
    private static final Logger logger = LoggerFactory.getLogger(SlooceSMPPSession.class);

    private final SlooceSMPPProvider provider;
    private final String serviceId;
    private final String serviceType;
    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final boolean stripSystemType;
    private final SlooceSMPPReceiver receiver;

    private SMPPSession smppSession = null;
    private boolean connected = false;

    public SlooceSMPPSession() {
        this.provider = SlooceSMPPProvider.OPEN_MARKET;
        this.serviceId = null;
        this.serviceType = SlooceSMPPConstants.SERVICE_TYPE_CELLULAR_MESSAGING;
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
                             final String host, final int port,
                             final String systemId, final String password,
                             final String systemType, final boolean stripSystemType,
                             final SlooceSMPPReceiver receiver) {
        this.provider = provider;
        this.serviceId = serviceId;
        this.serviceType = serviceType;
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
                logger.info("SlooceSMPPSession.connect - Connect attempt #{}...", attempt);
                connect();
            } catch (Exception e) {
                logger.error("SlooceSMPPSession.connect - Failed to connect - " + this, e);
                // wait before retrying
                try { Thread.sleep(retryInterval); } catch (Exception ignored) {
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
        smppSession = new SMPPSession();
        smppSession.setTransactionTimer(10000);
        smppSession.setMessageReceiverListener(new MessageReceiverListener() {
            @Override
            public void onAcceptDeliverSm(DeliverSm deliverSm) throws ProcessRequestException {
                logger.info("Receiving - from:{} to:{}", deliverSm.getSourceAddr(), deliverSm.getDestAddress());
                if (MessageType.SMSC_DEL_RECEIPT.containedIn(deliverSm.getEsmClass())) {
                    // this message is a delivery receipt
                    try {
                        DeliveryReceipt delReceipt = deliverSm.getShortMessageAsDeliveryReceipt();
                        String messageId = delReceipt.getId();
                        if (provider.isMessageIdDecimal()) {
                            // Provider sends the messageId in a delivery receipt as a decimal value string per the SMPP spec.
                            // Convert it to hex to match the messageId hex string returned when submitting the MT.
                            messageId = Integer.toHexString(Integer.valueOf(messageId));
                        }
                        logger.info("Received delivery receipt - messageId:{} from:{} to:{} {}{}",
                                messageId, deliverSm.getSourceAddr(), deliverSm.getDestAddress(), delReceipt, paramsToString(deliverSm.getOptionalParameters()));
                    } catch (InvalidDeliveryReceiptException e) {
                        logger.error("Failed getting delivery receipt", e);
                    }
                } else {
                    // this message is an incoming MO
                    String receiptedMessageId = getOptionalParameterValueAsString(deliverSm.getOptionalParameter(OptionalParameter.Tag.RECEIPTED_MESSAGE_ID));
                    String operator = getOptionalParameterValueAsString(OptionalParameters.get(provider.getOperatorTag(), deliverSm.getOptionalParameters()));
                    String message = new String(deliverSm.getShortMessage());
                    if (stripSystemType && systemType != null) {
                        message = Pattern.compile(systemType + "\\s*", Pattern.CASE_INSENSITIVE).matcher(message).replaceFirst("");
                    }
                    logger.info("Received message - id:{} from:{} operator:{} to:{} text:{}{}",
                            receiptedMessageId, deliverSm.getSourceAddr(), operator, deliverSm.getDestAddress(), message, paramsToString(deliverSm.getOptionalParameters()));
                    if (receiver != null) {
                        receiver.mo(receiptedMessageId, message, deliverSm.getSourceAddr(), operator, deliverSm.getDestAddress(), smpp);
                    }
                }
            }

            private String getOptionalParameterValueAsString(OptionalParameter optionalParameter) {
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
            public void onAcceptAlertNotification(AlertNotification alertNotification) {
            }

            @Override
            public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
                return null;
            }
        });
        smppSession.addSessionStateListener(new SessionStateListener() {
            @Override
            public void onStateChange(final SessionState newState, final SessionState oldState, final Session source) {
                if (newState.equals(SessionState.CLOSED)) {
                    connected = false;
                    logger.warn("Session closed - {}", this);
                    receiver.onClose(smpp);
                }
            }
        });

        try {
            smppSession.connectAndBind(host, port, new BindParameter(BindType.BIND_TRX, systemId, password, systemType, TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, null));
            connected = true;
            logger.info("Connected to {} using {}@{}:{} - {}", provider, systemId, host, port, this);
        } catch (IOException e) {
            logger.error("Failed to connect - " + this, e);
            throw e;
        }
    }

    public void mt(final String message, final String subscriber, final String shortOrLongCode, final String operator)
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
            sendMT(message,
                    shortOrLongCode.length() < 7 ? TypeOfNumber.NETWORK_SPECIFIC : TypeOfNumber.INTERNATIONAL, shortOrLongCode,
                    TypeOfNumber.INTERNATIONAL, subscriber,
                    optionalParameters.toArray(new OptionalParameter[optionalParameters.size()]));
        } else {
            sendMT(message,
                    shortOrLongCode.length() < 7 ? TypeOfNumber.NETWORK_SPECIFIC : TypeOfNumber.INTERNATIONAL, shortOrLongCode,
                    TypeOfNumber.INTERNATIONAL, subscriber);
        }
    }

    private void sendMT(final String message,
                        final TypeOfNumber sourceTon, final String source,
                        final TypeOfNumber destinationTon, final String destination,
                        OptionalParameter... optionalParameters)
            throws InvalidResponseException, PDUException, IOException, NegativeResponseException,
            ResponseTimeoutException {
        try {
            final String messageId = smppSession.submitShortMessage(serviceType, sourceTon, NumberingPlanIndicator.UNKNOWN, source, destinationTon, NumberingPlanIndicator.UNKNOWN, destination,
                    new ESMClass(), (byte) 0, (byte) 1, null, null, new RegisteredDelivery(SMSCDeliveryReceipt.SUCCESS_FAILURE), (byte) 0, DataCodings.ZERO, (byte) 0, message.getBytes(),
                    optionalParameters);
            logger.info("MT sent - messageId:{} to:{} from:{} text:{} {}", messageId, destination, source, message, paramsToString(optionalParameters));
        } catch (PDUException e) {
            logger.error("Failed to send MT - to:" + destination + " from:" + source + " text:" + message + paramsToString(optionalParameters), e);
            throw e;
        } catch (ResponseTimeoutException e) {
            logger.error("Failed to send MT - to:" + destination + " from:" + source + " text:" + message + paramsToString(optionalParameters), e);
            throw e;
        } catch (InvalidResponseException e) {
            logger.error("Failed to send MT - to:" + destination + " from:" + source + " text:" + message + paramsToString(optionalParameters), e);
            throw e;
        } catch (NegativeResponseException e) {
            logger.error("Failed to send MT - to:" + destination + " from:" + source + " text:" + message + paramsToString(optionalParameters), e);
            throw e;
        } catch (IOException e) {
            logger.error("Failed to send MT - to:" + destination + " from:" + source + " text:" + message + paramsToString(optionalParameters), e);
            throw e;
        } catch (Throwable e) {
            logger.error("Failed to send MT - to:" + destination + " from:" + source + " text:" + message + paramsToString(optionalParameters), e);
            throw new RuntimeException(e);
        }
    }

    protected static String paramsToString(final OptionalParameter[] optionalParameters) {
        StringBuilder params = new StringBuilder();
        try {
            if (optionalParameters != null) {
                for (OptionalParameter op : optionalParameters) {
                    params.append(" tag:0x").append(Integer.toHexString(op.tag));
                    params.append(" serialized:0x").append(HexUtil.conventBytesToHexString(op.serialize()));
                    if (op instanceof OptionalParameter.OctetString) {
                        params.append(" stringValue:").append(((OptionalParameter.OctetString) op).getValueAsString());
                    } else {
                        params.append(" stringValue:").append(op.toString());
                    }
                }
            }
        } catch (Throwable t) {
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
                ", password='" + password + '\'' +
                ", systemType='" + systemType + '\'' +
                ", stripSystemType=" + stripSystemType +
                ", receiver=" + receiver +
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
