package com.axelor.apps.purchase.db.repo;

import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.PurchaseOrderTax;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PurchaseOrderTaxManagementRepository extends PurchaseOrderTaxRepository {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject private PurchaseOrderManagementRepository poRepo;

  @Transactional(rollbackOn = {Exception.class})
  public void clearUnreferedTaxes() {

    Set<Long> usedTaxIds = new HashSet<Long>();
    List<PurchaseOrder> orders = poRepo.all().fetch();

    LOG.info("FOUND : {} PurchaseOrders in SYSTEM", orders.size());

    for (PurchaseOrder po : orders) {
      if (po.getSgstTax() != null) usedTaxIds.add(po.getSgstTax().getId());
      if (po.getCgstTax() != null) usedTaxIds.add(po.getCgstTax().getId());
      if (po.getIgstTax() != null) usedTaxIds.add(po.getIgstTax().getId());
    }

    LOG.info("Found totally used {} TAXIDS", usedTaxIds.size());

    List<PurchaseOrderTax> allTaxes = all().fetch();

    LOG.info("Found TOTAL {} TAXIDS", allTaxes.size());

    List<PurchaseOrderTax> unusedTaxes = new ArrayList<>();

    for (PurchaseOrderTax tax : allTaxes) {
      if (!usedTaxIds.contains(tax.getId())) {
        unusedTaxes.add(tax);
      }
    }

    LOG.info("Found TOTAL UNUSED {} TAXIDS", unusedTaxes.size());
    for (PurchaseOrderTax unusedTax : unusedTaxes) {
      remove(unusedTax);
    }

    flush();
  }
}
