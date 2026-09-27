package com.oop.disaster.alert.repository;

import com.oop.disaster.alert.model.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
}
