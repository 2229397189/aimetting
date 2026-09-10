# -*- coding: utf-8 -*-
"""生成面试题库数据文件 QuestionSeedData.java（紧凑、可复现、不截断）。

做法：每个方向维护一组真实、具体的面试话题短语；再按难度套用若干模板，
组合出大量语义明确、互不重复的题目。reference_points 用方向级通用维度标签，
analysis 按难度给出答题要求。这样能在保证正确性的前提下扩到约 1000 题。

用法: python gen_question_seed.py > QuestionSeedData.java
"""
import sys

# 每个方向的真实话题短语（短、具体、面试常考）
TOPICS = {
    "JAVA_BACKEND": [
        "HashMap 的底层结构与扩容", "ConcurrentHashMap 线程安全", "synchronized 实现", "ReentrantLock 实现",
        "JVM 内存结构", "垃圾回收器 G1", "垃圾回收器 ZGC", "对象创建与内存分配",
        "双亲委派模型", "类加载过程", "强软弱虚引用", "String 不可变设计",
        "volatile 语义", "CAS 与原子类", "AQS 与同步器", "线程池参数与执行流程",
        "拒绝策略", "ThreadLocal 与内存泄漏", "CompletableFuture", "ForkJoinPool",
        "伪共享与缓存行", "happens-before 原则", "JMM 内存模型", "偏向锁升级",
        "逃逸分析与锁消除", "方法内联与 JIT", "GraalVM 与 AOT", "Lambda 与 invokedynamic",
        "Stream API", "设计模式之单例", "设计模式之工厂", "设计模式之策略",
        "设计模式之观察者", "设计模式之责任链", "设计模式之装饰器", "IoC 与 DI",
        "MyBatis 一级二级缓存", "MyBatis # 与 $ 区别", "Spring 事务传播", "Spring Bean 生命周期",
        "Spring 循环依赖", "DDD 与贫血模型", "CQRS 读写分离", "分布式锁 Redisson",
        "Redis 持久化机制", "Redis 主从哨兵集群", "Lua 在 Redis", "缓存穿透击穿雪崩",
        "布隆过滤器", "本地缓存 Caffeine", "消息队列不丢失", "RocketMQ 事务消息",
        "Kafka 消费位点", "分布式事务 TCC", "分布式事务 Saga", "分库分表",
        "雪花算法 ID 生成", "读写分离延迟", "binlog CDC 订阅", "MVCC 多版本并发",
        "索引最左前缀", "慢 SQL 治理", "Online DDL 变更", "GC 日志与调优",
        "OOM 定位", "线程 dump 分析", "Arthas 线上诊断", "零拷贝技术",
        "Netty Reactor 模型", "序列化协议选型", "gRPC 与 RPC", "微服务拆分",
        "服务注册发现", "网关与限流", "熔断降级 Sentinel", "配置中心",
        "幂等设计", "重试风暴避免", "背压 back-pressure", "可观测三支柱",
        "链路追踪 TraceId", "CI/CD 流水线", "蓝绿与金丝雀发布", "等保三级合规",
    ],
    "DATABASE": [
        "B+ 树索引原理", "聚簇索引与二级索引", "覆盖索引", "回表代价",
        "联合索引最左前缀", "索引下推 ICP", "索引失效场景", "执行计划 EXPLAIN",
        "MVCC 实现", "快照读与当前读", "隔离级别与幻读", "间隙锁与 Next-Key",
        "死锁定位", "redo log 与 binlog", "两段提交", "Buffer Pool 机制",
        "WAL 预写日志", "主从复制", "半同步复制", "组复制 MGR",
        "读写分离延迟", "分库分表", "分片键设计", "跨分片事务",
        "热点行优化", "自增主键瓶颈", "逻辑删除坑", "binlog 格式",
        "延迟双删", "缓存与 DB 一致性", "库存不超卖", "统计信息过期",
        "Online DDL gh-ost", "窗口函数", "递归 CTE 查树", "EXISTS 与 IN",
        "keyset 分页", "JSON 字段索引", "检查约束 CHECK", "HTAP 架构",
        "列存数据库", "时序数据库", "图数据库", "文档数据库",
        "Vitess 分片", "数据迁移双写", "一致性哈希扩容", "异地多活",
        "数据脱敏策略", "动态数据脱敏", "数据库审计", "SQL 防火墙",
        "敏感数据发现", "数据分类分级", "湖仓一体", "流批一体",
        "数仓分层", "宽表建模", "主数据管理", "指标体系",
        "隐私计算平台", "特征一致性", "向量检索", "RAG 检索质量",
    ],
    "SYSTEM_DESIGN": [
        "幂等性设计", "短链接系统", "缓存与数据库一致性", "秒杀系统",
        "限流算法", "熔断与降级", "分布式锁", "分布式 ID 生成",
        "分布式事务", "本地消息表", "Saga 补偿事务", "读写分离",
        "分库分表", "CDN 加速", "对象存储直传", "静态资源优化",
        "API 网关", "服务注册发现", "配置中心", "链路追踪",
        "可观测性体系", "灰度发布", "蓝绿部署", "混沌工程",
        "容量评估", "连接池管理", "线程池隔离", "多级缓存",
        "热点 key 打散", "bigkey 治理", "延迟队列", "顺序消息",
        "消息不丢失", "死信队列", "Exactly-Once 语义", "最终一致性",
        "单元化架构", "异地多活", "数据迁移双写", "特征平台",
        "推荐系统召回排序", "向量库选型", "大模型推理优化", "RAG 架构",
        "Agent 记忆系统", "多 Agent 编排", "AI 网关", "提示词注入防护",
        "成本治理", "限流在 AI", "缓存 AI 结果", "评估集与回归",
    ],
    "NETWORK": [
        "TCP 与 UDP 区别", "三次握手与四次挥手", "TIME_WAIT 状态", "HTTPS 握手",
        "TLS 1.3 改进", "HTTP 与 HTTPS", "HTTP 缓存", "HTTP/2 多路复用",
        "HTTP/3 QUIC", "DNS 解析", "ARP 协议", "路由与 NAT",
        "从浏览器到服务器路径", "CDN 原理", "负载均衡", "反向代理",
        "跨域 CORS", "CSRF 防护", "XSS 防护", "SQL 注入防护",
        "对称与非对称加密", "数字签名", "证书链校验", "OAuth2 授权码",
        "JWT 原理", "Session 与 Cookie", "TCP 粘包拆包", "心跳保活",
        "零拷贝", "epoll 与 select", "Reactor 与 Proactor", "长连接短连接",
        "gRPC 基于 HTTP2", "服务网格 mTLS", "eBPF 观测", "W3C Trace Context",
    ],
    "OS": [
        "进程与线程区别", "死锁四个条件", "虚拟内存", "用户态与内核态",
        "上下文切换", "进程调度算法", "内存分页", "页面置换算法",
        "缺页中断", "缓冲区与缓存", "文件系统设计", "inode 与目录",
        "IO 多路复用", "零拷贝", "中断与系统调用", "并发与并行",
        "原子操作", "内存屏障", "CPU 缓存一致性", "协程",
        "信号与管道", "共享内存", "自旋锁", "读写锁",
    ],
    "ALGORITHM": [
        "快速排序", "归并排序", "堆排序", "冒泡排序",
        "二分查找", "链表环检测", "TopK 问题", "二叉树遍历",
        "红黑树", "跳表", "哈希表冲突", "LSM Tree",
        "B+ 树", "动态规划", "回溯算法", "贪心算法",
        "广度优先搜索", "深度优先搜索", "Dijkstra 最短路", "并查集",
        "前缀树 Trie", "位运算技巧", "滑动窗口", "双指针",
        "单调栈", "单调队列", "字符串匹配 KMP", "编辑距离",
        "最大子数组和", "背包问题", "最长公共子序列", "拓扑排序",
        "一致性哈希", "布隆过滤器原理", "SkipList 应用", "时间轮算法",
    ],
    "FRONTEND": [
        "从输入 URL 到渲染", "浏览器缓存", "重排与重绘", "Vue 响应式原理",
        "Vue2 与 Vue3 区别", "React  reconciliation", "虚拟 DOM", "Diff 算法",
        "跨域解决方案", "前端路由原理", "SPA 首屏优化", "Webpack 与 Vite",
        "Tree Shaking", "防抖与节流", "Event Loop", "微任务宏任务",
        "Promise 原理", "async/await", "闭包", "原型链",
        "CSS 盒模型", "Flex 布局", "Grid 布局", "BFC 清除浮动",
        "HTTP 缓存策略", "Service Worker", "PWA 离线", "Web 安全 XSS",
        "Web 安全 CSRF", "性能监控", "错误监控", "组件设计原则",
        "状态管理", "TypeScript 类型", "Canvas 与 SVG", "动画性能",
    ],
    "BEHAVIORAL": [
        "自我介绍", "最有挑战的技术问题", "技术选型分歧", "项目延期应对",
        "最大的失败", "团队协作冲突", "从零到一的项目", "带教与分享",
        "职业发展规划", "为什么选择我们", "你的缺点", "你的优点",
        "压力下的决策", "向上管理", "跨部门推动", "技术债务处理",
        "复盘与改进", "业务理解深度", "技术影响力", "学习新方法",
    ],
}

