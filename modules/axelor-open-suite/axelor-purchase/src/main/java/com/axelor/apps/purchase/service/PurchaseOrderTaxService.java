/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2020 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.purchase.service;

import com.axelor.apps.account.db.TaxLine;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.PurchaseOrderTax;
import com.google.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PurchaseOrderTaxService {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject private PurchaseOrderToolService purchaseOrderToolService;

  /**
   * Créer les lignes de TVA de la commande. La création des lignes de TVA se basent sur les lignes
   * de commande.
   *
   * @param purchaseOrder La commande.
   * @return La liste des lignes de TVA de la commande.
   */
  public List<PurchaseOrderTax> createsPurchaseOrderTax(PurchaseOrder purchaseOrder) {

    List<PurchaseOrderTax> purchaseOrderTaxList = new ArrayList<PurchaseOrderTax>();
    Map<TaxLine, PurchaseOrderTax> map = new HashMap<TaxLine, PurchaseOrderTax>();

    if (purchaseOrder.getSgstTaxLine() != null) {
      TaxLine taxLine = purchaseOrder.getSgstTaxLine();
      PurchaseOrderTax sgstTax = new PurchaseOrderTax();
      sgstTax.setPurchaseOrder(purchaseOrder);
      sgstTax.setExTaxBase(purchaseOrder.getExTaxTotal());
      sgstTax.setReverseCharged(false);
      sgstTax.setTaxLine(taxLine);

      map.put(taxLine, sgstTax);

      LOG.info(
          "For PO = {}, \n SGST_TAX_LINE = {} \n and SGST_TAX = {}",
          purchaseOrder,
          taxLine,
          sgstTax);
    }

    if (purchaseOrder.getCgstTaxLine() != null) {
      TaxLine taxLine = purchaseOrder.getCgstTaxLine();
      PurchaseOrderTax cgstTax = new PurchaseOrderTax();
      cgstTax.setPurchaseOrder(purchaseOrder);
      cgstTax.setExTaxBase(purchaseOrder.getExTaxTotal());
      cgstTax.setReverseCharged(false);
      cgstTax.setTaxLine(taxLine);

      map.put(taxLine, cgstTax);
      LOG.info(
          "For PO = {}, \n CGST_TAX_LINE = {} and \n CGST_TAX = {}",
          purchaseOrder,
          taxLine,
          cgstTax);
    }

    if (purchaseOrder.getIgstTaxLine() != null) {
      TaxLine taxLine = purchaseOrder.getIgstTaxLine();
      PurchaseOrderTax igstTax = new PurchaseOrderTax();
      igstTax.setPurchaseOrder(purchaseOrder);
      igstTax.setExTaxBase(purchaseOrder.getExTaxTotal());
      igstTax.setReverseCharged(false);
      igstTax.setTaxLine(taxLine);

      map.put(taxLine, igstTax);
      LOG.info(
          "For PO = {}, \n IGST_TAX_LINE = {} and \n IGST_TAX = {}",
          purchaseOrder,
          taxLine,
          igstTax);
    }

    for (PurchaseOrderTax purchaseOrderTax : map.values()) {

      // Dans la devise de la commande
      BigDecimal exTaxBase =
          (purchaseOrderTax.getReverseCharged())
              ? purchaseOrderTax.getExTaxBase().negate()
              : purchaseOrderTax.getExTaxBase();

      BigDecimal taxTotal = BigDecimal.ZERO;

      if (purchaseOrderTax.getTaxLine() != null)
        taxTotal =
            purchaseOrderToolService.computeAmount(
                exTaxBase, purchaseOrderTax.getTaxLine().getValue());

      purchaseOrderTax.setTaxTotal(taxTotal);
      purchaseOrderTax.setInTaxTotal(purchaseOrderTax.getExTaxBase().add(taxTotal));

      purchaseOrderTaxList.add(purchaseOrderTax);

      LOG.debug(
          "PO_EX_TAX => {}, TOTAL_TAX => {},  INCL_TAX => {}",
          new Object[] {
            purchaseOrder.getExTaxTotal(),
            purchaseOrderTax.getTaxTotal(),
            purchaseOrderTax.getInTaxTotal()
          });
    }

    return purchaseOrderTaxList;
  }
}
