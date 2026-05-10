#pragma once
#include <QString>
#include <QStringList>
#include <vector>

enum class DbType
{
    POSTGRESQL,
    MYSQL
};

inline DbType dbTypeFromString(const QString& s)
{
    if (s == "MYSQL") return DbType::MYSQL;
    return DbType::POSTGRESQL;
}

inline QString dbTypeToString(DbType t)
{
    switch (t) {
        case DbType::MYSQL: return "MYSQL";
        default: return "POSTGRESQL";
    }
}

inline QString dbTypeDisplayName(DbType t)
{
    switch (t) {
        case DbType::MYSQL: return "MySQL";
        default: return "PostgreSQL";
    }
}

struct ConnectionInfo
{
    QString name;
    DbType type = DbType::POSTGRESQL;
    QString host = "localhost";
    int port = 5432;
    QString database;
    QString username;
    QString password;

    QString displayName() const
    {
        if (!name.trimmed().isEmpty())
            return name;
        return host + ":" + QString::number(port);
    }

    int defaultPort() const
    {
        return (type == DbType::MYSQL) ? 3306 : 5432;
    }
};

struct ColumnInfo
{
    QString name;
    QString dataType;
    bool nullable = true;
    bool autoIncrement = false;
    bool primaryKey = false;
    bool foreignKey = false;
    int charMaxLength = 0;
    int numericPrecision = 0;
    int numericScale = 0;
    QString defaultValue;
    QString comment;
    QStringList enumValues;
};

struct IndexInfo
{
    QString name;
    QStringList columns;
    bool isUnique = false;
};

struct ForeignKeyInfo
{
    QString columnName;
    QString referencedTable;
    QString referencedColumn;
};
