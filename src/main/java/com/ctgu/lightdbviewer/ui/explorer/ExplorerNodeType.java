package com.ctgu.lightdbviewer.ui.explorer;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public enum ExplorerNodeType
{
  CONNECTION,// 连接
  DATABASES_FOLDER,// 数据库列表层
  DATABASE,// 数据库层
  SCHEMA,   // PostgreSQL schema 
  TABLES_FOLDER,// 表列表层
  VIEWS_FOLDER,// 视图列表
  PROCEDURES_FOLDER,// 存储过程列表
  FUNCTIONS_FOLDER,// 函数列表
  TRIGGERS_FOLDER,// 触发器列表层
  TABLE,// 表层
  VIEW,// 视图
  PROCEDURE,// 存储过程
  FUNCTION,// 函数
  TRIGGER,// 触发器层
  LOADING  // 懒加载占位节
}



