package com.urbanvogue.notification.service;

import com.urbanvogue.notification.dto.NotificationRequest;
import com.urbanvogue.notification.model.Notification;
import com.urbanvogue.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

        @Autowired
        private NotificationRepository notificationRepository;

        // ─────────────────────────────────────────
        // SEND: Create and send a notification
        // ─────────────────────────────────────────
        public Notification sendNotification(
                        NotificationRequest request) {

                // ── Validate required fields ──
                if (request.getRecipientUsername() == null ||
                                request.getRecipientUsername().trim().isEmpty()) {
                        throw new RuntimeException(
                                        "Recipient username is required.");
                }

                if (request.getMessage() == null ||
                                request.getMessage().trim().isEmpty()) {
                        throw new RuntimeException(
                                        "Notification message is required.");
                }

                // ── Validate channel ──
                if (request.getChannel() == null ||
                                (!request.getChannel().equalsIgnoreCase("EMAIL") &&
                                                !request.getChannel().equalsIgnoreCase("SMS"))) {
                        throw new RuntimeException(
                                        "Invalid channel. Use EMAIL or SMS.");
                }

                // ── Build notification ──
                Notification notification = new Notification();
                notification.setRecipientUsername(
                                request.getRecipientUsername());
                notification.setType(
                                request.getType().toUpperCase());
                notification.setMessage(request.getMessage());
                notification.setChannel(
                                request.getChannel().toUpperCase());
                notification.setReferenceId(request.getReferenceId());
                notification.setStatus("PENDING");
                notification.setCreatedAt(LocalDateTime.now());

                // ── Save first ──
                Notification saved = notificationRepository.save(notification);

                // ── Simulate sending ──
                // In production: replace with JavaMailSender
                // or Twilio SMS API
                boolean sent = simulateSending(saved);

                if (sent) {
                        saved.setStatus("SENT");
                        saved.setSentAt(LocalDateTime.now());

                        // Print to console (simulating real send)
                        printNotification(saved);

                } else {
                        saved.setStatus("FAILED");
                }

                return notificationRepository.save(saved);
        }

        // ─────────────────────────────────────────
        // PREDEFINED: Order Placed Notification
        // Called by Order Service after order created
        // ─────────────────────────────────────────
        public Notification notifyOrderPlaced(
                        String username, Long orderId, Double amount) {

                NotificationRequest request = new NotificationRequest();
                request.setRecipientUsername(username);
                request.setType("ORDER_PLACED");
                request.setChannel("EMAIL");
                request.setReferenceId(orderId);
                request.setMessage(
                                "Dear " + username + ", " +
                                                "Your Order #" + orderId +
                                                " has been placed successfully! " +
                                                "Total Amount: Rs." + amount +
                                                ". We will notify you once it ships. " +
                                                "Thank you for shopping with UrbanVogue!");

                return sendNotification(request);
        }

        // ─────────────────────────────────────────
        // PREDEFINED: Payment Success Notification
        // ─────────────────────────────────────────
        public Notification notifyPaymentSuccess(
                        String username, Long orderId,
                        Double amount, String transactionId) {

                NotificationRequest request = new NotificationRequest();
                request.setRecipientUsername(username);
                request.setType("PAYMENT_SUCCESS");
                request.setChannel("SMS");
                request.setReferenceId(orderId);
                request.setMessage(
                                "UrbanVogue: Payment of Rs." + amount +
                                                " received for Order #" + orderId +
                                                ". Transaction ID: " + transactionId +
                                                ". Your order is now confirmed!");

                return sendNotification(request);
        }

        // ─────────────────────────────────────────
        // PREDEFINED: Payment Failed Notification
        // ─────────────────────────────────────────
        public Notification notifyPaymentFailed(
                        String username, Long orderId,
                        Double amount) {

                NotificationRequest request = new NotificationRequest();
                request.setRecipientUsername(username);
                request.setType("PAYMENT_FAILED");
                request.setChannel("SMS");
                request.setReferenceId(orderId);
                request.setMessage(
                                "UrbanVogue: Payment of Rs." + amount +
                                                " FAILED for Order #" + orderId +
                                                ". Please retry your payment. " +
                                                "Your order is still pending.");

                return sendNotification(request);
        }

        // ─────────────────────────────────────────
        // PREDEFINED: Order Shipped Notification
        // ─────────────────────────────────────────
        public Notification notifyOrderShipped(
                        String username, Long orderId) {

                NotificationRequest request = new NotificationRequest();
                request.setRecipientUsername(username);
                request.setType("ORDER_SHIPPED");
                request.setChannel("EMAIL");
                request.setReferenceId(orderId);
                request.setMessage(
                                "Dear " + username + ", " +
                                                "Great news! Your Order #" + orderId +
                                                " has been shipped! " +
                                                "Expected delivery within 3-5 business days. " +
                                                "Track your order on UrbanVogue app.");

                return sendNotification(request);
        }

        // ─────────────────────────────────────────
        // PREDEFINED: Order Delivered Notification
        // ─────────────────────────────────────────
        public Notification notifyOrderDelivered(
                        String username, Long orderId) {

                NotificationRequest request = new NotificationRequest();
                request.setRecipientUsername(username);
                request.setType("ORDER_DELIVERED");
                request.setChannel("EMAIL");
                request.setReferenceId(orderId);
                request.setMessage(
                                "Dear " + username + ", " +
                                                "Your Order #" + orderId +
                                                " has been delivered successfully! " +
                                                "We hope you love your purchase. " +
                                                "Please rate your experience on UrbanVogue. " +
                                                "Thank you for shopping with us!");

                return sendNotification(request);
        }

        // ─────────────────────────────────────────
        // PREDEFINED: Order Cancelled Notification
        // ─────────────────────────────────────────
        public Notification notifyOrderCancelled(
                        String username, Long orderId) {

                NotificationRequest request = new NotificationRequest();
                request.setRecipientUsername(username);
                request.setType("ORDER_CANCELLED");
                request.setChannel("EMAIL");
                request.setReferenceId(orderId);
                request.setMessage(
                                "Dear " + username + ", " +
                                                "Your Order #" + orderId +
                                                " has been cancelled as requested. " +
                                                "If you paid online, refund will be " +
                                                "processed within 5-7 business days. " +
                                                "We hope to see you again at UrbanVogue!");

                return sendNotification(request);
        }

        // ─────────────────────────────────────────
        // PREDEFINED: Refund Initiated Notification
        // ─────────────────────────────────────────
        public Notification notifyRefundInitiated(
                        String username, Long orderId, Double amount) {

                NotificationRequest request = new NotificationRequest();
                request.setRecipientUsername(username);
                request.setType("REFUND_INITIATED");
                request.setChannel("SMS");
                request.setReferenceId(orderId);
                request.setMessage(
                                "UrbanVogue: Refund of Rs." + amount +
                                                " has been initiated for Order #" + orderId +
                                                ". Amount will be credited within " +
                                                "5-7 business days to your original " +
                                                "payment method.");

                return sendNotification(request);
        }

        // ─────────────────────────────────────────
        // PREDEFINED: Welcome Notification
        // Called when new user registers
        // ─────────────────────────────────────────
        public Notification notifyWelcome(String username) {

                NotificationRequest request = new NotificationRequest();
                request.setRecipientUsername(username);
                request.setType("WELCOME");
                request.setChannel("EMAIL");
                request.setReferenceId(null);
                request.setMessage(
                                "Welcome to UrbanVogue, " + username + "! " +
                                                "Thank you for joining us. " +
                                                "Discover the latest fashion trends and " +
                                                "exclusive deals just for you. " +
                                                "Happy Shopping!");

                return sendNotification(request);
        }

        // ─────────────────────────────────────────
        // GET: All notifications for a user
        // ─────────────────────────────────────────
        public List<Notification> getUserNotifications(
                        String username) {

                List<Notification> notifications = notificationRepository
                                .findByRecipientUsername(username);

                if (notifications.isEmpty()) {
                        throw new RuntimeException(
                                        "No notifications found for: " + username);
                }
                return notifications;
        }

        // ─────────────────────────────────────────
        // GET: All notifications (Admin)
        // ─────────────────────────────────────────
        public List<Notification> getAllNotifications() {
                return notificationRepository.findAll();
        }

        // ─────────────────────────────────────────
        // GET: One notification by ID
        // ─────────────────────────────────────────
        public Notification getNotificationById(Long id) {
                return notificationRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Notification not found with id: " + id));
        }

        // ─────────────────────────────────────────
        // GET: By type
        // ─────────────────────────────────────────
        public List<Notification> getByType(String type) {
                return notificationRepository
                                .findByType(type.toUpperCase());
        }

        // ─────────────────────────────────────────
        // GET: By status
        // ─────────────────────────────────────────
        public List<Notification> getByStatus(String status) {
                return notificationRepository
                                .findByStatus(status.toUpperCase());
        }

        // ─────────────────────────────────────────
        // PRIVATE: Simulate sending email/SMS
        // In production → JavaMailSender or Twilio
        // ─────────────────────────────────────────
        private boolean simulateSending(Notification n) {
                // Always succeeds in simulation
                // In production this would be
                // actual email/SMS API call
                return true;
        }

        // ─────────────────────────────────────────
        // PRIVATE: Print notification to console
        // This simulates what email/SMS would look like
        // ─────────────────────────────────────────
        private void printNotification(Notification n) {
                System.out.println(
                                "\n");
                System.out.println(
                                "     URBANVOGUE NOTIFICATION SENT     ");
                System.out.println(
                                " ");
                System.out.println(
                                " Channel   : " + n.getChannel());
                System.out.println(
                                " Type      : " + n.getType());
                System.out.println(
                                " To        : " + n.getRecipientUsername());
                System.out.println(
                                " Message   : " + n.getMessage());
                System.out.println(
                                " Sent At   : " + n.getSentAt());
                System.out.println(
                                "\n");
        }
}