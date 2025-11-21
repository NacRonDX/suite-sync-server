package com.nacrondx.suitesync.controller;

import com.nacrondx.suitesync.api.RoomsApi;
import com.nacrondx.suitesync.model.room.AvailabilityResponse;
import com.nacrondx.suitesync.model.room.CreateRoomRequest;
import com.nacrondx.suitesync.model.room.RoomPageResponse;
import com.nacrondx.suitesync.model.room.RoomResponse;
import com.nacrondx.suitesync.model.room.RoomType;
import com.nacrondx.suitesync.model.room.UpdateRoomRequest;
import com.nacrondx.suitesync.service.RoomService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RoomController implements RoomsApi {
  private final RoomService roomService;

  @Override
  public ResponseEntity<RoomPageResponse> getAllRooms(
      LocalDate checkInDate,
      LocalDate checkOutDate,
      Integer numberOfGuests,
      RoomType roomType,
      Double minPrice,
      Double maxPrice,
      Integer page,
      Integer size) {
    log.info(
        "Received request to get all rooms - checkIn: {}, checkOut: {}, guests: {}, type: {}, minPrice: {}, maxPrice: {}, page: {}, size: {}",
        checkInDate,
        checkOutDate,
        numberOfGuests,
        roomType,
        minPrice,
        maxPrice,
        page,
        size);
    return ResponseEntity.ok(
        roomService.getAllRooms(
            checkInDate, checkOutDate, numberOfGuests, roomType, minPrice, maxPrice, page, size));
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public ResponseEntity<RoomResponse> createRoom(CreateRoomRequest createRoomRequest) {
    log.info("Received request to create room: {}", createRoomRequest.getRoomNumber());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(roomService.createRoom(createRoomRequest));
  }

  @Override
  public ResponseEntity<RoomResponse> getRoomById(Long roomId) {
    log.info("Received request to get room with ID: {}", roomId);
    return ResponseEntity.ok(roomService.getRoomById(roomId));
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public ResponseEntity<RoomResponse> updateRoom(Long roomId, UpdateRoomRequest updateRoomRequest) {
    log.info("Received request to update room with ID: {}", roomId);
    return ResponseEntity.ok(roomService.updateRoom(roomId, updateRoomRequest));
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteRoom(Long roomId) {
    log.info("Received request to delete room with ID: {}", roomId);
    roomService.deleteRoom(roomId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<AvailabilityResponse> checkRoomAvailability(
      Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
    log.info(
        "Received request to check availability for room ID: {} from {} to {}",
        roomId,
        checkInDate,
        checkOutDate);
    return ResponseEntity.ok(roomService.checkRoomAvailability(roomId, checkInDate, checkOutDate));
  }
}
