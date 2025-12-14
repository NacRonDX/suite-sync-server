package com.nacrondx.suitesync.service;

import com.nacrondx.suitesync.entity.Review;
import com.nacrondx.suitesync.exception.ResourceNotFoundException;
import com.nacrondx.suitesync.model.review.CreateReviewRequest;
import com.nacrondx.suitesync.model.review.ReviewResponse;
import com.nacrondx.suitesync.repository.ReviewRepository;
import com.nacrondx.suitesync.repository.RoomRepository;
import com.nacrondx.suitesync.repository.UserRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
  private final ReviewRepository reviewRepository;
  private final RoomRepository roomRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public List<ReviewResponse> getReviewsByRoomId(Long roomId) {
    log.info("Fetching reviews for room ID: {}", roomId);

    var room =
        roomRepository
            .findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

    var reviews = reviewRepository.findByRoomIdOrderByCreatedAtDesc(roomId);

    log.info("Successfully fetched {} reviews for room ID: {}", reviews.size(), roomId);
    return reviews.stream().map(this::mapToReviewResponse).toList();
  }

  @Transactional
  public ReviewResponse createReview(Long roomId, CreateReviewRequest request) {
    log.info("Creating review for room ID: {}", roomId);

    var room =
        roomRepository
            .findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var jwt = (Jwt) authentication.getPrincipal();
    var userId = jwt.getClaim("userId");

    var user =
        userRepository
            .findById(((Number) userId).longValue())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

    var review =
        Review.builder()
            .room(room)
            .user(user)
            .rating(request.getRating())
            .comment(request.getComment())
            .build();

    var savedReview = reviewRepository.save(review);
    log.info("Successfully created review with ID: {}", savedReview.getId());
    return mapToReviewResponse(savedReview);
  }

  private ReviewResponse mapToReviewResponse(Review review) {
    var response = new ReviewResponse();
    response.setId(review.getId());
    response.setRoomId(review.getRoom().getId());
    response.setUserId(review.getUser().getId());
    response.setUserFirstName(review.getUser().getFirstName());
    response.setUserLastName(review.getUser().getLastName());
    response.setRating(review.getRating());
    response.setComment(review.getComment());
    response.setCreatedAt(OffsetDateTime.of(review.getCreatedAt(), ZoneOffset.UTC));
    response.setUpdatedAt(OffsetDateTime.of(review.getUpdatedAt(), ZoneOffset.UTC));
    return response;
  }
}
