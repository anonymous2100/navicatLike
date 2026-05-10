#include "LoadingIndicator.h"
#include <QTimer>

LoadingIndicator::LoadingIndicator(QWidget* parent)
    : QLabel(parent)
{
    setAlignment(Qt::AlignCenter);
    setStyleSheet("color: #0078D7; font-weight: bold; padding: 8px;");
    hide();
}

void LoadingIndicator::start()
{
    m_running = true;
    setText("Loading...");
    show();
}

void LoadingIndicator::stop()
{
    m_running = false;
    hide();
}
