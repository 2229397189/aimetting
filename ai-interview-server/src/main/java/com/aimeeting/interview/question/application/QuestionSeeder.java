package com.aimeeting.interview.question.application;

import com.aimeeting.interview.common.util.JsonUtil;
import com.aimeeting.interview.question.dao.entity.QuestionDO;
import com.aimeeting.interview.question.dao.mapper.QuestionMapper;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 题库种子数据（首次启动且表为空时写入）。
 *
 * <p>题目用于：内置题库兜底出题、题库浏览、随机抽题与管理员录入参考。
 * 真实面试题目优先由 AI 生成，题库作为降级与展示来源。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionSeeder {

    private final QuestionMapper questionMapper;

    /** 内置题目：{方向, 难度, 题面, 考察要点(|分隔), 解析}。 */
    private static final Object[][] SEED = {
            {"JAVA_BACKEND", "EASY", "说说 HashMap 的底层结构与扩容机制", "数组+链表/红黑树|负载因子0.75|扩容2倍并rehash", "JDK8 后链表长度>8且数组长度≥64转红黑树，扩容阈值=容量*负载因子。"},
            {"JAVA_BACKEND", "MEDIUM", "Synchronized 和 ReentrantLock 的区别？各自适用场景？", "实现层面|可中断/公平/条件变量|性能与语义", "ReentrantLock 支持可中断、公平锁、多条件队列；synchronized 自动释放、JVM 持续优化。"},
            {"JAVA_BACKEND", "MEDIUM", "JVM 内存结构是怎样的？堆和栈分别存什么？", "程序计数器/虚拟机栈/本地方法栈/堆/方法区|线程私有vs共享", "虚拟机栈存栈帧（局部变量、操作数），堆存对象实例，方法区存类元数据。"},
            {"JAVA_BACKEND", "HARD", "讲讲 G1 垃圾回收器的工作原理与适用场景", "Region分区|可预测停顿|并发标记清理", "G1 将堆划分为 Region，优先回收收益高的区域，满足停顿时间目标。"},

            {"FRONTEND", "EASY", "说一下浏览器从输入 URL 到页面渲染的过程", "DNS|TCP|渲染树|重排重绘", "经历 DNS 解析、建连、响应、解析 DOM/CSSOM、布局绘制。"},
            {"FRONTEND", "MEDIUM", "Vue 的响应式原理是什么？3.x 和 2.x 有何不同？", "Object.defineProperty vs Proxy|依赖收集", "Vue3 用 Proxy 代理整个对象，无需递归初始化，支持数组/新增属性。"},
            {"FRONTEND", "MEDIUM", "什么是跨域？前端有哪些解决跨域的方案？", "CORS|代理|JSONP", "开发用 devServer 代理，生产用 Nginx 反代或后端配置 CORS。"},
            {"FRONTEND", "HARD", "如何优化一个首屏加载很慢的 SPA 应用？", "代码分割|预加载|缓存|骨架屏", "路由级懒加载、资源预取、HTTP 缓存、SSR/骨架屏降低白屏。"},

            {"DATABASE", "EASY", "什么是索引？为什么索引能加快查询？", "B+树|减少IO|最左前缀", "索引以 B+ 树有序存储，把随机 IO 变成顺序/少量 IO。"},
            {"DATABASE", "MEDIUM", "事务的 ACID 特性分别靠什么保证？", "原子性undo|隔离性锁/MVCC|持久性redo|一致性约束", "undo log 保证回滚，redo log 保证落盘，MVCC 解决读写冲突。"},
            {"DATABASE", "MEDIUM", "说说 MySQL 的隔离级别和脏读/不可重复读/幻读", "读未提交/已提交/可重复读/串行|MVCC", "InnoDB 默认可重复读，靠 MVCC+间隙锁解决幻读。"},
            {"DATABASE", "HARD", "什么情况下索引会失效？如何排查慢查询？", "函数/隐式转换/前导模糊|EXPLAIN", "对索引列做函数或类型转换会失效，用 EXPLAIN 看 type/rows。"},

            {"OS", "EASY", "进程和线程的区别是什么？", "资源拥有者|调度单位|通信方式", "进程是资源分配单位，线程是 CPU 调度单位，同进程线程共享内存。"},
            {"OS", "MEDIUM", "什么是死锁？产生死锁的四个必要条件？", "互斥|占有等待|不可剥夺|循环等待", "破坏任一条件即可预防死锁，如按序加锁避免循环等待。"},
            {"OS", "MEDIUM", "虚拟内存的作用是什么？", "扩展内存|局部性|页面置换", "用磁盘扩展逻辑内存，按需调页，提升多任务并发能力。"},
            {"OS", "HARD", "用户态和内核态的区别？为什么要区分？", "权限级别|系统调用开销|安全", "区分保护核心资源，用户态需经系统调用陷入内核，带来一定开销。"},

            {"NETWORK", "EASY", "TCP 和 UDP 的区别？各自适用什么场景？", "连接|可靠|有序|首部开销", "TCP 可靠面向连接适合传输，UDP 低延迟适合音视频/游戏。"},
            {"NETWORK", "MEDIUM", "TCP 三次握手和四次挥手的过程？", "SYN/ACK|为何三次/四次|TIME_WAIT", "握手确认双向可达，挥手因全双工需各自关闭，TIME_WAIT 防旧报文。"},
            {"NETWORK", "MEDIUM", "HTTPS 的握手过程是怎样的？", "非对称交换密钥|CA|对称加密传输", "用证书中的公钥协商出对称密钥，后续用对称加密传输。"},
            {"NETWORK", "HARD", "从浏览器到服务器经历了哪些网络设备和协议？", "ARP|路由|NAT|DNS", "局域网经 ARP/交换机，跨网经路由与 NAT，域名经 DNS 解析。"},

            {"ALGORITHM", "EASY", "说一下快速排序的思想和时间复杂度", "分治|基准|最好/最坏/平均", "平均 O(n log n)，最坏 O(n^2)（已有序且选端点在首），随机选基准缓解。"},
            {"ALGORITHM", "MEDIUM", "如何判断一个链表是否有环？", "快慢指针|相遇|入口", "快慢指针相遇说明有环，再让一指针从头部同步前进求入口。"},
            {"ALGORITHM", "MEDIUM", "讲讲二分查找的适用条件和实现要点", "有序|边界|防溢出", "区间开闭要一致，mid 用 low+(high-low)/2 防溢出。"},
            {"ALGORITHM", "HARD", "TopK 问题有哪些解法？时间复杂度各是多少？", "排序|堆|快排partition", "全排序 O(n log n)，堆 O(n log k)，快排 partition 平均 O(n)。"},

            {"SYSTEM_DESIGN", "EASY", "什么是幂等性？接口如何保证幂等？", "唯一ID|去重表|状态机", "通过请求唯一号+去重表或状态机，保证重复调用结果一致。"},
            {"SYSTEM_DESIGN", "MEDIUM", "如何设计一个短链接系统？", "发号器|哈希|跳转302", "长链映射短码，发号器或哈希生成，访问 302 跳转并统计。"},
            {"SYSTEM_DESIGN", "MEDIUM", "缓存和数据库一致性如何保证？", "先更新DB再删缓存|延时双删|binlog", "更新 DB 后删缓存，配合延时双删与 binlog 补偿降低不一致。"},
            {"SYSTEM_DESIGN", "HARD", "如何设计一个支持高并发的秒杀系统？", "动静分离|队列削峰|库存预扣|限流", "静态化+CDN，请求进 MQ 削峰，Redis 预扣库存，网关限流。"},

            {"BEHAVIORAL", "EASY", "请做一个简短的自我介绍", "背景|项目|目标", "用 30 秒说清专业背景、核心项目与目标岗位匹配点。"},
            {"BEHAVIORAL", "MEDIUM", "说一个你解决过的最有挑战的技术问题", "背景|行动|结果(STAR)", "用 STAR 结构：情境-任务-行动-结果，突出个人贡献。"},
            {"BEHAVIORAL", "MEDIUM", "你和同事在技术选型上有分歧会怎么处理？", "数据说话|沟通|灰度", "先对齐目标，用数据/原型验证，小流量灰度降低风险。"},
            {"BEHAVIORAL", "HARD", "如果项目延期了，你会如何应对？", "风险识别|优先级|向上同步", "尽早暴露风险，重排优先级，及时同步干系人并调整范围。"},
    };

    @PostConstruct
    public void seed() {
        try {
            Long count = questionMapper.selectCount(null);
            if (count != null && count > 0) {
                return;
            }
            List<QuestionDO> list = new ArrayList<>();
            for (Object[] row : SEED) {
                QuestionDO q = new QuestionDO();
                q.setDirection((String) row[0]);
                q.setDifficulty((String) row[1]);
                q.setTitle((String) row[2]);
                q.setReferencePoints(JsonUtil.toJson(split((String) row[3])));
                q.setAnalysis((String) row[4]);
                q.setSource("SEED");
                q.setStatus(1);
                list.add(q);
            }
            for (QuestionDO q : list) {
                questionMapper.insert(q);
            }
            log.info("[QuestionSeeder] 初始化内置题目 {} 条", list.size());
        } catch (Exception e) {
            log.warn("[QuestionSeeder] 种子初始化跳过: {}", e.getMessage());
        }
    }

    private List<String> split(String s) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isEmpty()) {
            return out;
        }
        for (String part : s.split("\\|")) {
            out.add(part.trim());
        }
        return out;
    }
}
