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
import com.axelor.apps.account.db.repo.TaxLineRepository;
import com.axelor.apps.base.db.TaxType;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.PurchaseOrderTax;
import com.google.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PurchaseOrderTaxService {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject private PurchaseOrderToolService purchaseOrderToolService;

  @Inject private TaxLineRepository taxLineRepo;

  public void initPOTaxLines(PurchaseOrder po) {

    List<TaxLine> taxLines = taxLineRepo.all().fetch();

    TaxLine SGST_0 =
        taxLines.stream()
            .filter(
                tx ->
                    tx.getName().startsWith("SGST")
                        && tx.getValue().compareTo(BigDecimal.ZERO) == 0)
            .findFirst()
            .orElse(null);

    TaxLine CGST_0 =
        taxLines.stream()
            .filter(
                tx ->
                    tx.getName().startsWith("CGST")
                        && tx.getValue().compareTo(BigDecimal.ZERO) == 0)
            .findFirst()
            .orElse(null);

    TaxLine IGST_0 =
        taxLines.stream()
            .filter(
                tx ->
                    tx.getName().startsWith("IGST")
                        && tx.getValue().compareTo(BigDecimal.ZERO) == 0)
            .findFirst()
            .orElse(null);

    LOG.info(
        "The zero TAX_LINES are \n SGST0 : {} \n CGST0 : {} \n IGST0 : {}", SGST_0, CGST_0, IGST_0);

    TaxType taxType = po.getTaxType();

    if (null == taxType) {
      LOG.info("Since TAX_TYPE = EMPTY, setting all taxes to 0");

      po.setCgstTaxLine(CGST_0);
      po.setSgstTaxLine(SGST_0);
      po.setIgstTaxLine(IGST_0);

      return;
    }

    if (taxType == TaxType.INTRA_STATE) {
      LOG.info("Since TAX_TYPE = INTRA_STATE, only CGST and SGST will be applicable, HENCE setting IGST to 0");
      po.setIgstTaxLine(IGST_0);
      return;
    }

    if (taxType == TaxType.INTER_STATE) {
      LOG.info("Since TAX_TYPE = INTER_STATE, only IGST will be applicable, HENCE setting CGST and SGST to 0");
      po.setCgstTaxLine(CGST_0);
      po.setSgstTaxLine(SGST_0);
      return;
    }

    LOG.error("ILLEGAL TAX TYPE configured - TAXTYPE = {}", taxType);
  }

  public void setSGST(PurchaseOrder po) {

    if (po.getSgstTaxLine() == null) {
      LOG.info("NO SGST LINE FOUND");
      return;
    }

    PurchaseOrder fkey = new PurchaseOrder();
    fkey.setId(po.getId());

    TaxLine taxLine = po.getSgstTaxLine();
    PurchaseOrderTax tax = new PurchaseOrderTax();

    // tax.setId("SGST".hashCode() * po.getId());
    tax.setTaxName("SGST");
    tax.setTaxRate(
        taxLine.getValue().multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_EVEN) + "%");

    tax.setPurchaseOrder(fkey);
    tax.setExTaxBase(po.getExTaxTotal());
    tax.setReverseCharged(false);
    tax.setTaxLine(taxLine);

    LOG.info("For PO = {}, \n SGST_TAX_LINE = {} \n and SGST_TAX = {}", po, taxLine, tax);

    // Dans la devise de la commande
    BigDecimal exTaxBase =
        (tax.getReverseCharged()) ? tax.getExTaxBase().negate() : tax.getExTaxBase();

    BigDecimal taxTotal = BigDecimal.ZERO;

    if (tax.getTaxLine() != null)
      taxTotal = purchaseOrderToolService.computeAmount(exTaxBase, tax.getTaxLine().getValue());

    tax.setTaxTotal(taxTotal);
    tax.setInTaxTotal(tax.getExTaxBase().add(taxTotal));

    LOG.debug(
        "PO_EX_TAX => {}, TOTAL_SGST_TAX => {},  INCL_TAX => {}",
        new Object[] {tax.getExTaxBase(), tax.getTaxTotal(), tax.getInTaxTotal()});

    po.setSgstTax(tax);
  }

  public void setCGST(PurchaseOrder po) {

    if (po.getCgstTaxLine() == null) {
      LOG.info("NO CGST LINE FOUND");
      return;
    }

    PurchaseOrder fkey = new PurchaseOrder();
    fkey.setId(po.getId());

    TaxLine taxLine = po.getCgstTaxLine();
    PurchaseOrderTax tax = new PurchaseOrderTax();

    // tax.setId("CGST".hashCode() * po.getId());
    tax.setTaxName("CGST");
    tax.setTaxRate(
        taxLine.getValue().multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_EVEN) + "%");

    tax.setPurchaseOrder(fkey);
    tax.setExTaxBase(po.getExTaxTotal());
    tax.setReverseCharged(false);
    tax.setTaxLine(taxLine);

    LOG.info("For PO = {}, \n CGST_TAX_LINE = {} \n and CGST_TAX = {}", po, taxLine, tax);

    // Dans la devise de la commande
    BigDecimal exTaxBase =
        (tax.getReverseCharged()) ? tax.getExTaxBase().negate() : tax.getExTaxBase();

    BigDecimal taxTotal = BigDecimal.ZERO;

    if (tax.getTaxLine() != null)
      taxTotal = purchaseOrderToolService.computeAmount(exTaxBase, tax.getTaxLine().getValue());

    tax.setTaxTotal(taxTotal);
    tax.setInTaxTotal(tax.getExTaxBase().add(taxTotal));

    LOG.debug(
        "PO_EX_TAX => {}, TOTAL_CGST_TAX => {},  INCL_TAX => {}",
        new Object[] {tax.getExTaxBase(), tax.getTaxTotal(), tax.getInTaxTotal()});

    po.setCgstTax(tax);
  }

  public void setIGST(PurchaseOrder po) {

    if (po.getIgstTaxLine() == null) {
      LOG.info("NO IGST LINE FOUND");
      return;
    }

    PurchaseOrder fkey = new PurchaseOrder();
    fkey.setId(po.getId());

    TaxLine taxLine = po.getIgstTaxLine();
    PurchaseOrderTax tax = new PurchaseOrderTax();

    // tax.setId("IGST".hashCode() * po.getId());
    tax.setTaxName("IGST");
    tax.setTaxRate(
        taxLine.getValue().multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_EVEN) + "%");
    tax.setPurchaseOrder(fkey);
    tax.setExTaxBase(po.getExTaxTotal());
    tax.setReverseCharged(false);
    tax.setTaxLine(taxLine);

    LOG.info("For PO = {}, \n IGST_TAX_LINE = {} \n and IGST_TAX = {}", po, taxLine, tax);

    // Dans la devise de la commande
    BigDecimal exTaxBase =
        (tax.getReverseCharged()) ? tax.getExTaxBase().negate() : tax.getExTaxBase();

    BigDecimal taxTotal = BigDecimal.ZERO;

    if (tax.getTaxLine() != null)
      taxTotal = purchaseOrderToolService.computeAmount(exTaxBase, tax.getTaxLine().getValue());

    tax.setTaxTotal(taxTotal);
    tax.setInTaxTotal(tax.getExTaxBase().add(taxTotal));

    LOG.debug(
        "PO_EX_TAX => {}, TOTAL_IGST_TAX => {},  INCL_TAX => {}",
        new Object[] {tax.getExTaxBase(), tax.getTaxTotal(), tax.getInTaxTotal()});

    po.setIgstTax(tax);
  }

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

    PurchaseOrder fkey = new PurchaseOrder();
    fkey.setId(purchaseOrder.getId());

    if (purchaseOrder.getSgstTaxLine() != null) {
      TaxLine taxLine = purchaseOrder.getSgstTaxLine();
      PurchaseOrderTax sgstTax = new PurchaseOrderTax();

      sgstTax.setPurchaseOrder(fkey);
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

      cgstTax.setPurchaseOrder(fkey);
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

      igstTax.setPurchaseOrder(fkey);

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
