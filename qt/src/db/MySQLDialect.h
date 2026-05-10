#pragma once
#include "Dialect.h"

class MySQLDialect : public Dialect
{
public:
    DbType getDbType() const override { return DbType::MYSQL; }

    QString getLimitSql(const QString& sql, int offset, int limit) const override;
    QString getCurrentTimeFunction() const override;
    QString getTablesSql(const QString& schema) const override;
    QString getViewsSql(const QString& schema) const override;
    QString getColumnsSql(const QString& schema, const QString& table) const override;
    QString getPrimaryKeysSql(const QString& schema, const QString& table) const override;
    QString getForeignKeysSql(const QString& schema, const QString& table) const override;
    QString getIndexesSql(const QString& schema, const QString& table) const override;
    bool supportsSchema() const override;
    QString getDefaultSchema() const override;
    QString escapeIdentifier(const QString& identifier) const override;
    QString escapeStringLiteral(const QString& literal) const override;
    bool supportsCubeRollup() const override;
    QString getSequencesSql(const QString& schema) const override;
};
