---
name: python-to-java
description: 使用大模型将 Python 代码转换为可 Java，保证编译通过和正常执行。 当用户需要将 Python 文件转换为 Java、 揯要求包含编译验证和运行验证, 适合处理简单到复杂的 Python 代码转换任务。触发词包括："python 转 java"、"转换 python 代码"、"将 python 转为 java"。
---

# Python To Java

## Overview

将 Python 代码转换为等价的 Java 代码，确保生成的 Java 代码能够编译通过并正常运行,产生相同的输出。

## When to Use This Skill

当用户需要将 Python 文件或代码片段转换为 Java 时。触发场景包括:

- 将 Python 脚本转为 Java 类
- 将 Python 代码片段转换为完整的 Java 程序
- 将 Python 项目结构转换为 Java 项目结构

## How It Works

1. **Input**: Python 文件路径或代码内容
2. **LLM Conversion**: 使用 Codex CLI 将 Python 代码转换为 Java
3. **Compilation**: 自动调用 `javac` 编译生成的 Java 文件
4. **Execution**: 自动运行编译后的 Java 程序
5. **Verification**: 比较输出结果，确保正确性

6. **Fix**: 如果编译或运行失败，使用 LLM 修复问题

7. **Output**: 生成的 Java 文件路径

## Usage Examples

**Example 1: Convert simple Python script**
```
User: "Convert hello.py to Java"
Assistant: 读取 hello.py, converts it using LLM, and outputs HelloWorld.java
```

**Example 2: Convert complex Python code**
```
User: "Convert my_script.py to Java, make sure it handles edge cases like type hints and async functions"
Assistant: Reads the Python file, uses LLM to convert, compiles, runs, and user
 verifying the output matches
```

## Resources

### scripts/convert.py
主脚本执行完整的转换流程:
- 读取 Python 源文件
- 使用 LLM 转换代码
- 编译 Java 文件
- 运行 Java 程序
- 比较输出

处理常见问题:
- 缺少 codex CLI
- JDK 未安装
