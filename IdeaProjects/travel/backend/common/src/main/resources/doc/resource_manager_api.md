# 智旅资源管理器API文档

## 1. 概述

本文档描述了智旅系统中资源管理器的API接口，包括文件上传、下载、管理、标签、评论等功能。

## 2. 基础信息

- **API前缀**: `/api/resources`
- **请求方法**: GET, POST, PUT, DELETE
- **响应格式**: JSON
- **状态码**: 200(成功), 404(未找到), 500(服务器错误)

## 3. API接口列表

### 3.1 文件上传

#### 3.1.1 单个文件上传
- **URL**: `/api/resources/upload`
- **方法**: POST
- **参数**:
  - `file`: 文件(必填)
  - `userId`: 上传用户ID(必填)
  - `description`: 文件描述(可选)
- **响应**:
  ```json
  {
    "success": true,
    "fileId": 1,
    "fileName": "example.jpg",
    "filePath": "/path/to/file",
    "fileSize": 1024000
  }
  ```

#### 3.1.2 批量文件上传
- **URL**: `/api/resources/batch-upload`
- **方法**: POST
- **参数**:
  - `files`: 文件列表(必填)
  - `userId`: 上传用户ID(必填)
  - `description`: 文件描述(可选)
- **响应**:
  ```json
  [
    {
      "success": true,
      "fileId": 1,
      "fileName": "example1.jpg",
      "filePath": "/path/to/file1",
      "fileSize": 1024000
    },
    {
      "success": true,
      "fileId": 2,
      "fileName": "example2.jpg",
      "filePath": "/path/to/file2",
      "fileSize": 2048000
    }
  ]
  ```

### 3.2 文件获取

#### 3.2.1 获取所有文件
- **URL**: `/api/resources`
- **方法**: GET
- **响应**:
  ```json
  [
    {
      "id": 1,
      "fileName": "example.jpg",
      "filePath": "/path/to/file",
      "fileSize": 1024000,
      "fileType": "image/jpeg",
      "uploadTime": "2024-01-01T12:00:00",
      "uploadUserId": 1,
      "description": "示例文件",
      "status": 1,
      "routeId": 1,
      "tags": "风景,旅游",
      "previewUrl": "https://example.com/preview.jpg",
      "downloadCount": 10,
      "commentCount": 2,
      "rating": 4.5,
      "lastAccessTime": "2024-01-02T12:00:00"
    }
  ]
  ```

#### 3.2.2 获取单个文件
- **URL**: `/api/resources/{id}`
- **方法**: GET
- **响应**:
  ```json
  {
    "id": 1,
    "fileName": "example.jpg",
    "filePath": "/path/to/file",
    "fileSize": 1024000,
    "fileType": "image/jpeg",
    "uploadTime": "2024-01-01T12:00:00",
    "uploadUserId": 1,
    "description": "示例文件",
    "status": 1,
    "routeId": 1,
    "tags": "风景,旅游",
    "previewUrl": "https://example.com/preview.jpg",
    "downloadCount": 10,
    "commentCount": 2,
    "rating": 4.5,
    "lastAccessTime": "2024-01-02T12:00:00"
  }
  ```

#### 3.2.3 获取路线相关文件
- **URL**: `/api/resources/route/{routeId}`
- **方法**: GET
- **响应**: 文件列表

#### 3.2.4 获取用户上传文件
- **URL**: `/api/resources/user/{userId}`
- **方法**: GET
- **响应**: 文件列表

#### 3.2.5 获取热门文件
- **URL**: `/api/resources/hot?limit=10`
- **方法**: GET
- **参数**:
  - `limit`: 限制数量(可选，默认10)
- **响应**: 文件列表

#### 3.2.6 获取最新文件
- **URL**: `/api/resources/new?limit=10`
- **方法**: GET
- **参数**:
  - `limit`: 限制数量(可选，默认10)
- **响应**: 文件列表

### 3.3 文件下载

- **URL**: `/api/resources/download/{id}`
- **方法**: GET
- **响应**: 文件流

### 3.4 文件删除

#### 3.4.1 单个文件删除
- **URL**: `/api/resources/{id}`
- **方法**: DELETE
- **响应**:
  ```json
  {
    "success": true,
    "msg": "文件删除成功"
  }
  ```

#### 3.4.2 批量文件删除
- **URL**: `/api/resources/batch-delete`
- **方法**: DELETE
- **参数**:
  - `ids`: 文件ID列表(必填)
- **响应**:
  ```json
  {
    "success": true,
    "msg": "批量删除成功"
  }
  ```

### 3.5 文件关联

#### 3.5.1 关联路线
- **URL**: `/api/resources/{id}/associate-route`
- **方法**: POST
- **参数**:
  - `routeId`: 路线ID(必填)
- **响应**:
  ```json
  {
    "success": true,
    "msg": "关联路线成功"
  }
  ```

#### 3.5.2 解除路线关联
- **URL**: `/api/resources/{id}/dissociate-route`
- **方法**: POST
- **响应**:
  ```json
  {
    "success": true,
    "msg": "解除路线关联成功"
  }
  ```

### 3.6 标签管理

