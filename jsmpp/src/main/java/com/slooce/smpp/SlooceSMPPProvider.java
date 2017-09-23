package com.slooce.smpp;

import static com.slooce.smpp.SlooceSMPPConstants.DECIMAL_MSG_ID;
import static com.slooce.smpp.SlooceSMPPConstants.HEX_MSG_ID;
import static com.slooce.smpp.SlooceSMPPConstants.TAG_INFOBIP_OPERATOR_MCCMNC;
import static com.slooce.smpp.SlooceSMPPConstants.TAG_MBLOX_OPERATOR;
import static com.slooce.smpp.SlooceSMPPConstants.TAG_OPEN_MARKET_OPERATOR;

import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.DataCoding;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.slf4j.Logger;
import static org.jsmpp.bean.Alphabet.ALPHA_DEFAULT;
import static org.jsmpp.bean.Alphabet.ALPHA_IA5;
import static org.jsmpp.bean.Alphabet.ALPHA_LATIN1;
import static org.jsmpp.bean.DataCodings.ZERO;
import static org.jsmpp.bean.NumberingPlanIndicator.ISDN;
import static org.jsmpp.bean.NumberingPlanIndicator.UNKNOWN;

public enum SlooceSMPPProvider {
    MBLOX(TAG_MBLOX_OPERATOR, DECIMAL_MSG_ID, ALPHA_LATIN1, ZERO, UNKNOWN, 30000 /* 60 sec inactivity timeout */, 20000 /* sometimes takes 14 sec to get response */),
    MBLOX_ATLAS(TAG_MBLOX_OPERATOR, HEX_MSG_ID, ALPHA_LATIN1, new GeneralDataCoding(ALPHA_LATIN1), UNKNOWN, 30000 /* 60 sec inactivity timeout */, 20000 /* sometimes takes 14 sec to get response */),
    CLX(TAG_MBLOX_OPERATOR, HEX_MSG_ID, ALPHA_DEFAULT, new GeneralDataCoding(ALPHA_LATIN1), ISDN, 30000 /* 60 sec inactivity timeout */, 20000 /* sometimes takes 14 sec to get response */),
    INFOBIP(TAG_INFOBIP_OPERATOR_MCCMNC, HEX_MSG_ID, ALPHA_DEFAULT, new GeneralDataCoding(ALPHA_LATIN1), UNKNOWN, 20000 /* 30 sec inactivity timeout */, 10000),
    OPEN_MARKET(TAG_OPEN_MARKET_OPERATOR, HEX_MSG_ID, null, ZERO, UNKNOWN, 20000 /* 30 sec inactivity timeout */, 10000),
    MT_RESPONDER(TAG_MBLOX_OPERATOR, DECIMAL_MSG_ID, ALPHA_LATIN1, ZERO, UNKNOWN, 20000 /* 30 sec inactivity timeout */, 10000);

    private short operatorTag;
    private boolean incomingMessageIdDecimal;
    private Alphabet incomingAlphabet;
    private DataCoding outgoingDataCoding;
    private NumberingPlanIndicator outgoingNumberingPlanIndicator;
    private int enquireLinkTimer;
    private int transactionTimer;

    SlooceSMPPProvider(final short operatorTag,
                       final boolean incomingMessageIdDecimal,
                       final Alphabet incomingAlphabet,
                       final DataCoding outgoingDataCoding,
                       final NumberingPlanIndicator outgoingNumberingPlanIndicator,
                       final int enquireLinkTimer,
                       final int transactionTimer) {
        this.operatorTag = operatorTag;
        this.incomingMessageIdDecimal = incomingMessageIdDecimal;
        this.incomingAlphabet = incomingAlphabet;
        this.outgoingDataCoding = outgoingDataCoding;
        this.outgoingNumberingPlanIndicator = outgoingNumberingPlanIndicator;
        this.enquireLinkTimer = enquireLinkTimer;
        this.transactionTimer = transactionTimer;
    }

    public short getOperatorTag() {
        return operatorTag;
    }

    public boolean isIncomingMessageIdDecimal() {
        return incomingMessageIdDecimal;
    }

    public Alphabet getIncomingAlphabet(final DeliverSm deliverSm, final Logger logger) {
        Alphabet parsedAlphabet;
        try {
            parsedAlphabet = Alphabet.parseDataCoding(deliverSm.getDataCoding());
        } catch (Exception e) {
            logger.warn(e.getMessage());
            parsedAlphabet = ALPHA_DEFAULT;
        }
        if ((parsedAlphabet == ALPHA_DEFAULT || parsedAlphabet == ALPHA_IA5) && incomingAlphabet != null) {
            // if 0x00 or 0x01, use the expected alphabet
            // mblox sends latin1 with dataCoding:0
            // infobip sends gsm with dataCoding:0 or dataCoding:1
            return incomingAlphabet;
        }
        // both mblox and infobip send ucs-2 with dataCoding:8
        return parsedAlphabet;
    }

    public DataCoding getOutgoingDataCoding() {
        return outgoingDataCoding;
    }

    public NumberingPlanIndicator getOutgoingNumberingPlanIndicator() {
        return outgoingNumberingPlanIndicator;
    }

    public int getEnquireLinkTimer() {
        return enquireLinkTimer;
    }

    public int getTransactionTimer() {
        return transactionTimer;
    }
}
