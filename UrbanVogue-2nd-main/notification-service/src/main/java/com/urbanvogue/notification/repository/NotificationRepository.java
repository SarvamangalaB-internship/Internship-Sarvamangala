package com.urbanvogue.notification.repository;

import com.urbanvogue.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    // All notifications for a user
    List<Notification> findByRecipientUsername(
            String recipientUsername
    );

    // All notifications by type
    List<Notification> findByType(String type);

    // All notifications by status
    List<Notification> findByStatus(String status);

    // All notifications for a user by type
    List<Notification> findByRecipientUsernameAndType(
            String recipientUsername, String type
    );
}