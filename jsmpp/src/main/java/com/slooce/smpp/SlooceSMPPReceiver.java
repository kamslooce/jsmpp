package com.slooce.smpp;

import org.jsmpp.util.DeliveryReceiptState;

/**
 * Implement the mo method to receive MO messages or onClose events.
 * This interface is an extremely simplified MessageReceiverListener.
 */
public interface SlooceSMPPReceiver {
    void mo(String messageId, String message, String subscriber, String operator, String shortcode, SlooceSMPPSession smpp);
    void deliveryReceipt(String messageId, String message, String subscriber, String operator, String shortcode, DeliveryReceiptState status, String errorCode, SlooceSMPPSession smpp);
    void onClose(SlooceSMPPSession smpp);
}
