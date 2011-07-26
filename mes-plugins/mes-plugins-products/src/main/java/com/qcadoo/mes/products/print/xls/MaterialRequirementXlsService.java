/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 0.1
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */

package com.qcadoo.mes.products.print.xls;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.api.Entity;
import com.qcadoo.mes.internal.ProxyEntity;
import com.qcadoo.mes.products.print.MaterialRequirementDocumentService;
import com.qcadoo.mes.products.print.xls.util.XlsCopyUtil;

@Service
public final class MaterialRequirementXlsService extends MaterialRequirementDocumentService {

    private static final Logger LOG = LoggerFactory.getLogger(MaterialRequirementXlsService.class);

    @Override
    public void generateDocument(final Entity entity, final Locale locale) throws IOException {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet(getTranslationService().translate("products.materialRequirement.report.title",
                locale));
        addHeader(sheet, locale);
        addSeries(sheet, entity);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(getFileName((Date) entity.getField("date")) + XlsCopyUtil.XLS_EXTENSION);
            workbook.write(outputStream);
        } catch (IOException e) {
            LOG.error("Problem with generating document - " + e.getMessage());
            if (outputStream != null) {
                outputStream.close();
            }
            throw e;
        }
        outputStream.close();
        updateFileName(entity, getFileName((Date) entity.getField("date")));
    }

    private void addHeader(final HSSFSheet sheet, final Locale locale) {
        HSSFRow header = sheet.createRow(0);
        header.createCell(0).setCellValue(getTranslationService().translate("products.product.number.label", locale));
        header.createCell(1).setCellValue(getTranslationService().translate("products.product.name.label", locale));
        header.createCell(2).setCellValue(
                getTranslationService().translate("products.instructionBomComponent.quantity.label", locale));
        header.createCell(3).setCellValue(getTranslationService().translate("products.product.unit.label", locale));
    }

    private void addSeries(final HSSFSheet sheet, final Entity entity) {
        int rowNum = 1;
        List<Entity> orders = entity.getHasManyField("orders");
        Map<ProxyEntity, BigDecimal> products = getBomSeries(entity, orders);
        for (Entry<ProxyEntity, BigDecimal> entry : products.entrySet()) {
            HSSFRow row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey().getField("number").toString());
            row.createCell(1).setCellValue(entry.getKey().getField("name").toString());
            row.createCell(2).setCellValue(entry.getValue().doubleValue());
            Object unit = entry.getKey().getField("unit");
            if (unit != null) {
                row.createCell(3).setCellValue(unit.toString());
            } else {
                row.createCell(3).setCellValue("");
            }
        }
    }
}