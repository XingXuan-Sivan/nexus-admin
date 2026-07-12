# Nexus Admin


**简介**:Nexus Admin


**HOST**:http://localhost:8081


**联系人**:


**Version**:0.1.0-SNAPSHOT


**接口路径**:/v3/api-docs


[TOC]






# 用户管理


## 获取用户详情


**接口地址**:`/admin/v1/users/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultUser|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||User|User|
|&emsp;&emsp;id||string||
|&emsp;&emsp;username||string||
|&emsp;&emsp;displayName||string||
|&emsp;&emsp;roleIds||array|string|
|&emsp;&emsp;departmentIds||array|string|
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": "",
		"username": "",
		"displayName": "",
		"roleIds": [],
		"departmentIds": [],
		"attributes": {}
	},
	"success": true
}
```


## 更新用户


**接口地址**:`/admin/v1/users/{id}`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "id": "",
  "username": "",
  "displayName": "",
  "roleIds": [],
  "departmentIds": [],
  "attributes": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||
|user|User|body|true|User|User|
|&emsp;&emsp;id|||false|string||
|&emsp;&emsp;username|||false|string||
|&emsp;&emsp;displayName|||false|string||
|&emsp;&emsp;roleIds|||false|array|string|
|&emsp;&emsp;departmentIds|||false|array|string|
|&emsp;&emsp;attributes|||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultUser|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||User|User|
|&emsp;&emsp;id||string||
|&emsp;&emsp;username||string||
|&emsp;&emsp;displayName||string||
|&emsp;&emsp;roleIds||array|string|
|&emsp;&emsp;departmentIds||array|string|
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": "",
		"username": "",
		"displayName": "",
		"roleIds": [],
		"departmentIds": [],
		"attributes": {}
	},
	"success": true
}
```


## 删除用户


**接口地址**:`/admin/v1/users/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


## 获取用户列表


**接口地址**:`/admin/v1/users`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|arg0||query|false|integer(int32)||
|arg1||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|PageResultUser|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|total||integer(int64)|integer(int64)|
|page||integer(int32)|integer(int32)|
|size||integer(int32)|integer(int32)|
|items||array|User|
|&emsp;&emsp;id||string||
|&emsp;&emsp;username||string||
|&emsp;&emsp;displayName||string||
|&emsp;&emsp;roleIds||array|string|
|&emsp;&emsp;departmentIds||array|string|
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"total": 0,
	"page": 0,
	"size": 0,
	"items": [
		{
			"id": "",
			"username": "",
			"displayName": "",
			"roleIds": [],
			"departmentIds": [],
			"attributes": {}
		}
	],
	"success": true
}
```


## 创建用户


**接口地址**:`/admin/v1/users`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "id": "",
  "username": "",
  "displayName": "",
  "roleIds": [],
  "departmentIds": [],
  "attributes": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|user|User|body|true|User|User|
|&emsp;&emsp;id|||false|string||
|&emsp;&emsp;username|||false|string||
|&emsp;&emsp;displayName|||false|string||
|&emsp;&emsp;roleIds|||false|array|string|
|&emsp;&emsp;departmentIds|||false|array|string|
|&emsp;&emsp;attributes|||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultUser|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||User|User|
|&emsp;&emsp;id||string||
|&emsp;&emsp;username||string||
|&emsp;&emsp;displayName||string||
|&emsp;&emsp;roleIds||array|string|
|&emsp;&emsp;departmentIds||array|string|
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": "",
		"username": "",
		"displayName": "",
		"roleIds": [],
		"departmentIds": [],
		"attributes": {}
	},
	"success": true
}
```


# 角色管理


## 获取角色详情


**接口地址**:`/admin/v1/roles/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultRole|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||Role|Role|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;permissionCodes||array|string|
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": "",
		"name": "",
		"permissionCodes": [],
		"attributes": {}
	},
	"success": true
}
```


## 更新角色


**接口地址**:`/admin/v1/roles/{id}`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "id": "",
  "name": "",
  "permissionCodes": [],
  "attributes": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||
|role|Role|body|true|Role|Role|
|&emsp;&emsp;id|||false|string||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;permissionCodes|||false|array|string|
|&emsp;&emsp;attributes|||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultRole|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||Role|Role|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;permissionCodes||array|string|
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": "",
		"name": "",
		"permissionCodes": [],
		"attributes": {}
	},
	"success": true
}
```


## 删除角色


**接口地址**:`/admin/v1/roles/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


## 获取角色列表


