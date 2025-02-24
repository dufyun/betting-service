# Betting Server 项目

功能说明  
> 基于Java HttpServer 实现的投注服务，支持会话管理、投注提交及查询投注列表。


环境说明   
> JDK1.8+ 、Maven3+


---

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   ├── com/
│   │   │   └── betting/
│   │   │       ├── controller/          
│   │   │       │   ├── SessionController.java # 会话控制器
│   │   │       │   ├── StakeController.java # 提交投注控制器
│   │   │       │   └── HighStakesController.java # 投注项列表控制器
│   │   │       ├── service/             
│   │   │       │   ├── SessionService.java # 会话业务服务
│   │   │       │   └── StakeService.java # 投注业务服务
│   │   │       ├── repository/         
│   │   │       │   ├── SessionRepository.java # 会话存储仓库
│   │   │       │   └── StakeRepository.java # 投注数据存储仓库
│   │   │       ├── handler/               
│   │   │       │   └── RouterHandler.java # 请求路由分发器
│   │   │       ├── enums/               
│   │   │       │   └── HttpStatusEnum.java # HTTP状态码枚举
│   │   │       ├── util/                
│   │   │       │   ├── HttpHelperUtils.java # HTTP工具类
│   │   │       │   ├── SessionKeyUtils.java # 会话密钥生成器
│   │   │       │   └── ThreadPoolManagerUtils.java # 线程池管理器
│   │   │       ├── util/                
│   │   │       │   ├── HttpHelperUtils.java # HTTP工具类
│   │   │       │   └── SessionKeyUtils.java # 会话密钥生成器
│   │   │       └── BettingServerMain.java # 服务启动入口
│   ├── resources/
└── test/                                # 测试代码
│   ├── java/
│   │   ├── com/
│   │   │   └── betting/
│   │   │       ├── service/          
│   │   │       │   ├── SessionServiceTest.java # 会话业务服务测试类
│   │   │       │   ├── StakeServiceTest.java # 投注业务服务测试类
│   │   │       ├── util/                
│   │   │       │   ├── SessionKeyUtilsTest.java # 会话密钥生成器测试类 
└── 
pom.xml
```
 
---

## 核心实现

### 1. 会话获取和管理
- 按CPU核心数使用分片式`ConcurrentHashMap`存储会话，降低锁竞争
- 基于时间戳+序列号的Base62算法生成唯一sessionKey
- 用`ConcurrentHashMap`实现sessionKey→customerID快速查找
- 定时轮询分片扫描清理过期会话，避免清理全局锁

### 2. 投注提交和存储
- 校验SessionKey有效性，反向索引快速查找
- 使用分段锁，用`ReentrantLock`数组，保护客户投注数据
- 动态数组存储客户历史投注，仅保留最高金额
- `ConcurrentSkipListMap`实现自动排序

### 3. 获取高投注列表
- 直接从预排序的`ConcurrentSkipListMap`获取排序好的数据
- 无实时计算开销,响应时间快




## 构建运行

### 1、编译打包

```bash
mvn clean package
```


### 2、运行 JAR

```bash
java -jar betting-service-1.0.0.jar
```

### 3、验证接口：
```bash
curl http://localhost:8001/1234/session

curl -X POST -d 4500 "http://localhost:8001/888/stake?sessionkey=QWER12A"

curl http://localhost:8001/888/highstakes

```


## 测试说明
### 功能测试
1、模拟30个用户，首先用户先获取sessionkey，然后同一个在一个投注项888上多次进行投注不同金额，如果未获取sessionkey或者sessionkey过期，则不能进行投注。  

2、查询投注项888对应的最高投注和用户列表，返回预期排序结果，查询另一个投注项999，返回空。

### 并发测试
在windows机器上做了下简单的并发性能测试。

应用运行配置最大堆内存为1g，在Jmeter中进行并发测试，利用jconsole监控应用情况。  
- 获取Session和提交投注接口，配置50个线程，持续运行3分钟  
- 获取投注项最高金额列表，配置50个线程，持续运行3分钟  

上面过程持续运行两次。 运行后的结果如下：  

![windows机器运行情况](https://img.picui.cn/free/2025/02/24/67bc0142c8dd8.png)

![应用运行情况](https://img.picui.cn/free/2025/02/24/67bc014307796.png)

![Jmeter运行报告](https://img.picui.cn/free/2025/02/24/67bc0142b638a.png)

简单的测试，看数据当前应用在配置1G最大堆内存下，能够支持百万级别用户的数据。

