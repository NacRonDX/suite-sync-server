package com.nacrondx.suitesync.service;

import com.nacrondx.suitesync.entity.Room;
import com.nacrondx.suitesync.exception.ResourceNotFoundException;
import com.nacrondx.suitesync.model.room.AvailabilityResponse;
import com.nacrondx.suitesync.model.room.CreateRoomRequest;
import com.nacrondx.suitesync.model.room.RoomPageResponse;
import com.nacrondx.suitesync.model.room.RoomResponse;
import com.nacrondx.suitesync.model.room.RoomStatus;
import com.nacrondx.suitesync.model.room.RoomType;
import com.nacrondx.suitesync.model.room.UpdateRoomRequest;
import com.nacrondx.suitesync.repository.RoomRepository;
import com.nacrondx.suitesync.repository.RoomSpecification;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {
  private final RoomRepository roomRepository;

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

    int pageNumber = page != null ? page : 0;
    int pageSize = size != null ? size : 20;

    var entityRoomType = roomType != null ? Room.RoomType.valueOf(roomType.name()) : null;

    var specification =
        RoomSpecification.withFilters(
            checkInDate, checkOutDate, numberOfGuests, entityRoomType, minPrice, maxPrice);

    var pageable = PageRequest.of(pageNumber, pageSize);
    var roomPage = roomRepository.findAll(specification, pageable);

    var response = new RoomPageResponse();
    response.setContent(roomPage.getContent().stream().map(this::mapToRoomResponse).toList());
    response.setPage(roomPage.getNumber());
    response.setSize(roomPage.getSize());
    response.setTotalElements(roomPage.getTotalElements());
    response.setTotalPages(roomPage.getTotalPages());

    log.info(
        "Successfully fetched {} rooms out of {} total",
        roomPage.getNumberOfElements(),
        roomPage.getTotalElements());

    return response;
  }

  @Transactional
  public RoomResponse createRoom(CreateRoomRequest request) {
    log.info("Creating room: {}", request.getRoomNumber());

    var room =
        Room.builder()
            .roomNumber(request.getRoomNumber())
            .roomType(Room.RoomType.valueOf(request.getRoomType().name()))
            .maxOccupancy(request.getMaxOccupancy())
            .pricePerNight(request.getPricePerNight())
            .size(request.getSize())
            .floor(request.getFloor())
            .description(request.getDescription())
            .status(Room.RoomStatus.AVAILABLE)
            .build();

    if (request.getAmenities() != null && !request.getAmenities().isEmpty()) {
      room.getAmenities().addAll(request.getAmenities());
    }

    if (request.getImages() != null && !request.getImages().isEmpty()) {
      room.getImages().addAll(request.getImages().stream().map(URI::toString).toList());
    }

    var savedRoom = roomRepository.save(room);
    log.info("Successfully created room: {}", savedRoom.getRoomNumber());
    return mapToRoomResponse(savedRoom);
  }

  @Transactional(readOnly = true)
  public RoomResponse getRoomById(Long roomId) {
    log.info("Fetching room with ID: {}", roomId);

    var room =
        roomRepository
            .findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

    log.info("Successfully fetched room: {}", room.getRoomNumber());
    return mapToRoomResponse(room);
  }

  @Transactional
  public RoomResponse updateRoom(Long roomId, UpdateRoomRequest request) {
    log.info("Updating room with ID: {}", roomId);

    var room =
        roomRepository
            .findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

    if (request.getRoomType() != null) {
      room.setRoomType(Room.RoomType.valueOf(request.getRoomType().name()));
    }
    if (request.getMaxOccupancy() != null) {
      room.setMaxOccupancy(request.getMaxOccupancy());
    }
    if (request.getPricePerNight() != null) {
      room.setPricePerNight(request.getPricePerNight());
    }
    if (request.getSize() != null) {
      room.setSize(request.getSize());
    }
    if (request.getFloor() != null) {
      room.setFloor(request.getFloor());
    }
    if (request.getDescription() != null) {
      room.setDescription(request.getDescription());
    }
    if (request.getStatus() != null) {
      room.setStatus(Room.RoomStatus.valueOf(request.getStatus().name()));
    }
    if (request.getAmenities() != null) {
      room.getAmenities().clear();
      room.getAmenities().addAll(request.getAmenities());
    }
    if (request.getImages() != null) {
      room.getImages().clear();
      room.getImages().addAll(request.getImages().stream().map(URI::toString).toList());
    }

    var updatedRoom = roomRepository.save(room);
    log.info("Successfully updated room: {}", updatedRoom.getRoomNumber());
    return mapToRoomResponse(updatedRoom);
  }

  @Transactional
  public void deleteRoom(Long roomId) {
    log.info("Deleting room with ID: {}", roomId);

    var room =
        roomRepository
            .findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

    roomRepository.delete(room);
    log.info("Successfully deleted room: {}", room.getRoomNumber());
  }

  @Transactional(readOnly = true)
  public AvailabilityResponse checkRoomAvailability(
      Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
    log.info(
        "Checking availability for room ID: {} from {} to {}", roomId, checkInDate, checkOutDate);
    // TODO: Implement availability check logic
    throw new UnsupportedOperationException("Room availability check not yet implemented");
  }

  private RoomResponse mapToRoomResponse(Room room) {
    var response = new RoomResponse();
    response.setId(room.getId());
    response.setRoomNumber(room.getRoomNumber());
    response.setRoomType(RoomType.fromValue(room.getRoomType().name()));
    response.setMaxOccupancy(room.getMaxOccupancy());
    response.setPricePerNight(room.getPricePerNight());
    response.setSize(room.getSize());
    response.setFloor(room.getFloor());
    response.setDescription(room.getDescription());
    response.setStatus(RoomStatus.fromValue(room.getStatus().name()));
    response.setAmenities(room.getAmenities());

    if (room.getImages() != null && !room.getImages().isEmpty()) {
      response.setImages(room.getImages().stream().map(URI::create).toList());
    }

    response.setCreatedAt(OffsetDateTime.of(room.getCreatedAt(), ZoneOffset.UTC));
    response.setUpdatedAt(OffsetDateTime.of(room.getUpdatedAt(), ZoneOffset.UTC));
    return response;
  }
}
