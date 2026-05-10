#pragma once
#include "Dialect.h"
#include "common/Types.h"
#include <memory>
#include <unordered_map>

class DialectFactory
{
public:
    static DialectFactory& instance();

    const Dialect* getDialect(DbType dbType) const;
    const Dialect* getDialect(const QString& databaseProductName) const;

    void registerDialect(DbType dbType, std::unique_ptr<Dialect> dialect);
    bool isSupported(DbType dbType) const;

private:
    DialectFactory();
    std::unordered_map<DbType, std::unique_ptr<Dialect>> m_cache;
};
