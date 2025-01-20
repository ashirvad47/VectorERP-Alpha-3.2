package com.example.erpsystem.service;

import com.example.erpsystem.model.Product;
import com.example.erpsystem.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public Product updateProduct(Long id, Product updatedProduct) {
        Product product = getProductById(id);
        product.setName(updatedProduct.getName());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setStock(updatedProduct.getStock());
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
    @Transactional
    public void updateProductStock(Long productId, Integer quantity, String transactionType) {
        Product product = getProductById(productId);
        
        if (transactionType.equals("IN")) {
            product.setStock(product.getStock() + quantity);
        } else if (transactionType.equals("OUT")) {
            if (product.getStock() < quantity) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName());
            }
            product.setStock(product.getStock() - quantity);
        } else {
            throw new IllegalArgumentException("Invalid transaction type. Must be either 'IN' or 'OUT'");
        }
        
        productRepository.save(product);
    }
}
