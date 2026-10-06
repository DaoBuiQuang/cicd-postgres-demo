package com.example.cicddemo.controller;

import com.example.cicddemo.entity.AppUser;
import com.example.cicddemo.entity.CustomerOrder;
import com.example.cicddemo.entity.OrderStatus;
import com.example.cicddemo.entity.Product;
import com.example.cicddemo.repository.AppUserRepository;
import com.example.cicddemo.repository.CustomerOrderRepository;
import com.example.cicddemo.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CustomerOrderRepository orderRepository;
    private final AppUserRepository userRepository;
    private final ProductRepository productRepository;

    public OrderController(CustomerOrderRepository orderRepository,
                           AppUserRepository userRepository,
                           ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<CustomerOrder> findAll() {
        return orderRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerOrder create(@RequestBody CreateOrderRequest request) {
        AppUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (request.quantity() == null || request.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than 0");
        }

        CustomerOrder order = new CustomerOrder(user, product, request.quantity(), OrderStatus.NEW);
        return orderRepository.save(order);
    }

    public record CreateOrderRequest(Long userId, Long productId, Integer quantity) {
    }
}