**接口地址**:`/admin/v1/roles`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|arg0||query|false|integer(int32)||
|arg1||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|PageResultRole|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|total||integer(int64)|integer(int64)|
|page||integer(int32)|integer(int32)|
|size||integer(int32)|integer(int32)|
|items||array|Role|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;permissionCodes||array|string|
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"total": 0,
	"page": 0,
	"size": 0,
	"items": [
		{
			"id": "",
			"name": "",
			"permissionCodes": [],
			"attributes": {}
		}
	],
	"success": true
}
```


## 创建角色


**接口地址**:`/admin/v1/roles`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "id": "",
  "name": "",
  "permissionCodes": [],
  "attributes": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|role|Role|body|true|Role|Role|
|&emsp;&emsp;id|||false|string||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;permissionCodes|||false|array|string|
|&emsp;&emsp;attributes|||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultRole|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||Role|Role|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;permissionCodes||array|string|
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": "",
		"name": "",
		"permissionCodes": [],
		"attributes": {}
	},
	"success": true
}
```


# 岗位管理


## 获取岗位详情


**接口地址**:`/admin/v1/positions/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultPosition|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||Position|Position|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;departmentId||string||
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": "",
		"name": "",
		"departmentId": "",
		"attributes": {}
	},
	"success": true
}
```


## 更新岗位


**接口地址**:`/admin/v1/positions/{id}`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "id": "",
  "name": "",
  "departmentId": "",
  "attributes": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||
|position|Position|body|true|Position|Position|
|&emsp;&emsp;id|||false|string||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;departmentId|||false|string||
|&emsp;&emsp;attributes|||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultPosition|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||Position|Position|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;departmentId||string||
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": "",
		"name": "",
		"departmentId": "",
		"attributes": {}
	},
	"success": true
}
```


## 删除岗位


**接口地址**:`/admin/v1/positions/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


## 获取岗位列表


**接口地址**:`/admin/v1/positions`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|arg0||query|false|integer(int32)||
|arg1||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|PageResultPosition|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|total||integer(int64)|integer(int64)|
|page||integer(int32)|integer(int32)|
|size||integer(int32)|integer(int32)|
|items||array|Position|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;departmentId||string||
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"total": 0,
	"page": 0,
	"size": 0,
	"items": [
		{
			"id": "",
			"name": "",
			"departmentId": "",
			"attributes": {}
		}
	],
	"success": true
}
```


## 创建岗位


**接口地址**:`/admin/v1/positions`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "id": "",
  "name": "",
  "departmentId": "",
  "attributes": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|position|Position|body|true|Position|Position|
|&emsp;&emsp;id|||false|string||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;departmentId|||false|string||
|&emsp;&emsp;attributes|||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultPosition|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||Position|Position|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;departmentId||string||
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": "",
		"name": "",
		"departmentId": "",
		"attributes": {}
	},
	"success": true
}
```


# MCP 客户端管理


## 获取 MCP 客户端连接详情


**接口地址**:`/admin/v1/mcp/clients/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultMcpConnectionInfo|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||McpConnectionInfo|McpConnectionInfo|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;url||string||
|&emsp;&emsp;protocol||string||
|&emsp;&emsp;authToken||string||
|&emsp;&emsp;enabled||boolean||
|&emsp;&emsp;status||string||
|&emsp;&emsp;bridgeEnabled||boolean||
|&emsp;&emsp;bridgeMode||string||
|&emsp;&emsp;displayStatus||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": "",
		"name": "",
		"url": "",
		"protocol": "",
		"authToken": "",
		"enabled": true,
		"status": "",
		"bridgeEnabled": true,
		"bridgeMode": "",
		"displayStatus": ""
	},
	"success": true
}
```


## 更新 MCP 客户端连接


**接口地址**:`/admin/v1/mcp/clients/{id}`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "id": "",
  "name": "",
  "url": "",
  "protocol": "",
  "authToken": "",
  "enabled": true,
  "status": "",
  "bridgeEnabled": true,
  "bridgeMode": "",
  "displayStatus": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||
|mcpConnectionInfo|McpConnectionInfo|body|true|McpConnectionInfo|McpConnectionInfo|
|&emsp;&emsp;id|||false|string||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;url|||false|string||
|&emsp;&emsp;protocol|||false|string||
|&emsp;&emsp;authToken|||false|string||
|&emsp;&emsp;enabled|||false|boolean||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;bridgeEnabled|||false|boolean||
|&emsp;&emsp;bridgeMode|||false|string||
|&emsp;&emsp;displayStatus|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultMcpConnectionInfo|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||McpConnectionInfo|McpConnectionInfo|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;url||string||
|&emsp;&emsp;protocol||string||
|&emsp;&emsp;authToken||string||
|&emsp;&emsp;enabled||boolean||
|&emsp;&emsp;status||string||
|&emsp;&emsp;bridgeEnabled||boolean||
|&emsp;&emsp;bridgeMode||string||
|&emsp;&emsp;displayStatus||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": "",
		"name": "",
		"url": "",
		"protocol": "",
		"authToken": "",
		"enabled": true,
		"status": "",
		"bridgeEnabled": true,
		"bridgeMode": "",
		"displayStatus": ""
	},
	"success": true
}
```


## 删除 MCP 客户端连接


**接口地址**:`/admin/v1/mcp/clients/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


