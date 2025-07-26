package com.commerce.platform.product.service.domain;


import com.commerce.platform.product.service.domain.dto.create.CreateProductCommand;
import com.commerce.platform.product.service.domain.dto.create.CreateProductResponse;
import com.commerce.platform.product.service.domain.dto.query.SearchProductsQuery;
import com.commerce.platform.product.service.domain.dto.query.SearchProductsResponse;
import com.commerce.platform.product.service.domain.entity.Product;
import com.commerce.platform.product.service.domain.mapper.ProductDataMapper;
import com.commerce.platform.product.service.domain.ports.input.service.ProductApplicationService;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Validated
@Service
class ProductApplicationServiceImpl implements ProductApplicationService {

    private final ProductDomainService productDomainService;
    private final ProductRepository productRepository;
    private final ProductDataMapper productDataMapper;

    public ProductApplicationServiceImpl(ProductDomainService productDomainService,
                                         ProductRepository productRepository,
                                         ProductDataMapper productDataMapper) {
        this.productDomainService = productDomainService;
        this.productRepository = productRepository;
        this.productDataMapper = productDataMapper;
    }

    @Override
    public CreateProductResponse createProduct(CreateProductCommand createProductCommand) {
        ZonedDateTime requestTime = ZonedDateTime.now();
        
        Product product = productDataMapper.createProductCommandToProduct(createProductCommand);
        Product createdProduct = productDomainService.createProduct(product, requestTime);
        Product savedProduct = productRepository.save(createdProduct);
        
        log.info("Product is created with id: {} at {}", savedProduct.getId().getValue(), requestTime);
        return productDataMapper.productToCreateProductResponse(savedProduct, "Product created successfully");
    }

    @Override
    public SearchProductsResponse searchProducts(SearchProductsQuery searchProductsQuery) {
        List<Product> products = productRepository.findByIds(searchProductsQuery.getProductIds());
        log.info("Found {} products for requested {} product IDs", 
                products.size(), searchProductsQuery.getProductIds().size());
        return productDataMapper.productsToSearchProductsResponse(products);
    }
} 