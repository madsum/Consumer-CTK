package org.dcsa.ctk.consumer.reporter.impl;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.dcsa.ctk.consumer.constants.CheckListStatus;
import org.dcsa.ctk.consumer.mock.service.impl.ConfigService;
import org.dcsa.ctk.consumer.model.CheckListItem;
import org.dcsa.ctk.consumer.reporter.CustomReporter;
import org.dcsa.ctk.consumer.util.JsonUtility;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Component
public class ExcelReporter implements CustomReporter {
    public  String generateTestReport(String outputDirectory) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Map<String, List<CheckListItem>> groupedCheckListItem = getGroupedCheckListItem(ConfigService.checkListItemMap);
        String fileName = ("TestResult_" + new Date().getTime() + ".xlsx");
        Map<String, Integer[]> resultSummary = getAggregatedResultSummary(groupedCheckListItem);
        XSSFCreationHelper createHelper = workbook.getCreationHelper();

        Object[] header = new Object[]{"Test Name", "Total", "Covered", "Not Covered", ""};
        XSSFSheet spreadsheetSummary = workbook.createSheet("TestResult");
        Set<String> keyIds = resultSummary.keySet();
        List<String> testCaseList = new ArrayList<>(keyIds);
        Collections.sort(testCaseList);
        int rowId = 0;
        XSSFRow sheetRow = spreadsheetSummary.createRow(rowId++);

        int cellId = 0;
        for (Object obj : header) {
            Cell cell = sheetRow.createCell(cellId++);
            cell.setCellValue((String) obj);
            cell.setCellStyle(getCellStyle("header", workbook));
        }

        int sheetNo = 1;
        for (String key : testCaseList) {
            String sheetName = "TestGroup-" + sheetNo++;
            XSSFSheet spreadsheet = workbook.createSheet(sheetName);
            sheetRow = spreadsheetSummary.createRow(rowId++);
            Object[] objectArr = resultSummary.get(key);
            cellId = 0;
            Cell cell = sheetRow.createCell(cellId++);
            cell.setCellValue(key);
            cell.setCellStyle(getCellStyle("normal", workbook));
            XSSFHyperlink link = createHelper.createHyperlink(HyperlinkType.FILE);
            link.setAddress(fileName);
            link.setLocation("'" + sheetName + "'!A2");
            for (Object obj : objectArr) {
                cell = sheetRow.createCell(cellId++);
                cell.setCellValue((Integer) obj);
                cell.setCellStyle(getCellStyle("normal", workbook));
            }
            fillWithTestResult(spreadsheet, groupedCheckListItem.get(key), workbook);
            cell = sheetRow.createCell(cellId++);
            cell.setCellValue("details");
            cell.setHyperlink(link);
            cell.setCellStyle(getCellStyle("link", workbook));
        }

        try {
            FileOutputStream out = new FileOutputStream(new File(outputDirectory + "/" + fileName));
            workbook.write(out);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileName;
    }

    private  Map<String, Integer[]> getAggregatedResultSummary(Map<String, List<CheckListItem>> groupedCheckListItem) {
        Map<String, Integer[]> resultSummary = new TreeMap<>();
        for (Map.Entry<String, List<CheckListItem>> entry : groupedCheckListItem.entrySet()) {
            String key = entry.getKey();
            List<CheckListItem> checkListItems = entry.getValue();
            for (CheckListItem item : checkListItems) {
                if (resultSummary.containsKey(key)) {
                    resultSummary.get(key)[0]++;
                    if (item.getStatus().equals(CheckListStatus.COVERED))
                        resultSummary.get(key)[1]++;
                    else
                        resultSummary.get(key)[2]++;
                } else {
                    Integer[] stats = new Integer[]{0, 0, 0};
                    stats[0]++;
                    if (item.getStatus().equals(CheckListStatus.COVERED))
                        stats[1]++;
                    else
                        stats[2]++;
                    resultSummary.put(key, stats);
                }
            }
        }
        return resultSummary;
    }

