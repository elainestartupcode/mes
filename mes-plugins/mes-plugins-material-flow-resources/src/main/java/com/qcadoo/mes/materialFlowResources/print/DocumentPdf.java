/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.4
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
package com.qcadoo.mes.materialFlowResources.print;

import com.google.common.collect.Lists;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.mes.basic.ParameterService;
import com.qcadoo.mes.materialFlowResources.constants.ParameterFieldsMFR;
import com.qcadoo.mes.materialFlowResources.print.helper.DocumentPdfHelper;
import com.qcadoo.mes.materialFlowResources.print.helper.DocumentPdfHelper.HeaderPair;
import com.qcadoo.mes.materialFlowResources.print.helper.HeaderAlignmentWithWidth;
import com.qcadoo.mes.materialFlowResources.print.helper.PositionDataProvider;
import com.qcadoo.model.api.Entity;
import com.qcadoo.report.api.FontUtils;
import com.qcadoo.report.api.pdf.PdfHelper;
import com.qcadoo.report.api.pdf.ReportPdfView;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component(value = "documentPdf")
public class DocumentPdf extends ReportPdfView {

    @Autowired
    private TranslationService translationService;

    @Autowired
    private PdfHelper pdfHelper;

    @Autowired
    private DocumentPdfHelper documentPdfHelper;

    @Autowired
    private ParameterService parameterService;

    @Override
    protected String addContent(Document document, Map<String, Object> model, Locale locale, PdfWriter writer)
            throws DocumentException, IOException {
        Long id = Long.valueOf(model.get("id").toString());
        Entity documentEntity = documentPdfHelper.getDocumentEntity(id);
        String documentHeader = documentPdfHelper.getDocumentHeader(documentEntity, locale);
        pdfHelper.addDocumentHeader(document, "", documentHeader, "", new Date());
        addHeaderTable(document, documentEntity, locale);
        addPositionsTable(document, documentEntity, locale);
        return documentPdfHelper.getFileName(documentEntity, locale);
    }

    @Override
    protected void addTitle(Document document, Locale locale) {
        document.addTitle(translationService.translate("materialFlowResources.report.title", locale));
    }

    private void addHeaderTable(Document document, Entity documentEntity, Locale locale) throws DocumentException {

        Entity documentPositionParameters = parameterService.getParameter().getBelongsToField(
                ParameterFieldsMFR.DOCUMENT_POSITION_PARAMETERS);

        boolean notShowPrices = documentPositionParameters.getBooleanField("notShowPrices");

        PdfPTable table = pdfHelper.createPanelTable(3);

        List<HeaderPair> headerValues = documentPdfHelper.getDocumentHeaderTableContent(documentEntity, locale);
        for (HeaderPair pair : headerValues) {
            if (pair.getValue() != null && !pair.getValue().isEmpty()) {
                pdfHelper.addTableCellAsOneColumnTable(table, pair.getLabel(), pair.getValue());
            } else {
                pdfHelper.addTableCellAsOneColumnTable(table, StringUtils.EMPTY, StringUtils.EMPTY);
            }
        }
        HeaderPair description = documentPdfHelper.getDescription(documentEntity, locale);

        Phrase label = new Phrase(description.getValue().isEmpty() ? StringUtils.EMPTY : description.getLabel(),
                FontUtils.getDejavuBold8Dark());
        PdfPCell descriptionLabelCell = new PdfPCell(label);
        descriptionLabelCell.setColspan(2);
        descriptionLabelCell.setBorder(PdfPCell.NO_BORDER);
        descriptionLabelCell.setPaddingLeft(7);
        table.addCell(descriptionLabelCell);

        if (!notShowPrices) {
            Phrase totalValueLabel = new Phrase(documentPdfHelper.getTotalValueLabel(locale), FontUtils.getDejavuBold8Dark());
            PdfPCell totalValueLabelCell = new PdfPCell(totalValueLabel);
            totalValueLabelCell.setBorder(PdfPCell.NO_BORDER);
            totalValueLabelCell.setPaddingLeft(7);
            table.addCell(totalValueLabelCell);
            table.completeRow();

        } else {
            Phrase totalValueLabel = new Phrase("", FontUtils.getDejavuBold8Dark());
            PdfPCell totalValueLabelCell = new PdfPCell(totalValueLabel);
            totalValueLabelCell.setBorder(PdfPCell.NO_BORDER);
            totalValueLabelCell.setPaddingLeft(7);
            table.addCell(totalValueLabelCell);
            table.completeRow();
        }
        Phrase value = new Phrase(description.getValue(), FontUtils.getDejavuRegular9Dark());
        PdfPCell descriptionValueCell = new PdfPCell(value);
        descriptionValueCell.setColspan(2);
        descriptionValueCell.setBorder(PdfPCell.NO_BORDER);
        descriptionValueCell.setPaddingLeft(7);
        descriptionValueCell.setPaddingBottom(15);
        table.addCell(descriptionValueCell);

        if (!notShowPrices) {
            Phrase totalValue = new Phrase(PositionDataProvider.totalValue(documentEntity), FontUtils.getDejavuRegular9Dark());
            PdfPCell totalValueCell = new PdfPCell(totalValue);
            totalValueCell.setBorder(PdfPCell.NO_BORDER);
            totalValueCell.setPaddingLeft(7);
            totalValueCell.setPaddingBottom(15);
            table.addCell(totalValueCell);
        } else {
            Phrase totalValue = new Phrase("", FontUtils.getDejavuRegular9Dark());
            PdfPCell totalValueCell = new PdfPCell(totalValue);
            totalValueCell.setBorder(PdfPCell.NO_BORDER);
            totalValueCell.setPaddingLeft(7);
            totalValueCell.setPaddingBottom(15);
            table.addCell(totalValueCell);
        }
        table.completeRow();

        table.setSpacingAfter(20);

        document.add(table);

    }

