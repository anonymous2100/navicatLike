#pragma once
#include <QLabel>

class LoadingIndicator : public QLabel
{
    Q_OBJECT

public:
    explicit LoadingIndicator(QWidget* parent = nullptr);

    void start();
    void stop();

private:
    QString m_text;
    bool m_running = false;
};
