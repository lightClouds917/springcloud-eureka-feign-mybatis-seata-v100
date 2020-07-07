package io.seata.sample.service;

import io.seata.sample.dao.AppleDao;
import io.seata.sample.dao.BananaDao;
import io.seata.sample.dao.OrderDao;
import io.seata.sample.entity.Order;
import io.seata.sample.feign.AccountApi;
import io.seata.sample.feign.StorageApi;
import io.seata.spring.annotation.GlobalTransactional;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author IT云清
 */
@SuppressWarnings("all")
@Service("orderServiceImpl")
public class OrderServiceImpl implements OrderService{

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderDao orderDao;
    @Autowired
    private StorageApi storageApi;
    @Autowired
    private AccountApi accountApi;
    @Autowired
    private BananaDao bananaDao;
    @Autowired
    private AppleDao appleDao;

    /**
     * 创建订单
     * @param order
     * @return
     * 测试结果：
     * 1.添加本地事务：仅仅扣减库存
     * 2.不添加本地事务：创建订单，扣减库存
     */
    @Override
    @GlobalTransactional(name = "fsp_create_order")
    public void create(Order order) {
        LOGGER.info("------->交易开始");
        //本地方法
        orderDao.create(order);

//        storageApi.getById(order.getProductId());

        //远程方法 扣减库存
        storageApi.decrease(order.getProductId(),order.getCount());

        //远程方法 扣减账户余额
        accountApi.decrease(order.getUserId(),order.getMoney());

        LOGGER.info("------->交易结束");
    }


    /**
     * 此方法用于测试本地事务
     * @param count
     */
    @Override
    @Transactional
    public void changeCount(Integer count){
        bananaDao.updateCount(count);
        if(count == 200){
            throw new NullPointerException("测试事务异常");
        }
        appleDao.updateCount(count);
    }

    /**
     * 修改订单状态
     */
    @Override
    public void update(Long userId,BigDecimal money,Integer status) {
        LOGGER.info("修改订单状态，入参为：userId={},money={},status={}",userId,money,status);
        orderDao.update(userId,money,status);
    }
}
