package com.fixmate.mapper;

import com.fixmate.dto.complaint.ComplaintDetailDto;
import com.fixmate.dto.complaint.ComplaintListDto;
import com.fixmate.model.Complaint;
import com.fixmate.model.ComplaintPhoto;
import com.fixmate.model.ComplaintTimeline;
import com.fixmate.model.enums.ComplaintStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Mapper(componentModel = "spring", uses = {UserMapper.class, CommentMapper.class})
public interface ComplaintMapper {

    @Mapping(target = "studentName", source = "student.name")
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "assignedStaffName", source = "assignedStaff.name")
    @Mapping(target = "assignedStaffId", source = "assignedStaff.id")
    @Mapping(target = "slaStatus", expression = "java(calculateSlaStatus(complaint))")
    ComplaintListDto toListDto(Complaint complaint);

    @Mapping(target = "student", source = "student")
    @Mapping(target = "assignedStaff", source = "assignedStaff")
    @Mapping(target = "parentComplaintId", source = "parentComplaint.id")
    @Mapping(target = "slaStatus", expression = "java(calculateSlaStatus(complaint))")
    @Mapping(target = "photos", source = "photos")
    @Mapping(target = "timeline", source = "timeline")
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "hasRating", ignore = true)
    @Mapping(target = "userHasUpvoted", ignore = true)
    ComplaintDetailDto toDetailDto(Complaint complaint);

    @Mapping(target = "uploadedByName", source = "uploadedBy.name")
    @Mapping(target = "photoType", expression = "java(photo.getPhotoType().name())")
    ComplaintDetailDto.PhotoDto toPhotoDto(ComplaintPhoto photo);

    @Mapping(target = "actorName", source = "actor.name")
    @Mapping(target = "oldStatus", expression = "java(timeline.getOldStatus() != null ? timeline.getOldStatus().name() : null)")
    @Mapping(target = "newStatus", expression = "java(timeline.getNewStatus() != null ? timeline.getNewStatus().name() : null)")
    ComplaintDetailDto.TimelineDto toTimelineDto(ComplaintTimeline timeline);

    @Named("calculateSlaStatus")
    default String calculateSlaStatus(Complaint complaint) {
        if (complaint.getSlaDeadline() == null) {
            return "NO_SLA";
        }
        if (complaint.getStatus() == ComplaintStatus.RESOLVED ||
                complaint.getStatus() == ComplaintStatus.CLOSED) {
            return "COMPLETED";
        }

        LocalDateTime now = LocalDateTime.now();
        long hoursRemaining = ChronoUnit.HOURS.between(now, complaint.getSlaDeadline());

        if (now.isAfter(complaint.getSlaDeadline())) {
            long overdueHours = ChronoUnit.HOURS.between(complaint.getSlaDeadline(), now);
            return "OVERDUE_" + overdueHours + "h";
        }

        long totalSlaHours = complaint.getPriority().getSlaHours();
        double percentRemaining = (double) hoursRemaining / totalSlaHours * 100;

        if (percentRemaining > 50) {
            return "ON_TRACK";
        } else if (percentRemaining > 0) {
            return "AT_RISK";
        }
        return "OVERDUE";
    }
}
