package com.apollocurrency.aplwallet.apl.exchange.service.graph;

import com.apollocurrency.aplwallet.api.trading.SimpleTradingEntry;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutput;
import com.apollocurrency.aplwallet.apl.data.DexTradingTestData;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexCandlestickDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCandlestick;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderDbIdPaginationDbRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.apollocurrency.aplwallet.apl.exchange.service.graph.CandlestickTestUtil.apl;
import static com.apollocurrency.aplwallet.apl.exchange.service.graph.CandlestickTestUtil.dec;
import static com.apollocurrency.aplwallet.apl.exchange.service.graph.CandlestickTestUtil.empty;
import static com.apollocurrency.aplwallet.apl.exchange.service.graph.CandlestickTestUtil.fromCandlestick;
import static com.apollocurrency.aplwallet.apl.exchange.service.graph.CandlestickTestUtil.fromRawData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class DexTradingDataServiceTest {
    @Mock
    DexCandlestickDao candlestickDao;
    @Mock
    DexOrderDao orderDao;

    DexTradingDataService service;
    DexTradingTestData td = new DexTradingTestData();


    @BeforeEach
    void setUp() {
        service = new DexTradingDataService(true, false, candlestickDao, orderDao, 2);
    }

    @Test
    void testGetForTimeFrameFromCandlesticks() {
        int toTimestamp = 1574857800;
        int fromTimestamp = 1574853300;
        List<DexCandlestick> candlesticks = List.of(td.ETH_3_CANDLESTICK, td.ETH_4_CANDLESTICK);
        doReturn(candlesticks).when(candlestickDao).getForTimespan(fromTimestamp + 900, toTimestamp, DexCurrency.ETH);
        List<SimpleTradingEntry> tradingEntries = service.getFromCandlesticks(toTimestamp, 5, DexCurrency.ETH, TimeFrame.QUARTER);

        List<SimpleTradingEntry> expected = List.of(empty(fromTimestamp + 900), fromCandlestick(td.ETH_3_CANDLESTICK), empty(fromTimestamp + 2700), empty(fromTimestamp + 3600), fromCandlestick(td.ETH_4_CANDLESTICK));

        assertEquals(expected, tradingEntries);
    }
    @Test
    void testGetForTimeFrameFromCandlesticksFor1HTimeFrame() {
        int fromTimestamp = 1574848800;
        int toTimestamp   = 1574863200;
        List<DexCandlestick> candlesticks = List.of(td.ETH_0_CANDLESTICK, td.ETH_1_CANDLESTICK, td.ETH_2_CANDLESTICK, td.ETH_3_CANDLESTICK, td.ETH_4_CANDLESTICK, td.ETH_5_CANDLESTICK, td.ETH_6_CANDLESTICK, td.ETH_7_CANDLESTICK, td.ETH_8_CANDLESTICK);

        doReturn(candlesticks).when(candlestickDao).getForTimespan(fromTimestamp, toTimestamp, DexCurrency.ETH);
        List<SimpleTradingEntry> tradingEntries = service.getFromCandlesticks(toTimestamp, 5, DexCurrency.ETH, TimeFrame.HOUR);

        List<SimpleTradingEntry> expected = List.of(
                fromRawData("0.0000032"    ,"0.0000041" ,"0.0000034"   ,"0.0000039" ,"1559000"          , "1114.9034283" , 1574848800),
                fromRawData("0.0000031", "0.0000043", "0.0000039", "0.0000034", "2447800","1579.469088", 1574852400),
                fromRawData("0.0000031", "0.0000036", "0.0000035", "0.0000033", "945246","636.790145", 1574856000),
                fromRawData("0.0000025", "0.0000036", "0.0000033", "0.0000031", "3464000","2254.168052", 1574859600),
                empty(toTimestamp));

        assertEquals(expected, tradingEntries);
    }



    @Test
    void testGetFor1HTimeFrameFromOrders() {
        int initialTimestamp = 10_800;
        int toTimestamp = 22_000;
        List<DexOrder> firstIterationOrders = List.of(CandlestickTestUtil.eOrder(1, 10_805, dec("1.5"), apl(100_000)), CandlestickTestUtil.eOrder(2, 10_801, dec("1"), apl(500_000)));
        List<DexOrder> secondIterationOrders = List.of(CandlestickTestUtil.eOrder(3, 12_500, dec("1.2"), apl(200_000)), CandlestickTestUtil.eOrder(4, 11_309, dec("1.1"), apl(400_000)));
        List<DexOrder> thirdIterationOrders = List.of(CandlestickTestUtil.eOrder(5, 14_401, dec("1.1"), apl(100_000)), CandlestickTestUtil.eOrder(6, 14_399, dec("1"), apl(100_000)));
        List<DexOrder> fourthIterationOrders = List.of(CandlestickTestUtil.eOrder(7, 18_001, dec("3"), apl(200_000)), CandlestickTestUtil.eOrder(8, 14_500, dec("2"), apl(150_000)));
        List<DexOrder> fifthIterationOrders = List.of(CandlestickTestUtil.eOrder(9, 19_100, dec("2"), apl(500_000)));
        doReturn(firstIterationOrders).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(0, initialTimestamp, toTimestamp, 2));
        doReturn(secondIterationOrders).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(2, initialTimestamp, toTimestamp, 2));
        doReturn(thirdIterationOrders).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(4, initialTimestamp, toTimestamp, 2));
        doReturn(fourthIterationOrders).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(6, initialTimestamp, toTimestamp, 2));
        doReturn(fifthIterationOrders).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(8, initialTimestamp, toTimestamp, 2));

        List<SimpleTradingEntry> orders = service.getForTimeFrameFromDexOrders(initialTimestamp, toTimestamp, DexCurrency.ETH, TimeFrame.HOUR);

        List<SimpleTradingEntry> expected = List.of(
                fromRawData("1", "1.5", "1", "1", "1300000", "1430000.0", initialTimestamp),
                fromRawData("1.1", "2", "1.1", "2", "250000", "410000.0", 14400),
                fromRawData("2", "3", "3", "2", "700000", "1600000", 18000),
                empty(21600)

        );
        assertEquals(expected, orders);
    }


    @Test
    void testGetFor4HTimeFrameFromOrders() {
        int initialTimestamp = 14_400;
        int toTimestamp = 28_800;
        List<DexOrder> firstIterationOrders = List.of(CandlestickTestUtil.eOrder(1, 15_700, dec("1.2"), apl(200_000)), CandlestickTestUtil.eOrder(2, 18_800, dec("1"), apl(500_000)));
        doReturn(firstIterationOrders).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(0, initialTimestamp, toTimestamp, 2));
        doReturn(List.of()).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(2, initialTimestamp, toTimestamp, 2));

        List<SimpleTradingEntry> orders = service.getForTimeFrameFromDexOrders(initialTimestamp, toTimestamp, DexCurrency.ETH, TimeFrame.FOUR_HOURS);

        List<SimpleTradingEntry> expected = List.of(
                fromRawData("1", "1.2", "1.2", "1", "700000", "740000.0", initialTimestamp),
                empty(28800)

        );
        assertEquals(expected, orders);
    }

    @Test
    void testGetFromOrdersWithoutOrders() {
        int initialTimestamp = 7200;
        int toTimestamp = 14400;
        doReturn(List.of()).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(0, initialTimestamp, toTimestamp, 2));

        List<SimpleTradingEntry> orders = service.getForTimeFrameFromDexOrders(initialTimestamp, toTimestamp, DexCurrency.ETH, TimeFrame.HOUR);

        List<SimpleTradingEntry> expected = List.of(
                empty(7200),
                empty(10800),
                empty(14400)
        );
        assertEquals(expected, orders);
    }

    @Test
    void testGetForTimeFrameFromCandlesticksWithZeroLimit() {
        assertThrows(IllegalArgumentException.class, () -> service.getFromCandlesticks(100, 0, DexCurrency.ETH, TimeFrame.HOUR));
    }

    @Test
    void testGetForTimeFrameOnlyFromOrders() {
        int initialTimestamp = 3400;
        int toTimestamp = 14200;
        // +1 for order finishTime to coup with fromEpochTime/toEpochTime offset (500ms)
        doReturn(List.of(CandlestickTestUtil.eOrder(1, 7801, dec("2.1"), apl(90_000)), CandlestickTestUtil.eOrder(2, 7601, dec("2"), apl(100_000)))).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(0, initialTimestamp + 200, toTimestamp, 2));
        doReturn(List.of(CandlestickTestUtil.eOrder(3, 8500, dec("1.4"), apl(120_000)), CandlestickTestUtil.eOrder(4, 8500, dec("2.5"), apl(200_000)))).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(2, initialTimestamp + 200, toTimestamp, 2));
        doReturn(List.of(CandlestickTestUtil.eOrder(5, 11500, dec("1.2"), apl(150_000)))).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(4, initialTimestamp + 200, toTimestamp, 2));

        TradingDataOutput dataOutput = service.getForTimeFrame(toTimestamp, 3, DexCurrency.ETH, TimeFrame.HOUR);

        List<SimpleTradingEntry> expected = List.of(
                empty(3600),
                fromRawData("1.4", "2.5", "2", "1.4", "510000", "1057000.0", 7200),
                fromRawData("1.2", "1.2", "1.2", "1.2", "150000", "180000.0", 10800)
        );
        assertEquals(expected, dataOutput.getData());
        assertEquals(initialTimestamp, dataOutput.getTimeFrom());
    }
    @Test
    void testGetForTimeFrameOnlyFromOrdersWhenLastCandlestickTimeIsLessThanStartTime() {
        int initialTimestamp = 6400;
        int toTimestamp = 9100;
        // +1 for order finishTime to coup with fromEpochTime/toEpochTime offset (500ms)
        doReturn(new DexCandlestick(DexCurrency.ETH, dec("2"), dec("2"), dec("2"), dec("2"), dec("100000"), dec("20000"), 8099, 8099, 8099)).when(candlestickDao).getLast(DexCurrency.ETH);
        doReturn(List.of(CandlestickTestUtil.eOrder(1, 7201, dec("2"), apl(100_000)), CandlestickTestUtil.eOrder(2, 7601, dec("2.2"), apl(200_000)))).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(0, initialTimestamp + 800, toTimestamp, 2));
        doReturn(List.of(CandlestickTestUtil.eOrder(3, 8500, dec("1.8"), apl(120_000)), CandlestickTestUtil.eOrder(4, 8500, dec("2.5"), apl(300_000)))).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(2, initialTimestamp + 800, toTimestamp, 2));
        doReturn(List.of(CandlestickTestUtil.eOrder(5, 8500, dec("1.2"), apl(150_000)))).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(4, initialTimestamp + 800, toTimestamp, 2));

        TradingDataOutput dataOutput = service.getForTimeFrame(toTimestamp, 3, DexCurrency.ETH, TimeFrame.QUARTER);

        List<SimpleTradingEntry> expected = List.of(
                fromRawData("2", "2.2", "2", "2.2", "300000", "640000.0", 7200),
                fromRawData("1.2", "2.5", "1.8", "2.5", "570000", "1146000.0", 8100),
                empty(9000)
                );
        assertEquals(expected, dataOutput.getData());
        assertEquals(initialTimestamp, dataOutput.getTimeFrom());
    }

    @Test
    void testGetForTimeFrameOnlyFromCandlesticks() {
        int fromTimestamp = 1_574_852_399;
        int toTimestamp   = 1_574_866_799;
        List<DexCandlestick> candlesticks = List.of(td.ETH_1_CANDLESTICK, td.ETH_2_CANDLESTICK, td.ETH_3_CANDLESTICK, td.ETH_4_CANDLESTICK, td.ETH_5_CANDLESTICK, td.ETH_6_CANDLESTICK, td.ETH_7_CANDLESTICK, td.ETH_8_CANDLESTICK);

        doReturn(new DexCandlestick(DexCurrency.ETH, dec("2"), dec("2"), dec("2"), dec("2"), dec("100000"), dec("20000"), 1_574_866_800, 1_574_866_820, 1_574_866_900)).when(candlestickDao).getLast(DexCurrency.ETH);
        doReturn(candlesticks).when(candlestickDao).getForTimespan(fromTimestamp + 1, toTimestamp, DexCurrency.ETH);
        TradingDataOutput dataOutput = service.getForTimeFrame(toTimestamp, 4, DexCurrency.ETH, TimeFrame.HOUR);

        List<SimpleTradingEntry> expected = List.of(
                fromRawData("0.0000031", "0.0000043", "0.0000039", "0.0000034", "2447800","1579.469088", 1574852400),
                fromRawData("0.0000031", "0.0000036", "0.0000035", "0.0000033", "945246","636.790145", 1574856000),
                fromRawData("0.0000025", "0.0000036", "0.0000033", "0.0000031", "3464000","2254.168052", 1574859600),
                empty(1_574_863_200));

        assertEquals(expected, dataOutput.getData());
        assertEquals(fromTimestamp, dataOutput.getTimeFrom());
    }

    @Test
    void testGetForTimeFrameFromOrdersAndCandlesticks() {
        int fromTimestamp = 1_574_851_800;
        int toTimestamp   = 1_574_877_000;
        int candlestickOrderSeparationTime = 1_574_863_200;
        List<DexCandlestick> candlesticks = List.of(td.ETH_1_CANDLESTICK, td.ETH_2_CANDLESTICK, td.ETH_3_CANDLESTICK, td.ETH_4_CANDLESTICK, td.ETH_5_CANDLESTICK, td.ETH_6_CANDLESTICK, td.ETH_7_CANDLESTICK, td.ETH_8_CANDLESTICK);

        doReturn(td.ETH_9_CANDLESTICK).when(candlestickDao).getLast(DexCurrency.ETH);
        doReturn(candlesticks).when(candlestickDao).getForTimespan(fromTimestamp + 600, candlestickOrderSeparationTime - 1, DexCurrency.ETH);
        // +1 for order finishTime to coup with fromEpochTime/toEpochTime offset (500ms)
        doReturn(List.of(CandlestickTestUtil.eOrder(1, 1_574_863_201, dec("2.5") , apl(120_000)), CandlestickTestUtil.eOrder(3, 1_574_865_200, dec("2.3"),      apl(100_000)))).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(0,  candlestickOrderSeparationTime, toTimestamp, 2));
        doReturn(List.of(CandlestickTestUtil.eOrder(4, 1_574_870_801, dec("2.2") , apl(150_000)), CandlestickTestUtil.eOrder(5, 1_574_872_333, dec("2.1"),      apl(300_000)))).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(3,  candlestickOrderSeparationTime, toTimestamp, 2));
        doReturn(List.of(CandlestickTestUtil.eOrder(6, 1_574_865_200, dec("2.15"), apl(200_000)), CandlestickTestUtil.eOrder(7, 1_574_866_100, dec("2.33333"), apl(400_000)))).when(orderDao).getOrdersFromDbIdBetweenTimestamps(request(5,  candlestickOrderSeparationTime, toTimestamp, 2));


        TradingDataOutput dataOutput = service.getForTimeFrame(toTimestamp, 7, DexCurrency.ETH, TimeFrame.HOUR);

        List<SimpleTradingEntry> expected = List.of(
                fromRawData("0.0000031", "0.0000043", "0.0000039", "0.0000034", "2447800","1579.469088", 1574852400),
                fromRawData("0.0000031", "0.0000036", "0.0000035", "0.0000033", "945246","636.790145", 1574856000),
                fromRawData("0.0000025", "0.0000036", "0.0000033", "0.0000031", "3464000","2254.168052", 1574859600),
                fromRawData("2.15", "2.5", "2.5", "2.33333", "820000", "1893332.00000", 1_574_863_200),
                empty(1_574_866_800),
                fromRawData("2.1", "2.2", "2.2", "2.1", "450000", "960000.0", 1_574_870_400),
                empty(1_574_874_000)
                );

        assertEquals(expected, dataOutput.getData());
        assertEquals(fromTimestamp, dataOutput.getTimeFrom());
    }

    private OrderDbIdPaginationDbRequest request(long fromDbId, int fromTimestamp, int toTimestamp, int limit) {
        return OrderDbIdPaginationDbRequest.builder()
                .fromTime(fromTimestamp)
                .fromDbId(fromDbId)
                .toTime(toTimestamp)
                .limit(limit)
                .coin(DexCurrency.ETH)
                .build();
    }
}