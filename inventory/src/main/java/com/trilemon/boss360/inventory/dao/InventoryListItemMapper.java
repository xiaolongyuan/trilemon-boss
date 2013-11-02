package com.trilemon.boss360.inventory.dao;

import com.trilemon.boss360.inventory.model.InventoryListItem;
import com.trilemon.boss360.inventory.model.InventoryListItemExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface InventoryListItemMapper {
    int countByExample(InventoryListItemExample example);

    int deleteByExample(InventoryListItemExample example);

    int deleteByPrimaryKey(Long id);

    int insert(InventoryListItem record);

    int insertSelective(InventoryListItem record);

    List<InventoryListItem> selectByExample(InventoryListItemExample example);

    InventoryListItem selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") InventoryListItem record, @Param("example") InventoryListItemExample example);

    int updateByExample(@Param("record") InventoryListItem record, @Param("example") InventoryListItemExample example);

    int updateByPrimaryKeySelective(InventoryListItem record);

    int updateByPrimaryKey(InventoryListItem record);
}