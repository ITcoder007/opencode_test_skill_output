---
name: python-to-java
description: 将 Python 代码转换为等价的 Java 代码，确保编译通过并输出一致。当用户提到"python 转 java"、"convert python to java"、"用 Java 重写这个 python"、"把 .py 改成 .java"等意图时触发。也适用于用户上传 .py 文件并要求生成 Java 版本的场景。不适用于 Python 转其他语言（如 Kotlin、C++、Go）。
---

# Python To Java

将 Python 代码转换为等价的 Java 代码，编译验证通过，输出结果一致。

## 工作流程

1. **读取** Python 源文件内容
2. **转换** 直接将 Python 代码改写为 Java（你自己完成，不依赖外部工具）
3. **编译** 用 `javac` 编译生成的 .java 文件
4. **运行** 用 `java` 执行，捕获 stdout
5. **比较** 与 Python 原始输出做 trim 后精确匹配
6. **修复** 若编译或运行失败，根据错误信息修改 Java 代码，最多重试 3 次

## 转换规则

命名：snake_case → camelCase，文件名 → PascalCase 类名（如 `my_script.py` → `MyScript.java`）

常见映射：
- `list` → `ArrayList<>`, `dict` → `HashMap<>`, `set` → `HashSet<>`, `tuple` → `List.of()`
- `print()` → `System.out.println()`
- `for x in list` → enhanced for loop
- `list comprehension` → Stream API 或普通循环（优先可读性）
- `with open()` → try-with-resources
- `try/except` → try/catch（映射到对应的 Java 异常类型）
- `def` → method, `class` → class, 类型推断 → 显式声明类型
- `if __name__ == "__main__"` → `public static void main(String[] args)`

默认 Java 版本：17。生成的代码应包含完整的 import 语句和 main 方法入口。

## 验证步骤（必须执行）

```bash
# 1. 先运行 Python 获取基准输出
python3 source.py > /tmp/python_output.txt 2>&1

# 2. 编译 Java
javac MyScript.java

# 3. 运行 Java 获取输出
java MyScript > /tmp/java_output.txt 2>&1

# 4. 比较
diff /tmp/python_output.txt /tmp/java_output.txt
```

如果 diff 有差异或编译失败，阅读错误信息，修改 Java 代码，重新执行上述步骤。最多 3 轮。

## 输出

将最终通过验证的 .java 文件保存到 `/mnt/user-data/outputs/`。如果 3 轮后仍未通过，告知用户剩余问题并提供当前最佳版本。