# 每个方向、每个难度对应的通用答题维度（reference_points）
DIM = {
    "JAVA_BACKEND": ["原理", "源码", "实战", "边界", "性能"],
    "DATABASE": ["原理", "索引", "事务", "调优", "一致性"],
    "SYSTEM_DESIGN": ["场景", "权衡", "容错", "扩展", "落地"],
    "NETWORK": ["协议", "握手", "安全", "排障", "性能"],
    "OS": ["概念", "调度", "内存", "并发", "IO"],
    "ALGORITHM": ["思路", "复杂度", "边界", "优化", "编码"],
    "FRONTEND": ["原理", "渲染", "性能", "安全", "工程"],
    "BEHAVIORAL": ["STAR", "结果", "反思", "协作", "成长"],
}

# 每个难度的模板（{t}=话题短语）与分析文案
TEMPLATES = {
    "EASY": [
        ("说说你对 {t} 的理解", "掌握基础概念，能用自己的话讲清楚即可。"),
        ("请介绍一下 {t}", "从定义、用途与直观例子切入，讲清是什么、解决什么问题。"),
    ],
    "MEDIUM": [
        ("深入 {t}：实际使用中会遇到哪些坑", "结合实际经验说明边界条件、常见误用与取舍。"),
        ("{t} 的常见实现方式与选型", "对比主流实现，说明各自适用场景与代价。"),
    ],
    "HARD": [
        ("从原理到生产：如何排查 {t} 相关问题", "需要能从原理出发，结合监控/日志定位并给出优化方案。"),
        ("{t} 在大规模高并发下的挑战与优化", "关注扩展瓶颈、一致性代价与架构层面的应对。"),
    ],
}

