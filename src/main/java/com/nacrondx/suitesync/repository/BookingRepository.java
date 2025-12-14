package com.nacrondx.suitesync.repository;

import com.nacrondx.suitesync.entity.Booking;
import com.nacrondx.suitesync.entity.Booking.BookingStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
  Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

  @Query(
      "SELECT b FROM Booking b WHERE b.room.id = :roomId "
          + "AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT') "
          + "AND ((b.checkInDate <= :checkOutDate AND b.checkOutDate >= :checkInDate))")
  List<Booking> findOverlappingBookings(
      @Param("roomId") Long roomId,
      @Param("checkInDate") LocalDate checkInDate,
      @Param("checkOutDate") LocalDate checkOutDate);
}