## 获取 MCP 客户端连接列表


**接口地址**:`/admin/v1/mcp/clients`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultListMcpConnectionInfo|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|McpConnectionInfo|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;url||string||
|&emsp;&emsp;protocol||string||
|&emsp;&emsp;authToken||string||
|&emsp;&emsp;enabled||boolean||
|&emsp;&emsp;status||string||
|&emsp;&emsp;bridgeEnabled||boolean||
|&emsp;&emsp;bridgeMode||string||
|&emsp;&emsp;displayStatus||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"id": "",
			"name": "",
			"url": "",
			"protocol": "",
			"authToken": "",
			"enabled": true,
			"status": "",
			"bridgeEnabled": true,
			"bridgeMode": "",
			"displayStatus": ""
		}
	],
	"success": true
}
```


## 创建 MCP 客户端连接


**接口地址**:`/admin/v1/mcp/clients`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "id": "",
  "name": "",
  "url": "",
  "protocol": "",
  "authToken": "",
  "enabled": true,
  "status": "",
  "bridgeEnabled": true,
  "bridgeMode": "",
  "displayStatus": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|mcpConnectionInfo|McpConnectionInfo|body|true|McpConnectionInfo|McpConnectionInfo|
|&emsp;&emsp;id|||false|string||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;url|||false|string||
|&emsp;&emsp;protocol|||false|string||
|&emsp;&emsp;authToken|||false|string||
|&emsp;&emsp;enabled|||false|boolean||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;bridgeEnabled|||false|boolean||
|&emsp;&emsp;bridgeMode|||false|string||
|&emsp;&emsp;displayStatus|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultMcpConnectionInfo|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||McpConnectionInfo|McpConnectionInfo|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;url||string||
|&emsp;&emsp;protocol||string||
|&emsp;&emsp;authToken||string||
|&emsp;&emsp;enabled||boolean||
|&emsp;&emsp;status||string||
|&emsp;&emsp;bridgeEnabled||boolean||
|&emsp;&emsp;bridgeMode||string||
|&emsp;&emsp;displayStatus||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": "",
		"name": "",
		"url": "",
		"protocol": "",
		"authToken": "",
		"enabled": true,
		"status": "",
		"bridgeEnabled": true,
		"bridgeMode": "",
		"displayStatus": ""
	},
	"success": true
}
```


## 调用远程工具


**接口地址**:`/admin/v1/mcp/clients/{id}/tools/call`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultMcpToolResult|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||McpToolResult|McpToolResult|
|&emsp;&emsp;success||boolean||
|&emsp;&emsp;content||string||
|&emsp;&emsp;error||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"success": true,
		"content": "",
		"error": ""
	},
	"success": true
}
```


## 测试 MCP 客户端连接


**接口地址**:`/admin/v1/mcp/clients/{id}/test`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "id": "",
  "name": "",
  "url": "",
  "protocol": "",
  "authToken": "",
  "enabled": true,
  "status": "",
  "bridgeEnabled": true,
  "bridgeMode": "",
  "displayStatus": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||
|mcpConnectionInfo|McpConnectionInfo|body|true|McpConnectionInfo|McpConnectionInfo|
|&emsp;&emsp;id|||false|string||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;url|||false|string||
|&emsp;&emsp;protocol|||false|string||
|&emsp;&emsp;authToken|||false|string||
|&emsp;&emsp;enabled|||false|boolean||
|&emsp;&emsp;status|||false|string||
|&emsp;&emsp;bridgeEnabled|||false|boolean||
|&emsp;&emsp;bridgeMode|||false|string||
|&emsp;&emsp;displayStatus|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultBoolean|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||boolean||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": true,
	"success": true
}
```


## 刷新远程工具桥接


