#include "PasswordCipher.h"
#include <QByteArray>
#include <QString>
#include <openssl/evp.h>
#include <openssl/rand.h>
#include <openssl/err.h>
#include <openssl/hmac.h>
#include <cstring>

QString PasswordCipher::encrypt(const QString& plaintext)
{
    if (plaintext.isEmpty())
        return {};

    // Generate random salt + IV
    auto salt = generateRandomBytes(SALT_LENGTH);
    auto iv = generateRandomBytes(GCM_IV_LENGTH);

    // Derive key
    auto keyBytes = deriveKey(getApplicationKey(), salt);

    // Encrypt with AES-256-GCM
    EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
    if (!ctx)
        throw Exception("Failed to create cipher context");

    QByteArray plainBytes = plaintext.toUtf8();
    std::vector<unsigned char> ciphertext(plainBytes.size() + 16); // extra for tag
    int outLen = 0;
    int totalLen = 0;

    bool ok = false;
    do {
        if (EVP_EncryptInit_ex(ctx, EVP_aes_256_gcm(), nullptr, nullptr, nullptr) != 1)
            break;
        if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_IVLEN, GCM_IV_LENGTH, nullptr) != 1)
            break;
        if (EVP_EncryptInit_ex(ctx, nullptr, nullptr, keyBytes.data(), iv.data()) != 1)
            break;
        if (EVP_EncryptUpdate(ctx, ciphertext.data(), &outLen,
                              reinterpret_cast<const unsigned char*>(plainBytes.constData()),
                              static_cast<int>(plainBytes.size())) != 1)
            break;
        totalLen = outLen;

        // Finalize (writes tag)
        if (EVP_EncryptFinal_ex(ctx, ciphertext.data() + totalLen, &outLen) != 1)
            break;
        totalLen += outLen;

        // Get the tag
        std::vector<unsigned char> tag(GCM_TAG_LENGTH);
        if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_GET_TAG, GCM_TAG_LENGTH, tag.data()) != 1)
            break;

        ok = true;

        // Build output: version(1) + salt(16) + iv(12) + tag(16) + ciphertext
        std::vector<unsigned char> result;
        result.push_back(1); // version
        result.insert(result.end(), salt.begin(), salt.end());
        result.insert(result.end(), iv.begin(), iv.end());
        result.insert(result.end(), tag.begin(), tag.end());
        result.insert(result.end(), ciphertext.begin(), ciphertext.begin() + totalLen);

        EVP_CIPHER_CTX_free(ctx);

        // Base64 encode
        return QString::fromUtf8(QByteArray::fromRawData(
            reinterpret_cast<const char*>(result.data()),
            static_cast<int>(result.size())).toBase64());

    } while (false);

    EVP_CIPHER_CTX_free(ctx);
    auto msg = QString("Encryption failed: %1").arg(ERR_error_string(ERR_get_error(), nullptr));
    throw Exception(msg.toUtf8().constData());
}

QString PasswordCipher::decrypt(const QString& ciphertext)
{
    if (ciphertext.isEmpty())
        return {};

    QByteArray decoded = QByteArray::fromBase64(ciphertext.toUtf8());
    if (decoded.size() < 1 + SALT_LENGTH + GCM_IV_LENGTH + GCM_TAG_LENGTH) {
        throw Exception("Invalid ciphertext: too short");
    }

    const auto* data = reinterpret_cast<const unsigned char*>(decoded.constData());
    size_t offset = 0;

    // Read version
    uint8_t version = data[offset++];
    if (version != 1) {
        auto msg = QString("Unsupported ciphertext version: %1").arg(version);
        throw Exception(msg.toUtf8().constData());
    }

    // Read salt
    std::vector<unsigned char> salt(SALT_LENGTH);
    std::memcpy(salt.data(), data + offset, SALT_LENGTH);
    offset += SALT_LENGTH;

    // Read IV
    std::vector<unsigned char> iv(GCM_IV_LENGTH);
    std::memcpy(iv.data(), data + offset, GCM_IV_LENGTH);
    offset += GCM_IV_LENGTH;

    // Read tag
    std::vector<unsigned char> tag(GCM_TAG_LENGTH);
    std::memcpy(tag.data(), data + offset, GCM_TAG_LENGTH);
    offset += GCM_TAG_LENGTH;

    // Remaining data is ciphertext
    size_t ctLen = decoded.size() - offset;

    // Derive key
    auto keyBytes = deriveKey(getApplicationKey(), salt);

    // Decrypt
    EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
    if (!ctx)
        throw Exception("Failed to create cipher context");

    std::vector<unsigned char> plaintext(ctLen + 16);
    int outLen = 0;
    int totalLen = 0;

    bool ok = false;
    do {
        if (EVP_DecryptInit_ex(ctx, EVP_aes_256_gcm(), nullptr, nullptr, nullptr) != 1)
            break;
        if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_IVLEN, GCM_IV_LENGTH, nullptr) != 1)
            break;
        if (EVP_DecryptInit_ex(ctx, nullptr, nullptr, keyBytes.data(), iv.data()) != 1)
            break;
        if (EVP_DecryptUpdate(ctx, plaintext.data(), &outLen,
                              data + offset, static_cast<int>(ctLen)) != 1)
            break;
        totalLen = outLen;

        // Set expected tag
        if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_TAG, GCM_TAG_LENGTH,
                                const_cast<unsigned char*>(tag.data())) != 1)
            break;

        // Finalize (verifies tag)
        int ret = EVP_DecryptFinal_ex(ctx, plaintext.data() + totalLen, &outLen);
        if (ret != 1)
            break;
        totalLen += outLen;

        ok = true;
        EVP_CIPHER_CTX_free(ctx);
        return QString::fromUtf8(reinterpret_cast<const char*>(plaintext.data()), totalLen);

    } while (false);

    EVP_CIPHER_CTX_free(ctx);
    throw Exception("Decryption failed (wrong key or corrupted data)");
}

std::vector<unsigned char> PasswordCipher::deriveKey(const QString& mainKey,
                                                      const std::vector<unsigned char>& salt)
{
    QByteArray keyBytes = mainKey.toUtf8();
    std::vector<unsigned char> derived(KEY_LENGTH);

    int ret = PKCS5_PBKDF2_HMAC(
        keyBytes.constData(), static_cast<int>(keyBytes.size()),
        salt.data(), static_cast<int>(salt.size()),
        ITERATIONS,
        EVP_sha256(),
        KEY_LENGTH,
        derived.data());

    if (ret != 1) {
        throw Exception("PBKDF2 key derivation failed");
    }

    return derived;
}

std::vector<unsigned char> PasswordCipher::generateRandomBytes(int length)
{
    std::vector<unsigned char> bytes(length);
    if (RAND_bytes(bytes.data(), length) != 1) {
        throw Exception("Failed to generate random bytes");
    }
    return bytes;
}

QString PasswordCipher::getApplicationKey()
{
    QString key = "LightDB@ctgu2026";
#ifdef Q_OS_WIN
    key += "|" + qEnvironmentVariable("USERPROFILE");
#else
    key += "|" + qEnvironmentVariable("HOME");
#endif
    key += "|" + qEnvironmentVariable("USER", qEnvironmentVariable("USERNAME"));
    key += "|" + qEnvironmentVariable("OS");
    // Note: MAC address retrieval would add QNetworkInterface dependency;
    // omitted for simplicity - consistent with Java version's graceful degradation
    return key;
}
