package com.example.cicddemo.config;

import com.example.cicddemo.entity.AppUser;
import com.example.cicddemo.entity.CustomerOrder;
import com.example.cicddemo.entity.OrderStatus;
import com.example.cicddemo.entity.Product;
import com.example.cicddemo.repository.AppUserRepository;
import com.example.cicddemo.repository.CustomerOrderRepository;
import com.example.cicddemo.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(AppUserRepository userRepository,
                               ProductRepository productRepository,
                               CustomerOrderRepository orderRepository) {
        return args -> {
            if (userRepository.count() == 0 && productRepository.count() == 0) {
                AppUser user1 = userRepository.save(new AppUser("admin", "admin@example.com"));
                AppUser user2 = userRepository.save(new AppUser("demo", "demo@example.com"));

                Product laptop = productRepository.save(
                        new Product("Demo Laptop", new BigDecimal("1500.00"), 10));
                Product keyboard = productRepository.save(
                        new Product("Mechanical Keyboard", new BigDecimal("85.50"), 25));
                Product mouse = productRepository.save(
                        new Product("Wireless Mouse", new BigDecimal("35.00"), 40));

                orderRepository.save(new CustomerOrder(user1, laptop, 1, OrderStatus.PAID));
                orderRepository.save(new CustomerOrder(user2, keyboard, 2, OrderStatus.NEW));
            }
        };
    }
}