**接口地址**:`/admin/v1/mcp/clients/{id}/bridge/refresh`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultInteger|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||integer(int32)|integer(int32)|
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": 0,
	"success": true
}
```


## 获取远程工具列表


**接口地址**:`/admin/v1/mcp/clients/{id}/tools`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultListMcpRemoteTool|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|McpRemoteTool|
|&emsp;&emsp;name||string||
|&emsp;&emsp;description||string||
|&emsp;&emsp;inputSchema||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"name": "",
			"description": "",
			"inputSchema": ""
		}
	],
	"success": true
}
```


## 获取已桥接工具名称列表


**接口地址**:`/admin/v1/mcp/clients/{id}/bridged-tools`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultListString|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [],
	"success": true
}
```


# 日志管理


## 获取日志保留策略


**接口地址**:`/admin/v1/logs/retention`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultLogRetentionConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LogRetentionConfig|LogRetentionConfig|
|&emsp;&emsp;defaultRetention||object||
|&emsp;&emsp;auditRetention||object||
|&emsp;&emsp;errorRetention||object||
|&emsp;&emsp;autoCleanup||boolean||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"defaultRetention": {},
		"auditRetention": {},
		"errorRetention": {},
		"autoCleanup": true
	},
	"success": true
}
```


## 更新日志保留策略


**接口地址**:`/admin/v1/logs/retention`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "defaultRetention": {
    "seconds": 0,
    "nano": 0,
    "negative": true,
    "zero": true,
    "units": []
  },
  "auditRetention": {
    "seconds": 0,
    "nano": 0,
    "negative": true,
    "zero": true,
    "units": []
  },
  "errorRetention": {
    "seconds": 0,
    "nano": 0,
    "negative": true,
    "zero": true,
    "units": []
  },
  "autoCleanup": true
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|logRetentionConfig|LogRetentionConfig|body|true|LogRetentionConfig|LogRetentionConfig|
|&emsp;&emsp;defaultRetention|||false|object||
|&emsp;&emsp;auditRetention|||false|object||
|&emsp;&emsp;errorRetention|||false|object||
|&emsp;&emsp;autoCleanup|||false|boolean||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultLogRetentionConfig|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LogRetentionConfig|LogRetentionConfig|
|&emsp;&emsp;defaultRetention||object||
|&emsp;&emsp;auditRetention||object||
|&emsp;&emsp;errorRetention||object||
|&emsp;&emsp;autoCleanup||boolean||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"defaultRetention": {},
		"auditRetention": {},
		"errorRetention": {},
		"autoCleanup": true
	},
	"success": true
}
```


## 清理过期日志


**接口地址**:`/admin/v1/logs/cleanup`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultLong|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||integer(int64)|integer(int64)|
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": 0,
	"success": true
}
```


## 查询日志


**接口地址**:`/admin/v1/logs`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|arg0||query|false|string||
|arg1||query|false|string||
|arg2|可用值:TRACE,DEBUG,INFO,WARN,ERROR|query|false|string||
|arg3||query|false|string||
|arg4||query|false|string(date-time)||
|arg5||query|false|string(date-time)||
|arg6||query|false|integer(int32)||
|arg7||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|PageResultLogEntry|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|total||integer(int64)|integer(int64)|
|page||integer(int32)|integer(int32)|
|size||integer(int32)|integer(int32)|
|items||array|LogEntry|
|&emsp;&emsp;id||string||
|&emsp;&emsp;timestamp||string(date-time)||
|&emsp;&emsp;type||string||
|&emsp;&emsp;level|可用值:TRACE,DEBUG,INFO,WARN,ERROR|string||
|&emsp;&emsp;message||string||
|&emsp;&emsp;tenantId||string||
|&emsp;&emsp;userId||string||
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;sessionId||string||
|&emsp;&emsp;channelId||string||
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"total": 0,
	"page": 0,
	"size": 0,
	"items": [
		{
			"id": "",
			"timestamp": "",
			"type": "",
			"level": "",
			"message": "",
			"tenantId": "",
			"userId": "",
			"traceId": "",
			"sessionId": "",
			"channelId": "",
			"attributes": {}
		}
	],
	"success": true
}
```


# 字典管理


## 更新字典项


**接口地址**:`/admin/v1/dictionaries/{dictCode}/items/{itemCode}`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "key": "",
  "value": "",
  "label": "",
  "order": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|dictCode||path|true|string||
|itemCode||path|true|string||
|dictionaryItem|DictionaryItem|body|true|DictionaryItem|DictionaryItem|
|&emsp;&emsp;key|||false|string||
|&emsp;&emsp;value|||false|string||
|&emsp;&emsp;label|||false|string||
|&emsp;&emsp;order|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultDictionaryItem|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||DictionaryItem|DictionaryItem|
|&emsp;&emsp;key||string||
|&emsp;&emsp;value||string||
|&emsp;&emsp;label||string||
|&emsp;&emsp;order||integer(int32)||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"key": "",
		"value": "",
		"label": "",
		"order": 0
	},
	"success": true
}
```


