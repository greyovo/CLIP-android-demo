# CLIP-lite-android-demo

A demo for running quantized CLIP model (ViT-B/32) on Android.

## Usage

Run this [jupyter notebook](https://colab.research.google.com/drive/1bW1aMg0er1T4aOcU5pCNYVgmVzBJ4-x4#scrollTo=hPscj2wlZlHb) to get the quantized models:

-  `clip-image-encoder-quant-int8`
- `clip-text-encoder-quant-int8`

Place them into `app\src\main\assets`.

Then build and run in your IDE.



> Note: Do NOT use `PyTorch > 1.13` or it will failed when converting to ONNX format.
>
> This project is just for testing, so forgive my casual code. Good luck :)



## Performance

### Model Size

- Original (Float 32)
  - ImageEncoder: 335 MB
  - TextEncoder: 242 MB
- Quantized (Int8)
  - ImageEncoder: 91.2 MB
  - TextEncoder: 61.3 MB



### Loss

Accuracy compared to original CLIP ViT-B/32 model:

| CIFAR100  | int8  | Original (fp32) | Loss   |
| --------- | ----- | --------------- | ------ |
| 2000 pics | 0.825 | 0.871           | -0.046 |
| 5000 pics | 0.830 | 0.940           | -0.11  |

### Speed

Encode 500 pics in single thread:

> Device: Xiaomi 12S @ Snapdragon 8+ Gen 1

| Resolution | On-disk Size | Model | Time |
| ---------- | ------------ | ----- | ---- |
| 400px      | 21KB         | fp32  | ~54s |
| 400px      | 21KB         | int8  | ~20s |
| 1000px     | 779KB        | fp32  | ~62s |
| 1000px     | 779KB        | int8  | ~27s |
| 4096px     | 1.7MB        | int8  | ~60s |
| 4096px     | 4MB          | int8  | ~87s |

**Note:**

- The encode time for each image is 35~45ms
- For images with larger on-disk size, it takes more time to read the image. I have tried ''down-sample'' the large image instead of reading the whole file.



## Acknowledgement

- [openai/CLIP](https://github.com/openai/CLIP)
- [ONNX Runtime](https://onnxruntime.ai/)
- [mazzzystar/Queryable](https://github.com/mazzzystar/Queryable)

