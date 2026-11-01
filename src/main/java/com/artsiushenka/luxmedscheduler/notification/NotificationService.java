package com.artsiushenka.luxmedscheduler.notification;

import com.artsiushenka.luxmedscheduler.model.AppointmentSlot;

import java.util.List;

public interface NotificationService {

	void notifySlots(List<AppointmentSlot> slots);
}