## 删除字典项


**接口地址**:`/admin/v1/dictionaries/{dictCode}/items/{itemCode}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|dictCode||path|true|string||
|itemCode||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


## 获取字典详情


**接口地址**:`/admin/v1/dictionaries/{code}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|code||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultDictionary|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||Dictionary|Dictionary|
|&emsp;&emsp;code||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;items||array|DictionaryItem|
|&emsp;&emsp;&emsp;&emsp;key||string||
|&emsp;&emsp;&emsp;&emsp;value||string||
|&emsp;&emsp;&emsp;&emsp;label||string||
|&emsp;&emsp;&emsp;&emsp;order||integer(int32)||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"code": "",
		"name": "",
		"items": [
			{
				"key": "",
				"value": "",
				"label": "",
				"order": 0
			}
		]
	},
	"success": true
}
```


## 更新字典


**接口地址**:`/admin/v1/dictionaries/{code}`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "code": "",
  "name": "",
  "items": [
    {
      "key": "",
      "value": "",
      "label": "",
      "order": 0
    }
  ]
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|code||path|true|string||
|dictionary|Dictionary|body|true|Dictionary|Dictionary|
|&emsp;&emsp;code|||false|string||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;items|||false|array|DictionaryItem|
|&emsp;&emsp;&emsp;&emsp;key|||false|string||
|&emsp;&emsp;&emsp;&emsp;value|||false|string||
|&emsp;&emsp;&emsp;&emsp;label|||false|string||
|&emsp;&emsp;&emsp;&emsp;order|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultDictionary|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||Dictionary|Dictionary|
|&emsp;&emsp;code||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;items||array|DictionaryItem|
|&emsp;&emsp;&emsp;&emsp;key||string||
|&emsp;&emsp;&emsp;&emsp;value||string||
|&emsp;&emsp;&emsp;&emsp;label||string||
|&emsp;&emsp;&emsp;&emsp;order||integer(int32)||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"code": "",
		"name": "",
		"items": [
			{
				"key": "",
				"value": "",
				"label": "",
				"order": 0
			}
		]
	},
	"success": true
}
```


## 删除字典


**接口地址**:`/admin/v1/dictionaries/{code}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|code||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


## 获取字典列表


**接口地址**:`/admin/v1/dictionaries`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|arg0||query|false|integer(int32)||
|arg1||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|PageResultDictionary|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|total||integer(int64)|integer(int64)|
|page||integer(int32)|integer(int32)|
|size||integer(int32)|integer(int32)|
|items||array|Dictionary|
|&emsp;&emsp;code||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;items||array|DictionaryItem|
|&emsp;&emsp;&emsp;&emsp;key||string||
|&emsp;&emsp;&emsp;&emsp;value||string||
|&emsp;&emsp;&emsp;&emsp;label||string||
|&emsp;&emsp;&emsp;&emsp;order||integer(int32)||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"total": 0,
	"page": 0,
	"size": 0,
	"items": [
		{
			"code": "",
			"name": "",
			"items": [
				{
					"key": "",
					"value": "",
					"label": "",
					"order": 0
				}
			]
		}
	],
	"success": true
}
```


## 创建字典


**接口地址**:`/admin/v1/dictionaries`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "code": "",
  "name": "",
  "items": [
    {
      "key": "",
      "value": "",
      "label": "",
      "order": 0
    }
  ]
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|dictionary|Dictionary|body|true|Dictionary|Dictionary|
|&emsp;&emsp;code|||false|string||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;items|||false|array|DictionaryItem|
|&emsp;&emsp;&emsp;&emsp;key|||false|string||
|&emsp;&emsp;&emsp;&emsp;value|||false|string||
|&emsp;&emsp;&emsp;&emsp;label|||false|string||
|&emsp;&emsp;&emsp;&emsp;order|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultDictionary|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||Dictionary|Dictionary|
|&emsp;&emsp;code||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;items||array|DictionaryItem|
|&emsp;&emsp;&emsp;&emsp;key||string||
|&emsp;&emsp;&emsp;&emsp;value||string||
|&emsp;&emsp;&emsp;&emsp;label||string||
|&emsp;&emsp;&emsp;&emsp;order||integer(int32)||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"code": "",
		"name": "",
		"items": [
			{
				"key": "",
				"value": "",
				"label": "",
				"order": 0
			}
		]
	},
	"success": true
}
```


## 获取字典项列表


**接口地址**:`/admin/v1/dictionaries/{dictCode}/items`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|dictCode||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultListDictionaryItem|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|DictionaryItem|
|&emsp;&emsp;key||string||
|&emsp;&emsp;value||string||
|&emsp;&emsp;label||string||
|&emsp;&emsp;order||integer(int32)||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"key": "",
			"value": "",
			"label": "",
			"order": 0
		}
	],
	"success": true
}
```


