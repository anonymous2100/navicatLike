#include "ConnectionXmlIO.h"
#include <QFile>
#include <QXmlStreamReader>
#include <QXmlStreamWriter>
#include "common/Logging.h"

bool ConnectionXmlIO::exportConnections(const std::vector<ConnectionInfo>& connections,
                                         const QString& filePath,
                                         QString& errorMsg)
{
    QFile file(filePath);
    if (!file.open(QIODevice::WriteOnly | QIODevice::Text)) {
        errorMsg = QString("Cannot open file for writing: %1").arg(file.errorString());
        return false;
    }

    QXmlStreamWriter xml(&file);
    xml.setAutoFormatting(true);
    xml.writeStartDocument();
    xml.writeStartElement("connections");

    for (const auto& conn : connections) {
        xml.writeStartElement("connection");
        xml.writeAttribute("name", conn.name);
        xml.writeTextElement("type", dbTypeToString(conn.type));
        xml.writeTextElement("host", conn.host);
        xml.writeTextElement("port", QString::number(conn.port));
        xml.writeTextElement("database", conn.database);
        xml.writeTextElement("username", conn.username);
        // Password intentionally not exported for security
        xml.writeEndElement(); // connection
    }

    xml.writeEndElement(); // connections
    xml.writeEndDocument();
    file.close();
    qCInfo(lcLightDB) << "Exported" << connections.size() << "connections to" << filePath;
    return true;
}

std::vector<ConnectionInfo> ConnectionXmlIO::importConnections(const QString& filePath,
                                                                QString& errorMsg)
{
    std::vector<ConnectionInfo> result;
    QFile file(filePath);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        errorMsg = QString("Cannot open file for reading: %1").arg(file.errorString());
        return result;
    }

    QXmlStreamReader xml(&file);
    ConnectionInfo current;

    while (!xml.atEnd() && !xml.hasError()) {
        QXmlStreamReader::TokenType token = xml.readNext();

        if (token == QXmlStreamReader::StartElement) {
            if (xml.name() == QStringLiteral("connection")) {
                current = ConnectionInfo{};
                current.name = xml.attributes().value("name").toString();
            } else if (xml.name() == QStringLiteral("type")) {
                current.type = dbTypeFromString(xml.readElementText().trimmed());
            } else if (xml.name() == QStringLiteral("host")) {
                current.host = xml.readElementText().trimmed();
            } else if (xml.name() == QStringLiteral("port")) {
                current.port = xml.readElementText().toInt();
            } else if (xml.name() == QStringLiteral("database")) {
                current.database = xml.readElementText().trimmed();
            } else if (xml.name() == QStringLiteral("username")) {
                current.username = xml.readElementText().trimmed();
            }
        } else if (token == QXmlStreamReader::EndElement) {
            if (xml.name() == QStringLiteral("connection")) {
                result.push_back(current);
            }
        }
    }

    if (xml.hasError()) {
        errorMsg = QString("XML parse error: %1").arg(xml.errorString());
        qCWarning(lcLightDB) << "Failed to import connections:" << errorMsg;
    }

    file.close();
    qCInfo(lcLightDB) << "Imported" << result.size() << "connections from" << filePath;
    return result;
}
