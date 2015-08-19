package com.slooce.smpp;

import static com.slooce.smpp.SlooceSMPPConstants.*;

public enum SlooceSMPPProvider {
    MBLOX(TAG_MBLOX_OPERATOR, true),
    OPEN_MARKET(TAG_OPEN_MARKET_OPERATOR, false);

    private short operatorTag;
    private boolean messageIdDecimal;

    SlooceSMPPProvider(final short operatorTag, final boolean messageIdDecimal) {
        this.operatorTag = operatorTag;
        this.messageIdDecimal = messageIdDecimal;
    }

    public short getOperatorTag() {
        return operatorTag;
    }

    public boolean isMessageIdDecimal() {
        return messageIdDecimal;
    }
}