## 创建字典项


**接口地址**:`/admin/v1/dictionaries/{dictCode}/items`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "key": "",
  "value": "",
  "label": "",
  "order": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|dictCode||path|true|string||
|dictionaryItem|DictionaryItem|body|true|DictionaryItem|DictionaryItem|
|&emsp;&emsp;key|||false|string||
|&emsp;&emsp;value|||false|string||
|&emsp;&emsp;label|||false|string||
|&emsp;&emsp;order|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultDictionaryItem|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||DictionaryItem|DictionaryItem|
|&emsp;&emsp;key||string||
|&emsp;&emsp;value||string||
|&emsp;&emsp;label||string||
|&emsp;&emsp;order||integer(int32)||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"key": "",
		"value": "",
		"label": "",
		"order": 0
	},
	"success": true
}
```


# 部门管理


## 获取部门详情


**接口地址**:`/admin/v1/departments/{id}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultDepartment|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||Department|Department|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;parentId||string||
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": "",
		"name": "",
		"parentId": "",
		"attributes": {}
	},
	"success": true
}
```


## 更新部门


**接口地址**:`/admin/v1/departments/{id}`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "id": "",
  "name": "",
  "parentId": "",
  "attributes": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||
|department|Department|body|true|Department|Department|
|&emsp;&emsp;id|||false|string||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;parentId|||false|string||
|&emsp;&emsp;attributes|||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultDepartment|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||Department|Department|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;parentId||string||
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": "",
		"name": "",
		"parentId": "",
		"attributes": {}
	},
	"success": true
}
```


## 删除部门


**接口地址**:`/admin/v1/departments/{id}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


## 获取部门列表


**接口地址**:`/admin/v1/departments`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|arg0||query|false|integer(int32)||
|arg1||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|PageResultDepartment|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|total||integer(int64)|integer(int64)|
|page||integer(int32)|integer(int32)|
|size||integer(int32)|integer(int32)|
|items||array|Department|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;parentId||string||
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"total": 0,
	"page": 0,
	"size": 0,
	"items": [
		{
			"id": "",
			"name": "",
			"parentId": "",
			"attributes": {}
		}
	],
	"success": true
}
```


## 创建部门


**接口地址**:`/admin/v1/departments`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "id": "",
  "name": "",
  "parentId": "",
  "attributes": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|department|Department|body|true|Department|Department|
|&emsp;&emsp;id|||false|string||
|&emsp;&emsp;name|||false|string||
|&emsp;&emsp;parentId|||false|string||
|&emsp;&emsp;attributes|||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultDepartment|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||Department|Department|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;parentId||string||
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": "",
		"name": "",
		"parentId": "",
		"attributes": {}
	},
	"success": true
}
```


## 获取子部门列表


**接口地址**:`/admin/v1/departments/{parentId}/children`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|parentId||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultListDepartment|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|Department|
|&emsp;&emsp;id||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;parentId||string||
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"id": "",
			"name": "",
			"parentId": "",
			"attributes": {}
		}
	],
	"success": true
}
```


# 配置管理


## 获取配置值


**接口地址**:`/admin/v1/config/{scope}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|scope||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultMapStringString|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"success": true
}
```


## 更新配置值


**接口地址**:`/admin/v1/config/{scope}`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|scope||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


## 重置配置为默认值


**接口地址**:`/admin/v1/config/{scope}/reset`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|scope||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


## 获取配置域 Schema


**接口地址**:`/admin/v1/config/{scope}/schema`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|scope||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultMapStringObject|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"success": true
}
```


## 获取配置域列表


**接口地址**:`/admin/v1/config/scopes`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultListString|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [],
	"success": true
}
```


# 插件管理


## 停止插件


**接口地址**:`/admin/v1/plugins/{pluginId}/stop`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|pluginId||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


## 启动插件


**接口地址**:`/admin/v1/plugins/{pluginId}/start`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|pluginId||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


## 启用插件


**接口地址**:`/admin/v1/plugins/{pluginId}/enable`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|pluginId||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


## 禁用插件


**接口地址**:`/admin/v1/plugins/{pluginId}/disable`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|pluginId||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


## 获取插件列表