ORDER = ["JAVA_BACKEND", "DATABASE", "SYSTEM_DESIGN", "NETWORK", "OS", "ALGORITHM", "FRONTEND", "BEHAVIORAL"]
DIFFS = ["EASY", "MEDIUM", "HARD"]


def gen_rows():
    rows = []
    seen = set()
    for d in ORDER:
        for diff in DIFFS:
            for t in TOPICS[d]:
                for (tpl, analysis) in TEMPLATES[diff]:
                    title = tpl.replace("{t}", t)
                    if title in seen:
                        continue
                    seen.add(title)
                    pts = DIM[d]
                    # reference_points 以 | 分隔，analysis 用难度文案
                    rows.append((d, diff, title, "|".join(pts), analysis))
    return rows


def to_java_string(s):
    # 转义 Java 字符串中的双引号与反斜杠
    return s.replace("\\", "\\\\").replace('"', '\\"')


def main():
    rows = gen_rows()
    # javac 单方法字节码上限 64KB：单条巨大数组字面量会触发「代码过长」。
    # 因此把数据拆成多个独立静态方法（part0/part1/...），每个方法各自受 64KB 约束，
    # 再在静态块里用 concat 拼接成 SEED。每个 part 约 250 行，远小于上限。
    chunk = 250
    parts = [rows[i:i + chunk] for i in range(0, len(rows), chunk)]

    out = []
    out.append("package com.aimeeting.interview.question.application;")
    out.append("")
    out.append("import java.util.Arrays;")
    out.append("")
    out.append("/**")
    out.append(" * 题库种子数据（由 tools/gen_question_seed.py 生成）。")
    out.append(" * 结构: {direction, difficulty, title, referencePoints(|分隔), analysis}。")
    out.append(" * 共 %d 条，启动由 QuestionSeeder 按 title 幂等 upsert。" % len(rows))
    out.append(" * 数据拆为 part0..part%d 多个方法，避免单方法字节码超过 64KB 上限。" % (len(parts) - 1))
    out.append(" */")
    out.append("public final class QuestionSeedData {")
    out.append("")
    out.append("    private QuestionSeedData() {}")
    out.append("")
    out.append("    /** 拼接两个二维数组（用于把分片数据合并为 SEED）。 */")
    out.append("    private static Object[][] concat(Object[][] a, Object[][] b) {")
    out.append("        Object[][] r = Arrays.copyOf(a, a.length + b.length);")
    out.append("        System.arraycopy(b, 0, r, a.length, b.length);")
    out.append("        return r;")
    out.append("    }")
    out.append("")

    for idx, part in enumerate(parts):
        out.append("    private static Object[][] part%d() {" % idx)
        out.append("        return new Object[][] {")
        for (d, diff, title, pts, analysis) in part:
            out.append('            {"%s", "%s", "%s", "%s", "%s"},'
                       % (d, diff, to_java_string(title), to_java_string(pts), to_java_string(analysis)))
        out.append("        };")
        out.append("    }")
        out.append("")

    out.append("    /** 内置题目（合并所有分片）。 */")
    out.append("    public static final Object[][] SEED;")
    out.append("    static {")
    out.append("        Object[][] s = part0();")
    for idx in range(1, len(parts)):
        out.append("        s = concat(s, part%d());" % idx)
    out.append("        SEED = s;")
    out.append("    }")
    out.append("}")
    out.append("")
    sys.stdout.write("\n".join(out))


if __name__ == "__main__":
    main()
