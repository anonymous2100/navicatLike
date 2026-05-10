#include "TransactionService.h"
#include "common/Logging.h"

void TransactionService::begin(QSqlDatabase db)
{
    if (!db.transaction()) {
        qCWarning(lcLightDB) << "Failed to begin transaction";
    }
}

void TransactionService::commit(QSqlDatabase db)
{
    if (!db.commit()) {
        qCWarning(lcLightDB) << "Failed to commit transaction";
    }
}

void TransactionService::rollback(QSqlDatabase db)
{
    if (!db.rollback()) {
        qCWarning(lcLightDB) << "Failed to rollback transaction";
    }
}

bool TransactionService::isAutoCommit(QSqlDatabase db)
{
    // QSqlDriver doesn't expose auto-commit state directly
    // We track this externally via ConnectionPool
    return true;
}

void TransactionService::setAutoCommit(QSqlDatabase db, bool autoCommit)
{
    if (autoCommit) {
        db.commit();
    } else {
        db.transaction();
    }
}
