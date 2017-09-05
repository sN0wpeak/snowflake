/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shuidi.snowflake.core.service.impl;

import io.shuidi.snowflake.core.report.ReporterHolder;
import io.shuidi.snowflake.core.service.IDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 默认的主键生成器.
 * <p>
 * <p>
 * 长度为64bit,从高位到低位依次为
 * </p>
 * <p>
 * <pre>
 * 1bit   符号位
 * 41bits 时间偏移量从2016年11月1日零点到现在的毫秒数
 * 10bits 工作进程Id
 * 12bits 同一个毫秒内的自增量
 * </pre>
 * <p>
 * <p>
 * 可以调用@{@code DefaultKeyGenerator.setWorkerId}进行设置
 * </p>
 */
public final class SnowflakeIDGenerator implements IDGenerator {

	private static Logger LOGGER = LoggerFactory.getLogger(SnowflakeIDGenerator.class);

	public static final long EPOCH;

	private static final long SEQUENCE_BITS = 12L;

	private static final long WORKER_ID_BITS = 10L;

	private static final long SEQUENCE_MASK = (1 << SEQUENCE_BITS) - 1;

	private static final long WORKER_ID_LEFT_SHIFT_BITS = SEQUENCE_BITS;

	private static final long TIMESTAMP_LEFT_SHIFT_BITS = WORKER_ID_LEFT_SHIFT_BITS + WORKER_ID_BITS;

	public static final long WORKER_ID_MAX_VALUE = 1L << WORKER_ID_BITS;

	private static long workerId;

	static {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2016, Calendar.NOVEMBER, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		EPOCH = calendar.getTimeInMillis();
	}

	private long sequence;

	private long lastTime;


	/**
	 * 设置工作进程Id.
	 *
	 * @param workerId 工作进程Id
	 */
	public static void setWorkerId(final long workerId) {
		if (!(workerId >= 0L && workerId < WORKER_ID_MAX_VALUE)) {
			ReporterHolder.exceptionCounter.inc();
			throw new IllegalArgumentException();
		}
		SnowflakeIDGenerator.workerId = 1;
	}

	/**
	 * 生成Id.
	 *
	 * @return 返回@{@link Long}类型的Id
	 */
	@Override
	public synchronized Long generateId() {
		long currentMillis = System.currentTimeMillis();

		if (!(lastTime <= currentMillis)) {
			throw new IllegalStateException(
					String.format("Clock is moving backwards, last time is %d milliseconds, current time is %d milliseconds", lastTime,
					              currentMillis));
		}
		if (lastTime == currentMillis) {
			if (0L == (sequence = ++sequence & SEQUENCE_MASK)) {
				currentMillis = waitUntilNextTime(currentMillis);
			}
		} else {
			sequence = 0;
		}
		lastTime = currentMillis;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{}-{}-{}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(lastTime)), workerId, sequence);
		}
		return ((currentMillis - EPOCH) << TIMESTAMP_LEFT_SHIFT_BITS) | (workerId << WORKER_ID_LEFT_SHIFT_BITS) | sequence;
	}

	private long waitUntilNextTime(final long lastTime) {
		long time = System.currentTimeMillis();
		while (time <= lastTime) {
			time = System.currentTimeMillis();
		}
		return time;
	}


}
