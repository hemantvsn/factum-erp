package com.axelor.apps.purchase.service;

import com.axelor.apps.purchase.db.repo.PurchaseOrderTaxManagementRepository;
import com.google.inject.Inject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class POTaxCleanerJob implements Job {

  @Inject private PurchaseOrderTaxManagementRepository purchaseOrderTaxManagementRepository;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    purchaseOrderTaxManagementRepository.clearUnreferedTaxes();
  }
}
