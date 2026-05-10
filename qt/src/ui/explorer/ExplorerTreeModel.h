#pragma once
#include <QAbstractItemModel>
#include <QString>
#include <QList>
#include <QIcon>

enum class ExplorerNodeType
{
    ROOT,
    DATABASE,
    SCHEMA,
    TABLES_FOLDER,
    VIEWS_FOLDER,
    FUNCTIONS_FOLDER,
    PROCEDURES_FOLDER,
    TRIGGERS_FOLDER,
    TABLE,
    VIEW,
    FUNCTION,
    PROCEDURE,
    TRIGGER
};

class ExplorerTreeItem
{
public:
    explicit ExplorerTreeItem(const QString& name,
                               ExplorerNodeType type = ExplorerNodeType::ROOT,
                               ExplorerTreeItem* parent = nullptr);
    ~ExplorerTreeItem();

    ExplorerTreeItem* child(int row);
    int childCount() const;
    int row() const;
    ExplorerTreeItem* parent() const;
    void appendChild(ExplorerTreeItem* child);
    void removeChildren();

    QString name() const { return m_name; }
    ExplorerNodeType nodeType() const { return m_nodeType; }
    bool loaded() const { return m_loaded; }
    void setLoaded(bool l) { m_loaded = l; }

private:
    QString m_name;
    ExplorerNodeType m_nodeType;
    ExplorerTreeItem* m_parent = nullptr;
    QList<ExplorerTreeItem*> m_children;
    bool m_loaded = false;
};

class ExplorerTreeModel : public QAbstractItemModel
{
    Q_OBJECT

public:
    explicit ExplorerTreeModel(QObject* parent = nullptr);
    ~ExplorerTreeModel() override;

    void clear();
    void appendRootItem(ExplorerTreeItem* item);
    ExplorerTreeItem* itemFromIndex(const QModelIndex& index) const;

    // QAbstractItemModel interface
    QModelIndex index(int row, int column, const QModelIndex& parent) const override;
    QModelIndex parent(const QModelIndex& index) const override;
    int rowCount(const QModelIndex& parent) const override;
    int columnCount(const QModelIndex& parent) const override;
    QVariant data(const QModelIndex& index, int role) const override;

private:
    QList<ExplorerTreeItem*> m_rootItems;
};
