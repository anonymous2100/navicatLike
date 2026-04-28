package com.ctgu.lightdbviewer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码加密测试
 */
class PasswordCipherTest
{
  @Test
  void testEncryptDecrypt() throws Exception
  {
    String original = "testPassword123";
    String encrypted = PasswordCipher.encrypt(original);

    assertNotNull(encrypted);
    assertNotEquals(original, encrypted);
    assertFalse(encrypted.isEmpty());

    String decrypted = PasswordCipher.decrypt(encrypted);
    assertEquals(original, decrypted);
  }

  @Test
  void testEncryptEmptyString() throws Exception
  {
    String encrypted = PasswordCipher.encrypt("");
    assertEquals("", encrypted);
  }

  @Test
  void testEncryptNull() throws Exception
  {
    String encrypted = PasswordCipher.encrypt(null);
    assertEquals("", encrypted);
  }

  @Test
  void testDecryptEmptyString() throws Exception
  {
    String decrypted = PasswordCipher.decrypt("");
    assertEquals("", decrypted);
  }

  @Test
  void testDecryptNull() throws Exception
  {
    String decrypted = PasswordCipher.decrypt(null);
    assertEquals("", decrypted);
  }

  @Test
  void testEncryptDifferentResults() throws Exception
  {
    String password = "samePassword";
    String encrypted1 = PasswordCipher.encrypt(password);
    String encrypted2 = PasswordCipher.encrypt(password);

    // 由于每次使用随机IV和salt，加密结果应该不同
    assertNotEquals(encrypted1, encrypted2);

    // 但解密后应该得到相同的原始密码
    assertEquals(password, PasswordCipher.decrypt(encrypted1));
    assertEquals(password, PasswordCipher.decrypt(encrypted2));
  }

  @Test
  void testSpecialCharacters() throws Exception
  {
    String password = "p@ssw0rd!#$%^&*()_+-=[]{}|;':\",./<>?";
    String encrypted = PasswordCipher.encrypt(password);
    String decrypted = PasswordCipher.decrypt(encrypted);
    assertEquals(password, decrypted);
  }

  @Test
  void testUnicodeCharacters() throws Exception
  {
    String password = "密码123测试";
    String encrypted = PasswordCipher.encrypt(password);
    String decrypted = PasswordCipher.decrypt(encrypted);
    assertEquals(password, decrypted);
  }

  @Test
  void testLongPassword() throws Exception
  {
    StringBuilder longPassword = new StringBuilder();
    for(int i = 0; i < 1000; i++)
    {
      longPassword.append("a");
    }
    String password = longPassword.toString();

    String encrypted = PasswordCipher.encrypt(password);
    String decrypted = PasswordCipher.decrypt(encrypted);
    assertEquals(password, decrypted);
  }

  @Test
  void testInvalidEncryptedData()
  {
    assertThrows(PasswordCipher.PasswordCipherException.class, () -> {
      PasswordCipher.decrypt("invalid_encrypted_data");
    });
  }
}
