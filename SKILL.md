---
name: python-to-java
description: 将 Python 代码转换为等价的 Java 代码，确保编译通过并输出一致。当用户提到"python 转 java"、"convert python to java"、"用 Java 重写这个 python"、"把 .py 改成 .java"、"python to java"等意图时触发。也适用于用户上传 .py 文件并要求生成 Java 版本、粘贴 Python 代码片段要求转 Java、或对比 Python 与 Java 实现差异的场景。不适用于 Python 转其他语言（如 Kotlin、C++、Go）。
---

# Python To Java

将 Python 代码转换为等价的 Java 代码，编译验证通过，输出结果一致。

## 前置检查（首次执行时）

在开始转换之前，先确认 Java 环境可用：

```bash
javac -version && java -version
```

如果不可用，告知用户需要安装 JDK 17+，并提供安装建议（如 `brew install openjdk@17`、`sdkman`、或 `apt install openjdk-17-jdk`）。确认环境就绪后再继续。

## 工作流程

1. **读取** Python 源文件内容
2. **转换** 直接将 Python 代码改写为 Java（你自己完成，不依赖外部工具）
3. **编译** 用 `javac -encoding UTF-8` 编译生成的 .java 文件
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

将中间产物放在当前工作目录，文件名加 `_output` 后缀避免冲突：

```bash
# 假设源文件为 <source>.py，生成的 Java 文件为 <ClassName>.java
# 请用实际文件名替换下面的占位符

# 1. 先运行 Python 获取基准输出
python3 <source>.py > py_output.txt 2>&1

# 2. 编译 Java（-encoding UTF-8 防止中文注释导致编译失败）
javac -encoding UTF-8 <ClassName>.java

# 3. 运行 Java 获取输出
java <ClassName> > java_output.txt 2>&1

# 4. 比较（忽略尾部空白）
diff <(cat py_output.txt | sed 's/[[:space:]]*$//') \
     <(cat java_output.txt | sed 's/[[:space:]]*$//')
```

如果 diff 有差异或编译失败，阅读错误信息，修改 Java 代码，重新执行上述步骤。最多 3 轮。

验证完成后清理中间文件：`rm -f py_output.txt java_output.txt`

## 边界情况处理

- **Python 脚本含 `input()` 交互输入**：在运行时通过 `echo "模拟输入" | python3 <source>.py` 提供输入，Java 侧也用相同方式。如果输入场景复杂，告知用户手动验证。
- **Python 脚本无 stdout 输出**：跳过 diff 比较，仅验证 Java 编译通过且运行无异常。
- **Python 使用了第三方库（如 numpy、pandas）**：告知用户该部分无法自动转换，标注 `// TODO: 需要手动实现或引入对应 Java 库` 并给出建议（如 Apache Commons Math）。
- **Python 脚本含文件读写**：确保 Java 版本使用相同的相对路径，验证时注意工作目录一致。

## 输出

将最终通过验证的 `.java` 文件保存到**当前工作目录**（即用户执行命令的目录）。

如果 3 轮后仍未通过，告知用户剩余问题并提供当前最佳版本。
