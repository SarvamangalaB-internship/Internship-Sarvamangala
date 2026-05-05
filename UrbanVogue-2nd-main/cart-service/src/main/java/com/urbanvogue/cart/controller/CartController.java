package com.urbanvogue.cart.controller;

import com.urbanvogue.cart.model.Cart;
import com.urbanvogue.cart.model.CartItem;
import com.urbanvogue.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    // Notice we do NOT pass username in the body! 
    // It is injected securely via the header from the API Gateway.

    @GetMapping
    public Cart getCart(@RequestHeader("X-Logged-In-User") String username) {
        return cartService.getCartByUsername(username);
    }

    @PostMapping("/add")
    public Cart addItem(@RequestHeader("X-Logged-In-User") String username,
                        @RequestBody CartItem item) {
        return cartService.addItemToCart(username, item);
    }

    @DeleteMapping("/clear")
    public Cart clearCart(@RequestHeader("X-Logged-In-User") String username) {
        return cartService.clearCart(username);
    }
}
