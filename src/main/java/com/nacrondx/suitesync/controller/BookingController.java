package com.nacrondx.suitesync.controller;

import com.nacrondx.suitesync.api.BookingsApi;
import com.nacrondx.suitesync.model.booking.BookingPageResponse;
import com.nacrondx.suitesync.model.booking.BookingResponse;
import com.nacrondx.suitesync.model.booking.BookingStatus;
import com.nacrondx.suitesync.model.booking.CreateBookingRequest;
import com.nacrondx.suitesync.model.booking.UpdateBookingRequest;
import com.nacrondx.suitesync.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BookingController implements BookingsApi {
  private final BookingService bookingService;

  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BookingResponse> createBooking(CreateBookingRequest createBookingRequest) {
    log.info(
        "Received request to create booking for room ID: {}", createBookingRequest.getRoomId());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(bookingService.createBooking(createBookingRequest));
  }

  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BookingResponse> getBookingById(Long bookingId) {
    log.info("Received request to get booking with ID: {}", bookingId);
    return ResponseEntity.ok(bookingService.getBookingById(bookingId));
  }

  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BookingPageResponse> getAllBookings(
      BookingStatus status, Integer page, Integer size) {
    log.info(
        "Received request to get all bookings with status: {}, page: {}, size: {}",
        status,
        page,
        size);
    return ResponseEntity.ok(bookingService.getAllBookings(status, page, size));
  }

  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BookingResponse> updateBooking(
      Long bookingId, UpdateBookingRequest updateBookingRequest) {
    log.info("Received request to update booking with ID: {}", bookingId);
    return ResponseEntity.ok(bookingService.updateBooking(bookingId, updateBookingRequest));
  }

  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BookingResponse> cancelBooking(Long bookingId) {
    log.info("Received request to cancel booking with ID: {}", bookingId);
    return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
  }
}
