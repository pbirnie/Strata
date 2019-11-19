/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.examples.marketdata.ExampleData;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.ExampleMarketDataBuilder;
import com.opengamma.strata.loader.fpml.FpmlDocumentParser;
import com.opengamma.strata.loader.fpml.FpmlPartySelector;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fx.FxSwap;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.payment.BulletPayment;
import com.opengamma.strata.product.payment.BulletPaymentTrade;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;
import org.joda.beans.Bean;
import org.joda.beans.ser.JodaBeanSer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;

/**
 * Example to illustrate using the engine to price FX trades.
 * <p>
 * This makes use of the example engine and the example market data environment.
 * A bullet payment trade is also included.
 */

public class FpmlToFxPricingExample {

  private static final Logger log = LoggerFactory.getLogger(FpmlToFxPricingExample.class);


  /**
   * Runs the example, pricing the instruments, producing the output as an ASCII table.
   * 
   * @param args  ignored
   */
  public static void main(String[] args) {
    // setup calculation runner component, which needs life-cycle management
    // a typical application might use dependency injection to obtain the instance
    try (CalculationRunner runner = CalculationRunner.ofMultiThreaded()) {
      calculate(runner);
    }
  }

  public static List<Trade> loadFpml(String resourcePath, String partyId){

    ByteSource resource = ResourceLocator.of(resourcePath).getByteSource();
    return convert(resource, partyId);
  }

  /**
   * Loader configuration
   * The FpML loader requires an FpmlPartySelector
   * This is necessary as the FpML data structure is neutral as to the direction of a trade. Instead of referring
   * o “pay” and “receive”, it declares “party A pays” and party B receives. The Strata data model takes the opposite view, with each trade stored with
   * Pay/Receive or Buy/Sell concepts expressed from “our” point of view. The selector is used to bridge the gap between the two. As such, the job of the
   * selector is to pick the party that represents “us” in the FpML data.
   *
   * Advanced use cases may require configuring the FpML loader further. See FpmlTradeInfoParserPlugin and FpmlParserPlugin.
   */
  public static List<Trade> convert(ByteSource source, String partyId){

    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.matching(partyId));
    List<Trade> trades = parser.parseTrades(source);

    return trades;
  }

  // obtains the data and calculates the grid of results
  private static void calculate(CalculationRunner runner) {

    String location = "classpath:example-fpml/fx-ex01-fx-spot.xml";


    List<Trade> fpmlTrades =  loadFpml(location, "Party2");

    // the trades that will have measures calculated
    List<Trade> trades = ImmutableList.of(createTrade1(), createTrade2(), createTrade3(), createTrade4(), fpmlTrades.get(0));


    for (Trade trade : trades){

      String json = JodaBeanSer.PRETTY.jsonWriter().write((Bean)trade);
      log.info("trade={}", json);
    }

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.PRESENT_VALUE),
        Column.of(Measures.PV01_CALIBRATED_SUM),
        Column.of(Measures.PV01_CALIBRATED_BUCKETED));

    // use the built-in example market data
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    ExampleMarketDataBuilder marketDataBuilder = ExampleMarketData.builder();
    MarketData marketData = marketDataBuilder.buildSnapshot(valuationDate);

    // the complete set of rules for calculating measures
    CalculationFunctions functions = StandardComponents.calculationFunctions();
    CalculationRules rules = CalculationRules.of(functions, marketDataBuilder.ratesLookup(valuationDate));

    // the reference data, such as holidays and securities
    ReferenceData refData = ReferenceData.standard();

    // calculate the results
    Results results = runner.calculate(rules, trades, columns, marketData, refData);

    // use the report runner to transform the engine results into a trade report
    ReportCalculationResults calculationResults =
        ReportCalculationResults.of(valuationDate, trades, columns, results, functions, refData);

    TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("fx-report-template");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
    tradeReport.writeAsciiTable(System.out);
  }

  //-----------------------------------------------------------------------  
  // create an FX Forward trade
  private static Trade createTrade1() {
    FxSingle fx = FxSingle.of(CurrencyAmount.of(GBP, 10000), FxRate.of(GBP, USD, 1.62), LocalDate.of(2014, 9, 14));
    return FxSingleTrade.builder()
        .product(fx)
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "1"))
            .addAttribute(AttributeType.DESCRIPTION, "GBP 10,000/USD @ 1.62 fwd")
            .counterparty(StandardId.of("example", "BigBankA"))
            .settlementDate(LocalDate.of(2014, 9, 15))
            .build())
        .build();
  }

  // create an FX Forward trade
  private static Trade createTrade2() {
    FxSingle fx = FxSingle.of(CurrencyAmount.of(USD, 15000), FxRate.of(GBP, USD, 1.62), LocalDate.of(2014, 9, 14));
    return FxSingleTrade.builder()
        .product(fx)
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "2"))
            .addAttribute(AttributeType.DESCRIPTION, "USD 15,000/GBP @ 1.62 fwd")
            .counterparty(StandardId.of("example", "BigBankB"))
            .settlementDate(LocalDate.of(2014, 9, 15))
            .build())
        .build();
  }

  // create an FX Swap trade
  private static Trade createTrade3() {
    FxSwap swap = FxSwap.ofForwardPoints(
        CurrencyAmount.of(GBP, 10000), FxRate.of(GBP, USD, 1.62), 0.03, LocalDate.of(2014, 6, 14), LocalDate.of(2014, 9, 14));
    return FxSwapTrade.builder()
        .product(swap)
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "3"))
            .addAttribute(AttributeType.DESCRIPTION, "GBP 10,000/USD @ 1.62 swap")
            .counterparty(StandardId.of("example", "BigBankA"))
            .settlementDate(LocalDate.of(2014, 9, 15))
            .build())
        .build();
  }

  // create a Bullet Payment trade
  private static Trade createTrade4() {
    BulletPayment bp = BulletPayment.builder()
        .payReceive(PayReceive.PAY)
        .value(CurrencyAmount.of(GBP, 20_000))
        .date(AdjustableDate.of(LocalDate.of(2014, 9, 16)))
        .build();
    return BulletPaymentTrade.builder()
        .product(bp)
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "4"))
            .addAttribute(AttributeType.DESCRIPTION, "Bullet payment GBP 20,000")
            .counterparty(StandardId.of("example", "BigBankC"))
            .settlementDate(LocalDate.of(2014, 9, 16))
            .build())
        .build();
  }

}
