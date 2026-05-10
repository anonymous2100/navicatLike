#pragma once
#include <QStatusBar>
#include <QLabel>

class StatusBarWidget : public QStatusBar
{
    Q_OBJECT

public:
    explicit StatusBarWidget(QWidget* parent = nullptr);

    void setContext(const QString& text);
    void setMessage(const QString& text);
    void setElapsed(qint64 ms);
    void clearElapsed();

private:
    QLabel* m_contextLabel = nullptr;
    QLabel* m_messageLabel = nullptr;
    QLabel* m_elapsedLabel = nullptr;
};
