#include "ConnectionPool.h"
#include "common/Logging.h"
#include <QSqlError>
#include <QSqlQuery>
#include <QThread>
#include <stdexcept>

static QStringList mysqlOdbcDriverNames()
{
    // Try known driver names, newest first (ODBC driver name includes version)
    return {
        "MySQL ODBC 9.7 Unicode Driver",
        "MySQL ODBC 9.7 ANSI Driver",
        "MySQL ODBC 9.6 Unicode Driver",
        "MySQL ODBC 9.6 ANSI Driver",
        "MySQL ODBC 9.5 Unicode Driver",
        "MySQL ODBC 9.5 ANSI Driver",
        "MySQL ODBC 9.4 Unicode Driver",
        "MySQL ODBC 9.4 ANSI Driver",
        "MySQL ODBC 9.3 Unicode Driver",
        "MySQL ODBC 9.3 ANSI Driver",
        "MySQL ODBC 9.2 Unicode Driver",
        "MySQL ODBC 9.2 ANSI Driver",
        "MySQL ODBC 9.1 Unicode Driver",
        "MySQL ODBC 9.1 ANSI Driver",
        "MySQL ODBC 9.0 Unicode Driver",
        "MySQL ODBC 9.0 ANSI Driver",
        "MySQL ODBC 8.4 Unicode Driver",
        "MySQL ODBC 8.4 ANSI Driver",
        "MySQL ODBC 8.3 Unicode Driver",
        "MySQL ODBC 8.3 ANSI Driver",
        "MySQL ODBC 8.2 Unicode Driver",
        "MySQL ODBC 8.2 ANSI Driver",
        "MySQL ODBC 8.1 Unicode Driver",
        "MySQL ODBC 8.1 ANSI Driver",
        "MySQL ODBC 8.0 Unicode Driver",
        "MySQL ODBC 8.0 ANSI Driver",
    };
}

ConnectionPool::ConnectionPool(QObject* parent)
    : QObject(parent)
{
    m_healthTimer = new QTimer(this);
    QObject::connect(m_healthTimer, &QTimer::timeout, this, &ConnectionPool::performHealthCheck);
    m_healthTimer->start(60000);
}

ConnectionPool::~ConnectionPool()
{
    m_healthTimer->stop();
    closeAll();
}

ConnectionPool& ConnectionPool::instance()
{
    static ConnectionPool pool;
    return pool;
}

void ConnectionPool::connect(const QString& host, int port, const QString& database,
                              const QString& user, const QString& password, DbType dbType)
{
    QString jdbcUrl = buildJdbcUrl(host, port, database, dbType);
    QString key = buildConnectionKey(jdbcUrl, user);

    QMutexLocker lock(&m_mutex);

    if (m_poolEntries.contains(key)) {
        m_currentKey = key;
        m_currentDbName = database;
        m_databaseSelected = true;
        qCInfo(lcLightDB) << "Switched to existing connection:" << key;
        return;
    }

    QString connName = QString("ldb_%1").arg(m_poolEntries.size());
    {
        if (dbType == DbType::MYSQL) {
            // MySQL via ODBC bridge (QMYSQL driver not included in Qt6 by default)
            QString lastError;
            bool connected = false;

            for (const auto& driverName : mysqlOdbcDriverNames()) {
                QString connStr = QString("DRIVER={%1};SERVER=%2;PORT=%3;DATABASE=%4;UID=%5;PWD=%6;OPTION=3;")
                                      .arg(driverName, host)
                                      .arg(port)
                                      .arg(database, user, password);
                {
                    QSqlDatabase db = QSqlDatabase::addDatabase("QODBC", connName);
                    db.setDatabaseName(connStr);
                    db.setConnectOptions("SQL_ATTR_LOGIN_TIMEOUT=5;SQL_ATTR_CONNECTION_TIMEOUT=5;");
                    if (db.open()) {
                        connected = true;
                        break;
                    }
                    lastError = db.lastError().text();
                    db.close();
                } // db destroyed here before removeDatabase
                QSqlDatabase::removeDatabase(connName);
            }

            if (!connected) {
                throw std::runtime_error(
                    QString("Failed to connect MySQL via ODBC: %1\n\n"
                            "Please install MySQL ODBC Connector from:\n"
                            "https://dev.mysql.com/downloads/connector/odbc/")
                        .arg(lastError).toStdString());
            }
        } else {
            QSqlDatabase db = QSqlDatabase::addDatabase("QPSQL", connName);
            db.setHostName(host);
            db.setPort(port);
            db.setDatabaseName(database);
            db.setUserName(user);
            db.setPassword(password);

            if (!db.open()) {
                QSqlDatabase::removeDatabase(connName);
                throw std::runtime_error(
                    QString("Failed to connect: %1").arg(db.lastError().text()).toStdString());
            }
        }
    }

    PoolEntry entry;
    entry.connectionName = connName;
    entry.creds.jdbcUrl = jdbcUrl;
    entry.creds.user = user;
    entry.creds.password = password;
    entry.isOdbc = (dbType == DbType::MYSQL);
    m_poolEntries.insert(key, entry);

    m_currentKey = key;
    m_currentDbName = database;
    m_databaseSelected = true;

    qCInfo(lcLightDB) << "Created new connection:" << key;
}