    private void addPositionsTable(Document document, Entity documentEntity, Locale locale) throws DocumentException {
        Entity documentPositionParameters = parameterService.getParameter().getBelongsToField(
                ParameterFieldsMFR.DOCUMENT_POSITION_PARAMETERS);

        boolean notShowPrices = documentPositionParameters.getBooleanField("notShowPrices");
        boolean presentTotalAmountAndRest = documentPositionParameters.getBooleanField("presentTotalAmountAndRest");

        document.add(new Paragraph(documentPdfHelper.getTableHeader(locale), FontUtils.getDejavuBold10Dark()));
        Map<String, HeaderAlignmentWithWidth> headerValues = documentPdfHelper.getPositionsTableHeaderLabels(locale);

        int[] headerWidths = documentPdfHelper.headerWidths(headerValues);

        PdfPTable positionsTable = pdfHelper.createTableWithHeader(headerWidths.length,
                Lists.newArrayList(headerValues.keySet()), false, headerWidths, documentPdfHelper.headerValues(headerValues));
        positionsTable.getDefaultCell().disableBorderSide(PdfPCell.RIGHT);
        positionsTable.getDefaultCell().disableBorderSide(PdfPCell.LEFT);
        positionsTable.setHeaderRows(1);
        List<Entity> positions = PositionDataProvider.getPositions(documentEntity);
        Integer index = 1;
        for (Entity position : positions) {
            positionsTable.addCell(createCell(index.toString(), Element.ALIGN_LEFT));
            positionsTable.addCell(createCell(PositionDataProvider.product(position), Element.ALIGN_LEFT));
            positionsTable.addCell(createCell(PositionDataProvider.quantity(position), Element.ALIGN_RIGHT));
            positionsTable.addCell(createCell(PositionDataProvider.unit(position), Element.ALIGN_LEFT));
            positionsTable.addCell(createCell(PositionDataProvider.quantityAdd(position), Element.ALIGN_RIGHT));
            positionsTable.addCell(createCell(PositionDataProvider.unitAdd(position), Element.ALIGN_LEFT));
            if (presentTotalAmountAndRest) {
                positionsTable.addCell(createCell(PositionDataProvider.amountAndRest(position), Element.ALIGN_RIGHT));
            }
            if (!notShowPrices) {
                positionsTable.addCell(createCell(PositionDataProvider.price(position), Element.ALIGN_RIGHT));
            }
            positionsTable.addCell(createCell(PositionDataProvider.batch(position), Element.ALIGN_LEFT));
            positionsTable.addCell(createCell(PositionDataProvider.productionDate(position), Element.ALIGN_LEFT));
            if (!notShowPrices) {
                positionsTable.addCell(createCell(PositionDataProvider.value(position), Element.ALIGN_RIGHT));
            }
            index++;
        }

        if (!notShowPrices) {
            PdfPCell totalLabel = createCell(documentPdfHelper.getTotaLabel(locale), Element.ALIGN_LEFT);
            totalLabel.setColspan(headerWidths.length - 1);
            totalLabel.setBorderWidth(0.7f);
            positionsTable.addCell(totalLabel);
            PdfPCell totalValue = createCell(PositionDataProvider.totalValue(documentEntity), Element.ALIGN_RIGHT);
            totalValue.setBorderWidth(0.7f);
            positionsTable.addCell(totalValue);
            positionsTable.setSpacingAfter(20);
        }

        document.add(positionsTable);
    }

    private PdfPCell createCell(String content, int alignment) {
        PdfPCell cell = new PdfPCell();
        float border = 0.2f;
        cell.setPhrase(new Phrase(content, FontUtils.getDejavuRegular7Dark()));
        cell.setHorizontalAlignment(alignment);
        cell.setBorderWidth(border);
        cell.disableBorderSide(PdfPCell.RIGHT);
        cell.disableBorderSide(PdfPCell.LEFT);
        cell.setPadding(5);
        return cell;
    }
}
