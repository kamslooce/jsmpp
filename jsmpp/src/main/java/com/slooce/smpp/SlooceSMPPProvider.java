package com.slooce.smpp;

import static com.slooce.smpp.SlooceSMPPConstants.*;

import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.DataCoding;
import org.jsmpp.bean.DataCodings;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.GeneralDataCoding;
import org.slf4j.Logger;

public enum SlooceSMPPProvider {
    MBLOX(TAG_MBLOX_OPERATOR, true, Alphabet.ALPHA_LATIN1, DataCodings.ZERO, 30000 /* 60 sec inactivity timeout */, 20000 /* sometimes takes 14 sec to get response */),
    INFOBIP(TAG_INFOBIP_OPERATOR_MCCMNC, false, Alphabet.ALPHA_DEFAULT, new GeneralDataCoding(Alphabet.ALPHA_LATIN1), 20000 /* 30 sec inactivity timeout */, 10000),
    OPEN_MARKET(TAG_OPEN_MARKET_OPERATOR, false, null, DataCodings.ZERO, 20000 /* 30 sec inactivity timeout */, 10000),
    MT_RESPONDER(TAG_MBLOX_OPERATOR, true, Alphabet.ALPHA_LATIN1, DataCodings.ZERO, 20000 /* 30 sec inactivity timeout */, 10000);

    private short operatorTag;
    private boolean messageIdDecimal;
    private Alphabet alphabet;
    private DataCoding dataCoding;
    private int enquireLinkTimer;
    private int transactionTimer;

    SlooceSMPPProvider(final short operatorTag, final boolean messageIdDecimal, final Alphabet alphabet, final DataCoding dataCoding, final int enquireLinkTimer, final int transactionTimer) {
        this.operatorTag = operatorTag;
        this.messageIdDecimal = messageIdDecimal;
        this.alphabet = alphabet;
        this.dataCoding = dataCoding;
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
        Alphabet parsedAlphabet;
        try {
            parsedAlphabet = Alphabet.parseDataCoding(deliverSm.getDataCoding());
        } catch (Exception e) {
            logger.warn(e.getMessage());
            parsedAlphabet = Alphabet.ALPHA_DEFAULT;
        }
        if ((parsedAlphabet == Alphabet.ALPHA_DEFAULT || parsedAlphabet == Alphabet.ALPHA_IA5) && alphabet != null) {
            // if 0x00 or 0x01, use the expected alphabet
            // mblox sends latin1 with dataCoding:0
            // infobip sends gsm with dataCoding:0 or dataCoding:1
            return alphabet;
        }
        // both mblox and infobip send ucs-2 with dataCoding:8
        return parsedAlphabet;
    }

    public DataCoding getDataCoding() {
        return dataCoding;
    }

    public int getEnquireLinkTimer() {
        return enquireLinkTimer;
    }

    public int getTransactionTimer() {
        return transactionTimer;
    }
}
