package com.bbq.module.crm.chatbot.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * 企业微信/微信客服回调验签 + 消息解密（WXBizMsgCrypt 精简版）：
 * - 验签：msg_signature = sha1(sort(token, timestamp, nonce, encrypt))，四项排序后拼接
 * - URL 验证：GET 的 echostr 为 AES 密文，验签通过后需解密并返回明文
 * - 解密：AES-256-CBC，明文 = random(16) + msg_len(4, 网络字节序) + msg + receiveId，PKCS7 填充
 * 注意：企微/微信客服的签名参数名是 msg_signature（不是公众号的 signature）。
 * 回复走客服 API 主动发消息，不需要加密回复报文。
 */
@Slf4j
public class WechatCallbackCrypto {

    private WechatCallbackCrypto() {
    }

    /**
     * 四项验签：sha1(sort(token, timestamp, nonce, encrypt)) == msgSignature
     * @param encrypt URL 验证时传 echostr 密文；接收消息时传 XML 里 Encrypt 字段
     */
    public static boolean checkSignature(String token, String timestamp, String nonce,
                                         String encrypt, String msgSignature) {
        if (token == null || msgSignature == null) {
            return false;
        }
        List<String> list = new ArrayList<>(List.of(
                token,
                timestamp == null ? "" : timestamp,
                nonce == null ? "" : nonce,
                encrypt == null ? "" : encrypt));
        Collections.sort(list);
        String joined = String.join("", list);
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest(joined.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString().equals(msgSignature);
        } catch (Exception e) {
            log.error("[chatbot] 回调验签异常", e);
            return false;
        }
    }

    /**
     * 解密回调密文（XML 里 <Encrypt> 字段的值）
     * @param encodingAesKey 43 位 EncodingAESKey
     */
    public static String decrypt(String encryptBase64, String encodingAesKey) {
        try {
            byte[] aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
            byte[] cipherBytes = Base64.getDecoder().decode(encryptBase64);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(aesKey, 0, 16));
            byte[] plain = pkcs7Unpad(cipher.doFinal(cipherBytes));
            // plain = random(16) + msg_len(4) + msg + receiveId
            int msgLen = ((plain[16] & 0xFF) << 24) | ((plain[17] & 0xFF) << 16)
                    | ((plain[18] & 0xFF) << 8) | (plain[19] & 0xFF);
            return new String(plain, 20, msgLen, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("[chatbot] 回调密文解密失败", e);
        }
    }

    private static byte[] pkcs7Unpad(byte[] decrypted) {
        int pad = decrypted[decrypted.length - 1];
        if (pad < 1 || pad > 32) {
            pad = 0;
        }
        int outLen = decrypted.length - pad;
        byte[] result = new byte[outLen];
        System.arraycopy(decrypted, 0, result, 0, outLen);
        return result;
    }

    /** 简单抽取 XML 单值（回调报文结构固定，避免引完整 XML 库） */
    public static String extractXmlValue(String xml, String tagName) {
        if (xml == null) {
            return null;
        }
        String open = "<" + tagName + ">";
        String close = "</" + tagName + ">";
        int start = xml.indexOf(open);
        int end = xml.indexOf(close);
        if (start < 0 || end < 0) {
            return null;
        }
        String value = xml.substring(start + open.length(), end);
        // CDATA 兜底（注意：Encrypt 为 Base64，不能做 URL 解码，否则 '+' 会被破坏）
        if (value.startsWith("<![CDATA[")) {
            value = value.substring(9, value.lastIndexOf("]]>"));
        }
        return value;
    }
}
