package com.slooce.smpp;

/**
 * Implement the mo method to receive MO messages or onClose events.
 * This interface is an extremely simplified MessageReceiverListener.
 */
public interface SlooceSMPPReceiver {
    void mo(String id, String message, String subscriber, String operator, String shortcode, SlooceSMPPSession smpp);
    void onClose(SlooceSMPPSession smpp);
}
