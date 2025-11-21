package com.nacrondx.suitesync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nacrondx.suitesync.entity.Room;
import com.nacrondx.suitesync.model.room.RoomType;
import com.nacrondx.suitesync.repository.RoomRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {
  @Mock private RoomRepository roomRepository;

  @InjectMocks private RoomService roomService;

  private List<Room> sampleRooms;
  private Room singleRoom;
  private Room doubleRoom;
  private Room suiteRoom;

  @BeforeEach
  void setUp() {
    sampleRooms = new ArrayList<>();

    singleRoom =
        Room.builder()
            .id(1L)
            .roomNumber("101")
            .roomType(Room.RoomType.SINGLE)
            .maxOccupancy(1)
            .pricePerNight(99.99)
            .size(20.0)
            .floor(1)
            .description("Cozy single room with city view")
            .status(Room.RoomStatus.AVAILABLE)
            .amenities(List.of("WiFi", "TV", "Air Conditioning"))
            .images(new ArrayList<>())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    doubleRoom =
        Room.builder()
            .id(2L)
            .roomNumber("102")
            .roomType(Room.RoomType.DOUBLE)
            .maxOccupancy(2)
            .pricePerNight(149.99)
            .size(30.0)
            .floor(1)
            .description("Comfortable double room with queen-sized bed")
            .status(Room.RoomStatus.AVAILABLE)
            .amenities(List.of("WiFi", "TV", "Air Conditioning", "Mini Bar"))
            .images(new ArrayList<>())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    suiteRoom =
        Room.builder()
            .id(3L)
            .roomNumber("201")
            .roomType(Room.RoomType.SUITE)
            .maxOccupancy(4)
            .pricePerNight(299.99)
            .size(50.0)
            .floor(2)
            .description("Spacious suite with separate living area")
            .status(Room.RoomStatus.AVAILABLE)
            .amenities(List.of("WiFi", "TV", "Air Conditioning", "Mini Bar", "Safe", "Kitchenette"))
            .images(new ArrayList<>())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    sampleRooms.add(singleRoom);
    sampleRooms.add(doubleRoom);
    sampleRooms.add(suiteRoom);
  }

  @Test
  void getAllRoomsWithNoFiltersShouldReturnAllRooms() {
    var pageable = PageRequest.of(0, 20);
    var roomPage = new PageImpl<>(sampleRooms, pageable, sampleRooms.size());

    when(roomRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(roomPage);

    var response = roomService.getAllRooms(null, null, null, null, null, null, null, null);

    assertNotNull(response);
    assertEquals(3, response.getContent().size());
    assertEquals(0, response.getPage());
    assertEquals(20, response.getSize());
    assertEquals(3L, response.getTotalElements());
    assertEquals(1, response.getTotalPages());

    verify(roomRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void getAllRoomsWithRoomTypeFilterShouldReturnFilteredRooms() {
    var filteredRooms = List.of(singleRoom);
    var pageable = PageRequest.of(0, 20);
    var roomPage = new PageImpl<>(filteredRooms, pageable, filteredRooms.size());

    when(roomRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(roomPage);

    var response =
        roomService.getAllRooms(null, null, null, RoomType.SINGLE, null, null, null, null);

    assertNotNull(response);
    assertEquals(1, response.getContent().size());
    assertEquals("101", response.getContent().get(0).getRoomNumber());
    assertEquals(RoomType.SINGLE, response.getContent().get(0).getRoomType());

    verify(roomRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void getAllRoomsWithNumberOfGuestsFilterShouldReturnSuitableRooms() {
    var filteredRooms = List.of(doubleRoom, suiteRoom);
    var pageable = PageRequest.of(0, 20);
    var roomPage = new PageImpl<>(filteredRooms, pageable, filteredRooms.size());

    when(roomRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(roomPage);

    var response = roomService.getAllRooms(null, null, 2, null, null, null, null, null);

    assertNotNull(response);
    assertEquals(2, response.getContent().size());
    assertTrue(response.getContent().stream().allMatch(room -> room.getMaxOccupancy() >= 2));

    verify(roomRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void getAllRoomsWithPriceRangeFilterShouldReturnRoomsInRange() {
    var filteredRooms = List.of(singleRoom, doubleRoom);
    var pageable = PageRequest.of(0, 20);
    var roomPage = new PageImpl<>(filteredRooms, pageable, filteredRooms.size());

    when(roomRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(roomPage);

    var response = roomService.getAllRooms(null, null, null, null, 50.0, 200.0, null, null);

    assertNotNull(response);
    assertEquals(2, response.getContent().size());
    assertTrue(
        response.getContent().stream()
            .allMatch(room -> room.getPricePerNight() >= 50.0 && room.getPricePerNight() <= 200.0));

    verify(roomRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void getAllRoomsWithCustomPaginationShouldUseProvidedValues() {
    var firstPageRooms = List.of(singleRoom, doubleRoom);
    var pageable = PageRequest.of(0, 2);
    var roomPage = new PageImpl<>(firstPageRooms, pageable, sampleRooms.size());

    when(roomRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(roomPage);

    var response = roomService.getAllRooms(null, null, null, null, null, null, 0, 2);

    assertNotNull(response);
    assertEquals(2, response.getContent().size());
    assertEquals(0, response.getPage());
    assertEquals(2, response.getSize());
    assertEquals(3L, response.getTotalElements());
    assertEquals(2, response.getTotalPages());

    verify(roomRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void getAllRoomsWithSecondPageShouldReturnCorrectPage() {
    var secondPageRooms = List.of(suiteRoom);
    var pageable = PageRequest.of(1, 2);
    var roomPage = new PageImpl<>(secondPageRooms, pageable, sampleRooms.size());

    when(roomRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(roomPage);

    var response = roomService.getAllRooms(null, null, null, null, null, null, 1, 2);

    assertNotNull(response);
    assertEquals(1, response.getContent().size());
    assertEquals(1, response.getPage());
    assertEquals(2, response.getSize());

    verify(roomRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void getAllRoomsWithMultipleFiltersShouldApplyAllFilters() {
    var filteredRooms = List.of(doubleRoom);
    var pageable = PageRequest.of(0, 20);
    var roomPage = new PageImpl<>(filteredRooms, pageable, filteredRooms.size());

    when(roomRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(roomPage);

    var response =
        roomService.getAllRooms(null, null, 2, RoomType.DOUBLE, 100.0, 200.0, null, null);

    assertNotNull(response);
    assertEquals(1, response.getContent().size());
    assertEquals("102", response.getContent().get(0).getRoomNumber());
    assertEquals(RoomType.DOUBLE, response.getContent().get(0).getRoomType());
    assertEquals(2, response.getContent().get(0).getMaxOccupancy());

    verify(roomRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void getAllRoomsWithEmptyResultShouldReturnEmptyPage() {
    var emptyList = new ArrayList<>();
    var pageable = PageRequest.of(0, 20);
    var emptyPage = new PageImpl<>(emptyList, pageable, 0);

    when(roomRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(emptyPage);

    var response = roomService.getAllRooms(null, null, null, null, null, null, null, null);

    assertNotNull(response);
    assertEquals(0, response.getContent().size());
    assertEquals(0, response.getPage());
    assertEquals(20, response.getSize());
    assertEquals(0L, response.getTotalElements());
    assertEquals(0, response.getTotalPages());

    verify(roomRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void getAllRoomsWithDefaultPaginationShouldUsePage0Size20() {
    var pageable = PageRequest.of(0, 20);
    var roomPage = new PageImpl<>(sampleRooms, pageable, sampleRooms.size());

    when(roomRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(roomPage);

    var response = roomService.getAllRooms(null, null, null, null, null, null, null, null);

    assertNotNull(response);
    assertEquals(0, response.getPage());
    assertEquals(20, response.getSize());

    verify(roomRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void getAllRoomsShouldMapAllFieldsCorrectly() {
    var pageable = PageRequest.of(0, 20);
    var roomPage = new PageImpl<>(List.of(singleRoom), pageable, 1);

    when(roomRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(roomPage);

    var response = roomService.getAllRooms(null, null, null, null, null, null, null, null);

    assertNotNull(response);
    assertEquals(1, response.getContent().size());

    var roomResponse = response.getContent().get(0);
    assertEquals(1L, roomResponse.getId());
    assertEquals("101", roomResponse.getRoomNumber());
    assertEquals(RoomType.SINGLE, roomResponse.getRoomType());
    assertEquals(1, roomResponse.getMaxOccupancy());
    assertEquals(99.99, roomResponse.getPricePerNight());
    assertEquals(20.0, roomResponse.getSize());
    assertEquals(1, roomResponse.getFloor());
    assertEquals("Cozy single room with city view", roomResponse.getDescription());
    assertNotNull(roomResponse.getStatus());
    assertNotNull(roomResponse.getAmenities());
    assertEquals(3, roomResponse.getAmenities().size());
    assertTrue(roomResponse.getAmenities().contains("WiFi"));
    assertNotNull(roomResponse.getCreatedAt());
    assertNotNull(roomResponse.getUpdatedAt());

    verify(roomRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void getAllRoomsWithDatesShouldPassDatesToSpecification() {
    var checkIn = LocalDate.of(2024, 12, 20);
    var checkOut = LocalDate.of(2024, 12, 25);
    var pageable = PageRequest.of(0, 20);
    var roomPage = new PageImpl<>(sampleRooms, pageable, sampleRooms.size());

    when(roomRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(roomPage);

    var response = roomService.getAllRooms(checkIn, checkOut, null, null, null, null, null, null);

    assertNotNull(response);
    assertEquals(3, response.getContent().size());

    verify(roomRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void getAllRoomsWithMinPriceOnlyShouldFilterCorrectly() {
    var filteredRooms = List.of(doubleRoom, suiteRoom);
    var pageable = PageRequest.of(0, 20);
    var roomPage = new PageImpl<Room>(filteredRooms, pageable, filteredRooms.size());

    when(roomRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(roomPage);

    var response = roomService.getAllRooms(null, null, null, null, 100.0, null, null, null);

    assertNotNull(response);
    assertEquals(2, response.getContent().size());
    assertTrue(response.getContent().stream().allMatch(room -> room.getPricePerNight() >= 100.0));

    verify(roomRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void getAllRoomsWithMaxPriceOnlyShouldFilterCorrectly() {
    var filteredRooms = List.of(singleRoom, doubleRoom);
    var pageable = PageRequest.of(0, 20);
    var roomPage = new PageImpl<>(filteredRooms, pageable, filteredRooms.size());

    when(roomRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(roomPage);

    var response = roomService.getAllRooms(null, null, null, null, null, 200.0, null, null);

    assertNotNull(response);
    assertEquals(2, response.getContent().size());
    assertTrue(response.getContent().stream().allMatch(room -> room.getPricePerNight() <= 200.0));

    verify(roomRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void getAllRoomsShouldHandleRoomsWithImages() {
    var roomWithImages =
        Room.builder()
            .id(4L)
            .roomNumber("301")
            .roomType(Room.RoomType.DELUXE)
            .maxOccupancy(3)
            .pricePerNight(249.99)
            .size(40.0)
            .floor(3)
            .description("Deluxe room with ocean view")
            .status(Room.RoomStatus.AVAILABLE)
            .amenities(List.of("WiFi", "TV"))
            .images(
                List.of("https://example.com/room301-1.jpg", "https://example.com/room301-2.jpg"))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    var pageable = PageRequest.of(0, 20);
    var roomPage = new PageImpl<>(List.of(roomWithImages), pageable, 1);

    when(roomRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(roomPage);

    var response = roomService.getAllRooms(null, null, null, null, null, null, null, null);

    assertNotNull(response);
    assertEquals(1, response.getContent().size());

    var roomResponse = response.getContent().get(0);
    assertNotNull(roomResponse.getImages());
    assertEquals(2, roomResponse.getImages().size());
    assertEquals("https://example.com/room301-1.jpg", roomResponse.getImages().get(0).toString());
    assertEquals("https://example.com/room301-2.jpg", roomResponse.getImages().get(1).toString());

    verify(roomRepository).findAll(any(Specification.class), any(Pageable.class));
  }
}
