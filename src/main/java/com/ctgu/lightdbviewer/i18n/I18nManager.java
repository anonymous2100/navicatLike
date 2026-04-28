package com.ctgu.lightdbviewer.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 国际化管理器
 * <p>
 * 提供多语言支持，目前支持中文和英文
 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public final class I18nManager
{
  private static final String BUNDLE_NAME = "messages";
  private static Locale currentLocale = Locale.getDefault();
  private static ResourceBundle resourceBundle;

  static
  {
    loadBundle();
  }

  private I18nManager()
  {
  }

  /**
   * 设置当前语言环境
   *
   * @param locale 语言环境
   */
  public static void setLocale(Locale locale)
  {
    currentLocale = locale;
    loadBundle();
  }

  /**
   * 获取当前语言环境
   *
   * @return 当前语言环境
   */
  public static Locale getLocale()
  {
    return currentLocale;
  }

  /**
   * 获取本地化字符串
   *
   * @param key 消息   * @return 本地化字符串
   */
  public static String getMessage(String key)
  {
    try
    {
      return resourceBundle.getString(key);
    }
    catch(MissingResourceException e)
    {
      return '!' + key + '!';
    }
  }

  /**
   * 获取带参数的本地化字符串
   *
   * @param key  消息   * @param args 参数
   * @return 格式化后的本地化字符   */
  public static String getMessage(String key, Object... args)
  {
    try
    {
      String pattern = resourceBundle.getString(key);
      return MessageFormat.format(pattern, args);
    }
    catch(MissingResourceException e)
    {
      return '!' + key + '!';
    }
  }

  /**
   * 检查是否包含指定的消息   *
   * @param key 消息   * @return 是否包含
   */
  public static boolean containsKey(String key)
  {
    try
    {
      resourceBundle.getString(key);
      return true;
    }
    catch(MissingResourceException e)
    {
      return false;
    }
  }

  /**
   * 加载资源   */
  private static void loadBundle()
  {
    try
    {
      resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, currentLocale);
    }
    catch(MissingResourceException e)
    {
      // 回退到默认资源包
      try
      {
        resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ROOT);
      }
      catch(MissingResourceException ex)
      {
        // 如果还是找不到，创建一个空的资源包
        resourceBundle = new ResourceBundle()
        {
          @Override
          protected Object handleGetObject(String key)
          {
            return '!' + key + '!';
          }

          @Override
          public java.util.Enumeration<String> getKeys()
          {
            return java.util.Collections.emptyEnumeration();
          }
        };
      }
    }
  }

  /**
   * 常用消息键常   */
  public static class Keys
  {
    // 菜单
    public static final String MENU_FILE = "menu.file";
    public static final String MENU_EDIT = "menu.edit";
    public static final String MENU_VIEW = "menu.view";
    public static final String MENU_TOOLS = "menu.tools";
    public static final String MENU_HELP = "menu.help";

    // 菜单    public static final String MENU_NEW_CONNECTION = "menu.newConnection";
    public static final String MENU_CLOSE_CONNECTION = "menu.closeConnection";
    public static final String MENU_IMPORT_CONNECTION = "menu.importConnection";
    public static final String MENU_EXPORT_CONNECTION = "menu.exportConnection";
    public static final String MENU_EXIT = "menu.exit";

    public static final String MENU_COPY = "menu.copy";
    public static final String MENU_PASTE = "menu.paste";
    public static final String MENU_SELECT_ALL = "menu.selectAll";

    public static final String MENU_OPTIONS = "menu.options";

    public static final String MENU_ABOUT = "menu.about";

    // 按钮
    public static final String BUTTON_OK = "button.ok";
    public static final String BUTTON_CANCEL = "button.cancel";
    public static final String BUTTON_YES = "button.yes";
    public static final String BUTTON_NO = "button.no";
    public static final String BUTTON_CONNECT = "button.connect";
    public static final String BUTTON_DISCONNECT = "button.disconnect";
    public static final String BUTTON_REFRESH = "button.refresh";

    // 状    public static final String STATUS_READY = "status.ready";
    public static final String STATUS_CONNECTED = "status.connected";
    public static final String STATUS_DISCONNECTED = "status.disconnected";
    public static final String STATUS_LOADING = "status.loading";

    // 错误
    public static final String ERROR_CONNECTION_FAILED = "error.connectionFailed";
    public static final String ERROR_SQL_FAILED = "error.sqlFailed";
    public static final String ERROR_LOAD_FAILED = "error.loadFailed";

    // 确认
    public static final String CONFIRM_DELETE = "confirm.delete";
    public static final String CONFIRM_CLOSE = "confirm.close";

    // 其他
    public static final String APP_TITLE = "app.title";
    public static final String APP_VERSION = "app.version";
  }
}


