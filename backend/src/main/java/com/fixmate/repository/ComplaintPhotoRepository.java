package com.fixmate.repository;

import com.fixmate.model.ComplaintPhoto;
import com.fixmate.model.enums.PhotoType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplaintPhotoRepository extends JpaRepository<ComplaintPhoto, UUID> {

    List<ComplaintPhoto> findByComplaintId(UUID complaintId);

    Optional<ComplaintPhoto> findByComplaintIdAndPhotoType(UUID complaintId, PhotoType photoType);

    boolean existsByComplaintIdAndPhotoType(UUID complaintId, PhotoType photoType);
}
