package io.shuidi.snowflake.core.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.management.RuntimeMBeanException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Author: Alvin Tian
 * Date: 2017/8/30 21:11
 */
public class RetryRunner {
	private boolean throwException;
	private RetryErrorCallable retryErrorCallable;

	private RetryRunner() {
	}

	private static Logger LOGGER = LoggerFactory.getLogger(RetryRunner.class);


	private int retryLimit = 3;
	private long sleep = 1000;
	private List<Class<? extends Exception>> tryExceptions = Lists.newArrayList();

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

	public RetryRunner addTryExceptions(Class<? extends Exception>... es) {
		Preconditions.checkArgument(es.length > 0);
		tryExceptions.addAll(Lists.newArrayList(es));
		return this;
	}

	public RetryRunner onError(RetryErrorCallable retryErrorCallable) {
		this.retryErrorCallable = retryErrorCallable;
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
				boolean needTry = false;
				if (!CollectionUtils.isEmpty(tryExceptions)) {
					for (Class<? extends Exception> includeException : tryExceptions) {
						if (e.getClass() == includeException) {
							needTry = true;
						}
					}
				} else {
					needTry = true;
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
					LOGGER.warn(String.format("retried %d times... ", retryLimit), e);
					if (retryErrorCallable != null) {
						try {
							retryErrorCallable.onError(e);
						} catch (Exception e1) {
							LOGGER.warn("call retryErrorCallable error...", e);
						}
					}
					if (throwException) {
						throw new RuntimeException(e);
					}
				}
			}
		}

	}

	public interface RetryErrorCallable {
		void onError(Exception e);
	}

	public static void main(String[] args) {
		RetryRunner.create().addTryExceptions(RuntimeMBeanException.class).run(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				throw new RuntimeException("ceshi");
			}
		});
	}
}
