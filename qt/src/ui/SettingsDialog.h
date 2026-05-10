#pragma once
#include <QDialog>
#include <QComboBox>
#include <QSpinBox>
#include <QPushButton>
#include <QFontComboBox>

class SettingsDialog : public QDialog
{
    Q_OBJECT

public:
    explicit SettingsDialog(QWidget* parent = nullptr);

private slots:
    void onApply();
    void onOk();

private:
    void loadSettings();

    QComboBox* m_themeCombo = nullptr;
    QComboBox* m_bgColorCombo = nullptr;
    QFontComboBox* m_fontCombo = nullptr;
    QSpinBox* m_fontSizeSpin = nullptr;
    QFontComboBox* m_editorFontCombo = nullptr;
    QSpinBox* m_editorFontSizeSpin = nullptr;
};
