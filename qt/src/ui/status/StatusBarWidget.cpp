#include "StatusBarWidget.h"

StatusBarWidget::StatusBarWidget(QWidget* parent)
    : QStatusBar(parent)
{
    m_contextLabel = new QLabel(this);
    m_contextLabel->setFrameStyle(QFrame::StyledPanel);
    m_contextLabel->setMinimumWidth(200);

    m_messageLabel = new QLabel(this);
    m_messageLabel->setMinimumWidth(300);

    m_elapsedLabel = new QLabel(this);
    m_elapsedLabel->setFrameStyle(QFrame::StyledPanel);
    m_elapsedLabel->setMinimumWidth(80);
    m_elapsedLabel->setAlignment(Qt::AlignRight | Qt::AlignVCenter);

    addWidget(m_contextLabel, 1);
    addWidget(m_messageLabel, 2);
    addPermanentWidget(m_elapsedLabel, 0);
}

void StatusBarWidget::setContext(const QString& text)
{
    m_contextLabel->setText(" " + text + " ");
}

void StatusBarWidget::setMessage(const QString& text)
{
    m_messageLabel->setText(text);
}

void StatusBarWidget::setElapsed(qint64 ms)
{
    m_elapsedLabel->setText(QString(" %1 ms ").arg(ms));
}

void StatusBarWidget::clearElapsed()
{
    m_elapsedLabel->clear();
}
