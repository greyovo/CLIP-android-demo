# CLIP-lite-android-demo

A demo for running quantized CLIP model on Android.

## Usage

Run this [jupyter notebook](https://colab.research.google.com/drive/1bW1aMg0er1T4aOcU5pCNYVgmVzBJ4-x4#scrollTo=hPscj2wlZlHb) to get the quantized models:

-  `clip-image-encoder-quant-int8`
- `clip-text-encoder-quant-int8`

Place them into `app\src\main\assets`.

Then build and run in your IDE.



> Just for testing code, the structure is very casual. Good luck :)



## Performance

Accuracy compared to original CLIP ViT-B/32 model:

| CIFAR100  | int8  | Original (fp32) | Loss   |
| --------- | ----- | --------------- | ------ |
| 2000 pics | 0.825 | 0.871           | -0.046 |
| 5000 pics | 0.830 | 0.940           | -0.11  |

Encoding 500 pics costs:

> Device: Xiaomi 12S @ Snapdragon 8+ Gen 1

| Resolution | Disk size | Model | Time |
| ---------- | --------- | ----- | ---- |
| 400px      | 21KB      | fp32  | ~54s |
| 400px      | 21KB      | int8  | ~20s |
| 1000px     | 779KB     | fp32  | ~62s |
| 1000px     | 779KB     | int8  | ~27s |
| 4096px     | 1.7MB     | int8  | ~60s |
| 4096px     | 4MB       | int8  | ~87s |

**Note:**

- The encode time for each image is around 40~60ms
- For images with larger disk size (rather than resolution), it takes more time to read the image. I have tried ''down-sample'' the large image instead of reading the whole file.



## Credit

- [openai/CLIP](https://github.com/openai/CLIP)
- [ONNX Runtime](https://onnxruntime.ai/)
- [mazzzystar/Queryable](https://github.com/mazzzystar/Queryable)

