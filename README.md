# base-imageloader
Android本地、网络图片加载库。

下载直接将base-imageloader以module方式引入。

##使用

任何需要的地方调用以下代码

```java
 ImageLoader.with(context).load(url, imageview);
```

##相关情况

* 支持LIFO，FIFO 加载图片策略
* 支持内存缓存、硬盘缓存
* 支持网络图片加载
* 支持本地图片加载
* 支持file、drawable、assets、content等Schema图片的加载

##TODO

* 完善细节
* 添加本地demo


