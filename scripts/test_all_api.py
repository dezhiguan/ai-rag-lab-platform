from dotenv import load_dotenv  # 读取 .env 文件
import os
import dashscope
from dashscope import TextEmbedding
from openai import OpenAI
from http import HTTPStatus

# 加载 .env 文件（自动读取你写的密钥）
load_dotenv()

print("=" * 50)
print("正在检查环境变量是否加载成功...")
print("=" * 50)

# 查看是否读到密钥（隐藏中间部分，安全）
qwen_key = os.getenv("DASHSCOPE_API_KEY", "未找到")
deep_key = os.getenv("DEEPSEEK_API_KEY", "未找到")

print(f"✅ DASHSCOPE_API_KEY 读取状态: {'成功' if qwen_key != '未找到' else '失败'}")
print(f"✅ DEEPSEEK_API_KEY 读取状态: {'成功' if deep_key != '未找到' else '失败'}")
print()

# ----------------------
# 测试 1：Qwen Embedding
# ----------------------
print("=" * 50)
print("正在测试 Qwen Embedding...")
print("=" * 50)
try:
    dashscope.api_key = qwen_key
    resp = TextEmbedding.call(
        model="text-embedding-v2",
        input="测试连接"
    )
    if resp.status_code == HTTPStatus.OK:
        print("✅ Qwen Embedding 调用成功！")
    else:
        print(f"❌ Qwen 失败：{resp.code} | {resp.message}")
except Exception as e:
    print(f"❌ Qwen 错误：{str(e)}")

print()

# ----------------------
# 测试 2：DeepSeek Chat
# ----------------------
print("=" * 50)
print("正在测试 DeepSeek Chat...")
print("=" * 50)
try:
    client = OpenAI(
        api_key=deep_key,
        base_url="https://api.deepseek.com",
    )
    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=[{"role": "user", "content": "你好"}]
    )
    print("✅ DeepSeek 调用成功！")
    print("回复：", response.choices[0].message.content)
except Exception as e:
    print(f"❌ DeepSeek 错误：{str(e)}")

print("\n🎉 全部测试完成！")
