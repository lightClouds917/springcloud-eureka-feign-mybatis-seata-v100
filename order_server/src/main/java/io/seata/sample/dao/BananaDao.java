package io.seata.sample.dao;

import org.springframework.stereotype.Repository;

/**
 * @author wangzhongxiang
 * @date 2020年07月07日 13:55:56
 */
@Repository
public interface BananaDao {

    void updateCount(Integer count);

}
