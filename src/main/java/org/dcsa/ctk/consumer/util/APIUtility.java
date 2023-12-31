package org.dcsa.ctk.consumer.util;

import lombok.Data;
import lombok.extern.java.Log;
import org.dcsa.ctk.consumer.model.*;
import org.dcsa.ctk.consumer.webhook.SparkWebHook;
import org.dcsa.tnt.model.transferobjects.TNTEventSubscriptionTO;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.annotation.Validated;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Log
@Data
@Validated
public class APIUtility {

    public static Map<String, List<CheckListItem>>  populateCheckList(TestConfig testConfig) {
        Map<String, List<CheckListItem>> checkListItemMap = new LinkedHashMap<>();
        List<TestCase> testConfigItems = testConfig.getTestConfig();
        for (TestCase test : testConfigItems) {
            String route = test.getRoute();
            Map<String, List<ResponseDecoratorWrapper>> variantsHttp = test.getVariants();
            for (Map.Entry<String, List<ResponseDecoratorWrapper>> variantEntry : variantsHttp.entrySet()) {
                String method = variantEntry.getKey();
                List<ResponseDecoratorWrapper> variants = variantEntry.getValue();
                for (ResponseDecoratorWrapper resDecorator : variants) {
                    String key = generateKey(route, method);
                    List<CheckListItem> list = checkListItemMap.get(key);
                    if (list == null)
                        list = new LinkedList<>();
                    CheckListItem checkListItem = new CheckListItem();
                    checkListItem.setResponseDecoratorWrapper(resDecorator);
                    list.add(checkListItem);
                    checkListItemMap.put(generateKey(route, method), list);
                }
            }
        }
        return checkListItemMap;
    }


    public static String generateKey(String route, String method) {
        return route + ":" + method;
    }


    public static String changeDateFormat(String format, String date) {
        Date paredDate = null;
        try {
            paredDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse(date);
        } catch (ParseException e) {
            try {
                paredDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse(date);
            } catch (ParseException ex) {
                ex.printStackTrace();
                return date;
            }
        }
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(format);
        String newDate = DATE_FORMAT.format(paredDate);
        return newDate;
    }

    public static boolean isReferenceCallRequired(int httpCode) {
        if (httpCode == 200 || httpCode == 201 || httpCode == 202 ||
                httpCode == 204 || httpCode == 400 || httpCode == 404 )
            return true;
        else
            return false;
    }
    public static void runWebHook() {
        SparkWebHook sparkWebHook;
        sparkWebHook = new SparkWebHook();
        sparkWebHook.startServer();
    }

    static public String getCallBackUuid(String callBack){
        String uuid = "";
        if(callBack != null){
            String[] tokens = callBack.split("/");
            if(tokens.length > 0){
                uuid = tokens[tokens.length  -1];
            }
        }
         return uuid;
    }
    public static String getRoute(ServerHttpRequest request){
        String[] paths = request.getPath().toString().split("/");
        String route = "/";
        if(paths.length > 0){
            route = route+paths[paths.length -1];
        }
        return route;
    }
    public static void setDescription(CheckListItem checkListItem, String subscriptionId){
        String  description = checkListItem.getResponseDecoratorWrapper().getDescription()
                                .replaceAll("\\{subscriptionId\\}", subscriptionId);
        checkListItem.getResponseDecoratorWrapper().setDescription(description);
    }

    public static boolean verifySignature(String secret){
        String notificationBody = EventUtility.getEquipmentEvent();
        String signature = SignatureUtility.getSignature(secret, notificationBody);
        if(signature != null){
            return true;
        }else{
           return false;
        }
    }
    public static boolean isBase64Encoded(String s) {
        String pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(s);
        return m.find();
    }
}
