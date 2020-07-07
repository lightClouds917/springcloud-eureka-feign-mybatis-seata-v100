package io.seata.sample.dao;

import org.springframework.stereotype.Repository;

/**
 * @author wangzhongxiang
 * @date 2020年07月07日 13:56:08
 */
@Repository
public interface AppleDao {

    void updateCount(Integer count);

}
