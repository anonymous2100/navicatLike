#pragma once
#include <QString>
#include <stdexcept>

class PasswordCipher
{
public:
    class Exception : public std::runtime_error
    {
    public:
        explicit Exception(const char* msg) : std::runtime_error(msg) {}
    };

    static QString encrypt(const QString& plaintext);
    static QString decrypt(const QString& ciphertext);

    PasswordCipher() = delete;

private:
    static QString getApplicationKey();
    static std::vector<unsigned char> deriveKey(const QString& mainKey,
                                                 const std::vector<unsigned char>& salt);
    static std::vector<unsigned char> generateRandomBytes(int length);

    static constexpr int SALT_LENGTH = 16;
    static constexpr int GCM_IV_LENGTH = 12;
    static constexpr int GCM_TAG_LENGTH = 16;
    static constexpr int KEY_LENGTH = 32; // 256 bits
    static constexpr int ITERATIONS = 100000;
};
