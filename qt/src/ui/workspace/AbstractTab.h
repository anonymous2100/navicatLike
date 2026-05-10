#pragma once
#include <QWidget>

class AbstractTab : public QWidget
{
    Q_OBJECT

public:
    explicit AbstractTab(QWidget* parent = nullptr)
        : QWidget(parent) {}
    ~AbstractTab() override = default;

    virtual bool isDirty() const { return m_dirty; }
    virtual QString tabTitle() const = 0;

public slots:
    virtual void refresh() = 0;

signals:
    void dirtyChanged(bool dirty);
    void titleChanged(const QString& title);

protected:
    void setDirty(bool d)
    {
        if (m_dirty != d) {
            m_dirty = d;
            emit dirtyChanged(d);
        }
    }

    bool m_dirty = false;
};
