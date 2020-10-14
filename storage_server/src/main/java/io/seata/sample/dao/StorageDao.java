package io.seata.sample.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * @author IT云清
 */
@Repository
public interface StorageDao {

    /**
     * 扣减库存
     * @param productId 产品id
     * @param count 数量
     * @return
     */
    void decrease(@Param("productId") Long productId, @Param("count") Integer count);
    void decrease2(@Param("productId") Long productId, @Param("count") Integer count);
    void decrease3(@Param("productId") Long productId, @Param("count") Integer count);
    void decrease4(@Param("productId") Long productId, @Param("count") Integer count);
    void decrease5(@Param("productId") Long productId, @Param("count") Integer count);

    Integer getById(Long productId);
}
