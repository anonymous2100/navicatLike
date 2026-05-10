#pragma once
#include <QObject>
#include <QSqlDatabase>
#include <QTimer>
#include <QHash>
#include <QString>
#include <QMutex>
#include <memory>
#include "common/Types.h"

struct ConnectionCredentials
{
    QString jdbcUrl;  // JDBC-style URL, internally converted to Qt driver format
    QString user;
    QString password;
};

class ConnectionPool : public QObject
{
    Q_OBJECT

public:
    static ConnectionPool& instance();

    void connect(const QString& host, int port, const QString& database,
                 const QString& user, const QString& password, DbType dbType);
    void switchDatabase(const QString& dbName);
    QSqlDatabase getConnection();
    bool isConnected() const;
    QString currentDatabase() const;
    QString databaseProductName();
    QString currentUser();
    QString currentUrl();

    void setAutoCommit(bool autoCommit);
    void commit();
    void rollback();

    void closeCurrent();
    void closeAll();
    void shutdown();

    static QString buildJdbcUrl(const QString& host, int port,
                                 const QString& database, DbType dbType);
    static QString extractDatabaseName(const QString& jdbcUrl);
    static QString buildUrlWithDatabase(const QString& jdbcUrl, const QString& dbName);

    // Health check
    void performHealthCheck();

private:
    explicit ConnectionPool(QObject* parent = nullptr);
    ~ConnectionPool() override;

    QString buildConnectionKey(const QString& jdbcUrl, const QString& user) const;
    void ensureConnection(const QString& key);

    QMutex m_mutex;
    QString m_currentKey;
    QString m_currentDbName;
    bool m_databaseSelected = false;

    struct PoolEntry
    {
        QString connectionName;
        ConnectionCredentials creds;
        bool isDerived = false;
    };
    QHash<QString, PoolEntry> m_poolEntries;
    QHash<QString, QString> m_derivedRoots; // derivedKey -> rootKey

    QTimer* m_healthTimer = nullptr;

    static constexpr int MAX_POOL_SIZE = 10;
    static constexpr int DERIVED_POOL_SIZE = 5;
};
