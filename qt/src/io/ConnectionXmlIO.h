#pragma once
#include <QString>
#include <vector>
#include "common/Types.h"

class ConnectionXmlIO
{
public:
    static bool exportConnections(const std::vector<ConnectionInfo>& connections,
                                   const QString& filePath,
                                   QString& errorMsg);

    static std::vector<ConnectionInfo> importConnections(const QString& filePath,
                                                          QString& errorMsg);

    ConnectionXmlIO() = delete;
};
