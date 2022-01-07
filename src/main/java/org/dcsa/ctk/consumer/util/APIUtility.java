package org.dcsa.ctk.consumer.util;

import lombok.Data;
import org.dcsa.ctk.consumer.model.CheckListItem;
import org.dcsa.ctk.consumer.model.ResponseDecoratorWrapper;
import org.dcsa.ctk.consumer.model.TestCase;
import org.dcsa.ctk.consumer.model.TestConfig;
import org.springframework.validation.annotation.Validated;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Data
@Validated
public class APIUtility {

    public static Map<String, List<CheckListItem>> populateCheckList(TestConfig testConfig) {
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
        if (httpCode == 200 || httpCode == 201 || httpCode == 202 || httpCode == 204)
            return true;
        else
            return false;
    }
}