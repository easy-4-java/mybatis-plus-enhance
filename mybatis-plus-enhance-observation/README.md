# mybatis-plus-enhance-observation

提供 SQL 执行耗时、成功状态和异常观测，支持显式注册以及
`ServiceLoader` 发现 `SqlObservationSink`。

Sink 在 SQL 调用线程中执行，实现应快速、无阻塞。
