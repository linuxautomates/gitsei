package io.levelops.api.controllers;


import io.levelops.commons.databases.models.database.Product;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProductsControllerTest {
    private static final boolean INSERT = true;
    private static final boolean UPDATE = false;
    private static final ProductsController PRODUCTS_CONTROLLER = new ProductsController(null, null, null, null,null,null,null,null,null, null, false, null, null, true);

    @Test (expected = ResponseStatusException.class)
    public void testValidateInputNullProduct1(){
        PRODUCTS_CONTROLLER.validateInput(null, INSERT);
    }
    @Test (expected = ResponseStatusException.class)
    public void testValidateInputNullProduct2(){
        PRODUCTS_CONTROLLER.validateInput(null, UPDATE);
    }

    private List<Product> generateCommonInvalidProductName(){
        List<Product> products = List.of(
                Product.builder().name("  ").build(),
                Product.builder().name("abc!").build(),
                Product.builder().name("abc@").build(),
                Product.builder().name("abc#").build(),
                Product.builder().name("abc$").build(),
                Product.builder().name("abc%").build(),
                Product.builder().name("abc^").build(),
                Product.builder().name("abc&").build(),
                Product.builder().name("abc*").build(),
                Product.builder().name("abc(").build(),
                Product.builder().name("abc)").build(),
                Product.builder().name("abc+").build(),
                Product.builder().name("abc=").build(),
                Product.builder().name("abc~").build(),
                Product.builder().name("abc`").build()
        );
        return products;
    }
    @Test
    public void testValidateInputInsertProductName(){
        List<Product> products = new ArrayList<>(generateCommonInvalidProductName());
        products.add(Product.builder().build());
        products.add(Product.builder().name("").build());
        for (Product p : products) {
            try {
                PRODUCTS_CONTROLLER.validateInput(p, INSERT);
                Assert.fail("ResponseStatusException was expected !!");
            } catch(ResponseStatusException e) {
                Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
                Assert.assertTrue(e.getReason().startsWith("Workspace name "));
            }
        }
    }
    @Test
    public void testValidateInputUpdateProductName(){
        List<Product> products = new ArrayList<>(generateCommonInvalidProductName());
        products.add(Product.builder().name("").build());
        for (Product p : products) {
            try {
                PRODUCTS_CONTROLLER.validateInput(p, UPDATE);
            } catch(ResponseStatusException e) {
                Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
                Assert.assertTrue(e.getReason().startsWith("Workspace name "));
            }
        }
    }

    private List<Product> generateCommonInvalidProductkey(){
        List<Product> products = List.of(
                Product.builder().name("Abc-123").key("  ").build(),
                Product.builder().name("Abc-123").key("abc!").build(),
                Product.builder().name("Abc-123").key("abc@").build(),
                Product.builder().name("Abc-123").key("abc#").build(),
                Product.builder().name("Abc-123").key("abc$").build(),
                Product.builder().name("Abc-123").key("abc%").build(),
                Product.builder().name("Abc-123").key("abc^").build(),
                Product.builder().name("Abc-123").key("abc&").build(),
                Product.builder().name("Abc-123").key("abc*").build(),
                Product.builder().name("Abc-123").key("abc(").build(),
                Product.builder().name("Abc-123").key("abc)").build(),
                Product.builder().name("Abc-123").key("abc+").build(),
                Product.builder().name("Abc-123").key("abc=").build(),
                Product.builder().name("Abc-123").key("abc~").build(),
                Product.builder().name("Abc-123").key("abc`").build(),
                Product.builder().name("Abc  123").key("abc`").build(),
                Product.builder().name("  Abc  123  ").key("abc`").build()
        );
        return products;
    }
    @Test
    public void testValidateInputInsertProductKey(){
        List<Product> products = new ArrayList<>(generateCommonInvalidProductkey());
        products.add(Product.builder().name("Abc-123").build());
        products.add(Product.builder().name("Abc-123").key("").build());
        for (Product p : products) {
            try {
                PRODUCTS_CONTROLLER.validateInput(p, INSERT);
                Assert.fail("ResponseStatusException was expected !!");
            } catch(ResponseStatusException e) {
                Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
                Assert.assertTrue(e.getReason().startsWith("Workspace key "));
            }
        }
    }
    @Test
    public void testValidateInputUpdateProductKey(){
        List<Product> products = new ArrayList<>(generateCommonInvalidProductkey());
        products.add(Product.builder().name("").build());
        for (Product p : products) {
            try {
                PRODUCTS_CONTROLLER.validateInput(p, UPDATE);
            } catch(ResponseStatusException e) {
                Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
                Assert.assertTrue(e.getReason().startsWith("Workspace key "));
            }
        }
    }

    @Test
    public void testValidateInputInsertProductId(){
        List<Product> products = List.of(
                Product.builder().name("Abc-123").key("Abc-123").integrationIds(Set.of(1)).build(),
                Product.builder().name(" Abc-123 ").key("Abc-123").integrationIds(Set.of(1)).build(),
                Product.builder().name(" Abc  123 ").key("Abc-123").integrationIds(Set.of(1)).build()
        );
        for (Product p : products) {
            try {
                PRODUCTS_CONTROLLER.validateInput(p, INSERT);
            } catch(ResponseStatusException e) {
                Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
                Assert.assertTrue(e.getReason().startsWith("Workspace owner "));
            }
        }
    }

    @Test
    public void testValidateInputInsertProductValid(){
        List<Product> products = List.of(
                Product.builder().name("Abc-123").key("Abc-123").ownerId("10").integrationIds(Set.of(1)).build(),
                Product.builder().name("Abc  123").key("Abc-123").ownerId("10").integrationIds(Set.of(1)).build(),
                Product.builder().name(" Abc  123 ").key("Abc-123").ownerId("10").integrationIds(Set.of(1)).build()
        );
        for (Product p : products) {
            PRODUCTS_CONTROLLER.validateInput(p, INSERT);
        }
    }

    @Test
    public void testValidateInputUpdateProductValid(){
        List<Product> products = List.of(
                Product.builder().build(),
                Product.builder().name(null).build(),
                Product.builder().name("").build(),

                Product.builder().name("Abc-123").build(),
                Product.builder().name("Abc-123").key(null).build(),
                Product.builder().name("Abc-123").key("").build(),
                Product.builder().name("  Abc  123 ").key("").build(),
                Product.builder().name("Abc  123").key("").build(),

                Product.builder().name("Abc-123").key("Abc-123").ownerId(null).build(),
                Product.builder().name("Abc-123").key("Abc-123").ownerId("").build(),
                Product.builder().name("Abc-123").key("Abc-123").ownerId("10").build(),
                Product.builder().name("Abc  123").key("Abc-123").ownerId("10").build(),

                Product.builder().name("Abc-_  123-_").key("Abc-123").ownerId("10").build(),
                Product.builder().name("Abc-_  123-_").key("Abc-_-123-_").ownerId("10").build()
        );
        for (Product p : products) {
            PRODUCTS_CONTROLLER.validateInput(p, UPDATE);
        }
    }
}