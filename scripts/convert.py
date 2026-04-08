#!/usr/bin/env python3
"""Convert Python code to Java using LLM and ensure compilation."""

import subprocess
import sys
import tempfile
from pathlib import Path


def read_python_file(python_file: str) -> str:
    """Read Python source code from file."""
    path = Path(python_file)
    if not path.exists():
        raise FileNotFoundError(f"Python file not found: {python_file}")
    return path.read_text()


def convert_with_llm(python_code: str, output_file: Path) -> str:
    """Use LLM to convert Python to Java."""
    prompt = f"""Convert the following Python code to equivalent Java code.

Requirements:
1. The Java code must compile successfully with javac
2. The Java code must produce the same output as the Python code
3. Use appropriate Java idioms and conventions
4. Include necessary imports
5. Create a class named based on the output file: {output_file.stem}

Python code:
```python
{python_code}
```

Output only the Java code, no explanations."""

    result = subprocess.run(
        ["codex", "exec", "--sandbox", "read-only", prompt],
        capture_output=True,
        text=True,
        cwd=output_file.parent
    )
    
    if result.returncode != 0:
        raise RuntimeError(f"Codex conversion failed: {result.stderr}")
    
    java_code = result.stdout.strip()
    
    if not java_code:
        raise RuntimeError("LLM returned empty Java code")
    
    output_file.write_text(java_code)
    return java_code


def compile_java(java_file: Path) -> bool:
    """Compile Java file and return success status."""
    result = subprocess.run(
        ["javac", str(java_file)],
        capture_output=True,
        text=True,
        cwd=java_file.parent
    )
    
    if result.returncode != 0:
        print(f"Compilation failed:\n{result.stderr}", file=sys.stderr)
        return False
    
    return True


def run_java(java_file: Path) -> tuple[bool, str]:
    """Run compiled Java program and return (success, output)."""
    class_name = java_file.stem
    result = subprocess.run(
        ["java", class_name],
        capture_output=True,
        text=True,
        cwd=java_file.parent
    )
    
    return result.returncode == 0, result.stdout


def main():
    if len(sys.argv) < 2:
        print("Usage: convert.py <python_file> [output_dir]", file=sys.stderr)
        sys.exit(1)
    
    python_file = sys.argv[1]
    output_dir = Path(sys.argv[2]) if len(sys.argv) > 2 else Path.cwd()
    
    output_dir.mkdir(parents=True, exist_ok=True)
    
    python_code = read_python_file(python_file)
    print(f"Read Python code from {python_file}")
    
    java_file = output_dir / "Converted.java"
    
    max_attempts = 3
    for attempt in range(1, max_attempts + 1):
        print(f"\nAttempt {attempt}/{max_attempts}")
        
        java_code = convert_with_llm(python_code, java_file)
        print(f"Generated Java code in {java_file}")
        
        if compile_java(java_file):
            print("✓ Compilation successful")
            
            success, output = run_java(java_file)
            if success:
                print("✓ Execution successful")
                print(f"\nOutput:\n{output}")
                print(f"\n✅ Conversion complete! Java file: {java_file}")
                return
            else:
                print(f"✗ Execution failed, retrying...")
        else:
            print(f"✗ Compilation failed, retrying...")
    
    print(f"\n❌ Failed after {max_attempts} attempts", file=sys.stderr)
    sys.exit(1)


if __name__ == "__main__":
    main()
