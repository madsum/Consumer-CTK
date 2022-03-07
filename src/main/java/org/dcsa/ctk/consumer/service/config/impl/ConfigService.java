package org.dcsa.ctk.consumer.service.config.impl;

import org.dcsa.ctk.consumer.constant.CheckListStatus;
import org.dcsa.ctk.consumer.model.CheckListItem;
import org.dcsa.ctk.consumer.model.RequestMatcher;
import org.dcsa.ctk.consumer.model.TestConfig;
import org.dcsa.ctk.consumer.util.APIUtility;
import org.dcsa.ctk.consumer.util.JsonUtility;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ConfigService {
    TestConfig testConfig = JsonUtility.loadTestConfig("config.json");
    Map<String, List<CheckListItem>> checkListItemMap = APIUtility.populateCheckList(testConfig);

    static CheckListItem getCheckListItem(String key) {
        Collection<List<CheckListItem>> checkListItems = checkListItemMap.values();
        for (List<CheckListItem> items : checkListItems) {
            for (CheckListItem item : items) {
                if (item.getId().equals(key))
                    return item;
            }
        }
        return null;
    }

    static CheckListItem getNextCheckListItem(String route, ServerHttpResponse response, ServerHttpRequest request) {
        CheckListItem listItem = null;
        List<CheckListItem> checkListItemList = checkListItemMap.get(APIUtility.generateKey(route, request.getMethod().name()));
        if (checkListItemList != null) {
            listItem = getRequestMatcher(route, response, request);
            if (listItem != null)
                return listItem;
            for (CheckListItem item : checkListItemList) {
                if (item.getStatus().equals(CheckListStatus.NOT_COVERED) && item.getResponseDecoratorWrapper().getRequestMatcher()==null) {
                    listItem = item;
                    break;
                }
            }
        }
        return listItem;
    }

    private static CheckListItem getRequestMatcher(String route, ServerHttpResponse response, ServerHttpRequest request) {
        CheckListItem listItem = null;
        boolean headerMatch = false;
        boolean queryMatch = false;
        List<CheckListItem> checkListItemList = checkListItemMap.get(APIUtility.generateKey(route, request.getMethod().name()));
        Set<String> headerSet = request.getHeaders().keySet();
        Set<String> querySet = request.getQueryParams().keySet();
        if (checkListItemList != null) {
            for (CheckListItem item : checkListItemList) {
                if (item.getStatus().equals(CheckListStatus.NOT_COVERED)
                        && item.getResponseDecoratorWrapper() != null) {
                    RequestMatcher requestMatcher = item.getResponseDecoratorWrapper().getRequestMatcher();
                    if (requestMatcher != null) {
                        Set<String> headers = requestMatcher.getHeader();
                        Set<String> queryParameters = requestMatcher.getQueryParameter();
                        if (headers != null && headerSet != null) {
                            if (headerSet.containsAll(headers))
                                headerMatch = true;
                        }
                        if (queryParameters != null && querySet != null) {
                            if (querySet.containsAll(queryParameters))
                                queryMatch = true;
                        }
                        if (headerMatch || queryMatch) {
                            listItem = item;
                            break;
                        }
                    }
                }
            }
        }
        return listItem;
    }

     static CheckListItem getCheckListItemForHttpCode(String routeKey, int httpCode) {
        CheckListItem listItem = null;
        List<CheckListItem> checkListItemList = checkListItemMap.get(routeKey);
        if (checkListItemList != null) {
            for (CheckListItem item : checkListItemList) {
                if (item.getStatus().equals(CheckListStatus.NOT_COVERED) && item.getResponseDecoratorWrapper().getHttpCode() == httpCode) {
                    listItem = item;
                    break;
                }
            }
        }
        return listItem;
    }

}