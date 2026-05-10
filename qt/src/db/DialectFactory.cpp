#include "DialectFactory.h"
#include "MySQLDialect.h"
#include "PostgreSQLDialect.h"
#include <stdexcept>

DialectFactory::DialectFactory()
{
    registerDialect(DbType::MYSQL, std::make_unique<MySQLDialect>());
    registerDialect(DbType::POSTGRESQL, std::make_unique<PostgreSQLDialect>());
}

DialectFactory& DialectFactory::instance()
{
    static DialectFactory factory;
    return factory;
}

const Dialect* DialectFactory::getDialect(DbType dbType) const
{
    auto it = m_cache.find(dbType);
    if (it == m_cache.end()) {
        throw std::invalid_argument("Unsupported database type: " + dbTypeToString(dbType).toStdString());
    }
    return it->second.get();
}

const Dialect* DialectFactory::getDialect(const QString& databaseProductName) const
{
    if (databaseProductName.isEmpty()) {
        throw std::invalid_argument("Database product name cannot be null");
    }

    QString dbName = databaseProductName.toLower();
    if (dbName.contains("mysql")) {
        return getDialect(DbType::MYSQL);
    } else if (dbName.contains("postgresql")) {
        return getDialect(DbType::POSTGRESQL);
    }

    throw std::invalid_argument("Unrecognized database: " + databaseProductName.toStdString());
}

void DialectFactory::registerDialect(DbType dbType, std::unique_ptr<Dialect> dialect)
{
    m_cache[dbType] = std::move(dialect);
}

bool DialectFactory::isSupported(DbType dbType) const
{
    return m_cache.count(dbType) > 0;
}
