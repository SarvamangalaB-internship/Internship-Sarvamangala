package com.urbanvogue.cart.service;

import com.urbanvogue.cart.model.Cart;
import com.urbanvogue.cart.model.CartItem;
import com.urbanvogue.cart.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    public Cart getCartByUsername(String username) {
        return cartRepository.findByUsername(username)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUsername(username);
                    return cartRepository.save(newCart);
                });
    }

    public Cart addItemToCart(String username, CartItem item) {
        Cart cart = getCartByUsername(username);
        
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(item.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem found = existingItem.get();
            found.setQuantity(found.getQuantity() + item.getQuantity());
        } else {
            cart.getItems().add(item);
        }
        
        return cartRepository.save(cart);
    }

    public Cart clearCart(String username) {
        Cart cart = getCartByUsername(username);
        cart.getItems().clear();
        return cartRepository.save(cart);
    }
}