**接口地址**:`/admin/v1/plugins`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultListPluginView|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|PluginView|
|&emsp;&emsp;pluginId||string||
|&emsp;&emsp;version||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;description||string||
|&emsp;&emsp;state|可用值:DISCOVERED,LOADED,INITIALIZED,ACTIVE,STOPPED,DISABLED,FAILED|string||
|&emsp;&emsp;provider||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"pluginId": "",
			"version": "",
			"name": "",
			"description": "",
			"state": "",
			"provider": ""
		}
	],
	"success": true
}
```


## 获取插件详情


**接口地址**:`/admin/v1/plugins/{pluginId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|pluginId||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultPluginDetailView|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||PluginDetailView|PluginDetailView|
|&emsp;&emsp;pluginId||string||
|&emsp;&emsp;version||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;description||string||
|&emsp;&emsp;state|可用值:DISCOVERED,LOADED,INITIALIZED,ACTIVE,STOPPED,DISABLED,FAILED|string||
|&emsp;&emsp;provider||string||
|&emsp;&emsp;mainClass||string||
|&emsp;&emsp;dependencies||array|string|
|&emsp;&emsp;extensions||array|ExtensionView|
|&emsp;&emsp;&emsp;&emsp;extensionPoint||string||
|&emsp;&emsp;&emsp;&emsp;className||string||
|&emsp;&emsp;&emsp;&emsp;priority||integer(int32)||
|&emsp;&emsp;loadedAt||string(date-time)||
|&emsp;&emsp;startedAt||string(date-time)||
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"pluginId": "",
		"version": "",
		"name": "",
		"description": "",
		"state": "",
		"provider": "",
		"mainClass": "",
		"dependencies": [],
		"extensions": [
			{
				"extensionPoint": "",
				"className": "",
				"priority": 0
			}
		],
		"loadedAt": "",
		"startedAt": "",
		"attributes": {}
	},
	"success": true
}
```


## 卸载插件


**接口地址**:`/admin/v1/plugins/{pluginId}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|pluginId||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


# 认证管理


## 刷新 Token


**接口地址**:`/admin/v1/auth/refresh`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "refreshToken": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|refreshRequest|刷新 Token 请求|body|true|RefreshRequest|RefreshRequest|
|&emsp;&emsp;refreshToken|刷新令牌||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultTokenResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TokenResponse|TokenResponse|
|&emsp;&emsp;accessToken|访问令牌|string||
|&emsp;&emsp;refreshToken|刷新令牌|string||
|&emsp;&emsp;expiresIn|过期时间（秒）|integer(int64)||
|&emsp;&emsp;tokenType|令牌类型|string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"accessToken": "",
		"refreshToken": "",
		"expiresIn": 0,
		"tokenType": ""
	},
	"success": true
}
```


## 用户登出


**接口地址**:`/admin/v1/auth/logout`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


## 用户登录


**接口地址**:`/admin/v1/auth/login`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "username": "",
  "password": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|loginRequest|登录请求|body|true|LoginRequest|LoginRequest|
|&emsp;&emsp;username|用户名||true|string||
|&emsp;&emsp;password|密码||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultTokenResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TokenResponse|TokenResponse|
|&emsp;&emsp;accessToken|访问令牌|string||
|&emsp;&emsp;refreshToken|刷新令牌|string||
|&emsp;&emsp;expiresIn|过期时间（秒）|integer(int64)||
|&emsp;&emsp;tokenType|令牌类型|string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"accessToken": "",
		"refreshToken": "",
		"expiresIn": 0,
		"tokenType": ""
	},
	"success": true
}
```


## 获取当前用户信息


