package io.shuidi.snowflake.core.service;

import com.google.common.base.Preconditions;
import io.shuidi.snowflake.core.service.impl.ZKInt32IdGenerator;
import io.shuidi.snowflake.core.util.zk.ZkUtils;
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
public class MultPartnerInt32IdGenerator {
	private Map<String, ZKInt32IdGenerator> zkInt32IdGeneratorMap = new HashMap<>();
	private String lockPath;
	private String sequencePath;
	private static Logger LOGGER = LoggerFactory.getLogger(PartnerStore.class);

	public MultPartnerInt32IdGenerator(String lockPath, String sequencePath) {
		Preconditions.checkNotNull(lockPath);
		Preconditions.checkNotNull(sequencePath);

		this.lockPath = lockPath;
		this.sequencePath = sequencePath;

		Map<String, Partner> stringBizInfoMap = PartnerStoreHolder.getBizStore().getPartnerMap();
		createIdGen(stringBizInfoMap);

		PartnerStoreHolder.getBizStore().addListioner(new PartnerChangedListener() {
			@Override
			public void onAdd(List<String> partners) {
				createIdGen(
						partners.stream()
						        .collect(Collectors.toMap(Function.identity(), s -> PartnerStoreHolder.getBizStore().getPartner(s))));
			}

			@Override
			public void onDelete(List<String> partners) {

			}

			@Override
			public void onBizInfoChange(Map<String, Partner> partnerMap) {

			}
		});
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				for (Map.Entry<String, ZKInt32IdGenerator> zkInt32IdGeneratorMap : zkInt32IdGeneratorMap.entrySet()) {
					zkInt32IdGeneratorMap.getValue().stopRangeSeq();
				}
			}
		}));
	}

	private void createIdGen(Map<String, Partner> stringBizInfoMap) {
		for (Map.Entry<String, Partner> stringBizInfoEntry : stringBizInfoMap.entrySet()) {
			String biz = stringBizInfoEntry.getKey();
			Partner bizInfo = stringBizInfoEntry.getValue();
			if (!zkInt32IdGeneratorMap.containsKey(biz)) {
				LOGGER.info("createIdGen.. lockPath:{}, sequencePath:{}, Partner: {}, bizInfo: {}", lockPath, sequencePath, bizInfo);
				zkInt32IdGeneratorMap.put(biz, new ZKInt32IdGenerator(
						ZkUtils.create(), lockPath, sequencePath,
						biz,
						bizInfo.getStart(),
						bizInfo.getRangeCount()));
			}
		}
	}

	public int generateId(String partnerKey) {
		return zkInt32IdGeneratorMap.get(partnerKey).generateId();
	}


}
