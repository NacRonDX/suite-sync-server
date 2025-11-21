package com.nacrondx.suitesync.service;

import com.nacrondx.suitesync.model.room.AvailabilityResponse;
import com.nacrondx.suitesync.model.room.CreateRoomRequest;
import com.nacrondx.suitesync.model.room.RoomPageResponse;
import com.nacrondx.suitesync.model.room.RoomResponse;
import com.nacrondx.suitesync.model.room.RoomType;
import com.nacrondx.suitesync.model.room.UpdateRoomRequest;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

  @Transactional(readOnly = true)
  public RoomPageResponse getAllRooms(
      LocalDate checkInDate,
      LocalDate checkOutDate,
      Integer numberOfGuests,
      RoomType roomType,
      Double minPrice,
      Double maxPrice,
      Integer page,
      Integer size) {
    log.info(
        "Fetching all rooms with filters - checkIn: {}, checkOut: {}, guests: {}, type: {}, minPrice: {}, maxPrice: {}, page: {}, size: {}",
        checkInDate,
        checkOutDate,
        numberOfGuests,
        roomType,
        minPrice,
        maxPrice,
        page,
        size);
    // TODO: Implement room listing with filters and availability check
    throw new UnsupportedOperationException("Room listing not yet implemented");
  }

  @Transactional
  public RoomResponse createRoom(CreateRoomRequest request) {
    log.info("Creating room: {}", request.getRoomNumber());
    // TODO: Implement room creation logic
    throw new UnsupportedOperationException("Room creation not yet implemented");
  }

  @Transactional(readOnly = true)
  public RoomResponse getRoomById(Long roomId) {
    log.info("Fetching room with ID: {}", roomId);
    // TODO: Implement room retrieval logic
    throw new UnsupportedOperationException("Room retrieval not yet implemented");
  }

  @Transactional
  public RoomResponse updateRoom(Long roomId, UpdateRoomRequest request) {
    log.info("Updating room with ID: {}", roomId);
    // TODO: Implement room update logic
    throw new UnsupportedOperationException("Room update not yet implemented");
  }

  @Transactional
  public void deleteRoom(Long roomId) {
    log.info("Deleting room with ID: {}", roomId);
    // TODO: Implement room deletion logic (check for active bookings first)
    throw new UnsupportedOperationException("Room deletion not yet implemented");
  }

  @Transactional(readOnly = true)
  public AvailabilityResponse checkRoomAvailability(
      Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
    log.info(
        "Checking availability for room ID: {} from {} to {}", roomId, checkInDate, checkOutDate);
    // TODO: Implement availability check logic
    throw new UnsupportedOperationException("Room availability check not yet implemented");
  }
}
