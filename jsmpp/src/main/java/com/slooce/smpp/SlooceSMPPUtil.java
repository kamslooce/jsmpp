package com.slooce.smpp;

import java.io.ByteArrayOutputStream;
import java.util.regex.Pattern;

public class SlooceSMPPUtil {
    /**
     * See http://unicode.org/Public/MAPPINGS/ETSI/GSM0338.TXT
     * <br>See http://www.developershome.com/sms/gsmAlphabet.asp
     * <br>See http://www.smsitaly.com/Download/ETSI_GSM_03.38.pdf
     */
    public static final short[] ISO_GSM_0338 = {
            64,     163,    36,     165,    232,    233,    249,    236,
            242,    199,    10,     216,    248,    13,     197,    229,
            0,      95,     0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      198,    230,    223,    201,
            32,     33,     34,     35,     164,    37,     38,     39,
            40,     41,     42,     43,     44,     45,     46,     47,
            48,     49,     50,     51,     52,     53,     54,     55,
            56,     57,     58,     59,     60,     61,     62,     63,
            161,    65,     66,     67,     68,     69,     70,     71,
            72,     73,     74,     75,     76,     77,     78,     79,
            80,     81,     82,     83,     84,     85,     86,     87,
            88,     89,     90,     196,    214,    209,    220,    167,
            191,    97,     98,     99,     100,    101,    102,    103,
            104,    105,    106,    107,    108,    109,    110,    111,
            112,    113,    114,    115,    116,    117,    118,    119,
            120,    121,    122,    228,    246,    241,    252,    224
    };

    public static final short ISO_GSM_0338_ESC_FOR_EXT = 27;

    public static final short[][] ISO_GSM_0338_EXT = {
            {10, 12},   {20, 94},   {40, 123},  {41, 125},  {47, 92},
            {60, 91},   {61, 126},  {62, 93},   {64, 124},  {101, 164}
    };

    /**
     * Found 72 unsupported Latin chars by actually sending all 191 printable ISO-8859-1 chars via INFOBIP
     */
    public static final int[] UNSUPPORTED_LATIN_CHARS = {
            96,
            125,
            162,
            164,
            166,
            168,
            166,
            167,
            168,
            169,
            170,
            171,
            172,
            173,
            174,
            175,
            176,
            177,
            178,
            179,
            180,
            181,
            182,
            183,
            184,
            185,
            186,
            187,
            188,
            189,
            190,
            192,
            193,
            194,
            195,
            200,
            202,
            203,
            204,
            205,
            206,
            207,
            208,
            210,
            211,
            212,
            213,
            215,
            217,
            218,
            219,
            221,
            222,
            225,
            226,
            227,
            231,
            233,
            234,
            237,
            238,
            239,
            240,
            243,
            244,
            245,
            247,
            250,
            251,
            253,
            254,
            255
    };

    public static String fromGSMCharset(byte[] aMessage) {
        StringBuilder builder = new StringBuilder(aMessage.length);
        outer:
        for (int i = 0; i < aMessage.length; i++) {
            if (ISO_GSM_0338_ESC_FOR_EXT == aMessage[i]) {
                i++;
                if (i < aMessage.length) {
                    for (short[] isoGsm0338Ext : ISO_GSM_0338_EXT) {
                        if (isoGsm0338Ext[0] == aMessage[i]) {
                            // Matched Extended GSM Alphabet
                            builder.append((char) isoGsm0338Ext[1]);
                            continue outer;
                        }
                    }
                }
            } else if (aMessage[i] < ISO_GSM_0338.length) {
                // Matched GSM Alphabet
                builder.append((char) ISO_GSM_0338[aMessage[i]]);
                continue;
            }
            // No match
            i--;
            builder.append('?');
        }

        return builder.toString();
    }

    public static byte[] toGSMCharset(final String aMessage) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(aMessage.length());
        outer:
        for (char unicodeChar : aMessage.toCharArray()) {
            for (short i = 0; i < ISO_GSM_0338.length; i++) {
                if (unicodeChar == ISO_GSM_0338[i]) {
                    baos.write((byte)i);
                    continue outer;
                }
            }
            for (final short[] aISO_GSM_0338_EXT : ISO_GSM_0338_EXT) {
                if (unicodeChar == aISO_GSM_0338_EXT[1]) {
                    baos.write((byte)ISO_GSM_0338_ESC_FOR_EXT);
                    baos.write((byte)aISO_GSM_0338_EXT[0]);
                    continue outer;
                }
            }
            baos.write('?');
        }
        return baos.toByteArray();
    }

    public static Pattern RE_PUNCT = Pattern.compile("\\p{Punct}");
    public static String sanitizeCharacters(final String s) {
        if (s == null) return null;
        if (s.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c != 0) {
                if (Character.isUnicodeIdentifierPart(c) || Character.isWhitespace(c)) {
                    sb.append(c);
                } else if (RE_PUNCT.matcher(Character.toString(c)).matches()) {
                    sb.append(c);
                } else {
                    sb.append('.');
                }
            }
        }
        return sb.toString();
    }

    public static boolean isLatinCharset(final String s) {
        if (s == null || s.isEmpty()) {
            return true;
        }
        for (int c : s.toCharArray()) {
            final Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(c);
            if (unicodeBlock != Character.UnicodeBlock.BASIC_LATIN && unicodeBlock != Character.UnicodeBlock.LATIN_1_SUPPLEMENT) {
                return false;
            }
            for (int uc : UNSUPPORTED_LATIN_CHARS) {
                if (c == uc) {
                    return false;
                }
            }
        }
        return true;
    }
}
