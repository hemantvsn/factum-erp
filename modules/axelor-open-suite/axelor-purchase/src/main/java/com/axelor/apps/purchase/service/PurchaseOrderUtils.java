package com.axelor.apps.purchase.service;

import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.common.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PurchaseOrderUtils {

  private static final Logger LOG = LoggerFactory.getLogger(PurchaseOrderUtils.class);

  public static final ObjectMapper mapper = new ObjectMapper();

  public static Map<String, Object> getPurchaseOrderAttributesMap(PurchaseOrder po) {

    if (null == po) {
      return Collections.emptyMap();
    }

    if (StringUtils.isBlank(po.getAttrs())) {
      return Collections.emptyMap();
    }

    try {
      TypeReference<HashMap<String, Object>> typeRef =
          new TypeReference<HashMap<String, Object>>() {};
      return mapper.readValue(po.getAttrs(), typeRef);
    } catch (Exception e) {
      LOG.error("For PO = {}, cannot convert ATTRS to MAP", po, e);

      return Collections.emptyMap();
    }
  }

  public static BigDecimal convertToBigDecimal(Map<String, Object> map, String attribute) {
    if (isEmpty(map)) {
      return BigDecimal.ZERO;
    }

    Object val = map.get(attribute);

    if (null == val) {
      return BigDecimal.ZERO;
    }

    try {
      String valStr = val.toString();
      BigDecimal decVal = new BigDecimal(valStr);

      LOG.info("For MAP : {}, ATTR = {}, BIGDECIMAL VAL = {}", map, attribute, decVal);
      return decVal;

    } catch (Exception e) {
      LOG.error(
          "For MAP : {}, ATTR = {}, COULDN'T convert to BIGDECIMAL. Returning 0", map, attribute);
      return BigDecimal.ZERO;
    }
  }

  public static boolean isEmpty(final Map<?, ?> map) {
    return map == null || map.isEmpty();
  }
}
