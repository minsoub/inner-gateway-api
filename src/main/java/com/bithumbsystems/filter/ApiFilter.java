package com.bithumbsystems.filter;

import com.bithumbsystems.config.Config;
import com.bithumbsystems.config.constant.GlobalConstant;
import com.bithumbsystems.exception.GatewayException;
import com.bithumbsystems.exception.GatewayExceptionHandler;
import com.bithumbsystems.exception.GatewayStatusException;
import com.bithumbsystems.model.enums.ErrorCode;
import com.bithumbsystems.request.TokenRequest;
import com.bithumbsystems.utils.CommonUtil;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Slf4j
@Component
public class ApiFilter extends AbstractGatewayFilterFactory<Config> {

  @Value("${sites.smart-admin-gateway-url}")
  private String smartAdminGatewayUrl;

  @Value("#{'${sites.lrc-token-ignore}'.split(',')}")
  private List<String> tokenIgnoreLrc;

  @Value("{$deny-list.uris}")
  private List<String> denyUriList;
  @Value("{$deny-list.methods}")
  private List<String> denyMethods;

  private static final Map<String, String> DENY_URI_LIST = Stream.of(new String[][] {
          { "/api/v1/lrc/lrcmanagement/project/submitted-document/file", "POST" },
          { "/api/v1/lrc/lrcmanagement/project/submitted-document/file/{id}",   "DELETE" },
          { "/api/v1/service/chat/file", "POST" },
          { "/api/v1/service/chat/file/{fileKey}", "GET" },
          { "/api/v1/service/chat/excel/export", "GET" },
          { "/api/v1/lrc/files/*", "ALL" },
          { "/api/v1/mng/cpc/board/upload", "POST" },
          { "/api/v1/mng/cpc/board/{boardMasterId}/{boardId}", "PUT" },
          { "/api/v1/mng/cpc/board/{boardMasterId}", "POST" },
          { "/api/v1/cpc/*", "ALL" },
          { "/api/v1/lrc/*", "ALL" },
          { "/api/v1/cms/*", "ALL" },
          { "/user/*", "ALL"}
  }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

  public ApiFilter() {
    super(Config.class);
  }

  @Bean
  public ErrorWebExceptionHandler exceptionHandler() {
    return new GatewayExceptionHandler();
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (exchange, chain) -> {
      log.info("ApiFilter called...");
      log.info("ApiFilter baseMessage: {}", config.getBaseMessage());

      if (config.isPreLogger()) {
        log.info("ApiFilter Start: {}", exchange.getRequest());
      }

      ServerHttpRequest request = exchange.getRequest();
      log.debug("header => {}", request.getHeaders());
      log.debug("host => {}", request.getURI().getHost());
      // 사용자 IP check
      String userIp = CommonUtil.getUserIp(request);
      log.debug("user IP => {}", userIp);

      String siteId = validateRequest(request);
      log.debug("site_id => {}", siteId);
      String mySiteId = validateRequestMySiteId(request);
      log.debug("my_site_id => {}", mySiteId);
      String host = request.getHeaders().getOrigin(); // 없을 수도 있다.

      if (host != null) {
        if (host.contains(":")) {
          host = host.substring(0, host.indexOf(":"));
          validateDomains(config, siteId, host);
        } else {
          validateDomains(config, siteId, host);  // host.substring(0, host.length() - 1)); // 마지막 문자 '/' 제거
        }
      }else {
        throw new GatewayException(ErrorCode.INVALID_ORIGIN_DOMAIN);
      }

      AtomicReference<String> goUrl = new AtomicReference<>(smartAdminGatewayUrl);

      // Header에 user_ip를 넣어야 한다.
      log.debug(exchange.getRequest().getURI().toString());
      log.debug(exchange.getRequest().getURI().getHost());
      log.debug(exchange.getRequest().getURI().getPath());
      log.debug(exchange.getRequest().getURI().getRawQuery());
      log.debug(exchange.getRequest().getURI().getRawPath());

      // 접근 가능한 URI Check
      if (!validationUriCheck(exchange.getRequest())) {
        throw new GatewayException(ErrorCode.INVALID_URI_PATH);
      }



      String replaceUrl = goUrl.get() + exchange.getRequest().getURI().getPath();
      if (StringUtils.hasLength(exchange.getRequest().getURI().getQuery())) {
        replaceUrl += "?" + exchange.getRequest().getURI().getRawQuery();
      }
      log.debug("replaceUrl:" + replaceUrl);

      return chain.filter(exchange).doOnError(e -> {
        log.error(e.getMessage());
        throw new GatewayException(ErrorCode.SERVER_RESPONSE_ERROR);
      }).then(Mono.fromRunnable(()-> {
        if (config.isPostLogger()) {
          log.info("UserFilter End: {}", exchange.getResponse());
        }
      }));
    };
  }

  /**
   * 접근 가능한 URI Check
   *
   * @param request
   * @return
   */
  private boolean validationUriCheck(ServerHttpRequest request) {
    AntPathMatcher pathMatcher = new AntPathMatcher();

    var uriPath = request.getURI().getPath();
    var method = request.getMethod().toString().toUpperCase();
    log.info("uriPath => {}", uriPath);
    log.info("method => {}", method);

    var isAvailable = DENY_URI_LIST.entrySet().stream().anyMatch(path -> pathMatcher.match(path.getKey(), uriPath)
            && (path.getValue().equals("ALL") || path.getValue().equals(method)));

    log.info("isAvailable => {}", isAvailable);

    return !isAvailable;
  }

  private String validateRequest(ServerHttpRequest request) {
    log.debug("validation check start");
    // 사이트 코드 체크
    if (!request.getHeaders().containsKey(GlobalConstant.SITE_ID)) {
      log.debug(">>>>> SITE ID NOT CONTAINS <<<<<");
      log.debug(">>>>>HEADER => {}", request.getHeaders());
      log.debug(">>>>>URI => {}", request.getURI());
      throw new GatewayException(ErrorCode.INVALID_HEADER_SITE_ID);
    }
    // 사이트 코드에 따른 Authorization check
    String siteId = request.getHeaders().getFirst(GlobalConstant.SITE_ID);
    if (!StringUtils.hasLength(siteId)) {
      log.debug(">>>>> SITE ID NOT FOUND <<<<<");
      log.debug(">>>>> header => {}", request.getHeaders());
      log.debug(">>>>> URI => {}", request.getURI());
      log.debug(">>>>> siteId => {}", siteId);
      throw new GatewayException(ErrorCode.INVALID_HEADER_SITE_ID);
    }
    // 접속 가능한 사이트 아이디만 검증. - 스마트 어드민만 접속이 가능.
    if (!siteId.equals(GlobalConstant.MNG_SITE_ID)) {
      throw new GatewayException(ErrorCode.INVALID_HEADER_SITE_ID);
    }
    return siteId;
  }

  private String validateRequestMySiteId(ServerHttpRequest request) {
    log.debug("validation check start");
    // My 사이트 코드 체크
    if (!request.getHeaders().containsKey(GlobalConstant.MY_SITE_ID)) {
      log.debug(">>>>> MY SITE ID NOT CONTAINS <<<<<");
      log.debug(">>>>>HEADER => {}", request.getHeaders());
      log.debug(">>>>>URI => {}", request.getURI());
      throw new GatewayException(ErrorCode.INVALID_HEADER_SITE_ID);
    }
    // 사이트 코드에 따른 Authorization check
    String siteId = request.getHeaders().getFirst(GlobalConstant.MY_SITE_ID);
    if (!StringUtils.hasLength(siteId)) {
      log.debug(">>>>> MY SITE ID NOT FOUND <<<<<");
      log.debug(">>>>> header => {}", request.getHeaders());
      log.debug(">>>>> URI => {}", request.getURI());
      log.debug(">>>>> siteId => {}", siteId);
      throw new GatewayException(ErrorCode.INVALID_HEADER_SITE_ID);
    }
    // 접속 가능한 사이트 아이디만 검증.
    if (!siteId.equals(GlobalConstant.MNG_SITE_ID) && !siteId.equals(GlobalConstant.CPC_SITE_ID)
            && !siteId.equals(GlobalConstant.LRC_SITE_ID) && !siteId.equals(GlobalConstant.CMS_SITE_ID)) {
      throw new GatewayException(ErrorCode.INVALID_HEADER_SITE_ID);
    }
    return siteId;
  }

  /**
   * 접속 가능한 도메인 체크
   *
   * @param config
   * @param siteId
   * @param host
   */
  private void validateDomains(final Config config, String siteId, String host) {
    log.debug("validation check Domains... start");
    log.debug("check domain => {}", host);
    log.debug("allow host mng list => {}", config.getAllowHostProperties().mng);
    // host : localhost, smartadmin.bithumbsystems.com

    if (siteId.equals(GlobalConstant.MNG_SITE_ID)) {
      if (!config.getAllowHostProperties().mng.stream().anyMatch( x -> x.indexOf(host) != -1)) {
        throw new GatewayException(ErrorCode.INVALID_DOMAIN);
      }
    } else {
      throw new GatewayException(ErrorCode.INVALID_HEADER_SITE_ID);
    }
  }
}
