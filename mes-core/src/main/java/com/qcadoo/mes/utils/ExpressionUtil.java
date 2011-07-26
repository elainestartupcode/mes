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

package com.qcadoo.mes.utils;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import com.qcadoo.mes.api.Entity;
import com.qcadoo.mes.api.TranslationService;
import com.qcadoo.mes.model.FieldDefinition;
import com.qcadoo.mes.model.types.internal.BooleanType;
import com.qcadoo.mes.model.types.internal.EnumType;
import com.qcadoo.mes.view.components.grid.ColumnDefinition;

/**
 * Helper class that contains methods to evaluate expression value.
 * 
 */

@Component
public final class ExpressionUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ExpressionUtil.class);

    private static TranslationService translationService;

    public ExpressionUtil() {
    }

    @Autowired
    public void setTranslationService(final TranslationService translationService) {
        ExpressionUtil.setStaticTranslationService(translationService);
    }

    private static void setStaticTranslationService(final TranslationService translationService) {
        ExpressionUtil.translationService = translationService;
    }

    /**
     * Generates text to display in grid cell. If columnDefinition has expression - uses it, otherwise result is value of field
     * (or comma separated fields values when columDefinition has more than one field). Returns null when generated value is null.
     * 
     * @param entity
     * @param columnDefinition
     * @param locale
     * @return text to display in grid cell
     */
    public static String getValue(final Entity entity, final ColumnDefinition columnDefinition, final Locale locale) {
        String value = null;
        if (StringUtils.isEmpty(columnDefinition.getExpression())) {
            value = getValueWithoutExpression(entity, columnDefinition, locale);
        } else {
            value = getValueWithExpression(entity, columnDefinition.getExpression());
        }

        if (StringUtils.isEmpty(value) || "null".equals(value)) {
            return null;
        } else {
            return value;
        }
    }

    /**
     * Evaluate expression value using entity fields values. Returns null when generated value is null.
     * 
     * @param entity
     * @param expression
     * @return result of expression evaluation
     */
    public static String getValue(final Entity entity, final String expression) {
        checkState(!isEmpty(expression), "Expression must be defined");

        String value = getValueWithExpression(entity, expression);

        if (StringUtils.isEmpty(value) || "null".equals(value)) {
            return null;
        } else {
            return value;
        }
    }

    private static String getValueWithExpression(final Entity entity, final String expression) {
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(expression);
        EvaluationContext context = new StandardEvaluationContext();

        if (entity != null) {
            context.setVariable("id", entity.getId());
            for (String fieldName : entity.getFields().keySet()) {
                Object value = entity.getField(fieldName);
                if (value instanceof Entity) {
                    Map<String, Object> values = ((Entity) value).getFields();
                    values.put("id", ((Entity) value).getId());
                    context.setVariable(fieldName, values);
                } else {
                    context.setVariable(fieldName, value);
                }
            }
        }

        String value = String.valueOf(exp.getValue(context));

        if (LOG.isDebugEnabled()) {
            LOG.debug("Calculating value of expression \"" + expression + "\" for " + entity + " : " + value);
        }

        return value;
    }

    private static String getValueWithoutExpression(final Entity entity, final ColumnDefinition columnDefinition,
            final Locale locale) {
        String value = null;

        if (columnDefinition.getFields().size() == 1) {
            FieldDefinition field = columnDefinition.getFields().get(0);
            value = field.getValue(entity.getField(field.getName()));
            if (field.getType() instanceof BooleanType) {
                if ("0".equals(value)) {
                    value = translationService.translate("commons.false", locale);
                } else {
                    value = translationService.translate("commons.true", locale);
                }
            } else if (field.getType() instanceof EnumType) {
                String messageCode = translationService.getEntityFieldBaseMessageCode(field.getDataDefinition(), field.getName())
                        + ".value." + value;
                value = translationService.translate(messageCode, locale);
            }
        } else {
            List<String> values = new ArrayList<String>();
            for (FieldDefinition fieldDefinition : columnDefinition.getFields()) {
                values.add(fieldDefinition.getValue(entity.getField(fieldDefinition.getName())));
            }
            value = StringUtils.join(values, ", ");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Calculating value of column " + columnDefinition.getName() + " for " + entity + " : " + value);
        }

        return value;
    }
}