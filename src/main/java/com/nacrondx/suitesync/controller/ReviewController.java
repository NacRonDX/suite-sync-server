package com.nacrondx.suitesync.controller;

import com.nacrondx.suitesync.api.ReviewsApi;
import com.nacrondx.suitesync.model.review.CreateReviewRequest;
import com.nacrondx.suitesync.model.review.ReviewResponse;
import com.nacrondx.suitesync.service.ReviewService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReviewController implements ReviewsApi {
  private final ReviewService reviewService;

  @Override
  public ResponseEntity<List<ReviewResponse>> getReviewsByRoomId(Long roomId) {
    log.info("Received request to get reviews for room ID: {}", roomId);
    return ResponseEntity.ok(reviewService.getReviewsByRoomId(roomId));
  }

  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ReviewResponse> createReview(
      Long roomId, CreateReviewRequest createReviewRequest) {
    log.info("Received request to create review for room ID: {}", roomId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(reviewService.createReview(roomId, createReviewRequest));
  }
}
