package com.ctgu.lightdbviewer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Enumeration;

/**
 * @author lihuahui
 * @version 1.0
 * @description: 密码 AES-256-GCM 加密工具（本地配置文件保护用）
 * <p>
 * 安全改进：
 * - 使用 PBKDF2 进行密钥派生
 * - 使用 AES-GCM 认证加密模式
 * - 每次加密使用随机 IV 和 salt
 * - 添加密钥版本管理
 */
public final class PasswordCipher
{
  private static final Logger logger = LoggerFactory.getLogger(PasswordCipher.class);
  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final int KEY_LENGTH = 256; // bits
  private static final int GCM_TAG_LENGTH = 128; // bits
  private static final int GCM_IV_LENGTH = 12; // bytes
  private static final int SALT_LENGTH = 16; // bytes
  private static final int ITERATIONS = 100000;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private PasswordCipher()
  {
  }

  /**
   * 加密密码
   *
   * @param plaintext 明文密码
   * @return Base64编码的密文（包含salt、IV、密文）
   * @throws PasswordCipherException 加密失败
   */
  public static String encrypt(String plaintext) throws PasswordCipherException
  {
    if(plaintext == null || plaintext.isEmpty())
    {
      return "";
    }

    try
    {
      // 生成随机 salt +IV
      byte[] salt = generateRandomBytes(SALT_LENGTH);
      byte[] iv = generateRandomBytes(GCM_IV_LENGTH);
      // 从应用密钥派生加密密钥
      SecretKey secretKey = deriveKey(getApplicationKey(), salt);
      // 加密
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      // 组合：version(1) + salt(16) + iv(12) + ciphertext
      ByteBuffer buffer = ByteBuffer.allocate(1 + SALT_LENGTH + GCM_IV_LENGTH + ciphertext.length);
      buffer.put((byte)1); // version
      buffer.put(salt);
      buffer.put(iv);
      buffer.put(ciphertext);
      return Base64.getEncoder().encodeToString(buffer.array());
    }
    catch(Exception e)
    {
      logger.error("密码加密失败", e);
      throw new PasswordCipherException("密码加密失败: " + e.getMessage(), e);
    }
  }

  /**
   * 解密密码
   *
   * @param ciphertext Base64编码的密码 @return 明文密码
   * @throws PasswordCipherException 解密失败
   */
  public static String decrypt(String ciphertext) throws PasswordCipherException
  {
    if(ciphertext == null || ciphertext.isEmpty())
    {
      return "";
    }
    try
    {
      byte[] decoded = Base64.getDecoder().decode(ciphertext);
      ByteBuffer buffer = ByteBuffer.wrap(decoded);
      // 读取版本
      byte version = buffer.get();
      if(version != 1)
      {
        throw new PasswordCipherException("不支持的密文版本: " + version);
      }
      // 读取 salt +IV
      byte[] salt = new byte[SALT_LENGTH];
      buffer.get(salt);
      byte[] iv = new byte[GCM_IV_LENGTH];
      buffer.get(iv);
      // 读取密文
      byte[] cipherText = new byte[buffer.remaining()];
      buffer.get(cipherText);
      // 派生密钥
      SecretKey secretKey = deriveKey(getApplicationKey(), salt);
      // 解密
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
      byte[] plaintext = cipher.doFinal(cipherText);
      return new String(plaintext, StandardCharsets.UTF_8);
    }
    catch(Exception e)
    {
      logger.error("密码解密失败", e);
      throw new PasswordCipherException("密码解密失败: " + e.getMessage(), e);
    }
  }

  /**
   * 使用 PBKDF2 从主密钥+salt 派生加密密钥
   */
  private static SecretKey deriveKey(String mainKey, byte[] salt) throws Exception
  {
    SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
    KeySpec spec = new PBEKeySpec(mainKey.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
    byte[] keyBytes = factory.generateSecret(spec).getEncoded();
    return new SecretKeySpec(keyBytes, ALGORITHM);
  }

  /**
   * 生成随机字节
   */
  private static byte[] generateRandomBytes(int length)
  {
    byte[] bytes = new byte[length];
    SECURE_RANDOM.nextBytes(bytes);
    return bytes;
  }

  /**
   * 获取应用主密钥。
   * 注意：这是客户端加密，只能防止意外泄露，无法防御有心的攻击者。
   * 使用机器相关信息和应用标识组合作为密钥基础。
   */
  private static String getApplicationKey()
  {
    StringBuilder key = new StringBuilder("LightDB@ctgu2026");
    key.append("|").append(System.getProperty("user.home", ""));
    key.append("|").append(System.getProperty("user.name", ""));
    key.append("|").append(System.getProperty("os.name", ""));
    // 添加机器MAC地址作为额外熵源
    try
    {
      Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
      while(nics.hasMoreElements())
      {
        NetworkInterface nic = nics.nextElement();
        if(nic != null && !nic.isLoopback() && !nic.isVirtual())
        {
          byte[] mac = nic.getHardwareAddress();
          if(mac != null && mac.length > 0)
          {
            StringBuilder macStr = new StringBuilder();
            for(byte b : mac)
              macStr.append(String.format("%02X", b));
            key.append("|mac:").append(macStr);
            break; // 只使用第一个非回环网卡
          }
        }
      }
    }
    catch(Exception ignored)
    {
      // MAC地址不可用时降级
    }
    return key.toString();
  }

  /**
   * 密码加密/解密异常
   */
  public static class PasswordCipherException extends Exception
  {
    public PasswordCipherException(String message)
    {
      super(message);
    }

    public PasswordCipherException(String message, Throwable cause)
    {
      super(message, cause);
    }
  }
}


