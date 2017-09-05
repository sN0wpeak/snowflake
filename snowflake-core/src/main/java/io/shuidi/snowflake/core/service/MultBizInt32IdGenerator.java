package io.shuidi.snowflake.core.service;

import com.google.common.base.Preconditions;
import io.shuidi.snowflake.core.service.impl.ZKInt32IdGenerator;
import io.shuidi.snowflake.core.util.zk.CuratorFrameworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Author: Alvin Tian
 * Date: 2017/9/5 14:24
 */
public class MultBizInt32IdGenerator {
	private Map<String, ZKInt32IdGenerator> zkInt32IdGeneratorMap = new HashMap<>();
	private String lockPath;
	private String storePath;
	private static Logger LOGGER = LoggerFactory.getLogger(BizStore.class);

	public MultBizInt32IdGenerator(String lockPath, String storePath) {
		Preconditions.checkNotNull(lockPath);
		Preconditions.checkNotNull(storePath);

		this.lockPath = lockPath;
		this.storePath = storePath;

		Map<String, BizInfo> stringBizInfoMap = BizStoreHolder.getBizStore().getBizsMap();
		createIdGen(stringBizInfoMap);

		BizStoreHolder.getBizStore().addListioner(new BizChangedListener() {
			@Override
			public void onAdd(List<String> bizs) {
				createIdGen(bizs.stream().collect(Collectors.toMap(Function.identity(), s -> BizStoreHolder.getBizStore().getBiz(s))));
			}

			@Override
			public void onDelete(List<String> bizs) {

			}

			@Override
			public void onBizInfoChange(Map<String, BizInfo> infoMap) {

			}
		});
	}

	private void createIdGen(Map<String, BizInfo> stringBizInfoMap) {
		for (Map.Entry<String, BizInfo> stringBizInfoEntry : stringBizInfoMap.entrySet()) {
			String biz = stringBizInfoEntry.getKey();
			BizInfo bizInfo = stringBizInfoEntry.getValue();
			if (!zkInt32IdGeneratorMap.containsKey(biz)) {
				LOGGER.info("createIdGen.. lockPath:{}, storePath:{}, biz: {}, bizInfo: {}", lockPath, storePath, bizInfo);
				zkInt32IdGeneratorMap.put(biz,
				                          new ZKInt32IdGenerator(CuratorFrameworkUtils.create(), lockPath, storePath,
				                                                 biz,
				                                                 bizInfo.getStart(),
				                                                 bizInfo.getRangeCount()));
			}
		}

	}

	public int generateId(String useragent) {
		return zkInt32IdGeneratorMap.get(useragent).generateId();
	}


}
