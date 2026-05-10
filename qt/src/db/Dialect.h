#pragma once
#include <QString>
#include "common/Types.h"

class Dialect
{
public:
    virtual ~Dialect() = default;

    virtual DbType getDbType() const = 0;

    virtual QString getLimitSql(const QString& sql, int offset, int limit) const = 0;
    virtual QString getCurrentTimeFunction() const = 0;
    virtual QString getTablesSql(const QString& schema) const = 0;
    virtual QString getViewsSql(const QString& schema) const = 0;
    virtual QString getColumnsSql(const QString& schema, const QString& table) const = 0;
    virtual QString getPrimaryKeysSql(const QString& schema, const QString& table) const = 0;
    virtual QString getForeignKeysSql(const QString& schema, const QString& table) const = 0;
    virtual QString getIndexesSql(const QString& schema, const QString& table) const = 0;
    virtual bool supportsSchema() const = 0;
    virtual QString getDefaultSchema() const = 0;
    virtual QString escapeIdentifier(const QString& identifier) const = 0;
    virtual QString escapeStringLiteral(const QString& literal) const = 0;
    virtual bool supportsCubeRollup() const = 0;
    virtual QString getSequencesSql(const QString& schema) const = 0;
};
