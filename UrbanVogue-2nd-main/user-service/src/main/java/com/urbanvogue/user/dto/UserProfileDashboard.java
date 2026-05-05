package com.urbanvogue.user.dto;

import com.urbanvogue.user.model.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDashboard {
    private UserProfile profile;
    private List<Object> orderHistory; // Using Object to represent raw JSON from order-service

    public UserProfile getProfile() { return profile; }
    public void setProfile(UserProfile profile) { this.profile = profile; }
    
    public List<Object> getOrderHistory() { return orderHistory; }
    public void setOrderHistory(List<Object> orderHistory) { this.orderHistory = orderHistory; }
}
