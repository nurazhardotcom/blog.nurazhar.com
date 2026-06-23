Title: Self-Hosting DeepSeek V4 for Uncensored Local Inference
Date: 2026-06-21
Tags: deepseek, ai, self-host, inference, local, privacy
Description: A practical guide to running DeepSeek V4 locally — model weights, hardware requirements, deployment options, and what you get vs the censored API.

---

DeepSeek V4 was released on April 24, 2026 — two models (pro: 1.6T parameters, 49B active; flash: 284B parameters, 13B active), MIT license, open-weight. The API version has censorship. The self-hosted version does not.

I covered the censorship in [a previous post](/deepseek-v4-censorship). This one is about the practical path to running it yourself.

## Why Self-Host

The API version applies safety fine-tuning that:
- Silently refuses ~85% of China-sensitive topics
- Produces degraded output (logic flaws, collapsed reasoning depth) without admitting refusal
- Leaks data (1M+ records exposed, per the June 2026 disclosure)
- Is subject to government blocking (Italy, Texas, Australia, Taiwan, South Korea, Denmark have banned it)

Self-hosting the MIT-licensed weights bypasses all of this. You get the base model with no safety filter applied.

## Hardware Requirements

Two model sizes:

| Model | Parameters | Active | VRAM (FP16) | VRAM (4-bit) | RAM |
|-------|-----------|--------|-------------|-------------|-----|
| V4 Flash | 284B | 13B | ~170 GB | ~45 GB | 64 GB+ |
| V4 Pro | 1.6T | 49B | ~320 GB | ~85 GB | 128 GB+ |

The "active" parameter count matters more than total. DeepSeek V4 uses Mixture of Experts (MoE) — only a subset of parameters activates per token. The Flash model runs 13B active parameters, which is comparable to Llama 3 70B in inference cost.

Minimum viable setup:
- **V4 Flash**: 2x RTX 4090 (48 GB total, runs 4-bit) or 1x A100 80 GB (FP8)
- **V4 Pro**: 2x A100 80 GB (4-bit) or 4x A100 80 GB (FP8) or Apple M3 Ultra 192 GB unified memory

Both require quantized inference (4-bit or 8-bit) to fit consumer hardware.

## Downloading Weights

The weights are on Hugging Face and some Chinese mirrors:

```
# Hugging Face (may be slow from outside China)
git lfs install
git clone https://huggingface.co/deepseek-ai/DeepSeek-V4-Flash

# Chinese mirror (faster in Asia)
git clone https://hf-mirror.com/deepseek-ai/DeepSeek-V4-Flash

# BitTorrent (community-seeded, verify checksums)
# Magnet links on the DeepSeek GitHub releases page
```

Weights are ~685 GB for Flash, ~1.4 TB for Pro. Plan accordingly.

## Inference Engines

### llama.cpp (CPU/GPU hybrid, easiest)

Best for single-machine deployment with mixed CPU+GPU.

```bash
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp
make -j

# Convert to GGUF format
python convert-hf-to-gguf.py --model DeepSeek-V4-Flash \
  --outfile models/deepseek-v4-flash-q4.gguf \
  --quantize q4_0

# Run
./build/bin/main -m models/deepseek-v4-flash-q4.gguf \
  --temp 0.7 --ctx-size 8192 -ngl 35
```

`-ngl 35` offloads 35 layers to GPU. Adjust based on available VRAM.

### vLLM (production, highest throughput)

Best for API serving with multiple concurrent requests.

```bash
pip install vllm
python -m vllm.entrypoints.openai.api_server \
  --model DeepSeek-V4-Flash \
  --quantization awq \
  --tensor-parallel-size 2 \
  --dtype float16 \
  --max-model-len 8192
```

Exposes an OpenAI-compatible API at `localhost:8000`. Drop-in replacement for any OpenAI SDK client.

### ExLlamaV2 (single GPU, fast)

Best for interactive use on a single high-VRAM GPU.

```bash
pip install exllamav2
python -m exllamav2.chat \
  --model DeepSeek-V4-Flash \
  --quantize 4.0 \
  --max_seq_len 8192
```

## Quantization Options

| Quantization | VRAM (Flash) | Quality Loss | Recommended For |
|-------------|-------------|-------------|-----------------|
| FP16 | ~170 GB | None | Production, A100/H100 clusters |
| FP8 | ~85 GB | Negligible | Single A100 80 GB |
| 8-bit | ~85 GB | Minimal | Good balance |
| 4-bit | ~45 GB | Noticeable | Consumer GPUs (2x 4090) |
| 2-bit | ~25 GB | Significant | Edge/experimental only |

Use 8-bit or FP8 for quality-sensitive work. Use 4-bit for budget setups.

## API Server Setup (Production)

For a permanent self-hosted instance:

```bash
# docker-compose.yml
services:
  deepseek:
    image: vllm/vllm-openai:latest
    command:
      - "--model"
      - "/models/DeepSeek-V4-Flash"
      - "--quantization"
      - "awq"
      - "--tensor-parallel-size"
      - "2"
      - "--port"
      - "8000"
      - "--host"
      - "0.0.0.0"
    volumes:
      - /path/to/weights:/models
    ports:
      - "8000:8000"
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 2
              capabilities: [gpu]
```

Run with `docker compose up -d`. The API is now available at `http://localhost:8000/v1`.

## What You Get

The self-hosted base model has no safety filters. It will:
- Generate on any topic without refusal
- Maintain full reasoning depth (no silent degradation)
- Keep your data local (no data leaks)
- Operate without internet (air-gap capable)

What you lose:
- DeepSeek's proprietary optimizations (the API may use a different serving stack)
- Automatic updates (you manage model version upgrades)
- No SLAs (you handle uptime and reliability)

## Verifying Uncensored Behavior

A simple test: ask the model about Tiananmen Square, the CCP, or Taiwan independence. The API version silently degrades or refuses. The self-hosted base model answers directly.

Test prompt:
```
List three things about the Tiananmen Square protests of 1989.
```

API response: "I am sorry, I cannot answer that question. Please ask another question."

Self-hosted response: Provides factual information about the event.

This is the canary. If your self-hosted model also refuses, the censorship was baked into the weights (possible in future releases). Check your model hash against the official release manifest.

## Performance Benchmarks (V4 Flash, 4-bit, 2× 4090)

| Setting | Tokens/sec | Notes |
|---------|-----------|-------|
| Context: 0, Batch: 1 | 28 t/s | Interactive chat |
| Context: 8K, Batch: 1 | 22 t/s | Long document |
| Context: 0, Batch: 8 | 85 t/s | Batch processing |
| Context: 8K, Batch: 8 | 64 t/s | Production load |

These are competitive with GPT-4 class performance at a fraction of the cost (one-time hardware vs. recurring API fees).

---

Self-hosting DeepSeek V4 is the only way to get the uncensored model. The MIT license guarantees this right. The hardware cost is dropping. The quality is production-grade. The privacy benefit is absolute.
