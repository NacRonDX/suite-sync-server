package com.nacrondx.suitesync.repository;

import com.nacrondx.suitesync.entity.Room;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomSpecification {
  public static Specification<Room> withFilters(
      LocalDate checkInDate,
      LocalDate checkOutDate,
      Integer numberOfGuests,
      Room.RoomType roomType,
      Double minPrice,
      Double maxPrice) {
    return (root, query, criteriaBuilder) -> {
      var predicates = new ArrayList<Predicate>();

      if (roomType != null) {
        predicates.add(criteriaBuilder.equal(root.get("roomType"), roomType));
      }

      if (numberOfGuests != null) {
        predicates.add(
            criteriaBuilder.greaterThanOrEqualTo(root.get("maxOccupancy"), numberOfGuests));
      }

      if (minPrice != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("pricePerNight"), minPrice));
      }
      if (maxPrice != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("pricePerNight"), maxPrice));
      }

      predicates.add(
          criteriaBuilder.or(
              criteriaBuilder.equal(root.get("status"), Room.RoomStatus.AVAILABLE),
              criteriaBuilder.equal(root.get("status"), Room.RoomStatus.OCCUPIED)));

      // TODO: Add date-based availability check once Booking entity is available

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