#### 3.6.1 获取文件标签
- **URL**: `/api/resources/{id}/tags`
- **方法**: GET
- **响应**:
  ```json
  [
    {
      "tagName": "风景",
      "tagType": "category",
      "usageCount": 10
    }
  ]
  ```

#### 3.6.2 添加标签
- **URL**: `/api/resources/{id}/tags`
- **方法**: POST
- **参数**:
  - `tagName`: 标签名称(必填)
  - `userId`: 用户ID(必填)
- **响应**:
  ```json
  {
    "success": true,
    "msg": "添加标签成功"
  }
  ```

#### 3.6.3 移除标签
- **URL**: `/api/resources/{id}/tags/{tagName}`
- **方法**: DELETE
- **响应**:
  ```json
  {
    "success": true,
    "msg": "移除标签成功"
  }
  ```

### 3.7 评论管理

#### 3.7.1 获取文件评论
- **URL**: `/api/resources/{id}/comments`
- **方法**: GET
- **响应**:
  ```json
  [
    {
      "id": 1,
      "content": "很好的文件",
      "userName": "张三",
      "rating": 5,
      "likes": 2,
      "createTime": "2024-01-01T12:00:00"
    }
  ]
  ```

#### 3.7.2 添加评论
- **URL**: `/api/resources/{id}/comments`
- **方法**: POST
- **参数**:
  - `userId`: 用户ID(必填)
  - `userName`: 用户名(必填)
  - `content`: 评论内容(必填)
  - `rating`: 评分(可选，1-5)
  - `parentId`: 父评论ID(可选，用于回复)
- **响应**:
  ```json
  {
    "success": true,
    "msg": "添加评论成功"
  }
  ```

#### 3.7.3 点赞评论
- **URL**: `/api/resources/comments/{commentId}/like`
- **方法**: POST
- **响应**:
  ```json
  {
    "success": true,
    "msg": "点赞成功"
  }
  ```

### 3.8 文件统计

#### 3.8.1 获取文件评分
- **URL**: `/api/resources/{id}/rating`
- **方法**: GET
- **响应**:
  ```json
  {
    "fileId": 1,
    "averageRating": 4.5
  }
  ```

#### 3.8.2 获取文件统计信息
- **URL**: `/api/resources/{id}/statistics`
- **方法**: GET
- **响应**:
  ```json
  {
    "fileId": 1,
    "downloadCount": 10,
    "commentCount": 2,
    "rating": 4.5,
    "lastAccessTime": "2024-01-02T12:00:00"
  }
  ```

### 3.9 文件元数据更新

- **URL**: `/api/resources/{id}/metadata`
- **方法**: PUT
- **参数**:
  - `fileName`: 文件名(可选)
  - `description`: 文件描述(可选)
  - `tags`: 标签(可选)
- **响应**:
  ```json
  {
    "success": true,
    "msg": "更新元数据成功"
  }
  ```

### 3.10 文件搜索

#### 3.10.1 按文件名搜索
- **URL**: `/api/resources/search?fileName=example`
- **方法**: GET
- **参数**:
  - `fileName`: 文件名关键词(必填)
- **响应**: 文件列表

#### 3.10.2 多条件搜索
- **URL**: `/api/resources/search/multiple`
- **方法**: GET
- **参数**:
  - `fileName`: 文件名关键词(可选)
  - `fileType`: 文件类型(可选)
  - `tags`: 标签(可选)
  - `userId`: 用户ID(可选)
  - `routeId`: 路线ID(可选)
- **响应**: 文件列表

## 4. 错误处理

- **404 Not Found**: 文件不存在
- **500 Internal Server Error**: 服务器内部错误
- **400 Bad Request**: 请求参数错误

## 5. 使用示例

### 5.1 上传文件示例

```bash
curl -X POST "http://localhost:8080/api/resources/upload" \
  -F "file=@example.jpg" \
  -F "userId=1" \
  -F "description=示例图片"
```

### 5.2 批量上传文件示例

```bash
curl -X POST "http://localhost:8080/api/resources/batch-upload" \
  -F "files=@example1.jpg" \
  -F "files=@example2.jpg" \
  -F "userId=1" \
  -F "description=批量上传示例"
```

### 5.3 关联路线示例

```bash
curl -X POST "http://localhost:8080/api/resources/1/associate-route" \
  -d "routeId=1"
```

### 5.4 添加标签示例

```bash
curl -X POST "http://localhost:8080/api/resources/1/tags" \
  -d "tagName=风景" \
  -d "userId=1"
```

### 5.5 添加评论示例

```bash
curl -X POST "http://localhost:8080/api/resources/1/comments" \
  -d "userId=1" \
  -d "userName=张三" \
  -d "content=很好的文件" \
  -d "rating=5"
```

## 6. 注意事项

1. 文件上传大小限制：默认100MB
2. 支持的文件类型：图片、文档、视频等
3. 标签长度限制：每个标签不超过20个字符
4. 评论内容限制：最多500个字符
5. 评分范围：1-5分

## 7. 版本历史

- **v1.0**: 初始版本
- **v1.1**: 增加批量操作功能
- **v1.2**: 增加标签和评论功能
- **v1.3**: 增加路线关联功能
