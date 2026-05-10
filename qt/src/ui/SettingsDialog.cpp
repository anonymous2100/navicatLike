#include "SettingsDialog.h"
#include "ThemeManager.h"
#include "FontManager.h"
#include "config/AppSettings.h"
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QFormLayout>
#include <QGroupBox>
#include <QDialogButtonBox>
#include <QColor>
#include <QLabel>

SettingsDialog::SettingsDialog(QWidget* parent)
    : QDialog(parent)
{
    setWindowTitle(tr("Settings"));
    setMinimumWidth(450);

    auto* mainLayout = new QVBoxLayout(this);

    // --- Theme Group ---
    auto* themeGroup = new QGroupBox(tr("Appearance"), this);
    auto* themeLayout = new QFormLayout(themeGroup);

    m_themeCombo = new QComboBox(themeGroup);
    for (const auto& t : ThemeManager::instance().availableThemes()) {
        m_themeCombo->addItem(t.name, t.name);
    }
    themeLayout->addRow(tr("Theme:"), m_themeCombo);

    m_bgColorCombo = new QComboBox(themeGroup);
    for (const auto& [name, hex] : ThemeManager::instance().bgPresets()) {
        m_bgColorCombo->addItem(name, hex);
    }
    themeLayout->addRow(tr("Background:"), m_bgColorCombo);

    // --- Font Group ---
    auto* fontGroup = new QGroupBox(tr("Fonts"), this);
    auto* fontLayout = new QFormLayout(fontGroup);

    m_fontCombo = new QFontComboBox(fontGroup);
    fontLayout->addRow(tr("UI Font:"), m_fontCombo);

    m_fontSizeSpin = new QSpinBox(fontGroup);
    m_fontSizeSpin->setRange(8, 48);
    m_fontSizeSpin->setValue(16);
    fontLayout->addRow(tr("UI Font Size:"), m_fontSizeSpin);

    m_editorFontCombo = new QFontComboBox(fontGroup);
    m_editorFontCombo->setFontFilters(QFontComboBox::MonospacedFonts);
    fontLayout->addRow(tr("Editor Font:"), m_editorFontCombo);

    m_editorFontSizeSpin = new QSpinBox(fontGroup);
    m_editorFontSizeSpin->setRange(8, 48);
    m_editorFontSizeSpin->setValue(16);
    fontLayout->addRow(tr("Editor Font Size:"), m_editorFontSizeSpin);

    // --- Buttons ---
    auto* btnBox = new QDialogButtonBox(QDialogButtonBox::Ok
                                         | QDialogButtonBox::Apply
                                         | QDialogButtonBox::Cancel, this);
    connect(btnBox->button(QDialogButtonBox::Ok), &QPushButton::clicked,
            this, &SettingsDialog::onOk);
    connect(btnBox->button(QDialogButtonBox::Apply), &QPushButton::clicked,
            this, &SettingsDialog::onApply);
    connect(btnBox->button(QDialogButtonBox::Cancel), &QPushButton::clicked,
            this, &QDialog::reject);

    mainLayout->addWidget(themeGroup);
    mainLayout->addWidget(fontGroup);
    mainLayout->addWidget(btnBox);

    loadSettings();
}

void SettingsDialog::loadSettings()
{
    AppSettings& s = AppSettings::instance();

    // Theme
    QString theme = s.themeName();
    int themeIdx = m_themeCombo->findData(theme);
    if (themeIdx >= 0) m_themeCombo->setCurrentIndex(themeIdx);

    // Background
    QString bgHex = s.bgColorHex();
    int bgIdx = m_bgColorCombo->findData(bgHex);
    if (bgIdx >= 0) m_bgColorCombo->setCurrentIndex(bgIdx);

    // Fonts
    m_fontCombo->setCurrentFont(QFont(s.fontFamily()));
    m_fontSizeSpin->setValue(s.fontSize());
    m_editorFontCombo->setCurrentFont(QFont(s.editorFontFamily()));
    m_editorFontSizeSpin->setValue(s.editorFontSize());
}

void SettingsDialog::onApply()
{
    AppSettings& s = AppSettings::instance();

    s.setThemeName(m_themeCombo->currentData().toString());
    s.setBgColorHex(m_bgColorCombo->currentData().toString());
    s.setFontFamily(m_fontCombo->currentFont().family());
    s.setFontSize(m_fontSizeSpin->value());
    s.setEditorFontFamily(m_editorFontCombo->currentFont().family());
    s.setEditorFontSize(m_editorFontSizeSpin->value());
    s.save();

    // Apply immediately
    ThemeManager::instance().applyTheme(s.themeName());
    FontManager::applyUiFont(s.fontFamily(), s.fontSize());
    FontManager::applyEditorFont(s.editorFontFamily(), s.editorFontSize());
    QColor bg = ThemeManager::parseBgColor(s.bgColorHex());
    if (bg.isValid()) ThemeManager::instance().applyBgColor(bg);
}

void SettingsDialog::onOk()
{
    onApply();
    accept();
}