void ConnectionPool::switchDatabase(const QString& dbName)
{
    QMutexLocker lock(&m_mutex);

    if (m_currentKey.isEmpty()) {
        throw std::runtime_error("No active connection. Call connect() first.");
    }

    if (dbName == m_currentDbName)
        return;

    QString oldKey = m_currentKey;
    QString oldDbName = m_currentDbName;

    auto& entry = m_poolEntries[m_currentKey];

    if (entry.isOdbc) {
        // ODBC-based MySQL: use USE statement to switch database
        QSqlDatabase db = QSqlDatabase::database(entry.connectionName);
        QSqlQuery query(db);
        if (!query.exec(QString("USE `%1`").arg(dbName))) {
            throw std::runtime_error(query.lastError().text().toStdString());
        }
        m_currentDbName = dbName;
        m_databaseSelected = true;
        qCInfo(lcLightDB) << "MySQL (ODBC) switched to database:" << dbName;
        return;
    }

    // PostgreSQL: create new connection pool for the target database
    QSqlDatabase db = QSqlDatabase::database(entry.connectionName);
    ConnectionCredentials creds = entry.creds;
    QString rootKey = m_derivedRoots.value(m_currentKey, m_currentKey);
    QString newUrl = buildUrlWithDatabase(creds.jdbcUrl, dbName);
    QString newKey = buildConnectionKey(newUrl, creds.user);

    if (!m_poolEntries.contains(newKey)) {
        QString newConnName = QString("ldb_%1").arg(m_poolEntries.size());
        {
            QSqlDatabase newDb = QSqlDatabase::addDatabase("QPSQL", newConnName);
            newDb.setHostName(db.hostName());
            newDb.setPort(db.port());
            newDb.setDatabaseName(dbName);
            newDb.setUserName(creds.user);
            newDb.setPassword(creds.password);

            if (!newDb.open()) {
                QSqlDatabase::removeDatabase(newConnName);
                m_currentKey = oldKey;
                m_currentDbName = oldDbName;
                throw std::runtime_error(
                    QString("Failed to switch to database %1: %2")
                        .arg(dbName, newDb.lastError().text()).toStdString());
            }
        }

        PoolEntry newEntry;
        newEntry.connectionName = newConnName;
        newEntry.creds.jdbcUrl = newUrl;
        newEntry.creds.user = creds.user;
        newEntry.creds.password = creds.password;
        newEntry.isDerived = true;
        newEntry.isOdbc = false;
        m_poolEntries.insert(newKey, newEntry);
        m_derivedRoots.insert(newKey, rootKey);

        qCInfo(lcLightDB) << "PostgreSQL created new connection pool for database:" << dbName;
    }

    m_currentKey = newKey;
    m_currentDbName = dbName;
    m_databaseSelected = true;
    qCInfo(lcLightDB) << "PostgreSQL switched to database:" << dbName;
}

QSqlDatabase ConnectionPool::getConnection()
{
    QMutexLocker lock(&m_mutex);

    if (m_currentKey.isEmpty()) {
        throw std::runtime_error("No active connection. Call connect() first.");
    }

    auto it = m_poolEntries.find(m_currentKey);
    if (it == m_poolEntries.end()) {
        throw std::runtime_error("Connection pool is closed");
    }

    QSqlDatabase db = QSqlDatabase::database(it->connectionName);
    if (!db.isOpen()) {
        throw std::runtime_error("Database connection is not open");
    }

    return db;
}

bool ConnectionPool::isConnected() const
{
    if (m_currentKey.isEmpty()) return false;
    auto it = m_poolEntries.find(m_currentKey);
    if (it == m_poolEntries.end()) return false;
    return QSqlDatabase::database(it->connectionName).isOpen();
}

QString ConnectionPool::currentDatabase() const
{
    return m_currentDbName;
}

QString ConnectionPool::databaseProductName()
{
    QSqlDatabase db = getConnection();
    QString driver = db.driverName().toLower();
    if (driver == "qmysql" || driver == "qodbc") return "MySQL";
    if (driver == "qpsql") return "PostgreSQL";
    return db.driverName();
}

