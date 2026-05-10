#pragma once
#include <QSqlDatabase>

class TransactionService
{
public:
    TransactionService() = delete;

    static void begin(QSqlDatabase db);
    static void commit(QSqlDatabase db);
    static void rollback(QSqlDatabase db);
    static bool isAutoCommit(QSqlDatabase db);
    static void setAutoCommit(QSqlDatabase db, bool autoCommit);
};
