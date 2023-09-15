package androidx.iot.utils;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * 许可工具
 */
public class AES {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * 随机16位密钥
     *
     * @return
     */
    public static String randomKey() {
        String keyFormat = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 16; i++) {
            int index = random.nextInt(keyFormat.length());
            sb.append(keyFormat.charAt(index));
            if (i == 3 || i == 7 || i == 11) {
                sb.append("-");
            }
        }
        return sb.toString();
    }

    private static String inputFilter(String value) {
        if (value != null) {
            return value.replace("\n", "");
        }
        return null;
    }

    private static String keyFilter(String value) {
        if (value != null) {
            return value.replace("-", "").replace("\n", "");
        }
        return null;
    }

    /**
     * 加密
     *
     * @param input 输入
     * @param key   密钥
     * @return
     */
    public static String encrypt(String input, String key) {
        try {
            input = inputFilter(input);
            key = keyFilter(key);
            // 创建密钥
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES_ALGORITHM);
            // 创建加密器
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            // 加密
            byte[] encryptedBytes = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
            // 将加密结果进行 Base64 编码
            byte[] encryptedBase64 = Base64.getEncoder().encode(encryptedBytes);
            return new String(encryptedBase64, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解密
     * @param input 内容
     * @param key   密钥
     * @return
     */
    public static String decrypt(String input, String key) {
        try {
            input = inputFilter(input);
            key = keyFilter(key);
            //对 Base64 编码的加密数据进行解码
            byte[] encryptedBytes = Base64.getDecoder().decode(input.getBytes(StandardCharsets.UTF_8));
            //创建密钥
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES_ALGORITHM);
            //创建解密器
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            //解密
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            //将解密结果转为字符串
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
