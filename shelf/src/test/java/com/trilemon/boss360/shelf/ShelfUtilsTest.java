package com.trilemon.boss360.shelf;

import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.taobao.api.domain.Item;
import com.trilemon.commons.LocalTimeInterval;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author kevin
 */
public class ShelfUtilsTest {

    @Test
    public void testParseDistribution() {
        Table<Integer, LocalTimeInterval, Integer> table = ShelfUtils.parseAndFillZeroDistribution("1|9:00-10:00||2|9:00-10:00||2|10:00-11:00");
        assertEquals(2, table.rowKeySet().size());
        assertEquals(2, table.columnKeySet().size());
        assertEquals(3, table.cellSet().size());
        assertEquals(0, table.get(1, new LocalTimeInterval(9, 10)).intValue());
        assertEquals(0, table.get(2, new LocalTimeInterval(9, 10)).intValue());
        assertEquals(0, table.get(2, new LocalTimeInterval(10, 11)).intValue());
    }

    @Test
    public void testGetDefaultDistribution_0() {
        Table<Integer, LocalTimeInterval, Integer> table = ShelfUtils.getDefaultDistribution(0);
        assertTrue(table.isEmpty());
    }

    @Test
    public void testGetDefaultDistribution_1() {
        Table<Integer, LocalTimeInterval, Integer> table = ShelfUtils.getDefaultDistribution(1);
        assertEquals(1, table.get(1, new LocalTimeInterval(9, 10)).intValue());
    }

    @Test
    public void testGetDefaultDistribution_15() {
        Table<Integer, LocalTimeInterval, Integer> table = ShelfUtils.getDefaultDistribution(15);
        assertEquals(1, table.get(1, new LocalTimeInterval(9, 10)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(10, 11)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(11, 12)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(12, 13)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(13, 14)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(14, 15)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(15, 16)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(16, 17)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(17, 18)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(18, 19)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(19, 20)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(20, 21)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(21, 22)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(22, 23)).intValue());
        assertEquals(1, table.get(2, new LocalTimeInterval(9, 10)).intValue());
    }

    @Test
    public void testGetDefaultDistribution_98() {
        Table<Integer, LocalTimeInterval, Integer> table = ShelfUtils.getDefaultDistribution(98);
        assertEquals(98, table.size());
    }

    @Test
    public void testGetDefaultDistribution_99() {
        Table<Integer, LocalTimeInterval, Integer> table = ShelfUtils.getDefaultDistribution(99);
        assertEquals(98, table.size());
        assertEquals(2, table.get(1, new LocalTimeInterval(9, 10)).intValue());
    }

    @Test
    public void testAssignItems() {
        List<Item> items = Lists.newArrayList(new Item(), new Item());
        Table<Integer, LocalTimeInterval, Integer> table = ShelfUtils.getDefaultDistribution(items.size());
        Table<Integer, LocalTimeInterval, List<Item>> assignTable = ShelfUtils.assignItems(items, table);
        assertEquals(2, assignTable.size());
        assertEquals(Lists.newArrayList(items.get(0)), assignTable.get(1, new LocalTimeInterval(9, 10)));
        assertEquals(Lists.newArrayList(items.get(1)), assignTable.get(1, new LocalTimeInterval(10, 11)));
    }

    @Test
    public void testParseAndFillZeroDistribution_empty() {
        Table<Integer, LocalTimeInterval, Integer> table = ShelfUtils.parseAndFillZeroDistribution("");
        assertTrue(table.isEmpty());
    }

    @Test
    public void testParseAndFillZeroDistribution() {
        Table<Integer, LocalTimeInterval, Integer> table = ShelfUtils.parseAndFillZeroDistribution
                ("1|9:00-10:00||1|10:00-12:00||2|9:00-10:00||1|22:00-23:00");
        assertEquals(4, table.size());
        assertEquals(0, table.get(1, new LocalTimeInterval(9, 10)).intValue());
        assertEquals(0, table.get(1, new LocalTimeInterval(10, 12)).intValue());
        assertEquals(0, table.get(1, new LocalTimeInterval(22, 23)).intValue());
        assertEquals(0, table.get(2, new LocalTimeInterval(9, 10)).intValue());
    }

    @Test
    public void testFindMinCellOfDistribution() {
        Table<Integer, LocalTimeInterval, Integer> table=ShelfUtils.parseAndFillZeroDistribution
                ("1|9:00-10:00||1|10:00-11:00||2|9:00-10:00");
        table.put(1,new LocalTimeInterval(9, 10),2);
        table.put(1,new LocalTimeInterval(10, 11),3);
        table.put(2,new LocalTimeInterval(9, 10),1);

        Table.Cell<Integer, LocalTimeInterval, Integer> minCell=ShelfUtils.findMinCellOfDistribution(table);
        assertEquals(1, minCell.getValue().intValue());
    }

    @Test
    public void testGetNewItemDistribution() {
        Table<Integer, LocalTimeInterval, Integer> planDistribution=ShelfUtils.getDefaultDistribution(10);

        Table<Integer, LocalTimeInterval, Integer> currDistribution=ShelfUtils.parseAndFillZeroDistribution
                ("1|9:00-10:00||1|10:00-11:00||2|9:00-10:00");
        currDistribution.put(1,new LocalTimeInterval(9, 10),1);
        currDistribution.put(1,new LocalTimeInterval(10, 11),1);
        currDistribution.put(2,new LocalTimeInterval(9, 10),2);

        Table<Integer, LocalTimeInterval, Integer> table = ShelfUtils.getNewItemDistribution
                (10,planDistribution,currDistribution);
        assertEquals(1, table.get(1, new LocalTimeInterval(9, 10)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(10, 11)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(11, 12)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(12, 13)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(13, 14)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(14, 15)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(15, 16)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(16, 17)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(17, 18)).intValue());
        assertEquals(1, table.get(1, new LocalTimeInterval(18, 19)).intValue());
    }
}
