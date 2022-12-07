package com.bithumbsystems.routes;

import com.bithumbsystems.config.Config;
import com.bithumbsystems.config.properties.AllowHostProperties;
import com.bithumbsystems.config.properties.UrlProperties;
import com.bithumbsystems.filter.ApiFilter;
import com.bithumbsystems.filter.AuthFilter;
import com.bithumbsystems.filter.UserFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class UserAuthRoute {

    private final UrlProperties urlProperties;
    private final AllowHostProperties allowHostProperties;
    private final UserFilter userFilter;
    //    private final AuthFilter authFilter;
    private final ApiFilter apiFilter;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("adm-service",   // 운영자 로그인 처리
                        route -> route.path("/adm/**")
                                .filters(filter -> filter.filter(userFilter.apply(new Config("UserFilter apply", allowHostProperties,  true, true))))
                                .uri(urlProperties.getSmartAdminGatewayUrl())
                )
                .route("api-service-mng-lrc",   // API 서비스 호출 (LRC Smart Admin)
                        route -> route.path("/api/*/mng/lrc/**")
                                .filters(filter -> filter.filter(apiFilter.apply(new Config("LRC Smart Admin ApiFilter apply", allowHostProperties, true, true))))
                                .uri(urlProperties.getSmartAdminGatewayUrl())
                )
                .route("api-service-mng-cpc",   // API 서비스 호출 (CP Smart Admin)
                        route -> route.path("/api/*/mng/cpc/**")
                                .filters(filter -> filter.filter(apiFilter.apply(new Config("LRC Smart Admin ApiFilter apply", allowHostProperties, true, true))))
                                .uri(urlProperties.getSmartAdminGatewayUrl())
                )
                .route("api-service-mng-cms",   // API 서비스 호출 (CP Smart Admin)
                        route -> route.path("/api/*/mng/cms/**")
                                .filters(filter -> filter.filter(apiFilter.apply(new Config("CMS Smart Admin ApiFilter apply", allowHostProperties, true, true))))
                                .uri(urlProperties.getSmartAdminGatewayUrl())
                )
                .route("api-service",   // API 서비스 호출 (Smart Admin)
                        route -> route.path("/api/**")
                                .filters(filter -> filter.filter(apiFilter.apply(new Config("ApiFilter apply", allowHostProperties, true, true))))
                                .uri(urlProperties.getSmartAdminGatewayUrl())
                )
                .build();
    }
}
