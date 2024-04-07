
package com.crio.warmup.stock.portfolio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.impl.ObjectIdReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  RestTemplate restTemplate;


  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF




  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
        //get stocks available from start to end date
        //throw error if start date not before end date
      // if(from.compareTo(to)>=0){
      //   throw new RuntimeException();
      // } 
       //create url for the Api call 
      String url=buildUri(symbol, from, to);
      //api returns a list of result for each days stocks
      String response =restTemplate.getForObject(url, String.class);
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      Candle[] stocks = objectMapper.readValue(response, TiingoCandle[].class);
      //if stocks is null return an emty list
      if(stocks==null){
        return new ArrayList<Candle>();
      }else{
        List<Candle> stocksList=Arrays.asList(stocks);
        return stocksList;
      }
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
      //  String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
      //       + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
      String token="74716ebf5e20ea7a2e40799078d58c6cec642237";
      String uri = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
        + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
    return uri.replace("$APIKEY", token).replace("$SYMBOL", symbol)
        .replace("$STARTDATE", startDate.toString())
        .replace("$ENDDATE", endDate.toString());
      // String url=uriTemplate.replace("$SYMBOL", symbol).replace("$STARTDATE", startDate.toString())
      // .replace("$ENDDATE", endDate.toString()).replace("$APIKEY", token);
      //return uri;
            
  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) {
        List<AnnualizedReturn> annualizedReturns = new ArrayList<>();

        // Stream processing to populate the list
        portfolioTrades.stream()
                .map(trade -> getAnnualizedReturn(trade, endDate))
                .forEach(annualizedReturns::add);
    
        // Sorting the list
        annualizedReturns.sort(getComparator());
    
        return annualizedReturns;
  
}

public  AnnualizedReturn getAnnualizedReturn(PortfolioTrade trade,LocalDate endDate) {
      AnnualizedReturn annualizedReturn;
      String symbol=trade.getSymbol();
      LocalDate startDate=trade.getPurchaseDate();
      //calculate total return 
      try{
        //fetch data
        List<Candle> stocks;
        stocks=getStockQuote(symbol, startDate, endDate);
        //get start date and enddate from stocks
        Candle stockStartDate=stocks.get(0);
        Candle stockLatest=stocks.get(stocks.size()-1);
        Double buyPrice=stockStartDate.getOpen();
        Double sellPrice=stockLatest.getClose();
        //calculate total returns
        Double totalReturn=(sellPrice-buyPrice)/buyPrice;
        //calculate no.of years
        Double numYears=(double)ChronoUnit.DAYS.between(startDate, endDate)/365.24;
        //calculate annaulized return using formula
        Double annualizedReturns=Math.pow((1+totalReturn),(1/numYears))-1;
        annualizedReturn=new AnnualizedReturn(symbol, annualizedReturns, totalReturn);
      }
      catch(JsonProcessingException e){
        return new AnnualizedReturn(symbol,Double.NaN,Double.NaN);
      }
      return annualizedReturn;
    }
      
      
  }