**接口地址**:`/admin/v1/auth/me`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultCurrentUserInfo|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||CurrentUserInfo|CurrentUserInfo|
|&emsp;&emsp;userId|用户标识|string||
|&emsp;&emsp;username|用户名|string||
|&emsp;&emsp;displayName|显示名称|string||
|&emsp;&emsp;roles|角色集合|array|string|
|&emsp;&emsp;permissions|权限集合|array|string|
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"userId": "",
		"username": "",
		"displayName": "",
		"roles": [],
		"permissions": []
	},
	"success": true
}
```


# plugin-static-resource-controller


## getAsset


**接口地址**:`/plugins/{pluginId}/assets/**`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||


**响应参数**:


暂无


**响应示例**:
```javascript

```


# UI 元数据


## 获取动态路由


**接口地址**:`/admin/v1/ui/routes`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultListRouteContribution|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|RouteContribution|
|&emsp;&emsp;path||string||
|&emsp;&emsp;component||string||
|&emsp;&emsp;title||string||
|&emsp;&emsp;icon||string||
|&emsp;&emsp;permissions||array|string|
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"path": "",
			"component": "",
			"title": "",
			"icon": "",
			"permissions": []
		}
	],
	"success": true
}
```


## 获取菜单树


**接口地址**:`/admin/v1/ui/menus`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultListMenuContribution|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|MenuContribution|
|&emsp;&emsp;id||string||
|&emsp;&emsp;label||string||
|&emsp;&emsp;icon||string||
|&emsp;&emsp;parentId||string||
|&emsp;&emsp;order||integer(int32)||
|&emsp;&emsp;route||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"id": "",
			"label": "",
			"icon": "",
			"parentId": "",
			"order": 0,
			"route": ""
		}
	],
	"success": true
}
```


## 获取 UI 贡献清单


**接口地址**:`/admin/v1/ui/manifest`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultMapStringPluginContributes|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||PluginContributes|PluginContributes|
|&emsp;&emsp;empty||boolean||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"additionalProperties1": {
			"empty": true
		}
	},
	"success": true
}
```


# 系统状态


## 获取系统状态


**接口地址**:`/admin/v1/system/status`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultSystemStatusView|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||SystemStatusView|SystemStatusView|
|&emsp;&emsp;status||string||
|&emsp;&emsp;totalPlugins||integer(int32)||
|&emsp;&emsp;activePlugins||integer(int32)||
|&emsp;&emsp;disabledPlugins||integer(int32)||
|&emsp;&emsp;failedPlugins||integer(int32)||
|&emsp;&emsp;uptimeMillis||integer(int64)||
|&emsp;&emsp;jvmInfo||object||
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"status": "",
		"totalPlugins": 0,
		"activePlugins": 0,
		"disabledPlugins": 0,
		"failedPlugins": 0,
		"uptimeMillis": 0,
		"jvmInfo": {},
		"attributes": {}
	},
	"success": true
}
```


## 获取平台信息


**接口地址**:`/admin/v1/system/info`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultPlatformInfoView|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||PlatformInfoView|PlatformInfoView|
|&emsp;&emsp;name||string||
|&emsp;&emsp;version||string||
|&emsp;&emsp;description||string||
|&emsp;&emsp;buildInfo||object||
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"name": "",
		"version": "",
		"description": "",
		"buildInfo": {},
		"attributes": {}
	},
	"success": true
}
```


## 获取系统健康状态


**接口地址**:`/admin/v1/system/health`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultMapStringObject|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"success": true
}
```


# 存储浏览


## 查看存储对象


**接口地址**:`/admin/v1/storage/{namespace}/{key}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|namespace||path|true|string||
|key||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultMapStringObject|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"success": true
}
```


## 删除存储对象


**接口地址**:`/admin/v1/storage/{namespace}/{key}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|namespace||path|true|string||
|key||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


# 权限管理


## 获取权限列表


**接口地址**:`/admin/v1/permissions`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultListPermission|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|Permission|
|&emsp;&emsp;code||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;description||string||
|&emsp;&emsp;resource||string||
|&emsp;&emsp;action||string||
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"code": "",
			"name": "",
			"description": "",
			"resource": "",
			"action": "",
			"attributes": {}
		}
	],
	"success": true
}
```


## 获取权限树


**接口地址**:`/admin/v1/permissions/tree`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultMapStringListPermission|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||Permission|Permission|
|&emsp;&emsp;code||string||
|&emsp;&emsp;name||string||
|&emsp;&emsp;description||string||
|&emsp;&emsp;resource||string||
|&emsp;&emsp;action||string||
|&emsp;&emsp;attributes||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"additionalProperties1": {
			"code": "",
			"name": "",
			"description": "",
			"resource": "",
			"action": "",
			"attributes": {}
		}
	},
	"success": true
}
```


# 缓存检查


## 查看缓存条目


**接口地址**:`/admin/v1/cache/{namespace}/{key}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|namespace||path|true|string||
|key||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultMapStringObject|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"success": true
}
```


## 清除缓存条目


**接口地址**:`/admin/v1/cache/{namespace}/{key}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|namespace||path|true|string||
|key||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|Result|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"success": true
}
```


# platform-controller


## index


**接口地址**:`/`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|DataResultMapStringObject|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||
|success||boolean||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"success": true
}
```


# system-user-plugin


## listUsers


**接口地址**:`/plugins/system-user-plugin/user/list`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应参数**:


暂无


**响应示例**:
```javascript

```


## createUser


**接口地址**:`/plugins/system-user-plugin/user/add`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应参数**:


暂无


**响应示例**:
```javascript

```