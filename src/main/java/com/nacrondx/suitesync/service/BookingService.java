package com.nacrondx.suitesync.service;

import com.nacrondx.suitesync.entity.Booking;
import com.nacrondx.suitesync.exception.ResourceNotFoundException;
import com.nacrondx.suitesync.exception.RoomNotAvailableException;
import com.nacrondx.suitesync.model.booking.BookingPageResponse;
import com.nacrondx.suitesync.model.booking.BookingResponse;
import com.nacrondx.suitesync.model.booking.BookingStatus;
import com.nacrondx.suitesync.model.booking.CreateBookingRequest;
import com.nacrondx.suitesync.model.booking.UpdateBookingRequest;
import com.nacrondx.suitesync.repository.BookingRepository;
import com.nacrondx.suitesync.repository.RoomRepository;
import com.nacrondx.suitesync.repository.UserRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
  private final BookingRepository bookingRepository;
  private final RoomRepository roomRepository;
  private final UserRepository userRepository;

  @Transactional
  public BookingResponse createBooking(CreateBookingRequest request) {
    log.info("Creating booking for room ID: {}", request.getRoomId());

    if (request.getCheckOutDate().isBefore(request.getCheckInDate())
        || request.getCheckOutDate().isEqual(request.getCheckInDate())) {
      throw new IllegalArgumentException("Check-out date must be after check-in date");
    }

    var room =
        roomRepository
            .findById(request.getRoomId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Room not found with ID: " + request.getRoomId()));

    if (request.getNumberOfGuests() > room.getMaxOccupancy()) {
      throw new IllegalArgumentException(
          "Number of guests exceeds room capacity of " + room.getMaxOccupancy());
    }

    var overlappingBookings =
        bookingRepository.findOverlappingBookings(
            request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate());

    if (!overlappingBookings.isEmpty()) {
      throw new RoomNotAvailableException("Room is not available for the selected dates");
    }

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var jwt = (Jwt) authentication.getPrincipal();
    var userId = jwt.getClaim("userId");

    var user =
        userRepository
            .findById(((Number) userId).longValue())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

    long numberOfNights =
        java.time.temporal.ChronoUnit.DAYS.between(
            request.getCheckInDate(), request.getCheckOutDate());
    double totalPrice = numberOfNights * room.getPricePerNight();

    var booking =
        Booking.builder()
            .user(user)
            .room(room)
            .checkInDate(request.getCheckInDate())
            .checkOutDate(request.getCheckOutDate())
            .numberOfGuests(request.getNumberOfGuests())
            .specialRequests(request.getSpecialRequests())
            .totalPrice(totalPrice)
            .status(Booking.BookingStatus.PENDING)
            .build();

    var savedBooking = bookingRepository.save(booking);
    log.info("Successfully created booking with ID: {}", savedBooking.getId());
    return mapToBookingResponse(savedBooking);
  }

  @Transactional(readOnly = true)
  public BookingResponse getBookingById(Long bookingId) {
    log.info("Fetching booking with ID: {}", bookingId);

    var booking =
        bookingRepository
            .findById(bookingId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

    log.info("Successfully fetched booking with ID: {}", bookingId);
    return mapToBookingResponse(booking);
  }

  @Transactional(readOnly = true)
  public BookingPageResponse getAllBookings(BookingStatus status, Integer page, Integer size) {
    log.info("Fetching all bookings with status: {}, page: {}, size: {}", status, page, size);

    int pageNumber = page != null ? page : 0;
    int pageSize = size != null ? size : 20;

    var pageable = PageRequest.of(pageNumber, pageSize);
    var bookingPage =
        status != null
            ? bookingRepository.findByStatus(Booking.BookingStatus.valueOf(status.name()), pageable)
            : bookingRepository.findAll(pageable);

    var response = new BookingPageResponse();
    response.setContent(bookingPage.getContent().stream().map(this::mapToBookingResponse).toList());
    response.setPage(bookingPage.getNumber());
    response.setSize(bookingPage.getSize());
    response.setTotalElements(bookingPage.getTotalElements());
    response.setTotalPages(bookingPage.getTotalPages());

    log.info(
        "Successfully fetched {} bookings out of {} total",
        bookingPage.getNumberOfElements(),
        bookingPage.getTotalElements());

    return response;
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

  private BookingResponse mapToBookingResponse(Booking booking) {
    var response = new BookingResponse();
    response.setId(booking.getId());
    response.setUserId(booking.getUser().getId());
    response.setRoomId(booking.getRoom().getId());
    response.setCheckInDate(booking.getCheckInDate());
    response.setCheckOutDate(booking.getCheckOutDate());
    response.setNumberOfGuests(booking.getNumberOfGuests());
    response.setStatus(BookingStatus.fromValue(booking.getStatus().name()));
    response.setSpecialRequests(booking.getSpecialRequests());
    response.setTotalPrice(booking.getTotalPrice());
    response.setCreatedAt(OffsetDateTime.of(booking.getCreatedAt(), ZoneOffset.UTC));
    response.setUpdatedAt(OffsetDateTime.of(booking.getUpdatedAt(), ZoneOffset.UTC));
    return response;
  }
}
