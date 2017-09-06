package io.shuidi.snowflake.core.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Author: Alvin Tian
 * Date: 2017/8/30 21:11
 */
public class RetryRunner {
	private boolean throwException = false;
	private RetryErrorCallable retryErrorCallable;
	private RetryErrorCallable finalErrorCallable;


	private RetryRunner() {
	}

	private static Logger LOGGER = LoggerFactory.getLogger(RetryRunner.class);


	private int retryLimit = 3;
	private long sleep = 1000;
	private List<Class<? extends Exception>> includeExceptions = Lists.newArrayList();
	private List<Class<? extends Exception>> excludeExceptions = Lists.newArrayList();

	public static RetryRunner create() {
		return new RetryRunner();
	}

	public RetryRunner limit(int retryLimit) {
		Preconditions.checkArgument(retryLimit > 0);
		this.retryLimit = retryLimit;
		return this;
	}

	public RetryRunner sleep(int sleep) {
		Preconditions.checkArgument(sleep > 0);
		this.sleep = sleep;
		return this;
	}

	public RetryRunner includeExceptions(Class<? extends Exception>... es) {
		Preconditions.checkArgument(es.length > 0);
		includeExceptions.addAll(Lists.newArrayList(es));
		return this;
	}

	public RetryRunner excludeExceptions(Class<? extends Exception>... es) {
		Preconditions.checkArgument(es.length > 0);
		excludeExceptions.addAll(Lists.newArrayList(es));
		return this;
	}

	public RetryRunner onTryError(RetryErrorCallable retryErrorCallable) {
		this.retryErrorCallable = retryErrorCallable;
		return this;
	}

	public RetryRunner onFinalError(RetryErrorCallable retryErrorCallable) {
		this.finalErrorCallable = retryErrorCallable;
		return this;
	}

	public RetryRunner thenThrow() {
		this.throwException = true;
		return this;
	}

	public <V> V run(Callable<V> vCallable) {
		int tries = 0;
		while (true) {
			try {
				return vCallable.call();
			} catch (Exception e) {
				Class<?> eClass = e.getClass();
				if (retryErrorCallable != null) {
					retryErrorCallable.onError(e);
				}
				boolean needTry = false;
				if (!CollectionUtils.isEmpty(includeExceptions)) {
					needTry = includeExceptions.stream().anyMatch(aClass -> aClass.isAssignableFrom(eClass));
				}
				if (!CollectionUtils.isEmpty(excludeExceptions)) {
					needTry = !excludeExceptions.stream().anyMatch(aClass -> aClass.isAssignableFrom(eClass));
				}
				boolean emitError = false;
				if (needTry) {
					if (tries++ < retryLimit) {
						if (sleep > 0) {
							try {
								Thread.sleep(sleep);
							} catch (InterruptedException ig) {
								Thread.currentThread().interrupt();
							}
						}
					} else {
						emitError = true;

					}
				} else {
					emitError = true;
				}
				if (emitError) {
					LOGGER.warn(String.format("retried %d times... ", tries), e);
					if (retryErrorCallable != null) {
						try {
							retryErrorCallable.onError(e);
						} catch (Exception e1) {
							LOGGER.warn("call retryErrorCallable error...", e);
							throw e1;
						}
					}
					if (throwException) {
						throw new RuntimeException(e);
					} else {
						return null;
					}
				}
			}
		}

	}

	public interface RetryErrorCallable {
		void onError(Exception e);
	}

}
