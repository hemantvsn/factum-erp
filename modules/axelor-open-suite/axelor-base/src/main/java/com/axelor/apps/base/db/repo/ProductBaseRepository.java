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
package com.axelor.apps.base.db.repo;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.ProductCategory;
import com.axelor.apps.base.service.BarcodeGeneratorService;
import com.axelor.apps.base.service.ProductService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.tool.service.TranslationService;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.google.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.PersistenceException;
import javax.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductBaseRepository extends ProductRepository {

  private final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject private MetaFiles metaFiles;

  @Inject protected AppBaseService appBaseService;

  @Inject protected TranslationService translationService;

  protected static final String FULL_NAME_FORMAT = "[%s] %s";

  @Inject protected BarcodeGeneratorService barcodeGeneratorService;

  @Override
  public Product save(Product product) {
    /*try {
      if (appBaseService.getAppBase().getGenerateProductSequence()
          && Strings.isNullOrEmpty(product.getCode())) {
        product.setCode(Beans.get(ProductService.class).getSequence());
      }
    } catch (Exception e) {
      throw new PersistenceException(e.getLocalizedMessage());
    }*/

    updateProductCode(product);

    product.setFullName(String.format(FULL_NAME_FORMAT, product.getCode(), product.getName()));

    if (product.getId() != null) {
      Product oldProduct = Beans.get(ProductRepository.class).find(product.getId());
      translationService.updateFormatedValueTranslations(
          oldProduct.getFullName(), FULL_NAME_FORMAT, product.getCode(), product.getName());
    } else {
      translationService.createFormatedValueTranslations(
          FULL_NAME_FORMAT, product.getCode(), product.getName());
    }

    product = super.save(product);
    if (product.getBarCode() == null
        && appBaseService.getAppBase().getActivateBarCodeGeneration()) {
      try {
        boolean addPadding = false;
        InputStream inStream;
        if (!appBaseService.getAppBase().getEditProductBarcodeType()) {
          inStream =
              barcodeGeneratorService.createBarCode(
                  product.getSerialNumber(),
                  appBaseService.getAppBase().getBarcodeTypeConfig(),
                  addPadding);
        } else {
          inStream =
              barcodeGeneratorService.createBarCode(
                  product.getSerialNumber(), product.getBarcodeTypeConfig(), addPadding);
        }
        if (inStream != null) {
          MetaFile barcodeFile =
              metaFiles.upload(inStream, String.format("ProductBarCode%d.png", product.getId()));
          product.setBarCode(barcodeFile);
        }
      } catch (IOException e) {
        e.printStackTrace();
      } catch (AxelorException e) {
        throw new ValidationException(e.getMessage());
      }
    }
    return super.save(product);
  }

  /**
   * Product Code should be auto-generated.
   *
   * <p>It should be combination of
   *
   * <p>1. PRODUCT_CATEGORY_CODE 2. NEXT NUMBER IN SEQUENCE FOR THAT CATEGORY
   *
   * @param product
   */
  private void updateProductCode(Product product) {

    // This cannot be null
    ProductCategory cat = product.getProductCategory();

    int seq = getNextNumberForCategory(cat);

    product.setCode(cat.getCode() + seq);

    LOG.info("The product code is now set to - {}", product.getCode());
  }

  /**
   * Finds the last product in same category
   *
   * @param cat
   * @return
   */
  private int getNextNumberForCategory(ProductCategory cat) {

    LOG.info("Finding NEXT_NUMBER_FOR_CATEGORY = {}", cat);

    List<Product> productsWithSameCategory =
        this.all().fetch().stream()
            .filter(p -> p.getProductCategory().equals(cat))
            .collect(Collectors.toList());

    LOG.info("Found {} products belonging to category - {}", productsWithSameCategory.size(), cat);

    return productsWithSameCategory.size() + 1;
  }

  @Override
  public Product copy(Product product, boolean deep) {
    Product copy = super.copy(product, deep);
    Beans.get(ProductService.class).copyProduct(product, copy);

    try {
      if (appBaseService.getAppBase().getGenerateProductSequence()) {
        copy.setCode(Beans.get(ProductService.class).getSequence());
      }
    } catch (Exception e) {
      throw new PersistenceException(e.getLocalizedMessage());
    }
    return copy;
  }
}
