# CodeMonitor 代码行数监控工具（又名：崽崽代码统计器）

## 项目介绍 / Project Introduction

本项目是一款用于监控 Java 项目代码行数的工具。它能够实时统计指定目录下的 Java 文件有效代码行数，并提供每日代码更新统计功能。该工具旨在帮助开发者更好地了解项目的代码量及每日代码贡献情况。

CodeMonitor is a tool designed to monitor the line count of Java project codes. It can real-time count the effective lines of code in Java files within a specified directory and provides daily code update statistics. This tool aims to help developers better understand the code volume of their projects and daily contributions.

## 功能特性 / Features

- **代码行数统计**：监控指定目录中的所有 Java 文件，统计每个文件的有效代码行数，排除空行和注释。
  
- **历史数据存储**：保存项目的统计数据，包括累计代码行数、每日新增代码行数和上次更新的日期。
  
- **每日代码更新统计**：自动检测日期变化，统计每天新增的代码行数，并保存至历史数据中。
  
- **文件变化监控**：实时监控代码目录，当 Java 文件被创建、修改或删除时，自动更新统计数据。
  
- **图形用户界面（GUI）**：提供用户友好的界面，显示当前累计的代码行数和当天新增的代码行数，支持动态刷新。

- **跨平台支持**：能够识别程序是否作为打包的可执行文件运行，并根据不同环境调整数据文件的路径。

- **每日统计显示**：提供每日代码行数的统计数据，方便开发者查看历史记录。

- **监控目录的动态更新**：用户可以随时修改监控的 Java 项目目录，确保监控数据的实时性。

## 安装和使用 / Installation and Usage

1. **下载源代码**：
   - 克隆或下载此项目的源代码。
   ```bash
   git clone <repository-url>
   ```
2. **编译和运行**：
在 IDE（如 IntelliJ IDEA）中打开项目，确保 JDK 已正确配置。
运行 CodeMonitor.java 主程序。
3. **选择监控目录**：
启动程序后，选择要监控的 Java 项目目录，程序将开始统计代码行数。
4. **查看统计结果**：
程序界面将显示累计代码行数和今日新增代码行数。
点击“每日统计”按钮可以查看每日代码贡献情况。
## 贡献 / Contributing
如果你有兴趣参与开发或改进本项目，欢迎提交 Pull Request 或提出 Issue。

## License
本项目遵循 MIT 许可证，详细信息请查看 LICENSE 文件。

## 联系我们 / Contact Us
如有任何问题或建议，请联系项目维护者。
