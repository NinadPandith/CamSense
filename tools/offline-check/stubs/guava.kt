package com.google.common.util.concurrent
interface ListenableFuture<V> : java.util.concurrent.Future<V> { fun addListener(listener: Runnable, executor: java.util.concurrent.Executor) }
