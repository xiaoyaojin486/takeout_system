# 12 · file — 文件上传

> 阅读级别：**略读**（约 700 字）。全项目最小的模块，只有一个上传接口。核心是**安全校验**和**可扩展的存储抽象**。

---

## 1. 是什么

`file` 模块提供图片上传能力，目前只支持**本地磁盘存储**。商家上传店铺 Logo、菜品图片，管理员上传审核材料，都走这个接口。

存储路径：`{uploadPath}/yyyy/MM/dd/{uuid}.{ext}`

## 2. 在全局的位置

```
file（独立模块，被 merchant / dish 等通过返回 URL 间接使用）
```

- **不依赖任何业务模块**
- **不被任何业务模块直接依赖**：各模块只是把上传拿到的 URL 存进自己的表

## 3. 精读 2 个文件

| 文件 | 作用 |
|------|------|
| [LocalFileStorageServiceImpl.java](file:///c:/Users/huhaohua/Desktop/myproject/外卖系统/backend/src/main/java/com/takeout/file/service/impl/LocalFileStorageServiceImpl.java) | 本地磁盘存储实现，含安全校验 |
| [FileStorageService.java](file:///c:/Users/huhaohua/Desktop/myproject/外卖系统/backend/src/main/java/com/takeout/file/service/FileStorageService.java) | 存储抽象接口，二期换 OSS 只需新增实现类 |

## 4. 方法一览

| 方法 | 说明 |
|------|------|
| `upload(file)` | 保存上传文件，返回可访问 URL |

## 5. 关键代码拆解

### 5.1 三道安全校验

```java
if (file == null || file.isEmpty()) throw new BusinessException(FILE_EMPTY);
if (file.getSize() > MAX_SIZE) throw new BusinessException(FILE_TOO_LARGE);  // 5MB
if (!ALLOWED_EXTENSIONS.contains(extension)) throw new BusinessException(FILE_TYPE_ERROR);
```

| 校验 | 值 | 目的 |
|------|-----|------|
| 非空 | — | 防止空上传 |
| 大小 | ≤ 5MB | 防止超大文件打满磁盘 |
| 类型 | jpg/jpeg/png/gif/webp | 限定图片，防止上传可执行脚本 |

**只校验扩展名**（不是 MIME type），因为 MIME type 可以伪造。扩展名用 `extractExtension()` 统一转小写，`.JPG` 也能放行。

### 5.2 UUID 重命名 + 日期分片

```java
String dateDir = LocalDate.now().format(DATE_DIR_FORMATTER);   // "2026/10/12"
String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
Path targetDir = Paths.get(fileProperties.getUploadPath(), dateDir);
Path targetFile = targetDir.resolve(fileName);
```

- **UUID 重命名**：防止同名文件互相覆盖，也避免暴露原始文件名
- **按日期分片目录**：避免单目录堆积海量文件（操作系统对单目录文件数有性能影响）

### 5.3 存储抽象：二期换 OSS 零改动

```java
public interface FileStorageService {
    UploadVO upload(MultipartFile file);
}
```

目前只有 `LocalFileStorageServiceImpl` 一个实现。二期要换阿里云 OSS，只需新增 `OssFileStorageServiceImpl implements FileStorageService`，把 `upload()` 里的 `file.transferTo()` 换成 OSS SDK 调用，**调用方（Controller、各业务模块）一行都不用改**。

## 6. 特有坑

### 坑 1：访问前缀靠 Nginx 静态托管

上传返回的 URL 是 `/upload/2026/10/12/xxx.jpg` 这种相对路径。要能在浏览器访问，需要 Nginx 把 `/upload/` 路径代理到本地存储目录：

```nginx
location /upload/ {
    alias /data/takeout/upload/;
}
```

这也是为什么你的部署方案里必须有 Nginx——后端不直接对外提供静态文件服务。

### 坑 2：异常包装不暴露堆栈

```java
} catch (IOException e) {
    log.error("文件上传失败 originalFilename={}", originalFilename, e);
    throw new BusinessException(FILE_UPLOAD_ERROR);
}
```

磁盘满、权限不足等系统级失败，包装成统一的 `FILE_UPLOAD_ERROR`，**不把 Java 堆栈暴露给前端**。

## 7. 自检问题

1. 用户上传一个 `shell.php` 文件，会被哪道校验拦住？
2. 上传成功后返回的 URL 是 `/upload/2026/10/12/abc.jpg`，这个 URL 是怎么被浏览器访问到的？需要什么部署配置？
3. 如果二期要把本地存储换成阿里云 OSS，需要改哪些文件？调用方需要改吗？
