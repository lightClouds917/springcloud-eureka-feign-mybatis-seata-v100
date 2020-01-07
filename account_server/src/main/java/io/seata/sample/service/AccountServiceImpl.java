package io.seata.sample.service;

import io.seata.sample.dao.AccountDao;
import io.seata.sample.feign.OrderApi;
import io.seata.spring.annotation.GlobalTransactional;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author IT云清
 */
@Service("accountServiceImpl")
public class AccountServiceImpl implements AccountService{

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountServiceImpl.class);
    private final BigDecimal ERROR_MONEY = new BigDecimal("200");
    @Autowired
    private AccountDao accountDao;

    /**
     * 扣减账户余额
     * @param userId 用户id
     * @param money 金额
     */
    @Override
    @GlobalTransactional
    public void decrease(Long userId, BigDecimal money) {
        LOGGER.info("------->扣减账户开始account中");
        //模拟超时异常，全局事务回滚 条件：money=200
        if(ERROR_MONEY.compareTo(money) == 0){
            throw new RuntimeException("非法参数,money为："+money);
        }
        accountDao.decrease(userId,money);
        LOGGER.info("------->扣减账户结束account中");
    }
}