    private  Map<String, List<CheckListItem>> getGroupedCheckListItem(Map<String, List<CheckListItem>> checkListItemMap) {
        Map<String, List<CheckListItem>> groupedCheckListItem = new TreeMap<>();
        for (Map.Entry<String, List<CheckListItem>> entry : checkListItemMap.entrySet()) {
            List<CheckListItem> checkListItems = entry.getValue();
            for (CheckListItem item : checkListItems) {
                String category = item.getResponseDecoratorWrapper().getCategory();
                if (groupedCheckListItem.containsKey(category)) {
                    groupedCheckListItem.get(category).add(item);
                } else {
                    List<CheckListItem> list = new ArrayList<>();
                    list.add(item);
                    groupedCheckListItem.put(category, list);
                }
            }
        }
        return groupedCheckListItem;
    }

    private  void fillWithTestResult(XSSFSheet spreadsheet, List<CheckListItem> checkListItemList, XSSFWorkbook workbook) {

        int rowId = 0;
        XSSFRow row;
        Object[] header = new Object[]{"Test Name", "Status", "Test Details"};
        int cellId = 0;
        row = spreadsheet.createRow(rowId++);
        for (Object obj : header) {
            Cell cell = row.createCell(cellId++);
            cell.setCellValue((String) obj);
            cell.setCellStyle(getCellStyle("header", workbook));
        }
        for (CheckListItem checkListItem : checkListItemList) {
            Object[] objectArr = new Object[3];
            CellStyle statusStyle = getCellStyle("notcovered", workbook);
            if (checkListItem.getStatus().equals(CheckListStatus.COVERED))
                statusStyle = getCellStyle("covered", workbook);
            objectArr[0] = checkListItem.getResponseDecoratorWrapper().getDescription();
            objectArr[1] = checkListItem.getStatus().getName();
            String log = JsonUtility.getStringFormat(checkListItem.getLog());
            if (log.length() > 32766)
                objectArr[2] = log.substring(0, 32766);
            else
                objectArr[2] = log;
            cellId = 0;
            row = spreadsheet.createRow(rowId++);
            Cell cell;
            for (Object obj : objectArr) {
                cell = row.createCell(cellId++);
                cell.setCellValue((String) obj);
                if(cellId==2)
                cell.setCellStyle(statusStyle);
                else
                    cell.setCellStyle(getCellStyle("normal", workbook));
            }

        }
    }

    private  CellStyle getCellStyle(String type, XSSFWorkbook workbook) {

        if (type.equals("link")) {
            XSSFCellStyle hlinkstyle = workbook.createCellStyle();
            XSSFFont hlinkfont = workbook.createFont();
            hlinkfont.setUnderline(XSSFFont.U_SINGLE);
            hlinkfont.setColor(IndexedColors.BLUE.getIndex());
            hlinkstyle.setFont(hlinkfont);
            hlinkstyle.setBorderBottom(BorderStyle.THIN);
            hlinkstyle.setBorderLeft(BorderStyle.THIN);
            hlinkstyle.setBorderRight(BorderStyle.THIN);
            hlinkstyle.setBorderTop(BorderStyle.THIN);
            return hlinkstyle;
        }
        else if (type.equals("header")) {
            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            borderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            borderStyle.setFont(font);
            return borderStyle;
        } else if (type.equals("covered")) {
                CellStyle borderStyle = workbook.createCellStyle();
                borderStyle.setBorderBottom(BorderStyle.THIN);
                borderStyle.setBorderLeft(BorderStyle.THIN);
                borderStyle.setBorderRight(BorderStyle.THIN);
                borderStyle.setBorderTop(BorderStyle.THIN);
                borderStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
                borderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                return borderStyle;
            }
        else if (type.equals("notcovered")) {
            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            borderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return borderStyle;
        }
        else {
            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            return borderStyle;
        }

    }
}



