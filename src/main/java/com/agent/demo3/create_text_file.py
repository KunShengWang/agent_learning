def build_tools() -> list[dict[str, Any]]:
    """
    定义可供模型调用的工具。
    演示一个更接近真实工作的工具：创建文件。
    """
    return [
        {
            "type": "function",
            "function": {
                "name": "create_text_file",
                "description": (
                    "Create a text-based file under demo3/generated_files. "
                    "Use this when the user asks to create a note, markdown file, "
                    "JSON file, Python file, config file, or any other text file."
                ),
                "parameters": {
                    "type": "object",
                    "properties": {
                        "relative_path": {
                            "type": "string",
                            "description": (
                                "Relative file path under demo3/generated_files, "
                                "for example notes/todo.md or scripts/hello.py."
                            ),
                        },
                        "content": {
                            "type": "string",
                            "description": (
                                "The complete file content to write. "
                                "Keep it complete and valid, but concise unless the user asks for a long document."
                            ),
                        },
                        "overwrite": {
                            "type": "boolean",
                            "description": (
                                "Whether to overwrite the file if it already exists."
                            ),
                        },
                    },
                    "required": ["relative_path", "content", "overwrite"],
                },
            },
        }
    ]