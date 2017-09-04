package io.shuidi.snowflake.core.report;

import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;

/**
 * Author: Alvin Tian
 * Date: 2017/8/30 11:36
 */
public final class ReporterHolder {

	public static final MetricRegistry metrics = new MetricRegistry();
	public static final Counter exceptionCounter = metrics.counter("exceptions");

	static {
		final JmxReporter reporter = JmxReporter.forRegistry(metrics).build();
		reporter.start();
	}

	public static final void incException(Exception e) {
		exceptionCounter.inc();
		metrics.counter("exceptions." + e.getClass().getName()).inc();
	}

	public static final void incException(Class e) {
		exceptionCounter.inc();
		metrics.counter("exceptions." + e.getName()).inc();
	}

}