QString ConnectionPool::currentUser()
{
    return getConnection().userName();
}

QString ConnectionPool::currentUrl()
{
    auto it = m_poolEntries.find(m_currentKey);
    if (it != m_poolEntries.end())
        return it->creds.jdbcUrl;
    return {};
}

void ConnectionPool::setAutoCommit(bool autoCommit)
{
    QSqlDatabase db = getConnection();
    if (autoCommit) {
        db.commit();
    } else {
        db.transaction();
    }
}

void ConnectionPool::commit()
{
    getConnection().commit();
}

void ConnectionPool::rollback()
{
    getConnection().rollback();
}

void ConnectionPool::closeCurrent()
{
    QMutexLocker lock(&m_mutex);
    if (m_currentKey.isEmpty()) return;

    QString rootKey = m_derivedRoots.value(m_currentKey, m_currentKey);
    QStringList toClose;
    toClose << rootKey;
    for (auto it = m_derivedRoots.begin(); it != m_derivedRoots.end(); ++it) {
        if (it.value() == rootKey)
            toClose << it.key();
    }

    for (const auto& key : toClose) {
        auto entry = m_poolEntries.take(key);
        if (!entry.connectionName.isEmpty()) {
            {
                QSqlDatabase db = QSqlDatabase::database(entry.connectionName);
                if (db.isOpen()) db.close();
            }
            QSqlDatabase::removeDatabase(entry.connectionName);
        }
        m_derivedRoots.remove(key);
    }

    m_currentKey.clear();
    m_currentDbName.clear();
    m_databaseSelected = false;
}

void ConnectionPool::closeAll()
{
    QMutexLocker lock(&m_mutex);
    for (auto it = m_poolEntries.begin(); it != m_poolEntries.end(); ++it) {
        {
            QSqlDatabase db = QSqlDatabase::database(it->connectionName);
            if (db.isOpen()) db.close();
        }
        QSqlDatabase::removeDatabase(it->connectionName);
    }
    m_poolEntries.clear();
    m_derivedRoots.clear();
    m_currentKey.clear();
    m_currentDbName.clear();
    m_databaseSelected = false;
}

void ConnectionPool::shutdown()
{
    m_healthTimer->stop();
    closeAll();
}

void ConnectionPool::performHealthCheck()
{
    QMutexLocker lock(&m_mutex);
    for (auto it = m_poolEntries.begin(); it != m_poolEntries.end(); ++it) {
        QSqlDatabase db = QSqlDatabase::database(it->connectionName);
        if (db.isOpen()) {
            QSqlQuery q(db);
            if (q.exec("SELECT 1")) {
                qCDebug(lcLightDB) << "Health check passed:" << it.key();
            } else {
                qCWarning(lcLightDB) << "Health check failed for:" << it.key();
            }
        }
    }
}

QString ConnectionPool::buildConnectionKey(const QString& jdbcUrl, const QString& user) const
{
    return user + "@" + jdbcUrl;
}

QString ConnectionPool::buildJdbcUrl(const QString& host, int port,
                                      const QString& database, DbType dbType)
{
    QString prefix = (dbType == DbType::MYSQL) ? "jdbc:mysql://" : "jdbc:postgresql://";
    return QString("%1%2:%3/%4").arg(prefix, host).arg(port).arg(database);
}

QString ConnectionPool::extractDatabaseName(const QString& jdbcUrl)
{
    int slashSlash = jdbcUrl.indexOf("//");
    if (slashSlash < 0) return {};
    QString afterHost = jdbcUrl.mid(slashSlash + 2);
    int slash = afterHost.indexOf('/');
    if (slash < 0) return {};
    QString db = afterHost.mid(slash + 1);
    int question = db.indexOf('?');
    if (question >= 0) db = db.left(question);
    return db.isEmpty() ? QString() : db;
}

QString ConnectionPool::buildUrlWithDatabase(const QString& jdbcUrl, const QString& dbName)
{
    int slashSlash = jdbcUrl.indexOf("//");
    if (slashSlash < 0) return jdbcUrl + "/" + dbName;

    QString afterAuth = jdbcUrl.mid(slashSlash + 2);
    int firstSlash = afterAuth.indexOf('/');
    if (firstSlash < 0) return jdbcUrl + "/" + dbName;

    QString base = jdbcUrl.left(slashSlash + 2) + afterAuth.left(firstSlash);
    QString remainder = afterAuth.mid(firstSlash + 1);
    int q = remainder.indexOf('?');
    QString params = (q >= 0) ? remainder.mid(q) : QString();
    return base + "/" + dbName + params;
}
