package com.slooce.smpp;

import static com.slooce.smpp.SlooceSMPPConstants.*;

import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.DeliverSm;
import org.slf4j.Logger;

public enum SlooceSMPPProvider {
    MBLOX(TAG_MBLOX_OPERATOR, true, Alphabet.ALPHA_LATIN1, 30000 /* 60 sec inactivity timeout */, 20000 /* sometimes takes 14 sec to get response */),
    INFOBIP(TAG_INFOBIP_OPERATOR_MCCMNC, false, Alphabet.ALPHA_DEFAULT, 20000 /* 30 sec inactivity timeout */, 10000),
    OPEN_MARKET(TAG_OPEN_MARKET_OPERATOR, false, null, 20000 /* 30 sec inactivity timeout */, 10000);

    private short operatorTag;
    private boolean messageIdDecimal;
    private Alphabet alphabet;
    private int enquireLinkTimer;
    private int transactionTimer;

    SlooceSMPPProvider(final short operatorTag, final boolean messageIdDecimal, final Alphabet alphabet, final int enquireLinkTimer, final int transactionTimer) {
        this.operatorTag = operatorTag;
        this.messageIdDecimal = messageIdDecimal;
        this.alphabet = alphabet;
        this.enquireLinkTimer = enquireLinkTimer;
        this.transactionTimer = transactionTimer;
    }

    public short getOperatorTag() {
        return operatorTag;
    }

    public boolean isMessageIdDecimal() {
        return messageIdDecimal;
    }

    public Alphabet getAlphabet(final DeliverSm deliverSm, final Logger logger) {
        if (alphabet != null) {
            return alphabet;
        }
        try {
            return Alphabet.parseDataCoding(deliverSm.getDataCoding());
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
        return Alphabet.ALPHA_DEFAULT;
    }

    public int getEnquireLinkTimer() {
        return enquireLinkTimer;
    }

    public int getTransactionTimer() {
        return transactionTimer;
    }
}
