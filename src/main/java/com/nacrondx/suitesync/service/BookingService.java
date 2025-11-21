package com.nacrondx.suitesync.service;

import com.nacrondx.suitesync.model.booking.BookingPageResponse;
import com.nacrondx.suitesync.model.booking.BookingResponse;
import com.nacrondx.suitesync.model.booking.BookingStatus;
import com.nacrondx.suitesync.model.booking.CreateBookingRequest;
import com.nacrondx.suitesync.model.booking.UpdateBookingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

  @Transactional
  public BookingResponse createBooking(CreateBookingRequest request) {
    log.info("Creating booking for room ID: {}", request.getRoomId());
    // TODO: Implement booking creation logic
    throw new UnsupportedOperationException("Booking creation not yet implemented");
  }

  @Transactional(readOnly = true)
  public BookingResponse getBookingById(Long bookingId) {
    log.info("Fetching booking with ID: {}", bookingId);
    // TODO: Implement booking retrieval logic
    throw new UnsupportedOperationException("Booking retrieval not yet implemented");
  }

  @Transactional(readOnly = true)
  public BookingPageResponse getAllBookings(BookingStatus status, Integer page, Integer size) {
    log.info("Fetching all bookings with status: {}, page: {}, size: {}", status, page, size);
    // TODO: Implement booking listing logic
    throw new UnsupportedOperationException("Booking listing not yet implemented");
  }

  @Transactional
  public BookingResponse updateBooking(Long bookingId, UpdateBookingRequest request) {
    log.info("Updating booking with ID: {}", bookingId);
    // TODO: Implement booking update logic
    throw new UnsupportedOperationException("Booking update not yet implemented");
  }

  @Transactional
  public BookingResponse cancelBooking(Long bookingId) {
    log.info("Cancelling booking with ID: {}", bookingId);
    // TODO: Implement booking cancellation logic
    throw new UnsupportedOperationException("Booking cancellation not yet implemented");
  }
}
