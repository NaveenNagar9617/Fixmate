package com.fixmate.service;

import com.fixmate.dto.rating.CreateRatingRequest;
import com.fixmate.dto.rating.RatingDto;
import com.fixmate.exception.DuplicateResourceException;
import com.fixmate.exception.ResourceNotFoundException;
import com.fixmate.exception.UnauthorizedActionException;
import com.fixmate.model.Complaint;
import com.fixmate.model.Rating;
import com.fixmate.model.StaffProfile;
import com.fixmate.model.User;
import com.fixmate.model.enums.ComplaintStatus;
import com.fixmate.repository.ComplaintRepository;
import com.fixmate.repository.RatingRepository;
import com.fixmate.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    private final RatingRepository ratingRepository;
    private final ComplaintRepository complaintRepository;
    private final StaffProfileRepository staffProfileRepository;

    @Transactional
    public RatingDto createRating(UUID complaintId, CreateRatingRequest request, User student) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", complaintId.toString()));

        if (complaint.getStatus() != ComplaintStatus.CLOSED) {
            throw new IllegalArgumentException("Can only rate closed complaints");
        }

        if (!complaint.getStudent().getId().equals(student.getId())) {
            throw new UnauthorizedActionException("You can only rate your own complaints");
        }

        if (ratingRepository.existsByComplaintId(complaintId)) {
            throw new DuplicateResourceException("This complaint has already been rated");
        }

        Rating rating = Rating.builder()
                .complaint(complaint)
                .student(student)
                .stars(request.getStars())
                .feedbackText(request.getFeedbackText())
                .build();
        rating = ratingRepository.save(rating);

        if (complaint.getAssignedStaff() != null) {
            recalculateStaffRating(complaint.getAssignedStaff().getId());
        }

        log.info("Rating created for complaint {} by student {}: {} stars",
                complaintId, student.getEmail(), request.getStars());

        return toDto(rating);
    }

    @Transactional(readOnly = true)
    public List<RatingDto> getRatingsForStaff(UUID staffId) {
        List<Rating> ratings = ratingRepository.findRatingsForStaff(staffId);
        return ratings.stream().map(this::toDto).collect(Collectors.toList());
    }

    private void recalculateStaffRating(UUID staffUserId) {
        Double avgRating = ratingRepository.avgRatingByStaffId(staffUserId).orElse(0.0);
        StaffProfile profile = staffProfileRepository.findByUserId(staffUserId).orElse(null);
        if (profile != null) {
            profile.setAvgRating(Math.round(avgRating * 100.0) / 100.0);
            staffProfileRepository.save(profile);
            log.debug("Staff {} avg rating updated to {}", staffUserId, avgRating);
        }
    }

    private RatingDto toDto(Rating rating) {
        return new RatingDto(
                rating.getId(),
                rating.getComplaint().getId(),
                rating.getComplaint().getTitle(),
                rating.getStudent().getId(),
                rating.getStudent().getName(),
                rating.getStars(),
                rating.getFeedbackText(),
                rating.getCreatedAt()
        );
    }
}
