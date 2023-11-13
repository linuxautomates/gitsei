package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductUtils {

    public static List<Product> createProducts(ProductService productService, final String company, int n) throws SQLException {
        List<Product> products = new ArrayList<>();
        for(int i=0; i<n; i++){
            Product p = Product.builder()
                    .name("name-" + i)
                    .key("key" + i)
                    .description("desc-" +i)
                    .build();
            String productId = productService.insert(company,p);
            products.add(p.toBuilder().id(productId).build());
        }
        return products;
    }

    public static Product createProduct(ProductService productService, final String company, int i, User user) throws SQLException {
        Product.ProductBuilder bldr = Product.builder()
                .name("name-" + i)
                .key("key" + i)
                .description("desc-" +i);
        if(user != null){
            bldr.ownerId(user.getId());
        }
        Product p = bldr.build();
        String productId = productService.insert(company,p);
        return p.toBuilder().id(productId).build();
    }

    public static List<Product> createProducts(ProductService productService, final String company, List<User> users) throws SQLException {
        List<Product> products = new ArrayList<>();
        for(int i=0; i<users.size(); i++){
            Product p = createProduct(productService, company, i, users.get(i));
            products.add(p);
        }
        return products;
    }
}
