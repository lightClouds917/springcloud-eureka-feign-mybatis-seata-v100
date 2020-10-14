package io.seata.sample.service;

import io.seata.sample.dao.StorageDao;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author IT云清
 */
@Service("storageServiceImpl")
public class StorageServiceImpl implements StorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageServiceImpl.class);

    @Autowired
    private StorageDao storageDao;

    /**
     * 扣减库存
     * @param productId 产品id
     * @param count 数量
     * @return
     */
    @Override
    public void decrease(Long productId, Integer count) {
        LOGGER.info("------->扣减库存开始");
        //模拟服务超时
        if(count == 15){
            try {
                LOGGER.info("模拟超时"+ LocalDateTime.now().toString());
                Thread.sleep(45000);
                LOGGER.info("模拟超时"+ LocalDateTime.now().toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        storageDao.decrease(productId,count);
        storageDao.decrease2(productId,count);
        storageDao.decrease3(productId,count);
        storageDao.decrease4(productId,count);
        storageDao.decrease5(productId,count);
        LOGGER.info("------->扣减库存结束");
    }

    @Override
    public String getById(Long productId) {
        Integer used = storageDao.getById(productId);
        return used.toString();
    }
}
