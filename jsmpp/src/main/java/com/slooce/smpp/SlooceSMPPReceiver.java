package com.slooce.smpp;

import org.jsmpp.util.DeliveryReceiptState;

/**
 * Implement the mo method to receive MO messages or onClose events.
 * This interface is an extremely simplified MessageReceiverListener.
 */
public interface SlooceSMPPReceiver {
    void mo(SlooceSMPPMessage mo, SlooceSMPPSession smpp);
    void deliveryReceipt(SlooceSMPPMessage mt, DeliveryReceiptState status, String errorCode, SlooceSMPPSession smpp);
    void onClose(SlooceSMPPSession smpp);
}
