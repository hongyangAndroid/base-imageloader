# base-imageloader
Android本地、网络图片加载库。

下载直接将base-imageloader以module方式引入。

##使用

任何需要的地方调用以下代码

```java
 ImageLoader.with(context).load(url, imageview);
```

##相关情况

* 支持LIFO
* 支持硬盘缓存
* 硬盘缓存使用[base-diskcache](https://github.com/hongyangAndroid/base-diskcache)
* http请求模块提取的volley的http模块

##TODO

* 完善细节
* 添加本地demo


