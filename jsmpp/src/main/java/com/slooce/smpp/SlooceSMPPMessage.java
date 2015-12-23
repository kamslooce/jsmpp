package com.slooce.smpp;

import java.util.Objects;

public class SlooceSMPPMessage {
    private String messageId;
    private String message;
    private String subscriber;
    private String operator;
    private String shortcode;
    private Integer csmsReference; // 0-255 (unsigned 8-bit) or 0-65535 (unsigned 16-bit)
    private Integer csmsTotalParts; // 1-255
    private Integer csmsPartNumber; // 1-255 but not greater than the total number of parts

    public SlooceSMPPMessage() {
    }

    public SlooceSMPPMessage(final String messageId, final String subscriber, final String operator, final String shortcode) {
        this.messageId = messageId;
        this.subscriber = subscriber;
        this.operator = operator;
        this.shortcode = shortcode;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(final String subscriber) {
        this.subscriber = subscriber;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(final String operator) {
        this.operator = operator;
    }

    public String getShortcode() {
        return shortcode;
    }

    public void setShortcode(final String shortcode) {
        this.shortcode = shortcode;
    }

    public Integer getCsmsReference() {
        return csmsReference;
    }

    public void setCsmsReference(final Integer csmsReference) {
        this.csmsReference = csmsReference;
    }

    public Integer getCsmsTotalParts() {
        return csmsTotalParts;
    }

    public void setCsmsTotalParts(final Integer csmsTotalParts) {
        this.csmsTotalParts = csmsTotalParts;
    }

    public Integer getCsmsPartNumber() {
        return csmsPartNumber;
    }

    public void setCsmsPartNumber(final Integer csmsPartNumber) {
        this.csmsPartNumber = csmsPartNumber;
    }

    @Override
    public String toString() {
        return "SlooceSMPPMessage{" +
                "messageId='" + messageId + '\'' +
                ", subscriber='" + subscriber + '\'' +
                ", operator='" + operator + '\'' +
                ", shortcode='" + shortcode + '\'' +
                ", csmsReference=" + csmsReference +
                ", csmsTotalParts=" + csmsTotalParts +
                ", csmsPartNumber=" + csmsPartNumber +
                ", message='" + SlooceSMPPUtil.sanitizeCharacters(message) + '\'' +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SlooceSMPPMessage that = (SlooceSMPPMessage) o;
        return Objects.equals(messageId, that.messageId) &&
                Objects.equals(message, that.message) &&
                Objects.equals(subscriber, that.subscriber) &&
                Objects.equals(operator, that.operator) &&
                Objects.equals(shortcode, that.shortcode) &&
                Objects.equals(csmsReference, that.csmsReference) &&
                Objects.equals(csmsTotalParts, that.csmsTotalParts) &&
                Objects.equals(csmsPartNumber, that.csmsPartNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, message, subscriber, operator, shortcode, csmsReference, csmsTotalParts, csmsPartNumber);
    }
}
